package com.outbrain.swinfra.metrics;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * An implementation of Prometheus' Collector abstract class. This collector should be registered in this client's
 * collector registry that is used for collecting the metrics.
 */
public class MetricCollector implements Iterable<Metric> {

  private final MetricRegistry metricRegistry;
  private final Map<String, String> staticLabels;


  public MetricCollector(final MetricRegistry metricRegistry) {
    this(metricRegistry, Collections.emptyMap());
  }

  public MetricCollector(final MetricRegistry metricRegistry, final Map<String, String> staticLabels) {
    this.metricRegistry = metricRegistry;
    this.staticLabels = Collections.unmodifiableMap(staticLabels);
  }

  public Map<String, String> getStaticLabels() {
    return staticLabels;
  }

  @Override
  public Iterator<Metric> iterator() {
    return metricRegistry.all().iterator();
  }
}
