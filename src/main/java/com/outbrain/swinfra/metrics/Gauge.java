package com.outbrain.swinfra.metrics;

import com.outbrain.swinfra.metrics.children.ChildMetricRepo;
import com.outbrain.swinfra.metrics.children.LabeledChildrenRepo;
import com.outbrain.swinfra.metrics.children.MetricData;
import com.outbrain.swinfra.metrics.children.UnlabeledChildRepo;
import com.outbrain.swinfra.metrics.data.MetricDataConsumer;
import com.outbrain.swinfra.metrics.utils.MetricType;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;

import static com.outbrain.swinfra.metrics.utils.MetricType.GAUGE;
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

  private final Map<List<String>, DoubleSupplier> valueSuppliers;

  private Gauge(final String name,
                final String help,
                final String[] labelNames,
                final Map<List<String>, DoubleSupplier> valueSuppliers) {
    super(name, help, labelNames);
    this.valueSuppliers = valueSuppliers;
  }

  public double getValue(final String... labelValues) {
    return metricForLabels(labelValues).getAsDouble();
  }

  @Override
  public void forEachMetricData(final MetricDataConsumer consumer) {
    forEachChild(metricData -> {
      final double value = metricData.getMetric().getAsDouble();
      consumer.consumeGauge(this, metricData.getLabelValues(), value);
    });
  }

  @Override
  ChildMetricRepo<DoubleSupplier> createChildMetricRepo() {
    if (valueSuppliers.size() == 1 && getLabelNames().isEmpty()) {
      final DoubleSupplier supplier = valueSuppliers.values().iterator().next();
      return new UnlabeledChildRepo<>(getName(), new MetricData<>(supplier));
    } else {
      final ChildMetricRepo<DoubleSupplier> result = new LabeledChildrenRepo<>(
              labelValues ->
                      new MetricData<>(
                              valueSuppliers.get(labelValues),
                              labelValues), labelValues -> {
      });

      valueSuppliers.keySet().forEach(result::metricForLabels);
      return result;
    }
  }

  @Override
  public MetricType getType() {
    return GAUGE;
  }

  public interface GaugeValueSuppliersBuilder {

    GaugeValueSuppliersBuilder withValueSupplier(final DoubleSupplier valueSupplier, final String... labelValues);

    Gauge build();
  }

  public static class GaugeBuilder extends AbstractMetricBuilder<Gauge, GaugeBuilder> implements GaugeValueSuppliersBuilder {

    private final Map<List<String>, DoubleSupplier> valueSuppliers = new HashMap<>();

    public GaugeBuilder(final String name, final String help) {
      super(name, help);
    }

    @Override
    void validateParams() {
      Validate.notEmpty(valueSuppliers, "At least one value supplier must be defined");
      super.validateParams();
    }

    /**
     * @see Gauge for more information on what value suppliers are and how they relate to label values
     */
    public GaugeValueSuppliersBuilder withValueSupplier(final DoubleSupplier valueSupplier,
                                                        final String... labelValues) {
      validateValueSupplier(valueSupplier);
      validateValueSupplierLabels(labelNames.length, labelValues);
      valueSuppliers.put(Arrays.asList(labelValues), valueSupplier);
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
