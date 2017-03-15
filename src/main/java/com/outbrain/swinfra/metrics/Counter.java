package com.outbrain.swinfra.metrics;


import com.outbrain.swinfra.metrics.children.ChildMetricRepo;
import com.outbrain.swinfra.metrics.children.LabeledChildrenRepo;
import com.outbrain.swinfra.metrics.children.MetricData;
import com.outbrain.swinfra.metrics.children.UnlabeledChildRepo;
import com.outbrain.swinfra.metrics.samples.SampleCreator;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;

import java.io.IOException;
import java.util.List;

import static com.outbrain.swinfra.metrics.utils.LabelUtils.commaDelimitedStringToLabels;
import static io.prometheus.client.Collector.Type.COUNTER;
import static java.util.Collections.singletonList;

/**
 * An implementation of a Counter metric. A counter is a whole number that can only increase its value.
 * <p>
 * The counter exposes a single time-series with its value and labels.
 * </p>
 *
 * @see <a href="https://prometheus.io/docs/concepts/metric_types/#counter">Prometheus counter metric</a>
 */
public class Counter extends AbstractMetric<com.codahale.metrics.Counter> {

  private Counter(final String name, final String help, final String[] labelNames) {
    super(name, help, labelNames);
  }

  public void inc(final String... labelValues) {
    validateLabelValues(labelValues);
    inc(1, labelValues);
  }

  public void inc(final long n, final String... labelValues) {
    validateLabelValues(labelValues);
    metricForLabels(labelValues).inc(n);
  }

  long getValue(final String... labelValues) {
    return metricForLabels(labelValues).getCount();
  }

  @Override
  ChildMetricRepo<com.codahale.metrics.Counter> createChildMetricRepo() {
    if (getLabelNames().size() == 0) {
      return new UnlabeledChildRepo<>(new MetricData<>(new com.codahale.metrics.Counter(), new String[]{}));
    } else {
      return new LabeledChildrenRepo<>(commaDelimitedLabelValues -> {
        final String[] labelValues = commaDelimitedStringToLabels(commaDelimitedLabelValues);
        return new MetricData<>(new com.codahale.metrics.Counter(), labelValues);
      });
    }
  }

  @Override
  public Collector.Type getType() {
    return COUNTER;
  }

  @Override
  public void forEachSample(final SampleConsumer sampleConsumer) throws IOException {
    for (final MetricData<com.codahale.metrics.Counter> metricData : allMetricData()) {
      sampleConsumer.apply(getName(), metricData.getMetric().getCount(), metricData.getLabelValues(), null, null);
    }
  }

  @Override
  List<Sample> createSamples(final MetricData<com.codahale.metrics.Counter> metricData,
                             final SampleCreator sampleCreator) {
    return singletonList(sampleCreator.createSample(getName(),
                                                    getLabelNames(),
                                                    metricData.getLabelValues(),
                                                    metricData.getMetric().getCount()
    ));
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
