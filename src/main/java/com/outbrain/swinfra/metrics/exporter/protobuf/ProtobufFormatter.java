package com.outbrain.swinfra.metrics.exporter.protobuf;

import com.outbrain.swinfra.metrics.Metric;
import com.outbrain.swinfra.metrics.MetricRegistry;
import com.outbrain.swinfra.metrics.data.HistogramData;
import com.outbrain.swinfra.metrics.data.MetricDataConsumer;
import com.outbrain.swinfra.metrics.data.SummaryData;
import com.outbrain.swinfra.metrics.exporter.MetricExporter;
import io.prometheus.client.Metrics;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Protocol buffers formatter
 *
 * @see <a href="https://github.com/prometheus/client_model/blob/master/metrics.proto">metrics.proto</a>
 */
public class ProtobufFormatter implements MetricExporter {

  public static final String CONTENT_TYPE_PROTOBUF = "application/vnd.google.protobuf; " +
    "proto=io.prometheus.client.MetricFamily; " +
    "encoding=delimited";

  private final Collection<MetricRegistry> registries;

  public ProtobufFormatter(final Collection<MetricRegistry> registries) {
    this.registries = registries;
  }

  @Override
  public void exportTo(final OutputStream stream) throws IOException {
    for (final MetricRegistry registry : registries) {
      final ProtobufMetricDataConsumer consumer = new ProtobufMetricDataConsumer(registry.getStaticLabels(), stream);
      for (final Metric metric : registry) {
        consumer.consumeMetric(metric);
      }
    }
  }

  private static class ProtobufMetricDataConsumer implements MetricDataConsumer {

    private final Map<String, String> staticLabels;
    private final OutputStream stream;
    private Metrics.MetricFamily.Builder familyBuilder;
    private Metrics.Metric.Builder metricBuilder;

    private ProtobufMetricDataConsumer(final Map<String, String> staticLabels, final OutputStream stream) {
      this.staticLabels = staticLabels;
      this.stream = stream;
    }

    private void consumeMetric(final Metric metric) throws IOException {
      familyBuilder = createMetricFamily(metric);
      metricBuilder = Metrics.Metric.newBuilder();
      // registering all static labelValues
      staticLabels.forEach((n, v) ->
          metricBuilder.addLabel(createLabel(n, v)));

      // build metric family
      metric.forEachMetricData(this);

      // flush metric family into output stream
      familyBuilder.build().writeDelimitedTo(stream);
    }


    @Override
    public void consumeCounter(final Metric metric, final List<String> labelValues, final double value) {
      familyBuilder.setType(Metrics.MetricType.COUNTER);
      final Metrics.Counter counter = Metrics.Counter.newBuilder().setValue(value).build();
      addMetric(() -> metricBuilder.setCounter(counter).build(), metric.getLabelNames(), labelValues);

    }

    @Override
    public void consumeGauge(final Metric metric, final List<String> labelValues, final double value) {
      familyBuilder.setType(Metrics.MetricType.GAUGE);
      final Metrics.Gauge gauge = Metrics.Gauge.newBuilder().setValue(value).build();
      addMetric(() -> metricBuilder.setGauge(gauge).build(), metric.getLabelNames(), labelValues);
    }

    @Override
    public void consumeSummary(final Metric metric, final List<String> labelValues, final SummaryData data) {
      familyBuilder.setType(Metrics.MetricType.SUMMARY);
      final Metrics.Summary.Builder summaryBuilder = Metrics.Summary.newBuilder().
        setSampleCount(data.getCount()).setSampleSum(data.getSum());
      summaryBuilder.addQuantile(Metrics.Quantile.newBuilder().setQuantile(0.5d).setValue(data.getMedian()));
      summaryBuilder.addQuantile(Metrics.Quantile.newBuilder().setQuantile(0.75d).setValue(data.get75thPercentile()));
      summaryBuilder.addQuantile(Metrics.Quantile.newBuilder().setQuantile(0.95d).setValue(data.get95thPercentile()));
      summaryBuilder.addQuantile(Metrics.Quantile.newBuilder().setQuantile(0.98d).setValue(data.get98thPercentile()));
      summaryBuilder.addQuantile(Metrics.Quantile.newBuilder().setQuantile(0.99d).setValue(data.get99thPercentile()));
      summaryBuilder.addQuantile(Metrics.Quantile.newBuilder().setQuantile(0.999d).setValue(data.get999thPercentile()));
      addMetric(() -> metricBuilder.setSummary(summaryBuilder).build(), metric.getLabelNames(), labelValues);
    }

    @Override
    public void consumeHistogram(final Metric metric, final List<String> labelValues, final HistogramData data) {
      familyBuilder.setType(Metrics.MetricType.HISTOGRAM);
      final Metrics.Histogram.Builder histogramBuilder = Metrics.Histogram.newBuilder().
          setSampleCount(data.getCount()).setSampleSum(data.getSum());
      data.consumeBuckets((upperBound, count) -> {
        histogramBuilder.addBucket(Metrics.Bucket.newBuilder().setCumulativeCount(count).setUpperBound(upperBound));
      });

      addMetric(() -> metricBuilder.setHistogram(histogramBuilder).build(), metric.getLabelNames(), labelValues);
    }

    private static Metrics.MetricFamily.Builder createMetricFamily(final Metric metric) {
      return Metrics.MetricFamily.newBuilder().
          setName(metric.getName()).
          setHelp(metric.getHelp());
    }
    private void addMetric(final Supplier<Metrics.Metric> metricSupplier,
                               final List<String> labelNames,
                               final List<String> labelValues)  {
      // add labels to the builder
      final int previousCount = addLabels(metricBuilder, labelNames, labelValues);

      // create the metric object
      final Metrics.Metric metric = metricSupplier.get();

      // reset builders to be reused
      resetMetricBuilderLabels(metricBuilder, labelValues, previousCount);

      familyBuilder.addMetric(metric);
    }

    private static int addLabels(final Metrics.Metric.Builder metricBuilder, final List<String> labelNames, final List<String> labelValues) {
      final int labelsCount = metricBuilder.getLabelCount();
      for (int i = 0; i < labelValues.size(); i++) {
        metricBuilder.addLabel(createLabel(labelNames.get(i), labelValues.get(i)));
      }
      return labelsCount;
    }

    private static void resetMetricBuilderLabels(final Metrics.Metric.Builder metricBuilder, final List<String> labelValues, final int currentCount) {
      for (int i = labelValues.size() - 1; i >= 0; i--) {
        metricBuilder.removeLabel(currentCount + i);
      }
    }

    private static Metrics.LabelPair createLabel(final String n, final String v) {
      return Metrics.LabelPair.newBuilder().
          setName(n).
          setValue(v).
          build();
    }
  }
}