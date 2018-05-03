package com.outbrain.swinfra.metrics;

import com.outbrain.swinfra.metrics.Summary.SummaryBuilder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Threads(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class SummaryThroughputTest {

  private static final String[] LABEL_NAMES = {"label1", "label2"};
  private static final List<String[]> LABEL_VALUES = Arrays.asList(new String[]{"val1", "val2"},
          new String[]{"val3", "val4"},
          new String[]{"val5", "val6"},
          new String[]{"val7", "val8"},
          new String[]{"val9", "val10"},
          new String[]{"val11", "val12"},
          new String[]{"val13", "val14"},
          new String[]{"val15", "val16"},
          new String[]{"val17", "val18"},
          new String[]{"val19", "val20"});

  @State(Scope.Benchmark)
  public static class PrometheusSummaryState {
    private io.prometheus.client.Summary summary = prometheusSummary().create();
  }

  @State(Scope.Benchmark)
  public static class LabeledPrometheusSummaryState {
    private io.prometheus.client.Summary summary = prometheusSummary().labelNames(LABEL_NAMES).create();
  }

  private static io.prometheus.client.Summary.Builder prometheusSummary() {
    return io.prometheus.client.Summary.build().
            quantile(0.5, 0.1).
            quantile(0.75, 0.1).
            quantile(0.95, 0.1).
            quantile(0.98, 0.01).
            quantile(0.99, 0.01).
            quantile(0.999, 0.001).
            name("name").help("help");
  }

  //todo validate end result
  @State(Scope.Benchmark)
  public static class SummaryState {
    private Summary summary;
    public SummaryState() {
      this.summary = new SummaryBuilder("name", "help").build();
      summary.observe(Long.MAX_VALUE);
    }
  }

  @State(Scope.Benchmark)
  public static class LabeledSummaryState {
    private Summary summary;
    public LabeledSummaryState() {
      this.summary = new SummaryBuilder("name", "help").withLabels("label1", "label2").build();
      summary.observe(Long.MAX_VALUE, "x", "y");
    }
  }

  @Benchmark
  public void measurePrometheusSummaryThroughput(final PrometheusSummaryState summary) {
    summary.summary.observe(sample());
  }

  @Benchmark
  public void measurePrometheusSummaryThroughputWithLabels(final LabeledPrometheusSummaryState summary) {
    final int sample = sample();
    summary.summary.labels(LABEL_VALUES.get(sample % LABEL_VALUES.size())).observe(sample);
  }

  @Benchmark
  public void measureSummaryThroughput(final SummaryState summary) {
    summary.summary.observe(sample());
  }

  @Benchmark
  public void measureSummaryThroughputWithLabels(final LabeledSummaryState summary) {
    final int sample = sample();
    summary.summary.observe(sample, LABEL_VALUES.get(sample % LABEL_VALUES.size()));
  }

  private int sample() {
    return ThreadLocalRandom.current().nextInt(1000);
  }

}
