package com.outbrain.swinfra.metrics;

import com.outbrain.swinfra.metrics.data.HistogramWithRunningCountAndSum;
import com.outbrain.swinfra.metrics.data.MetricDataConsumer;
import com.outbrain.swinfra.metrics.timing.Clock;
import com.outbrain.swinfra.metrics.timing.Timer;
import com.outbrain.swinfra.metrics.timing.TimingMetric;
import com.outbrain.swinfra.metrics.utils.MetricType;

import static com.outbrain.swinfra.metrics.timing.Clock.DEFAULT_CLOCK;
import static com.outbrain.swinfra.metrics.utils.MetricType.SUMMARY;

/**
 * An implementation of a Summary metric. A summary is a histogram that samples its measurements and has no predefined
 * buckets. The summary calculates several quantiles over its observed measurements.
 * <p>
 * The summary exposes several time series:
 * <ul>
 * <li>
 * Sum - the sum of all its measurements.
 * The name of this metric will consist of the original metric name with a '_sum' suffix
 * </li>
 * <li>
 * Count - the number of measurements taken.
 * The name of this metric will consist of the original metric name with a '_count' suffix
 * <li>
 * Quantiles - the 0.5, 0.75, 0.95, 0.98, 0.99 and 0.999 quantiles.
 * Each of these will have the same name as the original metric, but with a 'quantile' label added
 * </li>
 * </ul>
 * </p>
 *
 * @see <a href="https://prometheus.io/docs/concepts/metric_types/#summary">Prometheus summary metric</a>
 * @see <a href="https://prometheus.io/docs/practices/histograms/">Prometheus summary vs. histogram</a>
 */
public class Summary extends AbstractMetric<HistogramWithRunningCountAndSum> implements TimingMetric {

  private final Clock clock;
  private final int numberOfSignificantValueDigits;

  private Summary(final String name,
                  final String help,
                  final String[] labelNames,
                  final Clock clock,
                  final int numberOfSignificantValueDigits) {
    super(name, help, labelNames);
    this.clock = clock;
    this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
  }

  public void observe(final long value, final String... labelValues) {
    metricForLabels(labelValues).recordValue(value);
  }

  @Override
  HistogramWithRunningCountAndSum createMetric() {
    return new HistogramWithRunningCountAndSum(numberOfSignificantValueDigits);
  }

  @Override
  public MetricType getType() {
    return SUMMARY;
  }

  @Override
  public void forEachMetricData(final MetricDataConsumer consumer) {
    forEachChild(metricData -> consumer.consumeSummary(this, metricData.getLabelValues(), metricData.getMetric().summary()));
  }

  @Override
  public Timer startTimer(final String... labelValues) {
    final HistogramWithRunningCountAndSum histogram = metricForLabels(labelValues);
    return new Timer(clock, histogram::recordValue);
  }

  public static class SummaryBuilder extends AbstractMetricBuilder<Summary, SummaryBuilder> {

    private Clock clock = DEFAULT_CLOCK;
    private int numberOfSignificantValueDigits = 2;

    public SummaryBuilder(final String name, final String help) {
      super(name, help);
    }

    public SummaryBuilder withClock(final Clock clock) {
      this.clock = clock;
      return this;
    }

    public SummaryBuilder withNumberOfSignificantValueDigits(final int numberOfSignificantValueDigits) {
      this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
      return this;
    }

    @Override
    protected Summary create(final String fullName, final String help, final String[] labelNames) {
      return new Summary(fullName, help, labelNames, clock, numberOfSignificantValueDigits);
    }
  }

}
