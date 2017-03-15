package com.outbrain.swinfra.metrics.samples;

import io.prometheus.client.Collector.MetricFamilySamples.Sample;

import java.util.List;
import java.util.Map;

/**
 * The SampleCreator interface allows control over how a metric is transformed into a Prometheus-valid sample
 */
public interface SampleCreator {

  Sample createSample(final String name, final double value);

  Sample createSample(final String name,
                      final List<String> labelNames,
                      final List<String> labelValues,
                      final double value);

  Map<String, String> getStaticLabels();

}
