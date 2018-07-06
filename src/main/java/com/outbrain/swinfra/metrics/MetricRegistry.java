package com.outbrain.swinfra.metrics;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Objects.requireNonNull;

public class MetricRegistry implements Iterable<Metric> {

  private final ConcurrentMap<String, Metric> allMetrics = new ConcurrentHashMap<>(100);
  private final Map<String, String> staticLabels;

  public MetricRegistry(final Map<String, String> staticLabels) {
    this.staticLabels = staticLabels;
  }

  /**
   * Registers a metric in this registry if it doesn't already exist, and returns the existing metric if the same
   * metric already exists.
   * <p>
   * A metric already exists if a metric with the same name was already registered in this registry.
   * </p>
   * <p>
   * If an attempt to register two metrics with the same name , but different types, is made then a
   * ClassCastException will be thrown:
   * </p>
   * <pre>
   * final Counter counter = registry.getOrRegister(new Counter.CounterBuilder(<b>"name"</b>, "help").build());
   * final Summary summary = registry.getOrRegister(new Summary.SummaryBuilder(<b>"name"</b>, "help").build());
   * </pre>
   *
   * @throws ClassCastException if a metric with the same name but different type was already registered
   */
  @SuppressWarnings("unchecked")
  public <T extends Metric> T getOrRegister(final Metric metric) {
    requireNonNull(metric, "metric may not be null");
    final Metric result = allMetrics.putIfAbsent(metric.getName(), metric);
    return (T) (result == null ? metric : result);
  }

  /**
   * Deregisters a metric from this registry only if this metric already exists
   *
   * @param metric the metric to deregister
   * @return true if and only if the given metric existed in the registry prior to removal
   */
  public boolean deregister(final Metric metric) {
    return allMetrics.remove(metric.getName(), metric);
  }

  Collection<Metric> all() {
    return allMetrics.values();
  }

  public Map<String, String> getStaticLabels() {
    return staticLabels;
  }

  @Override
  public Iterator<Metric> iterator() {
    return all().iterator();
  }


}
