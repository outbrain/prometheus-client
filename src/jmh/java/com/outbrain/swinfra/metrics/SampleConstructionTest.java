package com.outbrain.swinfra.metrics;

import com.outbrain.swinfra.metrics.samples.SampleCreator;
import com.outbrain.swinfra.metrics.samples.StaticLablesSampleCreator;
import com.outbrain.swinfra.metrics.timing.Clock;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyMap;

/*
 * Usage:
 *
 * gradle jmh  -Pinclude=".*SampleConstructionTest.*"
 *
 */
@State(Scope.Benchmark)
public class SampleConstructionTest {

    private static final String NAME = "NAME";
    private static final String HELP = "HELP";
    private static final SampleCreator SAMPLE_CREATOR = new StaticLablesSampleCreator(emptyMap());
    private static final int NUMBER_OF_SAMPLES = 1000;

    private Counter counter;
    private Summary summary;
    private Histogram histogram;

    private List<Sample> samples;
    private int expected;

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void measureThroughputOfCounter() throws InterruptedException {
        samples = counter.getSample(SAMPLE_CREATOR).samples;
        expected = NUMBER_OF_SAMPLES;
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void measureThroughputOfHistogram() throws InterruptedException {
        samples = histogram.getSample(SAMPLE_CREATOR).samples;
        expected = NUMBER_OF_SAMPLES * 103;
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void measureThroughputOfSummary() throws InterruptedException {
        samples = summary.getSample(SAMPLE_CREATOR).samples;
        expected = NUMBER_OF_SAMPLES * 8;
    }

    @Setup
    public void setUp() {
        counter = new Counter.CounterBuilder(NAME, HELP)
            .withLabels("label1", "label2")
            .build();
        histogram = new Histogram.HistogramBuilder(NAME, HELP)
            .withLabels("label1", "label2")
            .withEqualWidthBuckets(0, NUMBER_OF_SAMPLES, 100)
            .build();
        summary = new Summary.SummaryBuilder(NAME, HELP)
            .withLabels("label1", "label2")
            .withClock(new Clock.SystemClock(TimeUnit.MILLISECONDS))
            .build();

        for (int i = 0; i < NUMBER_OF_SAMPLES; i++) {
            counter.inc(i, "val", "val" + i);
            histogram.observe((double)i, "val", "val" + i);
            summary.observe(i, "val", "val" + i);
        }
    }

    @TearDown
    public void verify() {
        if (expected != samples.size()) {
            throw new RuntimeException("Returned wrong number of samples: " + samples.size());
        }
    }
}
