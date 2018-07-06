package com.outbrain.swinfra.metrics.exporter.text;

import com.outbrain.swinfra.metrics.Metric;
import com.outbrain.swinfra.metrics.MetricRegistry;
import com.outbrain.swinfra.metrics.data.HistogramData;
import com.outbrain.swinfra.metrics.data.MetricDataConsumer;
import com.outbrain.swinfra.metrics.data.SummaryData;
import com.outbrain.swinfra.metrics.exporter.MetricExporter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TextFormatter implements MetricExporter {
    public static final String CONTENT_TYPE_004 = "text/plain; version=0.0.4; charset=utf-8";

    public static final String QUANTILE_LABEL = "quantile";
    public static final String COUNT_SUFFIX = "_count";
    public static final String SUM_SUFFIX = "_sum";
    public static final String BUCKET_LABEL = "le";
    public static final String SAMPLE_NAME_BUCKET_SUFFIX = "_bucket";

    private final Collection<MetricRegistry> registries;
    private final Map<Metric, String> headerByMetric = new ConcurrentHashMap<>();

    public TextFormatter(final Collection<MetricRegistry> registries) {
        this.registries = registries;
    }


    @Override
    public void exportTo(final OutputStream outputStream) throws IOException {
        final Writer stream = new OutputStreamWriter(outputStream);
        for (final MetricRegistry registry : registries) {
            final TextMetricDataConsumer consumer = new TextMetricDataConsumer(registry.getStaticLabels(), stream);
            for (final Metric metric : registry) {
                final String header = headerByMetric.computeIfAbsent(metric, this::createHeader);
                stream.append(header);

                metric.forEachMetricData(consumer);
            }
            stream.flush();
        }
    }

    private static class TextMetricDataConsumer implements MetricDataConsumer {

        private final Map<String, String> staticLabels;
        private final Writer stream;

        private TextMetricDataConsumer(final Map<String, String> staticLabels, final Writer stream) {
            this.staticLabels = staticLabels;
            this.stream = stream;
        }

        @Override
        public void consumeCounter(final Metric metric, final List<String> labelValues, final double value) {
            appendSample(metric.getName(), value, metric.getLabelNames(), labelValues);
        }

        @Override
        public void consumeGauge(final Metric metric, final List<String> labelValues, final double value) {
            appendSample(metric.getName(), value, metric.getLabelNames(), labelValues);
        }

        @Override
        public void consumeSummary(final Metric metric, final List<String> labelValues, final SummaryData data) {
            final String name = metric.getName();
            final List<String> labelNames = metric.getLabelNames();
            appendSample(name, data.getMedian(), labelNames, labelValues, QUANTILE_LABEL, "0.5");
            appendSample(name, data.get75thPercentile(), labelNames, labelValues, QUANTILE_LABEL, "0.75");
            appendSample(name, data.get95thPercentile(), labelNames, labelValues, QUANTILE_LABEL, "0.95");
            appendSample(name, data.get98thPercentile(), labelNames, labelValues, QUANTILE_LABEL, "0.98");
            appendSample(name, data.get99thPercentile(), labelNames, labelValues, QUANTILE_LABEL, "0.99");
            appendSample(name, data.get999thPercentile(), labelNames, labelValues, QUANTILE_LABEL, "0.999");
            appendSample(name, COUNT_SUFFIX, data.getCount(), labelNames, labelValues);
            appendSample(name, SUM_SUFFIX, data.getSum(), labelNames, labelValues);
        }

        @Override
        public void consumeHistogram(final Metric metric, final List<String> labelValues, final HistogramData data) {
            final String name = metric.getName();
            final List<String> labelNames = metric.getLabelNames();
            data.consumeBuckets((upperBound, count) -> {
                appendSample(name, SAMPLE_NAME_BUCKET_SUFFIX, count, labelNames, labelValues, BUCKET_LABEL, doubleToGoString(upperBound));
            });
            appendSample(name, COUNT_SUFFIX, data.getCount(), labelNames, labelValues);
            appendSample(name, SUM_SUFFIX, data.getSum(), labelNames, labelValues);
        }

        private void appendSample(final String name, final double value,
                                  final List<String> labelNames, final List<String> labelValues) {
            appendSample(name, null, value, labelNames, labelValues, null, null);
        }

        private void appendSample(final String name, final String nameSuffix, final double value,
                                  final List<String> labelNames, final List<String> labelValues) {
            appendSample(name, nameSuffix, value, labelNames, labelValues, null, null);
        }

        private void appendSample(final String name, final double value,
                                  final List<String> labelNames, final List<String> labelValues,
                                  final String sampleLevelLabelName, final String sampleLevelLabelValue) {
            appendSample(name, null, value, labelNames, labelValues, sampleLevelLabelName, sampleLevelLabelValue);
        }

        private void appendSample(final String name, final String nameSuffix, final double value,
                                  final List<String> labelNames, final List<String> labelValues,
                                  final String sampleLevelLabelName, final String sampleLevelLabelValue) {
            try {
                stream.append(name);
                if (nameSuffix != null) {
                    stream.append(nameSuffix);
                }
                appendLabels(labelNames, labelValues, sampleLevelLabelName, sampleLevelLabelValue);
                stream.append(" ").append(doubleToGoString(value)).append("\n");
            } catch (final IOException e) {
                throw new RuntimeException("failed appending to output stream");
            }
        }

        private void appendLabels(final List<String> labelNames, final List<String> labelValues,
                                  final String sampleLevelLabelName, final String sampleLevelLabelValue) throws IOException {
            if (containsLabels(staticLabels, labelNames, sampleLevelLabelName)) {
                stream.append("{");

                for (final Map.Entry<String, String> entry : staticLabels.entrySet()) {
                    appendLabel(stream, entry.getKey(), entry.getValue());
                }

                for (int i = 0; i < labelNames.size(); ++i) {
                    appendLabel(stream, labelNames.get(i), labelValues.get(i));
                }
                if (sampleLevelLabelName != null) {
                    appendLabel(stream, sampleLevelLabelName, sampleLevelLabelValue);
                }
                stream.append("}");
            }
        }

        private void appendLabel(final Appendable appendable, final String name, final String value) throws IOException {
            appendable.append(name).append("=\"").append(escapeLabelValue(value)).append("\",");
        }

        private boolean containsLabels(final Map<String, String> staticLabels, final List<String> labelNames, final String sampleLevelLabelName) {
            return !staticLabels.isEmpty() || !labelNames.isEmpty() || sampleLevelLabelName != null;
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

    private String createHeader(final Metric metric) {
        return "# HELP " + metric.getName() + " " + escapeHelp(metric.getHelp()) + "\n" +
               "# TYPE " + metric.getName() + " " + metric.getType().getName() + "\n";
    }

    private static String escapeHelp(final String help) {
        return help.replace("\\", "\\\\").replace("\n", "\\n");
    }

}
