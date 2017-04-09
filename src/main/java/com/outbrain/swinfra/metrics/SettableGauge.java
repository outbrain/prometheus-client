package com.outbrain.swinfra.metrics;

import com.outbrain.swinfra.metrics.SettableGauge.SettableDoubleSupplier;
import com.outbrain.swinfra.metrics.children.ChildMetricRepo;
import com.outbrain.swinfra.metrics.children.LabeledChildrenRepo;
import com.outbrain.swinfra.metrics.children.MetricData;
import com.outbrain.swinfra.metrics.children.UnlabeledChildRepo;
import com.outbrain.swinfra.metrics.samples.SampleCreator;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleSupplier;

import static com.outbrain.swinfra.metrics.utils.LabelUtils.commaDelimitedStringToLabels;
import static io.prometheus.client.Collector.Type.GAUGE;
import static java.util.Collections.singletonList;

/**
 * An implementation of a Gauge metric. A gauge is a decimal value that can increase or decrease.
 * <p>
 * The gauge exposes a single time-series with its value and labels.
 * </p>
 * <p>
 * The SettableGauge is meant to be set from the outside as opposed to the regular {@link Gauge}. This is an important
 * distinction because consecutive calls to the method <i>set</i> will not all show when sampling this metric, rather
 * the last value set will show.
 * </p>
 * <p>
 * So the following calls:
 * </p>
 * @see <a href="https://prometheus.io/docs/concepts/metric_types/#gauge">Prometheus gauge metric</a>
 */
public class SettableGauge extends AbstractMetric<SettableDoubleSupplier> {

  SettableGauge(final String name, final String help, final String[] labelNames) {
    super(name, help, labelNames);
  }

  @Override
  public Collector.Type getType() {
    return GAUGE;
  }

  public double getValue(final String... labelValues) {
    return metricForLabels(labelValues).getAsDouble();
  }

  @Override
  ChildMetricRepo<SettableDoubleSupplier> createChildMetricRepo() {
    if (getLabelNames().isEmpty()) {
      return new UnlabeledChildRepo<>(new MetricData<>(new SettableDoubleSupplier()));
    } else {
      return new LabeledChildrenRepo<>(commaDelimitedLabelValues -> {
        final String[] labelValues = commaDelimitedStringToLabels(commaDelimitedLabelValues);
        return new MetricData<>(new SettableDoubleSupplier(), labelValues);
      });
    }
  }

  @Override
  List<Sample> createSamples(final MetricData<SettableDoubleSupplier> metricData, final SampleCreator sampleCreator) {
    return singletonList(sampleCreator.createSample(getName(),
                                                    getLabelNames(),
                                                    metricData.getLabelValues(),
                                                    metricData.getMetric().getAsDouble()
    ));
  }

  public void set(final double value, final String... labelValues) {
    metricForLabels(labelValues).set(value);
  }

  public static class SettableGaugeBuilder extends AbstractMetricBuilder<SettableGauge, SettableGauge.SettableGaugeBuilder> {

    public SettableGaugeBuilder(final String name, final String help) {
      super(name, help);
    }

    @Override
    protected SettableGauge create(final String fullName, final String help, final String[] labelNames) {
      return new SettableGauge(fullName, help, labelNames);
    }

  }

  static class SettableDoubleSupplier implements DoubleSupplier {

    private AtomicLong value = new AtomicLong(0);

    void set(final double value) {
      this.value.set(Double.doubleToLongBits(value));
    }

    @Override
    public double getAsDouble() {
      return Double.longBitsToDouble(value.get());
    }
  }

}
