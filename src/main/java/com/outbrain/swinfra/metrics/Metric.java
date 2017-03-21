package com.outbrain.swinfra.metrics;

import com.outbrain.swinfra.metrics.utils.MetricType;

import java.io.IOException;
import java.util.List;

public interface Metric {
  MetricType getType();

  String getName();

  String getHelp();

  List<String> getLabelNames();

  void forEachSample(SampleConsumer sampleConsumer) throws IOException;
}
