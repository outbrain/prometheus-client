package com.outbrain.swinfra.metrics;

import com.outbrain.swinfra.metrics.children.ChildMetricRepo;
import com.outbrain.swinfra.metrics.children.MetricData;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
}
