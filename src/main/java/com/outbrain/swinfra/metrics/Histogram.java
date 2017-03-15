package com.outbrain.swinfra.metrics;

import com.outbrain.swinfra.metrics.children.ChildMetricRepo;
import com.outbrain.swinfra.metrics.children.LabeledChildrenRepo;
import com.outbrain.swinfra.metrics.children.MetricData;
import com.outbrain.swinfra.metrics.children.UnlabeledChildRepo;
import com.outbrain.swinfra.metrics.samples.SampleCreator;
import com.outbrain.swinfra.metrics.timing.Clock;
import com.outbrain.swinfra.metrics.timing.Timer;
import com.outbrain.swinfra.metrics.timing.TimingMetric;
import io.prometheus.client.Collector;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.DoubleStream;

import static com.outbrain.swinfra.metrics.timing.Clock.DEFAULT_CLOCK;
import static com.outbrain.swinfra.metrics.utils.LabelUtils.addLabelToList;
import static com.outbrain.swinfra.metrics.utils.LabelUtils.commaDelimitedStringToLabels;
import static io.prometheus.client.Collector.MetricFamilySamples.Sample;
import static io.prometheus.client.Collector.Type.HISTOGRAM;

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

  public static final String SAMPLE_NAME_BUCKET_SUFFIX = "_bucket";
  public static final String BUCKET_LABEL = "le";
  private final double[] buckets;
  private final Clock clock;
  private final String bucketSampleName;
  private final String countSampleName;
  private final String sumSampleName;

  private Histogram(final String name,
                    final String help,
                    final String[] labelNames,
                    final double[] buckets,
                    final Clock clock) {
    super(name, help, labelNames);
    this.buckets = buckets;
    this.clock = clock;
    this.bucketSampleName = name + SAMPLE_NAME_BUCKET_SUFFIX;
    this.countSampleName = name + "_count";
    this.sumSampleName = name + "_sum";
  }

  @Override
  ChildMetricRepo<Buckets> createChildMetricRepo() {
    if (getLabelNames().size() == 0) {
      return new UnlabeledChildRepo<>(new MetricData<>(new Buckets(buckets), new String[]{}));
    } else {
      return new LabeledChildrenRepo<>(commaDelimitedLabelValues -> {
        final String[] labelValues = commaDelimitedStringToLabels(commaDelimitedLabelValues);
        return new MetricData<>(new Buckets(buckets), labelValues);
      });
    }
  }

  @Override
  public void forEachSample(final SampleConsumer sampleConsumer) throws IOException {
    for (final MetricData<Buckets> metricData : allMetricData()) {
      final List<String> labelValues = metricData.getLabelValues();
      final BucketValues bucketValues = metricData.getMetric().getValues();
      final String[] bucketBounds = metricData.getMetric().getBucketBoundsAsString();
      for (int i = 0; i < bucketBounds.length; i++) {
        sampleConsumer.apply(bucketSampleName, bucketValues.getBuckets()[i], labelValues, BUCKET_LABEL, bucketBounds[i]);
      }
      //Add count and sum samples
      final long lastBucketValue = bucketValues.getBuckets()[bucketValues.getBuckets().length - 1];
      sampleConsumer.apply(countSampleName, lastBucketValue, labelValues, null, null);
      sampleConsumer.apply(sumSampleName, bucketValues.getSum(), labelValues, null, null);
    }
  }

  @Override
  List<Sample> createSamples(final MetricData<Buckets> metricData, final SampleCreator sampleCreator) {
    final BucketValues bucketValues = metricData.getMetric().getValues();
    final List<Sample> samples = new ArrayList<>(bucketValues.getBuckets().length + 2);

    //Add bucket samples
    final double[] bucketBounds = metricData.getMetric().bucketBounds;
    for (int i = 0; i < bucketBounds.length; i++) {
      final String bucketBound = bucketBoundToString(bucketBounds[i]);
      samples.add(sampleCreator.createSample(this.getName() + SAMPLE_NAME_BUCKET_SUFFIX,
                                             addLabelToList(getLabelNames(), BUCKET_LABEL),
                                             addLabelToList(metricData.getLabelValues(), bucketBound),
                                             bucketValues.getBuckets()[i]));
    }

    //Add count and sum samples
    final long lastBucketValue = bucketValues.getBuckets()[bucketValues.getBuckets().length - 1];
    samples.add(sampleCreator.createSample(getName() + "_count", getLabelNames(), metricData.getLabelValues(), lastBucketValue));
    samples.add(sampleCreator.createSample(getName() + "_sum", getLabelNames(), metricData.getLabelValues(), bucketValues.getSum()));

    return samples;
  }

  private static String bucketBoundToString(final double bucketBound) {
    return bucketBound == Double.MAX_VALUE ? "+Inf" : String.valueOf(bucketBound);
  }

  @Override
  public Collector.Type getType() {
    return HISTOGRAM;
  }

  public void observe(final double value, final String... labelValues) {
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

    final double[] bucketBounds;
    final LongAdder[] buckets;
    final DoubleAdder sum = new DoubleAdder();
    private final String[] bucketBoundsAsString;

    Buckets(final double... bucketBounds) {
      this.bucketBounds = Arrays.copyOf(bucketBounds, bucketBounds.length + 1);
      this.bucketBounds[this.bucketBounds.length - 1] = Double.MAX_VALUE;
      this.bucketBoundsAsString = new String[this.bucketBounds.length];
      this.buckets = new LongAdder[this.bucketBounds.length];
      for (int i = 0; i < this.bucketBounds.length; i++) {
        buckets[i] = new LongAdder();
        bucketBoundsAsString[i] = bucketBoundToString(this.bucketBounds[i]);
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
      final long[] cummulativeBuckets = new long[this.buckets.length];
      long accumulator = 0;

      //Saving a snapshot of the sum so it will not be affected by values added while the buckets are calculated
      final double sumSnapshot = sum.sum();

      for (int i = 0; i < buckets.length; i++) {
        accumulator += buckets[i].sum();
        cummulativeBuckets[i] = accumulator;
      }

      return new BucketValues(sumSnapshot, cummulativeBuckets);
    }

    String[] getBucketBoundsAsString() {
      return bucketBoundsAsString;
    }
  }

  private static class BucketValues {
    private final double sum;
    private final long[] buckets;

    BucketValues(final double sum, final long[] buckets) {
      this.sum = sum;
      this.buckets = buckets;
    }

    public double getSum() {
      return sum;
    }

    public long[] getBuckets() {
      return buckets;
    }
  }

  public static class HistogramBuilder extends AbstractMetricBuilder<Histogram, HistogramBuilder> {

    private double[] buckets = new double[]{.005, .01, .025, .05, .075, .1, .25, .5, .75, 1, 2.5, 5, 7.5, 10};
    private Clock clock = DEFAULT_CLOCK;

    public HistogramBuilder(final String name, final String help) {
      super(name, help);
    }

    @Override
    void validateParams() {
      super.validateParams();
      //Validate buckets all contain finite Double values
      Arrays.stream(buckets).forEach(this::validateBucket);
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

    @Override
    protected Histogram create(final String fullName, final String help, final String[] labelNames) {
      return new Histogram(fullName, help, labelNames, buckets, clock);
    }

  }
}
