package com.outbrain.swinfra.metrics.client;

import com.outbrain.swinfra.metrics.Counter;
import com.outbrain.swinfra.metrics.Histogram;
import com.outbrain.swinfra.metrics.MetricRegistry;
import com.outbrain.swinfra.metrics.exporter.MetricExporter;
import com.outbrain.swinfra.metrics.exporter.MetricExporterFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

import static java.util.Collections.emptyMap;

public class OutbrainClient extends AbstractPerfClient {

    private final MetricExporterFactory metricExporterFactory;
    private MetricExporter formatter;

    public OutbrainClient(final OutputMode mode, final MetricExporterFactory metricExporterFactory) {
        super(mode);
        this.metricExporterFactory = metricExporterFactory;
    }

    @Override
    public void setUp() {
        formatter = metricExporterFactory.create(Collections.singleton(createMetricRegistry()));
    }

    @Override
    public void executeLogic(final OutputStream outputStream) throws IOException {
        formatter.exportTo(outputStream);
    }

    private MetricRegistry createMetricRegistry() {
        final MetricRegistry registry = new MetricRegistry(emptyMap());
        final Counter counter = new Counter.CounterBuilder("Counter" + NAME, HELP)
            .withLabels("label1", "label2")
            .build();
        final Histogram histogram = new Histogram.HistogramBuilder("Histogram" + NAME, HELP)
            .withLabels("label1", "label2")
            .withEqualWidthBuckets(0, 1000, 100)
            .build();

        for (int i = 0; i < 100; i++) {
            counter.inc(i, "val", "val" + i);
            histogram.observe((double)i, "val", "val" + i);
        }
        registry.getOrRegister(counter);
        registry.getOrRegister(histogram);

        return registry;
    }
}
