package com.outbrain.swinfra.metrics.utils;

public enum MetricType {

    COUNTER("counter"),
    GAUGE("gauge"),
    SUMMARY("summary"),
    HISTOGRAM("histogram");

    private final String name;

    MetricType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
