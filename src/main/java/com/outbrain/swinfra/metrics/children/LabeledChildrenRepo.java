package com.outbrain.swinfra.metrics.children;

import java.util.Arrays;
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

  private final ConcurrentMap<StringsKey, MetricData<T>> children = new ConcurrentHashMap<>();
  private final Function<StringsKey, MetricData<T>> mappingFunction;

  public LabeledChildrenRepo(final Function<List<String>, MetricData<T>> mappingFunction) {
    this.mappingFunction = key -> mappingFunction.apply(asList(key.labelValues));
  }

  @Override
  public T metricForLabels(final List<String> labelValues) {
    return metricForLabels(labelValues.toArray(new String[0]));
  }

  @Override
  public T metricForLabels(final String... labelValues) {
    final StringsKey stringsKey = new StringsKey(labelValues);
    final MetricData<T> metricData = children.get(stringsKey);
    // We use get and fallback to computeIfAbsent to eliminate contention
    // in case the key is present.
    // See https://bugs.openjdk.java.net/browse/JDK-8161372 for details.
    if (metricData == null) {
      return children.computeIfAbsent(stringsKey, mappingFunction).getMetric();
    } else {
      return metricData.getMetric();
    }
  }

  @Override
  public void forEachMetricData(final Consumer<MetricData<T>> consumer) {
    for (final MetricData<T> metricData : children.values()) {
      consumer.accept(metricData);
    }
  }

  // This proved to be faster than using Arrays.asList as the key
  private static class StringsKey {
    private final String[] labelValues;

    private StringsKey(final String[] labelValues) {
      this.labelValues = labelValues;
    }

    @Override
    public boolean equals(final Object obj) {
      return Arrays.equals(labelValues, ((StringsKey) obj).labelValues);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(labelValues);
    }
  }

}
