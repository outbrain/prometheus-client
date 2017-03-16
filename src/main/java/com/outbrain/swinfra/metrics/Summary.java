package com.outbrain.swinfra.metrics;

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.SlidingTimeWindowReservoir;
import com.codahale.metrics.SlidingWindowReservoir;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.UniformReservoir;
import com.outbrain.swinfra.metrics.children.ChildMetricRepo;
import com.outbrain.swinfra.metrics.children.LabeledChildrenRepo;
import com.outbrain.swinfra.metrics.children.MetricData;
import com.outbrain.swinfra.metrics.children.UnlabeledChildRepo;
import com.outbrain.swinfra.metrics.samples.SampleCreator;
import com.outbrain.swinfra.metrics.timing.Clock;
import com.outbrain.swinfra.metrics.timing.Timer;
import com.outbrain.swinfra.metrics.timing.TimingMetric;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.outbrain.swinfra.metrics.timing.Clock.DEFAULT_CLOCK;
import static com.outbrain.swinfra.metrics.utils.LabelUtils.addLabelToList;
import static com.outbrain.swinfra.metrics.utils.LabelUtils.commaDelimitedStringToLabels;
import static io.prometheus.client.Collector.Type.SUMMARY;

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
public class Summary extends AbstractMetric<Histogram> implements TimingMetric {

  public static final String QUANTILE_LABEL = "quantile";

  private final Supplier<Reservoir> reservoirSupplier;
  private final Clock clock;
  private final String countSampleName;
  private final String sumSampleName;

  private Summary(final String name,
                  final String help,
                  final String[] labelNames,
                  final Supplier<Reservoir> reservoirSupplier,
                  final Clock clock) {
    super(name, help, labelNames);
    this.reservoirSupplier = reservoirSupplier;
    this.clock = clock;
    this.countSampleName = name + COUNT_SUFFIX;
    this.sumSampleName = name + SUM_SUFFIX;
  }

  public void observe(final int value, final String... labelValues) {
    validateLabelValues(labelValues);
    metricForLabels(labelValues).update(value);
  }

  @Override
  ChildMetricRepo<Histogram> createChildMetricRepo() {
    if (getLabelNames().size() == 0) {
      return new UnlabeledChildRepo<>(new MetricData<>(createHistogram(), new String[]{}));
    } else {
      return new LabeledChildrenRepo<>(commaDelimitedLabelValues -> {
        final String[] labelValues = commaDelimitedStringToLabels(commaDelimitedLabelValues);
        return new MetricData<>(createHistogram(), labelValues);
      });
    }
  }

  private Histogram createHistogram() {
    return new Histogram(reservoirSupplier.get());
  }

  @Override
  public Collector.Type getType() {
    return SUMMARY;
  }

  @Override
  public void forEachSample(final SampleConsumer sampleConsumer) throws IOException {
    for (final MetricData<Histogram> metricData : allMetricData()) {
      final List<String> labelValues = metricData.getLabelValues();
      final Snapshot snapshot = metricData.getMetric().getSnapshot();
      final String name = getName();
      sampleConsumer.apply(name, snapshot.getMedian(), labelValues, QUANTILE_LABEL,"0.5");
      sampleConsumer.apply(name, snapshot.get75thPercentile(), labelValues, QUANTILE_LABEL,"0.75");
      sampleConsumer.apply(name, snapshot.get95thPercentile(), labelValues, QUANTILE_LABEL,"0.95");
      sampleConsumer.apply(name, snapshot.get98thPercentile(), labelValues, QUANTILE_LABEL,"0.98");
      sampleConsumer.apply(name, snapshot.get99thPercentile(), labelValues, QUANTILE_LABEL,"0.99");
      sampleConsumer.apply(name, snapshot.get999thPercentile(), labelValues, QUANTILE_LABEL,"0.999");
      sampleConsumer.apply(countSampleName, metricData.getMetric().getCount(), labelValues, null, null);

      long sum = 0;
      for (final long value : snapshot.getValues()) {
        sum += value;
      }
      sampleConsumer.apply(sumSampleName, sum, labelValues, null, null);
    }
  }



  @Override
  List<Sample> createSamples(final MetricData<Histogram> metricData,
                             final SampleCreator sampleCreator) {
    final List<String> labelNames = getLabelNames();
    final String name = getName();
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

  @Override
  public Timer startTimer(final String... labelValues) {
    final Histogram histogram = metricForLabels(labelValues);
    return new Timer(clock, histogram::update);
  }

  public static class SummaryBuilder extends AbstractMetricBuilder<Summary, SummaryBuilder> {

    private Clock clock = DEFAULT_CLOCK;
    private Supplier<Reservoir> reservoirSupplier = ExponentiallyDecayingReservoir::new;

    public SummaryBuilder(final String name, final String help) {
      super(name, help);
    }

    public SummaryBuilder withClock(final Clock clock) {
      this.clock = clock;
      return this;
    }

    public ReservoirBuilder withReservoir() {
      return new ReservoirBuilder();
    }

    public class ReservoirBuilder {

      /**
       * Create this summary with an exponentially decaying reservoir - a reservoir that gives a lower
       * importance to older measurements.
       *
       * @param size  the size of the reservoir - the number of measurements that will be saved
       * @param alpha the exponential decay factor. The higher this is the more biased the reservoir will
       *              be towards newer measurements.
       * @see <a href="http://dimacs.rutgers.edu/~graham/pubs/papers/fwddecay.pdf">
       */
      public SummaryBuilder withExponentiallyDecayingReservoir(final int size, final double alpha) {
        reservoirSupplier = () -> new ExponentiallyDecayingReservoir(size, alpha);
        return SummaryBuilder.this;
      }

      /**
       * Create this summary with a sliding time window reservoir. This reservoir keeps the measurements made in the
       * last {@code window} seconds (or other time unit).
       *
       * @param window     the window to save
       * @param windowUnit the window's time units
       */
      public SummaryBuilder withSlidingTimeWindowReservoir(final int window, final TimeUnit windowUnit) {
        reservoirSupplier = () -> new SlidingTimeWindowReservoir(window, windowUnit);
        return SummaryBuilder.this;
      }

      /**
       * Create this summary with a sliding window reservoir. This reservoir keeps a constant amount of the last
       * measurements and is therefore memory-bound.
       *
       * @param size the number of measurements to save
       */
      public SummaryBuilder withSlidingWindowReservoir(final int size) {
        reservoirSupplier = () -> new SlidingWindowReservoir(size);
        return SummaryBuilder.this;
      }


      /**
       * Create this summary with a uniform reservoir - a reservoir that randomally saves the measurements and is
       * statistically representative of all measurements.
       *
       * @param size the size of the reservoir - the number of measurements that will be saved
       * @see <a href="http://www.cs.umd.edu/~samir/498/vitter.pdf">Random Sampling with a Reservoir</a>
       */
      public SummaryBuilder withUniformReservoir(final int size) {
        reservoirSupplier = () -> new UniformReservoir(size);
        return SummaryBuilder.this;
      }
    }

    @Override
    protected Summary create(final String fullName, final String help, final String[] labelNames) {
      return new Summary(fullName, help, labelNames, reservoirSupplier, clock);
    }
  }
}
