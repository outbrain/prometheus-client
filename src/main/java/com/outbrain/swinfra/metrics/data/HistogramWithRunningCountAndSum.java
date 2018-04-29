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
  private volatile Histogram negativeAndNonNegativeSum;
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
      if (negativeRecorder == null) {
        synchronized (negativeLock) {
          if (negativeRecorder == null) {
            negativeAndNonNegativeSum = new Histogram(numberOfSignificantValueDigits);
            negativeRecorder = new Recorder(numberOfSignificantValueDigits);
          }
        }
      }
      negativeRecorder.recordValue(-value);
    } else {
      nonNegativeRecorder.recordValue(value);
    }
  }

  public SummaryData summary() {
    synchronized (summaryLock) {
      nonNegativeHistogramToRecycle = nonNegativeRecorder.getIntervalHistogram(nonNegativeHistogramToRecycle);
      final long offset;
      final Histogram histogram;
      if (negativeRecorder != null) {
        negativeHistogramToRecycle = negativeRecorder.getIntervalHistogram(negativeHistogramToRecycle);
        if (negativeHistogramToRecycle.getTotalCount() == 0) {
          offset = 0;
          histogram = nonNegativeHistogramToRecycle;
        } else {
          offset = negativeHistogramToRecycle.getMaxValue();
          histogram = negativeAndNonNegativeSum;
          histogram.reset();
          negativeHistogramToRecycle.recordedValues().forEach(x -> histogram.recordValueWithCount(offset - x.getValueIteratedTo(), x.getCountAtValueIteratedTo()));
          nonNegativeHistogramToRecycle.recordedValues().forEach(x -> histogram.recordValueWithCount(offset + x.getValueIteratedTo(), x.getCountAtValueIteratedTo()));
        }
      } else {
        offset = 0;
        histogram = nonNegativeHistogramToRecycle;
      }
      updateSumAndCount(histogram, offset);

      return new HdrSummaryData(histogram, count, sum, offset);
    }
  }

  private void updateSumAndCount(final Histogram histogram, final long offset) {
    count += histogram.getTotalCount();

    HistogramIterationValue lastRecordedValue = null;
    for (final HistogramIterationValue x : histogram.recordedValues()) {
      lastRecordedValue = x;
    }
    if (lastRecordedValue != null) {
      sum += lastRecordedValue.getTotalValueToThisValue() - offset * histogram.getTotalCount();
    }
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
