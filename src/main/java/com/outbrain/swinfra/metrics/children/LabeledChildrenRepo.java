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
    final MetricData<T> data = children.get(labelValues);
    return (data == null ? children.computeIfAbsent(labelValues, mappingFunction) : data).getMetric();
  }

  @Override
  public void forEachMetricData(final Consumer<MetricData<T>> consumer) {
    for (MetricData<T> metricData : children.values()) {
      consumer.accept(metricData);
    }
  }
}
