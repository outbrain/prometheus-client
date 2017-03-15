package com.outbrain.swinfra.metrics;

import com.outbrain.swinfra.metrics.samples.SampleCreator;
import com.outbrain.swinfra.metrics.samples.StaticLablesSampleCreator;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * An implementation of Prometheus' Collector abstract class. This collector should be registered in this client's
 * collector registry that is used for collecting the metrics.
 */
public class MetricCollector extends Collector implements Iterable<Metric> {

  private final MetricRegistry metricRegistry;
  private final SampleCreator sampleCreator;

  public MetricCollector(final MetricRegistry metricRegistry) {
    this(metricRegistry, new StaticLablesSampleCreator(Collections.emptyMap()));
  }

  public MetricCollector(final MetricRegistry metricRegistry, final SampleCreator sampleCreator) {
    this.metricRegistry = metricRegistry;
    this.sampleCreator = sampleCreator;
  }

  @Override
  public <T extends Collector> T register() {
    throw new UnsupportedOperationException("Please register by getting hold of the CollectorRegistry and passing this collector to it.");
  }

  @Override
  public <T extends Collector> T register(final CollectorRegistry registry) {
    throw new UnsupportedOperationException("Please register by getting hold of the CollectorRegistry and passing this collector to it.");
  }

  @Override
  public List<MetricFamilySamples> collect() {
    final Collection<Metric> allMetrics = metricRegistry.all();
    final List<MetricFamilySamples> result = new ArrayList<>(allMetrics.size());
    for (final Metric metric : allMetrics) {
      result.add(metric.getSample(sampleCreator));
    }
    return result;
  }

  public Map<String, String> getStaticLabels() {
    return sampleCreator.getStaticLabels();
  }

  @Override
  public Iterator<Metric> iterator() {
    return metricRegistry.all().iterator();
  }
}
