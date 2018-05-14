package com.outbrain.swinfra.metrics.utils;

import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class NameUtils {

  public static final Pattern METRIC_NAME_PATTERN = Pattern.compile("[a-zA-Z_:][a-zA-Z0-9_:]*");
  public static final Pattern LABEL_NAME_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

  private static final String PROMETHEUS_NAMING_URL = "https://prometheus.io/docs/practices/naming/";

  /**
   * Validates that all entries in the supplised array are valid as label names
   */
  public static void validateLabelNames(final String... labelNames) {
    for (final String labelName : labelNames) {
      Validate.notBlank(labelName, "Label names must contain text");
      Validate.isTrue(LABEL_NAME_PATTERN.matcher(labelName).matches(),
                      "The label name '" + labelName + "' is invalid. See " + PROMETHEUS_NAMING_URL);
    }
  }

  /**
   * Validates that the supplied string is valid as a metric name
   */
  public static void validateMetricName(final String name) {
    Validate.notBlank(name, "The metric's name must contain text");
    Validate.isTrue(METRIC_NAME_PATTERN.matcher(name).matches(),
                    "The metric name '" + name + "' is invalid. See " + PROMETHEUS_NAMING_URL);
  }

  public static void validateLabelsCount(final String metricName, final List<String> labelNames, final String[] labelValues) {
    Validate.isTrue(labelNames.size() == labelValues.length, "%s has the following labels: %s but got: %s", metricName, labelNames, Arrays.toString(labelValues));
  }

  public void validateLabelValuesContainText(final String... labelValues) {
    for (final String labelValue : labelValues) {
      Validate.notBlank(labelValue, "Label values must contain text but got: %s", Arrays.toString(labelValues));
    }
  }

}
