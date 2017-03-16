package com.outbrain.swinfra.metrics.timing;

/**
 * This interface should be implemented by metrics that can store timed measurements
 */
public interface TimingMetric {

  String COUNT_SUFFIX = "_count";
  String SUM_SUFFIX = "_sum";

  Timer startTimer(final String... labelValues);
}
