package com.outbrain.swinfra.metrics;

import com.outbrain.swinfra.metrics.children.ChildMetricRepo;
import com.outbrain.swinfra.metrics.children.LabeledChildrenRepo;
import com.outbrain.swinfra.metrics.children.MetricData;
import com.outbrain.swinfra.metrics.children.UnlabeledChildRepo;
import com.outbrain.swinfra.metrics.samples.SampleCreator;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * <p>
 * This gauge uses a {@link DoubleSupplier} that provides a value every time this metric is sampled.
 * </p>
 *
 * @see <a href="https://prometheus.io/docs/concepts/metric_types/#gauge">Prometheus gauge metric</a>
 */
public class Gauge extends AbstractMetric<DoubleSupplier> {

  private final Map<String, MetricData<DoubleSupplier>> valueSuppliers;

  private Gauge(final String name,
                final String help,
                final String[] labelNames,
                final Map<String[], DoubleSupplier> valueSuppliers) {
    super(name, help, labelNames);
    this.valueSuppliers = convertToMetricData(valueSuppliers);
  }

  public double getValue(final String... labelValues) {
    return metricForLabels(labelValues).getAsDouble();
  }

  @Override
  List<Sample> createSamples(final MetricData<DoubleSupplier> metricData, final SampleCreator sampleCreator) {
    return singletonList(sampleCreator.createSample(getName(),
                                                    getLabelNames(),
                                                    metricData.getLabelValues(),
                                                    metricData.getMetric().getAsDouble()
    ));
  }

  @Override
  ChildMetricRepo<DoubleSupplier> createChildMetricRepo() {
    if (valueSuppliers.size() == 1 && getLabelNames().size() == 0) {
      final DoubleSupplier gauge = valueSuppliers.values().iterator().next().getMetric();
      return new UnlabeledChildRepo<>(new MetricData<>(gauge));
    } else {
      final ChildMetricRepo<DoubleSupplier> result = new LabeledChildrenRepo<>(valueSuppliers::get);
      valueSuppliers.keySet().forEach(metricLabels -> {
        final String[] labelValues = commaDelimitedStringToLabels(metricLabels);
        result.metricForLabels(labelValues);
      });
      return result;
    }
  }

  private Map<String, MetricData<DoubleSupplier>> convertToMetricData(final Map<String[], DoubleSupplier> valueSuppliers) {
    final Map<String, MetricData<DoubleSupplier>> metricData = new HashMap<>(valueSuppliers.size());
    valueSuppliers.forEach((labelValues, valueSupplier) -> metricData.put(
                      labelsToCommaDelimitedString(labelValues),
                      toMetricData(valueSupplier, labelValues)));
    return metricData;
  }

  private MetricData<DoubleSupplier> toMetricData(final DoubleSupplier valueSupplier,
                                                       final String[] labelValues) {
    return new MetricData<>(valueSupplier, labelValues);
  }

  @Override
  public Collector.Type getType() {
    return GAUGE;
  }

  public interface GaugeValueSuppliersBuilder {

    GaugeValueSuppliersBuilder withValueSupplier(final DoubleSupplier valueSupplier, final String... labelValues);
    Gauge build();
  }

  public static class GaugeBuilder extends AbstractMetricBuilder<Gauge, GaugeBuilder> implements GaugeValueSuppliersBuilder {

    private final Map<String[], DoubleSupplier> valueSuppliers = new HashMap<>();

    public GaugeBuilder(final String name, final String help) {
      super(name, help);
    }

    /**
     * @see Gauge for more information on what value suppliers are and how they relate to label values
     */
    public GaugeValueSuppliersBuilder withValueSupplier(final DoubleSupplier valueSupplier, final String... labelValues) {
      validateValueSupplier(valueSupplier);
      validateValueSupplierLabels(labelNames.length, labelValues);
      valueSuppliers.put(labelValues, valueSupplier);
      return this;
    }

    @Override
    protected Gauge create(final String fullName, final String help, final String[] labelNames) {
      return new Gauge(fullName, help, labelNames, valueSuppliers);
    }

    private void validateValueSupplier(final DoubleSupplier valueSupplier) {
      requireNonNull(valueSupplier, "Null value suppliers are not allowed");
    }

    private void validateValueSupplierLabels(final int numOfLabels, final String[] labelValues) {
      Validate.isTrue(
              labelValues.length == numOfLabels,
              "Labels %s does not contain the expected amount %s",
              Arrays.toString(labelValues),
              numOfLabels);
    }
  }
}
