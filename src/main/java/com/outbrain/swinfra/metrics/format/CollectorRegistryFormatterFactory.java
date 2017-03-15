package com.outbrain.swinfra.metrics.format;

import com.outbrain.swinfra.metrics.MetricCollectorRegistry;
import com.outbrain.swinfra.metrics.format.text.TextFormatter;

public enum CollectorRegistryFormatterFactory {

    TEXT_004 {
        @Override
        public CollectorRegistryFormatter create(final MetricCollectorRegistry registry) {
            return new CollectorRegistryFormatter(registry, TextFormatter::new);
        }
    },
    PROTOBUF {
        @Override
        public CollectorRegistryFormatter create(final MetricCollectorRegistry registry) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    };

    public abstract CollectorRegistryFormatter create(MetricCollectorRegistry registry);
}
