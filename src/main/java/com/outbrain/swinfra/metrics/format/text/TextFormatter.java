package com.outbrain.swinfra.metrics.format.text;

import com.outbrain.swinfra.metrics.Metric;
import com.outbrain.swinfra.metrics.MetricCollector;
import com.outbrain.swinfra.metrics.format.CollectorFormatter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TextFormatter implements CollectorFormatter {
    public static final String CONTENT_TYPE_004 = "text/plain; version=0.0.4; charset=utf-8";

    private final MetricCollector metricCollector;
    private final Map<Metric, String> headerByMetric = new ConcurrentHashMap<>();

    public TextFormatter(final MetricCollector metricCollector) {
        this.metricCollector = metricCollector;
    }


    @Override
    public void formatTo(final Appendable appendable) throws IOException {
        final Map<String, String> staticLabels = metricCollector.getStaticLabels();
        for (final Metric metric : metricCollector) {
            final String header = headerByMetric.computeIfAbsent(metric, this::createHeader);
            appendable.append(header);
            final List<String> labelNames = metric.getLabelNames();

            metric.forEachSample((name, value, labelValues, additionalLabelName, additionalLabelValue) -> {
                appendable.append(name);
                if (containsLabels(staticLabels, labelNames, additionalLabelName)) {
                    appendable.append("{");

                    for (final Map.Entry<String, String> entry : staticLabels.entrySet()) {
                        appendLabel(appendable, entry.getKey(), entry.getValue());
                    }

                    for(int i = 0; i < labelNames.size(); ++i) {
                        appendLabel(appendable, labelNames.get(i), labelValues.get(i));
                    }

                    if (additionalLabelName != null) {
                        appendLabel(appendable, additionalLabelName, additionalLabelValue);
                    }

                    appendable.append("}");
                }
                appendable.append(" ").append(doubleToGoString(value)).append("\n");
            });
        }
    }

    private void appendLabel(final Appendable appendable, final String name, final String value) throws IOException {
        appendable.append(name).append("=\"").append(escapeLabelValue(value)).append("\",");
    }

    private boolean containsLabels(final Map<String, String> staticLabels, final List<String> labelNames, final String additionalLabelName) {
        return !staticLabels.isEmpty() || !labelNames.isEmpty() || additionalLabelName != null;
    }

    private String createHeader(final Metric metric) {
        return "# HELP " + metric.getName() + " " + escapeHelp(metric.getHelp()) + "\n" +
               "# TYPE " + metric.getName() + " " + metric.getType().getName() + "\n";
    }

    private static String escapeHelp(final String help) {
        return help.replace("\\", "\\\\").replace("\n", "\\n");
    }

    private static String escapeLabelValue(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    /**
     * Convert a double to it's string representation in Go.
     */
    private static String doubleToGoString(final double value) {
        if (value == Double.POSITIVE_INFINITY) {
            return "+Inf";
        }
        if (value == Double.NEGATIVE_INFINITY) {
            return "-Inf";
        }
        if (Double.isNaN(value)) {
            return "NaN";
        }
        return Double.toString(value);
    }
}
