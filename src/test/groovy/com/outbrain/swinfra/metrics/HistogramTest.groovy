package com.outbrain.swinfra.metrics

import com.outbrain.swinfra.metrics.children.MetricData
import com.outbrain.swinfra.metrics.data.HistogramData
import com.outbrain.swinfra.metrics.data.MetricDataConsumer
import com.outbrain.swinfra.metrics.timing.Timer
import spock.lang.Specification
import spock.lang.Unroll

import java.util.function.Consumer

import static com.outbrain.swinfra.metrics.Histogram.Buckets
import static com.outbrain.swinfra.metrics.Histogram.HistogramBuilder
import static com.outbrain.swinfra.metrics.utils.MetricType.HISTOGRAM

class HistogramTest extends Specification {

    private static final String NAME = 'myHisto'
    private static final String HELP = 'HELP'

    private final Consumer<MetricData<Buckets>> consumer = Mock(Consumer)
    private final MetricDataConsumer metricDataConsumer = Mock(MetricDataConsumer)

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
            histogram.forEachChild(consumer)
        then:
            1 * consumer.accept({ it.metric.values.sum == 0 && it.labelValues == [] } as MetricData<Buckets>)
            0 * consumer.accept(_)
    }

    def 'consumeHistogram should be called for each metric data'() {
        given:
            final Histogram histogram = new HistogramBuilder(NAME, HELP).withBuckets(1, 10, 100).build()
            histogram.observe(1)
            histogram.observe(5)
            histogram.observe(5)
            histogram.observe(50)
            histogram.observe(50)
            histogram.observe(150)
        when:
            histogram.forEachMetricData(metricDataConsumer)
        then:
            1 * metricDataConsumer.consumeHistogram(histogram, [], {
                it.count == 6 &&
                it.sum ==  1 + 5 + 5 + 50 + 50 + 150 &&
                it.buckets == [1, 3, 5, 6]
            } as HistogramData)
            0 * metricDataConsumer._
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
            histogram.forEachMetricData(metricDataConsumer)
        then:
            1 * metricDataConsumer.consumeHistogram(histogram, ['val1'], {
                it.count == 6 &&
                        it.sum ==  1 + 5 + 5 + 50 + 50 + 150 &&
                        it.buckets == [1, 3, 5, 6]
            } as HistogramData)
            1 * metricDataConsumer.consumeHistogram(histogram, ['val2'], {
                it.count == 6 &&
                        it.sum ==  1 + 5 + 5 + 50 + 50 + 150 &&
                        it.buckets == [1, 3, 5, 6]
            } as HistogramData)
            1 * metricDataConsumer.consumeHistogram(histogram, ['val3'], {
                it.count == 6 &&
                        it.sum ==  1 + 5 + 5 + 50 + 50 + 150 &&
                        it.buckets == [1, 3, 5, 6]
            } as HistogramData)
            0 * metricDataConsumer._
    }

    def 'samples should contain non cummulative buckets upon nonCommulativeBuckets definition'() {
        given:
            final Histogram histogram = new HistogramBuilder(NAME, HELP)
                    .withBuckets(1, 10, 100)
                    .nonCummulativeBuckets()
                    .build()

            histogram.observe(1)
            histogram.observe(5)
            histogram.observe(5)
            histogram.observe(50)
            histogram.observe(50)
            histogram.observe(150)

        when:
            histogram.forEachMetricData(metricDataConsumer)

        then:
            1 * metricDataConsumer.consumeHistogram(histogram, [], {
                it.count == 6 &&
                        it.sum ==  1 + 5 + 5 + 50 + 50 + 150 &&
                        it.buckets == [1, 2, 2, 1]
            } as HistogramData)
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
            histogram.forEachChild(consumer)
        then:
            1 * consumer.accept({
                    it.metric.values.sum == 0.0 &&
                    it.metric.values.buckets == [0, 0] &&
                    it.metric.values.bucketUpperBounds == [0.5d, Double.POSITIVE_INFINITY] } as MetricData<Buckets>)
            0 * consumer.accept(_)
    }

    def "A Histogram with equal width buckets should return the correct buckets"() {
        given:
            final Histogram histogram = new HistogramBuilder(NAME, HELP).withEqualWidthBuckets(0.5, 1, 4).build()
        when:
            histogram.forEachChild(consumer)
        then:
            1 * consumer.accept({
                it.metric.values.sum == 0.0 &&
                        it.metric.values.buckets == [0, 0, 0, 0, 0] &&
                        it.metric.values.bucketUpperBounds == [0.5d, 1.5d, 2.5d, 3.5d, Double.POSITIVE_INFINITY] } as MetricData<Buckets>)
            0 * consumer.accept(_)
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
            histogram.forEachChild(consumer)
        then:
            1 * consumer.accept({
                it.metric.values.sum == 6.0 &&
                        it.metric.values.buckets == [1, 2, 3] &&
                        it.metric.values.bucketUpperBounds == [1.5d, 2.5d, Double.POSITIVE_INFINITY] } as MetricData<Buckets>)
            0 * consumer.accept(_)
    }

    def 'Histogram without labels should throw an exception when attempting to observe a value with labels'() {
        given:
            final Histogram histogram = new HistogramBuilder(NAME, HELP).build()

        when:
            histogram.observe(1, "labelValue")

        then:
            thrown(IllegalArgumentException.class)
    }

    @Unroll
    def 'Histogram with labels should throw an exception when attempting to observe a value with labels #labels'() {
        given:
            final Histogram histogram = new HistogramBuilder(NAME, HELP).withLabels("l1", "l2").build()

        when:
            histogram.observe(1, labels as String[])

        then:
            thrown(IllegalArgumentException.class)

        where:
            labels << [[], ["v1", ""], ["v1", "v2", "v3"]]
    }
}
