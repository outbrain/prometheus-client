package com.outbrain.swinfra.metrics.data;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;
import org.HdrHistogram.Recorder;

/**
 * Created by ahadadi on 26/04/2018.
 */
public class HistogramWithRunningCountAndSum {
  private final int numberOfSignificantValueDigits;
  private final Recorder nonNegativeRecorder;
  private Histogram nonNegativeHistogramToRecycle;
  private volatile Recorder negativeRecorder;
  private Histogram negativeHistogramToRecycle;
  private Histogram negativeAndNonNegativeSum;
  private long count;
  private long sum;
  private final Object negativeLock = new Object();
  private final Object summaryLock = new Object();

  public HistogramWithRunningCountAndSum(final int numberOfSignificantValueDigits) {
    this.nonNegativeRecorder = new Recorder(numberOfSignificantValueDigits);
    this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
  }

  public void recordValue(final long value) {
    if (value < 0) {
      // HdrHistogram does not support recording negative values, so we flip the sign.
      lazilyInitializedNegativeRecorder().recordValue(-value);
    } else {
      nonNegativeRecorder.recordValue(value);
    }
  }

  private Recorder lazilyInitializedNegativeRecorder() {
    // Lazily initialize lazilyInitializedNegativeRecorder to avoid allocating the needed memory upfront.
    if (negativeRecorder == null) {
      synchronized (negativeLock) {
        if (negativeRecorder == null) {
          negativeAndNonNegativeSum = new Histogram(numberOfSignificantValueDigits);
          negativeRecorder = new Recorder(numberOfSignificantValueDigits);
        }
      }
    }
    return negativeRecorder;
  }

  public SummaryData summary() {
    // Read the contents of the histograms under lock to prevent another thread from swapping the
    // recyclable histograms and making them active while we extract stats from them.
    synchronized (summaryLock) {
      // The Java Doc explaining the usage pattern involving getIntervalHistogram is explained here:
      // https://github.com/HdrHistogram/HdrHistogram/blob/34ac23d63b496d37eab966502153789153b3e492/src/main/java/org/HdrHistogram/Recorder.java#L26
      nonNegativeHistogramToRecycle = nonNegativeRecorder.getIntervalHistogram(nonNegativeHistogramToRecycle);

      if (negativeRecorder == null) {
        return summary(0, nonNegativeHistogramToRecycle);
      }

      negativeHistogramToRecycle = negativeRecorder.getIntervalHistogram(negativeHistogramToRecycle);
      if (negativeHistogramToRecycle.getTotalCount() == 0) {
        return summary(0, nonNegativeHistogramToRecycle);
      }

      // We need to sum the negative and non negative histograms.
      // For that we first need to put their values on the same scale.
      // As we recorded -v for each recorded negative value v, we need to flip the sign again to get (-(-v)) = v.
      // Since we cannot record negative values,
      // we add offset = negativeHistogramToRecycle.getMaxValue() to v.
      // That means we also have to add offset to the non negative histogram.
      // We later correct the sum by subtracting (offset * #{recorded values})
      // and correct the percentiles by subtracting offset.
      final long offset = negativeHistogramToRecycle.getMaxValue();
      negativeAndNonNegativeSum.reset();
      negativeHistogramToRecycle.recordedValues().forEach(x -> negativeAndNonNegativeSum.recordValueWithCount(offset - x.getValueIteratedTo(), x.getCountAtValueIteratedTo()));
      nonNegativeHistogramToRecycle.recordedValues().forEach(x -> negativeAndNonNegativeSum.recordValueWithCount(offset + x.getValueIteratedTo(), x.getCountAtValueIteratedTo()));
      return summary(offset, negativeAndNonNegativeSum);
    }
  }

  private SummaryData summary(final long offset, final Histogram histogram) {
    assert Thread.holdsLock(summaryLock);

    count += histogram.getTotalCount();

    HistogramIterationValue lastRecordedValue = null;
    for (final HistogramIterationValue x : histogram.recordedValues()) {
      lastRecordedValue = x;
    }

    if (lastRecordedValue != null) {
      sum += lastRecordedValue.getTotalValueToThisValue() - offset * histogram.getTotalCount();
    }

    return new HdrSummaryData(histogram, count, sum, offset);
  }

  private static class HdrSummaryData implements SummaryData {

    private final long count;
    private final long sum;
    private final long p50;
    private final long p75;
    private final long p95;
    private final long p98;
    private final long p99;
    private final long p999;

    private HdrSummaryData(final Histogram histogram, final long count, final long sum, final long offset) {
      this.count = count;
      this.sum = sum;
      this.p50 = histogram.getValueAtPercentile(50) - offset;
      this.p75 = histogram.getValueAtPercentile(75) - offset;
      this.p95 = histogram.getValueAtPercentile(95) - offset;
      this.p98 = histogram.getValueAtPercentile(98) - offset;
      this.p99 = histogram.getValueAtPercentile(99) - offset;
      this.p999 = histogram.getValueAtPercentile(99.9) - offset;
    }

    @Override
    public long getCount() {
      return count;
    }

    @Override
    public double getSum() {
      return sum;
    }

    @Override
    public double getMedian() {
      return p50;
    }

    @Override
    public double get75thPercentile() {
      return p75;
    }

    @Override
    public double get95thPercentile() {
      return p95;
    }

    @Override
    public double get98thPercentile() {
      return p98;
    }

    @Override
    public double get99thPercentile() {
      return p99;
    }

    @Override
    public double get999thPercentile() {
      return p999;
    }

    @Override
    public String toString() {
      return String.format("count %d, sum %f, 50p %f, 75p %f, 95p %f, 98p %f, 99p %f, 99.9p %f",
              getCount(), getSum(), getMedian(), get75thPercentile(), get95thPercentile(),
              get98thPercentile(), get99thPercentile(), get999thPercentile());
    }
  }

}
