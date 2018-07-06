package com.outbrain.swinfra.metrics;

import com.outbrain.swinfra.metrics.client.IoPrometheusClient;
import com.outbrain.swinfra.metrics.client.OutbrainClient;
import com.outbrain.swinfra.metrics.client.OutputMode;
import com.outbrain.swinfra.metrics.client.PerfTestClient;
import com.outbrain.swinfra.metrics.exporter.MetricExporterFactory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.concurrent.TimeUnit;

/*
 * Usage:
 *
 * gradle jmh  -Pinclude=".*ExportThroughputTest.*"
 *
 */
@State(Scope.Benchmark)
public class ExportThroughputTest {

    private PerfTestClient currentClient;
    private byte[] output;


    private final OutbrainClient outbrainTextClient = new OutbrainClient(OutputMode.TEXT, MetricExporterFactory.TEXT_004);
    private final OutbrainClient outbrainProtobufClient = new OutbrainClient(OutputMode.PROTOBUF, MetricExporterFactory.PROTOBUF);
    private final IoPrometheusClient ioPrometheusClient = new IoPrometheusClient();

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void measurePrometheusSimpleClientTextExportThroughput() throws InterruptedException {
        measureThroughput(ioPrometheusClient);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void measureTextExportThroughput() throws InterruptedException {
        measureThroughput(outbrainTextClient);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void measureProtobufExportThroughput() throws InterruptedException {
        measureThroughput(outbrainProtobufClient);
    }

    private void measureThroughput(final PerfTestClient client) {
        currentClient = client;
        output = currentClient.simulateEndpoint();
    }

    @Setup
    public void setUp() {
        outbrainTextClient.setUp();
        outbrainProtobufClient.setUp();
        ioPrometheusClient.setUp();
    }

    @TearDown
    public void verify() {
        currentClient.verify(output);
    }
}
