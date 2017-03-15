package com.outbrain.swinfra.metrics

import com.outbrain.swinfra.metrics.samples.SampleCreator
import com.outbrain.swinfra.metrics.samples.StaticLablesSampleCreator
import com.outbrain.swinfra.metrics.timing.Timer
import spock.lang.Specification
import spock.lang.Unroll

import static com.outbrain.swinfra.metrics.Histogram.*
import static io.prometheus.client.Collector.MetricFamilySamples
import static io.prometheus.client.Collector.MetricFamilySamples.Sample
import static io.prometheus.client.Collector.Type.HISTOGRAM

class HistogramTest extends Specification {

    private static final String NAME = "myHisto"
    private static final String HELP = "HELP"

    private final SampleConsumer sampleConsumer = Mock(SampleConsumer)
    private final SampleCreator sampleCreator = new StaticLablesSampleCreator([:])

    def 'Histogram should return the correct type'() {
        given:
            final Histogram histogram = new HistogramBuilder(NAME, HELP).build()

        expect:
            histogram.getType() == HISTOGRAM
    }

    def 'Newly created histogram, with no specific buckets, should contain the default buckets'() {
        given:
            final List<Sample> samples = generateHistogramSamples(["0.005": 0, "0.01": 0, "0.025": 0, "0.05": 0, "0.075": 0, "0.1": 0, "0.25": 0, "0.5": 0, "0.75": 0, "1.0": 0, "2.5": 0, "5.0": 0, "7.5": 0, "10.0": 0, "+Inf": 0], 0)
            final MetricFamilySamples metricFamilySamples = new MetricFamilySamples(NAME, HISTOGRAM, HELP, samples)

        when:
            final Histogram histogram = new HistogramBuilder(NAME, HELP).build()

        then:
            histogram.getSample(sampleCreator) == metricFamilySamples

        when:
            histogram.forEachSample(sampleConsumer)
        then:
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '0.005')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '0.01')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '0.025')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '0.05')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '0.075')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '0.1')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '0.25')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '0.5')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '0.75')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '1.0')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '2.5')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '5.0')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '7.5')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '10.0')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '+Inf')
            1 * sampleConsumer.apply(NAME+"_sum", 0, [], null, null)
            1 * sampleConsumer.apply(NAME+"_count", 0, [], null, null)

    }

    def 'Histogram with defined buckets should return samples relevant for these buckets'() {
        given:
            final List<Sample> samples = generateHistogramSamples(
                ["1.0": 1, "10.0": 2, "100.0": 2, "+Inf": 1],
                1 + 5 + 5 + 50 + 50 + 150)
            final MetricFamilySamples metricFamilySamples = new MetricFamilySamples(NAME, HISTOGRAM, HELP, samples)

            final Histogram histogram = new HistogramBuilder(NAME, HELP).withBuckets(1, 10, 100).build()

        when:
            histogram.observe(1)
            histogram.observe(5)
            histogram.observe(5)
            histogram.observe(50)
            histogram.observe(50)
            histogram.observe(150)

        then:
            histogram.getSample(sampleCreator) == metricFamilySamples

        when:
            histogram.forEachSample(sampleConsumer)
        then:
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 1, [], BUCKET_LABEL, '1.0')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 3, [], BUCKET_LABEL, '10.0')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 5, [], BUCKET_LABEL, '100.0')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 6, [], BUCKET_LABEL, '+Inf')
            1 * sampleConsumer.apply(NAME+"_sum", 1 + 5 + 5 + 50 + 50 + 150, [], null, null)
            1 * sampleConsumer.apply(NAME+"_count", 6, [], null, null)
    }

    def 'Histogram with defined buckets and labels should return correct samples with correct lables'() {
        given:
            final String labelName = "lab1"
            final List<String> labelValues = ["val1", "val2", "val3"]
            final List<Sample> samplesWithoutLabels = generateHistogramSamples(
                ["1.0": 1, "10.0": 2, "100.0": 2, "+Inf": 1],
                1 + 5 + 5 + 50 + 50 + 150)

            final List<Sample> samples = labelValues.collect {
                addLabelsToSample(samplesWithoutLabels, [labelName], [it])
            }.flatten()
            final MetricFamilySamples metricFamilySamples = new MetricFamilySamples(NAME, HISTOGRAM, HELP, samples)

            final Histogram histogram = new HistogramBuilder(NAME, HELP)
                .withLabels(labelName)
                .withBuckets(1, 10, 100)
                .build()

        when:
            labelValues.each {
                histogram.observe(1, it)
                histogram.observe(5, it)
                histogram.observe(5, it)
                histogram.observe(50, it)
                histogram.observe(50, it)
                histogram.observe(150, it)
            }

        then:
            final MetricFamilySamples actualMetricFamilySamples = histogram.getSample(sampleCreator)
            actualMetricFamilySamples.samples.sort() == metricFamilySamples.samples.sort()
            actualMetricFamilySamples.name == metricFamilySamples.name
            actualMetricFamilySamples.help == metricFamilySamples.help
            actualMetricFamilySamples.type == metricFamilySamples.type
    }

    @Unroll
    def "An attempt to create a Histogram with a bucket #bucket should throw an exception"() {
        given:
            final HistogramBuilder histogramBuilder = new HistogramBuilder(NAME, HELP)
                .withBuckets(bucket)

        when:
            histogramBuilder.build()

        then:
            thrown IllegalArgumentException

        where:
            bucket << [Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN]
    }

    def "A Histogram with equal width buckets with start: 0.5, width: 1; count: 1 should have exactly two buckets"() {
        given:
            final List<Sample> samples = generateHistogramSamples(["0.5": 0, "+Inf": 0], 0)
            final MetricFamilySamples metricFamilySamples = new MetricFamilySamples(NAME, HISTOGRAM, HELP, samples)

        when:
            final Histogram histogram = new HistogramBuilder(NAME, HELP).withEqualWidthBuckets(0.5, 1, 1).build()

        then:
            histogram.getSample(sampleCreator) == metricFamilySamples

        when:
            histogram.forEachSample(sampleConsumer)
        then:
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '0.5')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '+Inf')
            0 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, _, _, BUCKET_LABEL, _)
    }

    def "A Histogram with equal width buckets should return the correct buckets"() {
        given:
            final List<Sample> samples = generateHistogramSamples(["0.5": 0, "1.5": 0, "2.5": 0, "3.5": 0, "+Inf": 0], 0)
            final MetricFamilySamples metricFamilySamples = new MetricFamilySamples(NAME, HISTOGRAM, HELP, samples)

            final Histogram histogram = new HistogramBuilder(NAME, HELP).withEqualWidthBuckets(0.5, 1, 4).build()

        expect:
            histogram.getSample(sampleCreator) == metricFamilySamples
    }

    def "A timer should add the measured samples to the histogram"() {
        final TestClock clock = new TestClock()
        given:
            final List<Sample> samples = generateHistogramSamples(["1.5": 1, "2.5": 1, "+Inf": 1], 6)
            final MetricFamilySamples metricFamilySamples = new MetricFamilySamples(NAME, HISTOGRAM, HELP, samples)

            final Histogram histogram = new HistogramBuilder(NAME, HELP).withClock(clock).withBuckets(1.5, 2.5).build()

        when:
            [1, 2, 3].each {
                clock.setTick(0)
                final Timer timer = histogram.startTimer()
                clock.setTick(it)
                timer.stop()
            }

        then:
            histogram.getSample(sampleCreator) == metricFamilySamples

        when:
            histogram.forEachSample(sampleConsumer)
        then:
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 1, [], BUCKET_LABEL, '1.5')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 2, [], BUCKET_LABEL, '2.5')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 3, [], BUCKET_LABEL, '+Inf')
            1 * sampleConsumer.apply(NAME+"_sum", 6, [], null, null)
            1 * sampleConsumer.apply(NAME+"_count", 3, [], null, null)
    }

    /**
     *
     * @param eventsForBucket eventsForBucket - maps [bucket: numOfEvents]
     * For example, generateHistogramSamples(["1":1, "10":2, "100":3, "+Inf":4])
     * Means there was one event with value "1" or less, another one with the value between "1" (exclusive) and "10" (inclusive), another between
     * "10" and "100" and another that's more than "100"
     * The following observations correspond to this example:
     * histo.observe(1)
     * histo.observe(5)
     * histo.observe(50)
     * histo.observe(150)
     * @param sumOfSamples The sum of all the samples
     * @return
     */
    private static List<Sample> generateHistogramSamples(final Map<String, Long> eventsForBucket,
                                                         final double sumOfSamples) {
        long totalEvents = 0
        final List<Sample> result = []
        for (final Map.Entry<String, Long> bucketEvents : eventsForBucket) {
            final String bucket = bucketEvents.getKey()
            final long events = bucketEvents.getValue()
            totalEvents += events
            result.add(new Sample("${NAME}_bucket", ["le"] as List<String>, [bucket] as List<String>, totalEvents))
        }
        result.add(new Sample("${NAME}_count", [] as List<String>, [] as List<String>, totalEvents))
        result.add(new Sample("${NAME}_sum", [] as List<String>, [] as List<String>, sumOfSamples))
        return result
    }

    private static List<Sample> addLabelsToSample(final List<Sample> origin,
                                                  final List<String> labelNames,
                                                  final List<String> labelValues) {
        return origin.collect {
            final Sample newSample =
                new Sample(
                    it.name,
                    (labelNames + it.labelNames) as List<String>,
                    (labelValues + it.labelValues) as List<String>,
                    it.value)
            return newSample
        }
    }

}
