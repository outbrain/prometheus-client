package com.outbrain.swinfra.metrics

import com.outbrain.swinfra.metrics.timing.Timer
import spock.lang.Specification
import spock.lang.Unroll

import static com.outbrain.swinfra.metrics.Histogram.*
import static com.outbrain.swinfra.metrics.timing.TimingMetric.COUNT_SUFFIX
import static com.outbrain.swinfra.metrics.timing.TimingMetric.SUM_SUFFIX
import static com.outbrain.swinfra.metrics.utils.MetricType.HISTOGRAM

class HistogramTest extends Specification {

    private static final String NAME = "myHisto"
    private static final String HELP = "HELP"

    private final SampleConsumer sampleConsumer = Mock(SampleConsumer)

    def 'Histogram should return the correct type'() {
        given:
            final Histogram histogram = new HistogramBuilder(NAME, HELP).build()

        expect:
            histogram.getType() == HISTOGRAM
    }

    def 'Newly created histogram, with no specific buckets, should contain the default buckets'() {
        given:
            final Histogram histogram = new HistogramBuilder(NAME, HELP).build()
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
            1 * sampleConsumer.apply(NAME+SUM_SUFFIX, 0, [], null, null)
            1 * sampleConsumer.apply(NAME+COUNT_SUFFIX, 0, [], null, null)

    }

    def 'Histogram with defined buckets should return samples relevant for these buckets'() {
        given:
            final Histogram histogram = new HistogramBuilder(NAME, HELP).withBuckets(1, 10, 100).build()
            histogram.observe(1)
            histogram.observe(5)
            histogram.observe(5)
            histogram.observe(50)
            histogram.observe(50)
            histogram.observe(150)

        when:
            histogram.forEachSample(sampleConsumer)
        then:
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 1, [], BUCKET_LABEL, '1.0')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 3, [], BUCKET_LABEL, '10.0')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 5, [], BUCKET_LABEL, '100.0')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 6, [], BUCKET_LABEL, '+Inf')
            1 * sampleConsumer.apply(NAME+SUM_SUFFIX, 1 + 5 + 5 + 50 + 50 + 150, [], null, null)
            1 * sampleConsumer.apply(NAME+COUNT_SUFFIX, 6, [], null, null)
    }

    def 'Histogram with defined buckets and labels should return correct samples with correct lables'() {
        given:
            final String labelName = 'lab1'
            final List<String> labelValues = ['val1', 'val2', 'val3']
            final Histogram histogram = new HistogramBuilder(NAME, HELP)
                .withLabels(labelName)
                .withBuckets(1, 10, 100)
                .build()
            labelValues.each {
                histogram.observe(1, it)
                histogram.observe(5, it)
                histogram.observe(5, it)
                histogram.observe(50, it)
                histogram.observe(50, it)
                histogram.observe(150, it)
            }
        when:
            histogram.forEachSample(sampleConsumer)
        then:
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 1, ['val1'], BUCKET_LABEL, '1.0')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 3, ['val1'], BUCKET_LABEL, '10.0')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 5, ['val1'], BUCKET_LABEL, '100.0')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 6, ['val1'], BUCKET_LABEL, '+Inf')
            1 * sampleConsumer.apply(NAME+SUM_SUFFIX, 1 + 5 + 5 + 50 + 50 + 150, ['val1'], null, null)
            1 * sampleConsumer.apply(NAME+COUNT_SUFFIX, 6, ['val1'], null, null)
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 1, ['val2'], BUCKET_LABEL, '1.0')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 3, ['val2'], BUCKET_LABEL, '10.0')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 5, ['val2'], BUCKET_LABEL, '100.0')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 6, ['val2'], BUCKET_LABEL, '+Inf')
            1 * sampleConsumer.apply(NAME+SUM_SUFFIX, 1 + 5 + 5 + 50 + 50 + 150, ['val2'], null, null)
            1 * sampleConsumer.apply(NAME+COUNT_SUFFIX, 6, ['val2'], null, null)
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 1, ['val3'], BUCKET_LABEL, '1.0')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 3, ['val3'], BUCKET_LABEL, '10.0')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 5, ['val3'], BUCKET_LABEL, '100.0')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 6, ['val3'], BUCKET_LABEL, '+Inf')
            1 * sampleConsumer.apply(NAME+SUM_SUFFIX, 1 + 5 + 5 + 50 + 50 + 150, ['val3'], null, null)
            1 * sampleConsumer.apply(NAME+COUNT_SUFFIX, 6, ['val3'], null, null)
            0 * sampleConsumer.apply(_,_,_,_,_)
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
            final Histogram histogram = new HistogramBuilder(NAME, HELP).withEqualWidthBuckets(0.5, 1, 1).build()
        when:
            histogram.forEachSample(sampleConsumer)
        then:
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '0.5')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '+Inf')
            0 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, _, _, BUCKET_LABEL, _)
    }

    def "A Histogram with equal width buckets should return the correct buckets"() {
        given:
            final Histogram histogram = new HistogramBuilder(NAME, HELP).withEqualWidthBuckets(0.5, 1, 4).build()
        when:
            histogram.forEachSample(sampleConsumer)
        then:
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '0.5')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '1.5')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '2.5')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '3.5')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, '+Inf')
            0 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 0, [], BUCKET_LABEL, _)
    }

    def "A timer should add the measured samples to the histogram"() {
        final TestClock clock = new TestClock()
        given:
            final Histogram histogram = new HistogramBuilder(NAME, HELP).withClock(clock).withBuckets(1.5, 2.5).build()
            [1, 2, 3].each {
                clock.setTick(0)
                final Timer timer = histogram.startTimer()
                clock.setTick(it)
                timer.stop()
            }
        when:
            histogram.forEachSample(sampleConsumer)
        then:
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 1, [], BUCKET_LABEL, '1.5')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 2, [], BUCKET_LABEL, '2.5')
            1 * sampleConsumer.apply(NAME+SAMPLE_NAME_BUCKET_SUFFIX, 3, [], BUCKET_LABEL, '+Inf')
            1 * sampleConsumer.apply(NAME+SUM_SUFFIX, 6, [], null, null)
            1 * sampleConsumer.apply(NAME+COUNT_SUFFIX, 3, [], null, null)
    }
}
