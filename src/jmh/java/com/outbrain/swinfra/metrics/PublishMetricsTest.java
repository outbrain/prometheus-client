package com.outbrain.swinfra.metrics;

import com.outbrain.swinfra.metrics.format.CollectorRegistryFormatter;
import com.outbrain.swinfra.metrics.format.CollectorRegistryFormatterFactory;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyMap;

/*
 * Usage:
 *
 * gradle jmh  -Pinclude=".*PublishMetricsTest.*"
 *
 */
@State(Scope.Benchmark)
public class PublishMetricsTest {

    private static final String NAME = "NAME";
    private static final String HELP = "HELP";

    private String expected;
    private String expectedOutbrainClient;
    private String expectedSimpleClient;
    private String output;


    private final CollectorRegistry collectorRegistry = new CollectorRegistry();
    private final MetricCollectorRegistry metricCollectorRegistry = new MetricCollectorRegistry();
    private CollectorRegistryFormatter formatter = CollectorRegistryFormatterFactory.TEXT_004.create(metricCollectorRegistry);


    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void measurePrometheusPullSamplesThroughput() throws InterruptedException {
        simulateEndpoint((writer) -> TextFormat.write004(writer, collectorRegistry.metricFamilySamples()));
        expected = expectedSimpleClient;
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void measureSampleConsumerThroughput() throws InterruptedException {
        simulateEndpoint((writer) -> formatter.format(writer));
        expected = expectedOutbrainClient;
    }

    private void simulateEndpoint(final ImplementationInvoker invoker) {
        try (final StringWriter writer = new StringWriter()) {
            invoker.apply(writer);
            output = writer.toString();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Setup
    public void setUp() {
        for (int i = 0; i < 3; i++) {
            collectorRegistry.register(createPrometheusCollector());
            metricCollectorRegistry.register(createCollector());
        }

        try {
            expectedOutbrainClient = new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource("PublishMetricsTestOutput.txt").toURI())));
            expectedSimpleClient = new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource("PublishMetricsTestSimpleClientOutput.txt").toURI())));
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Failed to read expected file", e);
        }
    }

    @TearDown
    public void verify() {
        final String[] expectedLines = expected.split("\n");
        final String[] outputLines = output.split("\n");
        Arrays.sort(expectedLines);
        Arrays.sort(outputLines);
        if (!Arrays.equals(expectedLines, outputLines)) {
            System.out.println("=============");
            for (int i = 0; i < Math.max(expectedLines.length, outputLines.length); i++) {
                if (i < expectedLines.length && i < outputLines.length) {
                    if (!expectedLines[i].equals(outputLines[i])) {
                        System.out.println("expected: " + expectedLines[i]);
                        System.out.println("output  : " + outputLines[i]);
                    }
                }
                else if (i < expectedLines.length) {
                    System.out.println("expected: " + expectedLines[i]);
                    System.out.println("output  : <EOF>");
                }
                else if (i < outputLines.length) {
                    System.out.println("expected: <EOF>");
                    System.out.println("output  : " + outputLines[i]);
                }
            }
            System.out.println("=============");
            throw new RuntimeException("Unexpected output");
        }
    }

    private MetricCollector createCollector() {
        final MetricRegistry registry = new MetricRegistry();
        final Counter counter = new Counter.CounterBuilder("Counter" + NAME, HELP)
            .withLabels("label1", "label2")
            .build();
        final Histogram histogram = new Histogram.HistogramBuilder("Histogram" + NAME, HELP)
            .withLabels("label1", "label2")
            .withEqualWidthBuckets(0, 1000, 100)
            .build();
        final Summary summary = new Summary.SummaryBuilder("Summary" + NAME, HELP)
            .withLabels("label1", "label2")
            .build();

        for (int i = 0; i < 100; i++) {
            counter.inc(i, "val", "val" + i);
            histogram.observe((double)i, "val", "val" + i);
            summary.observe(i, "val", "val" + i);
        }
        registry.getOrRegister(counter);
        registry.getOrRegister(histogram);
        registry.getOrRegister(summary);

        return new MetricCollector(registry, emptyMap());
    }

    private Collector createPrometheusCollector() {
        final io.prometheus.client.Counter counter = io.prometheus.client.Counter.build()
            .name("Counter" + NAME).help(HELP).labelNames("label1", "label2")
                .register();


        final io.prometheus.client.Histogram histogram = io.prometheus.client.Histogram.build()
            .name("Histogram" + NAME).help(HELP).labelNames("label1", "label2").linearBuckets(0, 1000, 100)
                .register();

        final io.prometheus.client.Summary summary = io.prometheus.client.Summary.build()
            .name("Summary" + NAME).help(HELP).labelNames("label1", "label2")
                .register();

        for (int i = 0; i < 100; i++) {
            counter.labels("val", "val" + i).inc(i);
            histogram.labels("val", "val" + i).observe(i);
            summary.labels("val", "val" + i).observe(i);
        }
        return new Collector() {
            @Override
            public List<MetricFamilySamples> collect() {
                final List<MetricFamilySamples> samples = new ArrayList<>();
                samples.addAll(counter.collect());
                samples.addAll(histogram.collect());
                samples.addAll(summary.collect());
                return samples;
            }
        };
    }

    @FunctionalInterface
    private interface ImplementationInvoker {

        void apply(Writer writer) throws IOException;
    }
}
