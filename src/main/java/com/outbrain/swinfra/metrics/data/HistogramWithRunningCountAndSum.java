package com.outbrain.swinfra.metrics.data;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;
import org.HdrHistogram.Recorder;

/**
 * Created by ahadadi on 26/04/2018.
 */
public class HistogramWithRunningCountAndSum {
  private final Recorder recorder;
  private Histogram histogramToRecycle;
  private long count;
  private long sum;

  public HistogramWithRunningCountAndSum(final Recorder recorder) {
    this.recorder = recorder;
  }

  public void recordValue(final long value) {
    recorder.recordValue(value);
  }

  public synchronized SummaryData summary() {
    histogramToRecycle = recorder.getIntervalHistogram(histogramToRecycle);
    count += histogramToRecycle.getTotalCount();

    HistogramIterationValue lastRecordedValue = null;
    for (final HistogramIterationValue x : histogramToRecycle.recordedValues()) {
      lastRecordedValue = x;
    }
    if (lastRecordedValue != null) {
      sum += lastRecordedValue.getTotalValueToThisValue();
    }

    return new HdrSummaryData(histogramToRecycle, count, sum);
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

    private HdrSummaryData(final Histogram histogram, final long count, final long sum) {
      this.count = count;
      this.sum = sum;
      this.p50 = histogram.getValueAtPercentile(50);
      this.p75 = histogram.getValueAtPercentile(75);
      this.p95 = histogram.getValueAtPercentile(95);
      this.p98 = histogram.getValueAtPercentile(98);
      this.p99 = histogram.getValueAtPercentile(99);
      this.p999 = histogram.getValueAtPercentile(99.9);
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
      return String.format("count %d, sum %f, median %f, 75p %f, 95p %f, 98p %f, 99p %f, 999p %f",
              getCount(), getSum(), getMedian(), get75thPercentile(), get95thPercentile(),
              get98thPercentile(), get99thPercentile(), get999thPercentile());
    }
  }

}
