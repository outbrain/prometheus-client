package com.outbrain.swinfra.metrics;

import com.outbrain.swinfra.metrics.children.ChildMetricRepo;
import com.outbrain.swinfra.metrics.children.LabeledChildrenRepo;
import com.outbrain.swinfra.metrics.children.MetricData;
import com.outbrain.swinfra.metrics.children.UnlabeledChildRepo;
import com.outbrain.swinfra.metrics.data.HistogramBucketsConsumer;
import com.outbrain.swinfra.metrics.data.HistogramData;
import com.outbrain.swinfra.metrics.data.MetricDataConsumer;
import com.outbrain.swinfra.metrics.timing.Clock;
import com.outbrain.swinfra.metrics.timing.Timer;
import com.outbrain.swinfra.metrics.timing.TimingMetric;
import com.outbrain.swinfra.metrics.utils.MetricType;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.DoubleStream;

import static com.outbrain.swinfra.metrics.timing.Clock.DEFAULT_CLOCK;
import static com.outbrain.swinfra.metrics.utils.MetricType.HISTOGRAM;
import static java.util.Arrays.stream;

//todo document the fact that I favored throughput over consistency

/**
 * An implementation of a bucket-based histogram. For this type of histogram measurements are assigned to all the buckets
 * that have a value that is equal to or bigger than the measurements. All histograms have a bucket marked <i>+Inf</i>
 * that will be assigned all measurements.
 * <p>
 * For example - if our histogram is initialized with the buckets 1, 5, 10 then here is how the measurements will be assigned:
 * <table>
 *   <tr>
 *     <th>Measurement</th>
 *     <th>Buckets</th>
 *   </tr>
 *   <tr>
 *     <td>0.5</td>
 *     <td>1, 5, 10, +Inf</td>
 *   </tr>
 *   <tr>
 *     <td>6</td>
 *     <td>10, +Inf</td>
 *   </tr>
 * </table>
 * </p>
 * <p>
 *   If no buckets are provided the histogram will be initialized with the default values
 *   {.005, .01, .025, .05, .075, .1, .25, .5, .75, 1, 2.5, 5, 7.5, 10}
 * </p>
 * @see <a href="https://prometheus.io/docs/concepts/metric_types/#histogram">Prometheus summary metric</a>
 * @see <a href="https://prometheus.io/docs/practices/histograms/">Prometheus summary vs. histogram</a>
 */
public class Histogram extends AbstractMetric<Histogram.Buckets> implements TimingMetric {

  private final double[] buckets;
  private final Clock clock;
  private final boolean cummulativeBuckets;

  private Histogram(final String name,
                    final String help,
                    final String[] labelNames,
                    final double[] buckets,
                    final Clock clock,
                    final boolean cummulativeBuckets) {
    super(name, help, labelNames);
    this.buckets = buckets;
    this.clock = clock;
    this.cummulativeBuckets = cummulativeBuckets;
  }

  @Override
  ChildMetricRepo<Buckets> createChildMetricRepo() {
    if (getLabelNames().isEmpty()) {
      return new UnlabeledChildRepo<>(new MetricData<>(new Buckets(cummulativeBuckets, buckets)));
    } else {
      return new LabeledChildrenRepo<>(labelValues -> new MetricData<>(new Buckets(cummulativeBuckets, buckets), labelValues));
    }
  }

  @Override
  public void forEachMetricData(final MetricDataConsumer consumer) {
    forEachChild(metricData -> {
      final HistogramData snapshot = metricData.getMetric().getValues();
      consumer.consumeHistogram(this, metricData.getLabelValues(), snapshot);
    });
  }

  @Override
  public MetricType getType() {
    return HISTOGRAM;
  }

  public void observe(final double value, final String... labelValues) {
    validateLabelValues(labelValues);
    metricForLabels(labelValues).add(value);
  }

  @Override
  public Timer startTimer(final String... labelValues) {
    final Buckets buckets = metricForLabels(labelValues);
    return new Timer(clock, buckets::add);
  }

  /**
   * Contains bucket-related logic for the histogram.
   * When adding a value, only a single bucket is incremented, which is the first bucket in the array
   * that is associated with a number that's larger than or equal to that value.
   * <p>
   * When extracting the current values of the buckets, the counts of the buckets are summed so that each
   * bucket will contain its own count as well as the count of the previous bucket.
   * <p>
   * i.e. if the buckets contained the following values:
   * [3, 2, 1, 10]
   * when extracting the values, the following array will be created:
   * [3, 5, 6, 16]
   */
  static class Buckets {

    private final boolean cummulativeBuckets;
    private final double[] bucketBounds;
    private final LongAdder[] buckets;
    private final DoubleAdder sum = new DoubleAdder();

    Buckets(final boolean cummulativeBuckets, final double... bucketBounds) {
      this.cummulativeBuckets = cummulativeBuckets;
      this.bucketBounds = Arrays.copyOf(bucketBounds, bucketBounds.length + 1);
      this.bucketBounds[this.bucketBounds.length - 1] = Double.POSITIVE_INFINITY;
      this.buckets = new LongAdder[this.bucketBounds.length];
      for (int i = 0; i < this.bucketBounds.length; i++) {
        buckets[i] = new LongAdder();
      }
    }

    void add(final double value) {
      for (int i = 0; i < buckets.length; i++) {
        if (value <= bucketBounds[i]) {
          buckets[i].add(1);
          break;
        }
      }
      sum.add(value);
    }

    BucketValues getValues() {
      //Saving a snapshot of the sum so it will not be affected by values added while the buckets are calculated
      final double sumSnapshot = sum.sum();
      final long[] buckets;

      if (cummulativeBuckets) {
        buckets = calculateCummulativeBuckets();
      } else {
        buckets = calculateNonCummulativeBuckets();
      }

      return new BucketValues(sumSnapshot, buckets, bucketBounds, cummulativeBuckets);
    }

    private long[] calculateCummulativeBuckets() {
      final long[] cummulativeBuckets = new long[this.buckets.length];
      long accumulator = 0;

      for (int i = 0; i < buckets.length; i++) {
        accumulator += buckets[i].sum();
        cummulativeBuckets[i] = accumulator;
      }

      return cummulativeBuckets;
    }

    private long[] calculateNonCummulativeBuckets() {
      final long[] nonCummulativeBuckets = new long[this.buckets.length];

      for (int i = 0; i < buckets.length; i++) {
        nonCummulativeBuckets[i] = buckets[i].sum();
      }

      return nonCummulativeBuckets;
    }
  }

  private static class BucketValues implements HistogramData {

    private final double sum;
    private final long[] buckets;
    private final double[] bucketBounds;
    private boolean cummulativeBuckets;

    BucketValues(final double sum,
                 final long[] buckets,
                 final double[] bucketBounds,
                 final boolean cummulativeBuckets) {
      this.sum = sum;
      this.buckets = buckets;
      this.bucketBounds = bucketBounds;
      this.cummulativeBuckets = cummulativeBuckets;
    }

    @Override
    public double getSum() {
      return sum;
    }

    @Override
    public long getCount() {
      return cummulativeBuckets ? buckets[buckets.length - 1] : stream(buckets).sum();
    }

    @Override
    public void consumeBuckets(final HistogramBucketsConsumer consumer) {
      for (int i = 0; i < buckets.length; i++) {
        consumer.apply(bucketBounds[i], buckets[i]);
      }
    }

    public long[] getBuckets() {
      return buckets;
    }

    public double[] getBucketUpperBounds() {
      return bucketBounds;
    }

    @Override
    public String toString() {
      return "BucketValues{" +
          "sum=" + sum +
          ", buckets=" + Arrays.toString(buckets) +
          ", bucketBounds=" + Arrays.toString(bucketBounds) +
          '}';
    }
  }

  public static class HistogramBuilder extends AbstractMetricBuilder<Histogram, HistogramBuilder> {

    private double[] buckets = new double[]{.005, .01, .025, .05, .075, .1, .25, .5, .75, 1, 2.5, 5, 7.5, 10};
    private Clock clock = DEFAULT_CLOCK;
    private boolean cummulativeBuckets = true;

    public HistogramBuilder(final String name, final String help) {
      super(name, help);
    }

    @Override
    void validateParams() {
      super.validateParams();
      //Validate buckets all contain finite Double values
      stream(buckets).forEach(this::validateBucket);
    }

    private void validateBucket(final double bucket) {
      Validate.isTrue(Double.isFinite(bucket), "NaN, POSITIVE_INFINITY and NETGATIVE_INFINITY are invalid bucket values");
    }

    /**
     * Creates the given buckets for the histogram. The <i>+Inf</i> bucket is always added so a histogram will
     * always have at least one bucket.
     * <p>
     * Note that each bucket creates its own time-series so the cardinality limitation for labels,
     * as recommended by Prometheus, also applies to the number of buckets
     * </p>
     *
     * @param buckets the buckets to create
     */
    public HistogramBuilder withBuckets(final double... buckets) {
      this.buckets = buckets;
      return this;
    }

    /**
     * Creates equal width buckets for the histogram. If another method that also sets the buckets for the histogram
     * will be called, like <i>withBuckets</i>, it will overwrite this method's buckets
     * <p>
     * Note that each bucket creates its own time-series so the cardinality limitation for labels,
     * as recommended by Prometheus, also applies to the number of buckets
     * </p>
     *
     * <p>
     *   Here are two examples for what the method
     *   <ul>
     *     <li>withEqualWidthBuckets(0.5, 1, 1) - [0.5, +Inf]</li>
     *     <li>withEqualWidthBuckets(0.5, 1, 4) - [0.5, 1.5, 2.5, 3.5, +Inf]</li>
     *     <li>withEqualWidthBuckets(0.5, 1, 100) - Probably too many buckets</li>
     *   </ul>
     * </p>
     *
     * @param start the first bucket to create
     * @param width the width between the buckets
     * @param count the number of buckets to create
     */
    public HistogramBuilder withEqualWidthBuckets(final double start, final double width, final int count) {
      return withBuckets(DoubleStream.iterate(start, d -> d + width).limit(count).toArray());
    }

    public HistogramBuilder withClock(final Clock clock) {
      this.clock = clock;
      return this;
    }

    public HistogramBuilder nonCummulativeBuckets() {
      this.cummulativeBuckets = false;
      return this;
    }

    @Override
    protected Histogram create(final String fullName, final String help, final String[] labelNames) {
      return new Histogram(fullName, help, labelNames, buckets, clock, cummulativeBuckets);
    }

  }
}
