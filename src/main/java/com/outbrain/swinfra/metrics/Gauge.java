package com.outbrain.swinfra.metrics;

import com.codahale.metrics.CachedGauge;
import com.outbrain.swinfra.metrics.children.ChildMetricRepo;
import com.outbrain.swinfra.metrics.children.LabeledChildrenRepo;
import com.outbrain.swinfra.metrics.children.MetricData;
import com.outbrain.swinfra.metrics.children.UnlabeledChildRepo;
import com.outbrain.swinfra.metrics.samples.SampleCreator;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;

import static com.outbrain.swinfra.metrics.utils.LabelUtils.commaDelimitedStringToLabels;
import static com.outbrain.swinfra.metrics.utils.LabelUtils.labelsToCommaDelimitedString;
import static io.prometheus.client.Collector.Type.GAUGE;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * An implementation of a Gauge metric. A gauge is a decimal value that can increase or decrease.
 * <p>
 * The gauge exposes a single time-series with its value and labels.
 * </p>
 *
 * @see <a href="https://prometheus.io/docs/concepts/metric_types/#gauge">Prometheus gauge metric</a>
 */
public class Gauge extends AbstractMetric<CachedGauge<Double>> {

  private final Map<String, MetricData<CachedGauge<Double>>> valueSuppliers;

  private Gauge(final String name,
                final String help,
                final String[] labelNames,
                final Map<String[], DoubleSupplier> valueSuppliers) {
    super(name, help, labelNames);
    this.valueSuppliers = convertToMetricData(valueSuppliers);
  }

  private CachedGauge<Double> createGauge(final DoubleSupplier valueSupplier) {
    return new CachedGauge<Double>(10, TimeUnit.SECONDS) {
      @Override
      protected Double loadValue() {
        return valueSupplier.getAsDouble();
      }
    };
  }

  @Override
  public void forEachSample(final SampleConsumer sampleConsumer) throws IOException {
    for (final MetricData<CachedGauge<Double>> metricData : allMetricData()) {
      sampleConsumer.apply(getName(), metricData.getMetric().getValue(), metricData.getLabelValues(), null, null);
    }
  }

  @Override
  List<Sample> createSamples(final MetricData<CachedGauge<Double>> metricData, final SampleCreator sampleCreator) {
    return singletonList(sampleCreator.createSample(getName(),
                                                    getLabelNames(),
                                                    metricData.getLabelValues(),
                                                    metricData.getMetric().getValue()
    ));
  }

  @Override
  ChildMetricRepo<CachedGauge<Double>> createChildMetricRepo() {
    if (valueSuppliers.size() == 1) {
      final CachedGauge<Double> gauge = valueSuppliers.values().iterator().next().getMetric();
      return new UnlabeledChildRepo<>(new MetricData<>(gauge, new String[]{}));
    } else {
      final ChildMetricRepo<CachedGauge<Double>> result = new LabeledChildrenRepo<>(valueSuppliers::get);
      valueSuppliers.keySet().forEach(metricLabels -> {
        final String[] labelValues = commaDelimitedStringToLabels(metricLabels);
        result.metricForLabels(labelValues);
      });
      return result;
    }
  }

  private Map<String, MetricData<CachedGauge<Double>>> convertToMetricData(final Map<String[], DoubleSupplier> valueSuppliers) {
    final Map<String, MetricData<CachedGauge<Double>>> metricData = new HashMap<>(valueSuppliers.size());
    valueSuppliers.entrySet()
                  .forEach(entry -> metricData.put(
                      labelsToCommaDelimitedString(entry.getKey()),
                      toMetricData(entry.getValue(), entry.getKey())));
    return metricData;
  }

  private MetricData<CachedGauge<Double>> toMetricData(final DoubleSupplier valueSupplier,
                                                       final String[] labelValues) {
    final CachedGauge<Double> gauge = createGauge(valueSupplier);
    return new MetricData<>(gauge, labelValues);
  }

  @Override
  public Collector.Type getType() {
    return GAUGE;
  }

  public static class GaugeBuilder extends AbstractMetricBuilder<Gauge, GaugeBuilder> {

    private final Map<String[], DoubleSupplier> valueSuppliers = new HashMap<>();

    public GaugeBuilder(final String name, final String help) {
      super(name, help);
    }

    /**
     * @see Gauge for more information on what value suppliers are and how they relate to label values
     */
    public GaugeBuilder withValueSupplier(final DoubleSupplier valueSupplier, final String... labelValues) {
      valueSuppliers.put(labelValues, valueSupplier);
      return this;
    }

    @Override
    protected Gauge create(final String fullName, final String help, final String[] labelNames) {
      return new Gauge(fullName, help, labelNames, valueSuppliers);
    }

    @Override
    void validateParams() {
      super.validateParams();
      validateValueSuppliers();
      validateValueSuppliersLabels();
    }

    private void validateValueSuppliers() {
      valueSuppliers.values()
                    .forEach(valueSupplier -> requireNonNull(valueSupplier, "Null value suppliers are not allowed"));
    }

    private void validateValueSuppliersLabels() {
      final int numOfLabels = labelNames.length;
      valueSuppliers.keySet()
                    .forEach(labelValues -> Validate.isTrue(
                        labelValues.length == numOfLabels,
                        "Labels %s does not contain the expected amount %s",
                        Arrays.toString(labelValues),
                        numOfLabels));
    }
  }
}
