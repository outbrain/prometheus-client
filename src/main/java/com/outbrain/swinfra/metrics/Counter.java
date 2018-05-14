package com.outbrain.swinfra.metrics;


import com.outbrain.swinfra.metrics.data.MetricDataConsumer;
import com.outbrain.swinfra.metrics.utils.MetricType;

import java.util.concurrent.atomic.LongAdder;

import static com.outbrain.swinfra.metrics.utils.MetricType.COUNTER;

/**
 * An implementation of a Counter metric. A counter is a whole number that can only increase its value.
 * <p>
 * The counter exposes a single time-series with its value and labels.
 * </p>
 *
 * @see <a href="https://prometheus.io/docs/concepts/metric_types/#counter">Prometheus counter metric</a>
 */
public class Counter extends AbstractMetric<LongAdder> {

  private Counter(final String name, final String help, final String[] labelNames) {
    super(name, help, labelNames);
  }

  public void inc(final String... labelValues) {
    inc(1, labelValues);
  }

  public void inc(final long n, final String... labelValues) {
    metricForLabels(labelValues).add(n);
  }

  public long getValue(final String... labelValues) {
    return metricForLabels(labelValues).longValue();
  }

  @Override
  LongAdder createMetric() {
    return new LongAdder();
  }

  @Override
  public MetricType getType() {
    return COUNTER;
  }

  @Override
  public void forEachMetricData(final MetricDataConsumer consumer) {
    forEachChild(metricData -> {
      final long value = metricData.getMetric().longValue();
      consumer.consumeCounter(this, metricData.getLabelValues(), value);
    });
  }

  public static class CounterBuilder extends AbstractMetricBuilder<Counter, CounterBuilder> {

    public CounterBuilder(final String name, final String help) {
      super(name, help);
    }

    @Override
    protected Counter create(final String fullName, final String help, final String[] labelNames) {
      return new Counter(fullName, help, labelNames);
    }
  }

}
