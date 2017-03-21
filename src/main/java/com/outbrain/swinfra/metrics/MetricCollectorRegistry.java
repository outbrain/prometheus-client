package com.outbrain.swinfra.metrics;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class MetricCollectorRegistry implements Iterable<MetricCollector> {

    private final Map<MetricCollector, MetricCollector> collectors = new ConcurrentHashMap<>();

    /**
     * Register a Collector.
     */
    @SuppressWarnings("unchecked")
    public void register(final MetricCollector collector) {
        collectors.put(collector, collector);
    }

    /**
     * Unregister a Collector.
     */
    public void unregister(final MetricCollector collector) {
        collectors.remove(collector);
    }

    /**
     * Unregister all Collectors.
     */
    public void clear() {
        collectors.clear();
    }

    @Override
    public Iterator<MetricCollector> iterator() {
        return collectors.values().iterator();
    }
}
