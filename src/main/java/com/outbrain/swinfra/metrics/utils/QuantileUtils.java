package com.outbrain.swinfra.metrics.utils;

import com.codahale.metrics.Counting;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Sampling;
import com.codahale.metrics.Snapshot;
import com.outbrain.swinfra.metrics.children.MetricData;
import com.outbrain.swinfra.metrics.samples.SampleCreator;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;

import java.util.Arrays;
import java.util.List;

import static com.outbrain.swinfra.metrics.timing.TimingMetric.COUNT_SUFFIX;
import static com.outbrain.swinfra.metrics.timing.TimingMetric.SUM_SUFFIX;
import static com.outbrain.swinfra.metrics.utils.LabelUtils.addLabelToList;

public class QuantileUtils {

  private static final String QUANTILE_LABEL = "quantile";

  /**
   * Extract Prometheus {@link Sample} objects from a DropWizard metric that contains a {@link Snapshot} object - these
   * will be metrics implementing the {@link Sampling} interface
   *
   * @param metricData    the metric data object
   * @param name          the metric name
   * @param labelNames    the label names to add to all samples
   * @param sampleCreator the sample creator
   * @param <T>           the type of the DropWizard metric
   */
  public static <T extends Counting & Sampling & Metric> List<Sample> createSamplesFromSnapshot(final MetricData<T> metricData,
                                                                                                final String name,
                                                                                                final List<String> labelNames,
                                                                                                final SampleCreator sampleCreator) {
    final Snapshot snapshot = metricData.getMetric().getSnapshot();
    final List<String> labelValues = metricData.getLabelValues();

    final List<String> labels = addLabelToList(labelNames, QUANTILE_LABEL);

    long sum = 0;
    for (final long value : snapshot.getValues()) {
      sum += value;
    }

    return Arrays.asList(
      sampleCreator.createSample(name, labels, addLabelToList(labelValues, "0.5"), snapshot.getMedian()),
      sampleCreator.createSample(name, labels, addLabelToList(labelValues, "0.75"), snapshot.get75thPercentile()),
      sampleCreator.createSample(name, labels, addLabelToList(labelValues, "0.95"), snapshot.get95thPercentile()),
      sampleCreator.createSample(name, labels, addLabelToList(labelValues, "0.98"), snapshot.get98thPercentile()),
      sampleCreator.createSample(name, labels, addLabelToList(labelValues, "0.99"), snapshot.get99thPercentile()),
      sampleCreator.createSample(name, labels, addLabelToList(labelValues, "0.999"), snapshot.get999thPercentile()),
      sampleCreator.createSample(name + COUNT_SUFFIX, labelNames, labelValues, metricData.getMetric().getCount()),
      sampleCreator.createSample(name + SUM_SUFFIX, labelNames, labelValues, sum)
    );
  }

}
