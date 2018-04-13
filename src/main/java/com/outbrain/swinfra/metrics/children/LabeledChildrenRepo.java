package com.outbrain.swinfra.metrics.children;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Arrays.asList;

/**
 * A child metric container implementation for metrics that have labels.
 * Such metrics can expect multiple children.
 */
public class LabeledChildrenRepo<T> implements ChildMetricRepo<T> {

  private final ConcurrentMap<List<String>, MetricData<T>> children = new ConcurrentHashMap<>();
  private final Function<List<String>, MetricData<T>> mappingFunction;

  public LabeledChildrenRepo(final Function<List<String>, MetricData<T>> mappingFunction) {
    this.mappingFunction = mappingFunction;
  }

  @Override
  public T metricForLabels(final String... labelValues) {
    final List<String> metricId = asList(labelValues);
    return metricForLabels(metricId);
  }

  @Override
  public T metricForLabels(List<String> labelValues) {
    final MetricData<T> metricData = children.get(labelValues);
    // We use get and fallback to computeIfAbsent to eliminate contention
    // in case the key is present.
    // See https://bugs.openjdk.java.net/browse/JDK-8161372 for details.
    if (metricData == null) {
      return children.computeIfAbsent(labelValues, mappingFunction).getMetric();
    } else {
      return metricData.getMetric();
    }
  }

  @Override
  public void forEachMetricData(final Consumer<MetricData<T>> consumer) {
    for (MetricData<T> metricData : children.values()) {
      consumer.accept(metricData);
    }
  }
}
