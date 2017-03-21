package com.outbrain.swinfra.metrics.format;

import com.outbrain.swinfra.metrics.MetricCollector;
import com.outbrain.swinfra.metrics.MetricCollectorRegistry;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class CollectorRegistryFormatter {

    private final MetricCollectorRegistry registry;
    private final Map<MetricCollector, CollectorFormatter> formatterByCollectorMap = new ConcurrentHashMap<>();
    private final Function<? super MetricCollector, ? extends CollectorFormatter> createFormatter;

    public CollectorRegistryFormatter(final MetricCollectorRegistry registry, final Function<? super MetricCollector, ? extends CollectorFormatter> createFormatter) {
        this.registry = registry;
        this.createFormatter = createFormatter;
    }

    public void format(final Appendable appendable) throws IOException {
        for (final MetricCollector collector : registry) {
            final CollectorFormatter formatter = formatterByCollectorMap.computeIfAbsent(collector, createFormatter);
            formatter.formatTo(appendable);
        }

    }
}
