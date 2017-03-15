package com.outbrain.swinfra.metrics;

import com.outbrain.swinfra.metrics.format.CollectorRegistryFormatter;
import com.outbrain.swinfra.metrics.format.CollectorRegistryFormatterFactory;
import com.outbrain.swinfra.metrics.samples.StaticLablesSampleCreator;
import com.outbrain.swinfra.metrics.timing.Clock;
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
import java.util.Collections;
import java.util.concurrent.TimeUnit;

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
    private String output;


    private final CollectorRegistry collectorRegistry = new CollectorRegistry();
    private final MetricCollectorRegistry metricCollectorRegistry = new MetricCollectorRegistry();
    private CollectorRegistryFormatter formatter = CollectorRegistryFormatterFactory.TEXT_004.create(metricCollectorRegistry);


    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void measurePrometheusPullSamplesThroughput() throws InterruptedException {
        simulateEndpoint((writer) -> TextFormat.write004(writer, collectorRegistry.metricFamilySamples()));
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void measureSampleConsumerThroughput() throws InterruptedException {
        simulateEndpoint((writer) -> formatter.format(writer));
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
            collectorRegistry.register(createCollector());
            metricCollectorRegistry.register(createCollector());
        }

        try {
            expected = new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource("PublishMetricsTestOutput.txt").toURI())));
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Failed to read expected file", e);
        }
    }

    @TearDown
    public void verify() {
        if (!expected.equals(output)) {
            System.out.println("======");
            System.out.println(output);
            System.out.println("======");
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
            .withClock(new Clock.SystemClock(TimeUnit.MILLISECONDS))
            .build();

        for (int i = 0; i < 1000; i++) {
            counter.inc(i, "val", "val" + i);
            histogram.observe((double)i, "val", "val" + i);
            summary.observe(i, "val", "val" + i);
        }
        registry.getOrRegister(counter);
        registry.getOrRegister(histogram);
        registry.getOrRegister(summary);

        return new MetricCollector(registry, new StaticLablesSampleCreator(Collections.emptyMap()));
    }

    @FunctionalInterface
    private interface ImplementationInvoker {

        void apply(Writer writer) throws IOException;
    }
}
