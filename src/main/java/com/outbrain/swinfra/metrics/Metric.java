package com.outbrain.swinfra.metrics;

import com.outbrain.swinfra.metrics.samples.SampleCreator;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples;

import java.io.IOException;
import java.util.List;

public interface Metric {
  Collector.Type getType();

  String getName();

  String getHelp();

  List<String> getLabelNames();

  MetricFamilySamples getSample(final SampleCreator sampleCreator);

  void forEachSample(SampleConsumer sampleConsumer) throws IOException;
}
