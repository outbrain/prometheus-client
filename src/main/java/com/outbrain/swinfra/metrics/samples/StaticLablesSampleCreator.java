package com.outbrain.swinfra.metrics.samples;

import io.prometheus.client.Collector.MetricFamilySamples.Sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A {@link SampleCreator} that adds statically configured labels to all metrics on top of the labels the metric itself
 * has defined
 */
public class StaticLablesSampleCreator implements SampleCreator {

  private final List<String> staticLabelNames;
  private final List<String> staticLabelValues;
  private final Map<String, String> staticLabels;

  public StaticLablesSampleCreator(final Map<String, String> staticLabels) {
    staticLabelNames = new ArrayList<>(staticLabels.keySet());
    staticLabelValues = new ArrayList<>(staticLabels.values());
    this.staticLabels = staticLabels;
  }

  @Override
  public Sample createSample(final String name, final double value) {
    return createSample(name, Collections.emptyList(), Collections.emptyList(), value);
  }

  @Override
  public Sample createSample(final String name,
                             final List<String> labelNames,
                             final List<String> labelValues, final double value) {
    final List<String> finalLabelNames = new ArrayList<>(staticLabelNames);
    finalLabelNames.addAll(labelNames);

    final List<String> finalLabelValues = new ArrayList<>(staticLabelValues);
    finalLabelValues.addAll(labelValues);

    return new Sample(name, finalLabelNames, finalLabelValues, value);
  }

  @Override
  public Map<String, String> getStaticLabels() {
    return staticLabels;
  }
}
