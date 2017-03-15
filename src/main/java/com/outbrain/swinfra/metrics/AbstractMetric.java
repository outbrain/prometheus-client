package com.outbrain.swinfra.metrics;

import com.outbrain.swinfra.metrics.children.ChildMetricRepo;
import com.outbrain.swinfra.metrics.children.MetricData;
import com.outbrain.swinfra.metrics.samples.SampleCreator;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A base class for all the metrics.
 * This class contains logic related to child metrics, labels etc.
 * @param <T> the type of the wrapped metric
 */
abstract class AbstractMetric<T> implements Metric {

  private final String name;
  private final String help;
  private final List<String> labelNames;
  private ChildMetricRepo<T> childMetricRepo;

  AbstractMetric(final String name,
                 final String help,
                 final String[] labelNames) {
    this.name = name;
    this.help = help;
    this.labelNames = Arrays.asList(labelNames);
  }

  abstract ChildMetricRepo<T> createChildMetricRepo();

  abstract List<Sample> createSamples(MetricData<T> metricData, SampleCreator sampleCreator);

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getHelp() {
    return help;
  }

  @Override
  public List<String> getLabelNames() {
    return labelNames;
  }

  void initChildMetricRepo() {
    this.childMetricRepo = createChildMetricRepo();
  }

  void validateLabelValues(final String... labelValues) {
    if (labelNames.size() > 0) {
      Validate.isTrue(labelNames.size() == labelValues.length, "A label value must be supplied for each label name");
    }

    for (final String labelName : labelNames) {
      Validate.notBlank(labelName, "Label names must contain text");
    }
  }

  T metricForLabels(final String... labelValues) {
    return childMetricRepo.metricForLabels(labelValues).getMetric();
  }

  Collection<MetricData<T>> allMetricData() {
    return childMetricRepo.all();
  }

  @Override
  public MetricFamilySamples getSample(final SampleCreator sampleCreator) {
    final List<Sample> samples = allMetricData().stream()
        .flatMap(metricData -> createSamples(metricData, sampleCreator).stream())
        .collect(Collectors.toList());
    return new MetricFamilySamples(name, getType(), help, samples);
  }

}
