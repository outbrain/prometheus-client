package com.outbrain.swinfra.metrics.children;

import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * A child metric container implementation for metrics that do not have labels.
 * Such metrics by definition will have only one child.
 */
public class UnlabeledChildRepo<T> implements ChildMetricRepo<T> {

  private final MetricData<T> metricData;
  private final String metricName;

  public UnlabeledChildRepo(final String metricName, final MetricData<T> metricData) {
    this.metricName = metricName;
    this.metricData = metricData;
  }

  @Override
  public T metricForLabels(final String... labelValues) {
    Validate.isTrue(labelValues.length == 0, "%s has no labels but got %s", metricName, Arrays.toString(labelValues));
    return metricData.getMetric();
  }

  @Override
  public T metricForLabels(final List<String> labelValues) {
    Validate.isTrue(labelValues.isEmpty(), "%s has no labels but got %s", metricName, labelValues);
    return metricData.getMetric();
  }

  @Override
  public void forEachMetricData(final Consumer<MetricData<T>> consumer) {
    consumer.accept(metricData);
  }
}
