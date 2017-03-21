package com.outbrain.swinfra.metrics

import spock.lang.Specification

import static com.outbrain.swinfra.metrics.Summary.QUANTILE_LABEL
import static com.outbrain.swinfra.metrics.Summary.SummaryBuilder
import static com.outbrain.swinfra.metrics.timing.TimingMetric.COUNT_SUFFIX
import static com.outbrain.swinfra.metrics.timing.TimingMetric.SUM_SUFFIX

class SummaryTest extends Specification {

    private static final String NAME = "mySummary"
    private static final String SUM_NAME = NAME + SUM_SUFFIX
    private static final String COUNT_NAME = NAME + COUNT_SUFFIX
    private static final String HELP = "HELP"

    private final SampleConsumer sampleConsumer = Mock(SampleConsumer)


    def 'Summary with no labels should return correct samples for newly initialized metric'() {
        given:
            final Summary summary = new SummaryBuilder(NAME, HELP).build()
        when:
            summary.forEachSample(sampleConsumer)
        then:
            1 * sampleConsumer.apply(NAME, 0, [], QUANTILE_LABEL, '0.5')
            1 * sampleConsumer.apply(NAME, 0, [], QUANTILE_LABEL, '0.75')
            1 * sampleConsumer.apply(NAME, 0, [], QUANTILE_LABEL, '0.95')
            1 * sampleConsumer.apply(NAME, 0, [], QUANTILE_LABEL, '0.98')
            1 * sampleConsumer.apply(NAME, 0, [], QUANTILE_LABEL, '0.99')
            1 * sampleConsumer.apply(NAME, 0, [], QUANTILE_LABEL, '0.999')
            1 * sampleConsumer.apply(SUM_NAME, 0, [], null, null)
            1 * sampleConsumer.apply(COUNT_NAME, 0, [], null, null)
    }

    def 'Summary with no labels should return correct samples after some measurements'() {
        given:
            final Summary summary = new SummaryBuilder(NAME, HELP).build()
            1.upto(1000, { summary.observe(it) })
        when:
            summary.forEachSample(sampleConsumer)
        then:
            1 * sampleConsumer.apply(NAME, 500, [], QUANTILE_LABEL, '0.5')
            1 * sampleConsumer.apply(NAME, 750, [], QUANTILE_LABEL, '0.75')
            1 * sampleConsumer.apply(NAME, 950, [], QUANTILE_LABEL, '0.95')
            1 * sampleConsumer.apply(NAME, 980, [], QUANTILE_LABEL, '0.98')
            1 * sampleConsumer.apply(NAME, 990, [], QUANTILE_LABEL, '0.99')
            1 * sampleConsumer.apply(NAME, 999, [], QUANTILE_LABEL, '0.999')
            1 * sampleConsumer.apply(SUM_NAME, (1..1000).sum(), [], null, null)
            1 * sampleConsumer.apply(COUNT_NAME, 1000, [], null, null)
    }

    def 'Summary with labels should return correct samples after some measurements'() {
        final List<String> labelNames = ['label1', 'label2']
        final List<String> labelValues1 = ['value1', 'value2']
        final List<String> labelValues2 = ['value3', 'value4']
        given:
            final Summary summary = new SummaryBuilder(NAME, HELP)
                .withReservoir().withExponentiallyDecayingReservoir(1028, 0.15) //Use a "custom" reservoir just for the sake of having compilation error if this is not working
                .withLabels(labelNames as String[])
                .build()
            1.upto(1000, { summary.observe(it, labelValues1 as String[]) })
            1.upto(1000, { summary.observe(it, labelValues2 as String[]) })

        when:
            summary.forEachSample(sampleConsumer)
        then:
            1 * sampleConsumer.apply(NAME, 500, labelValues1, QUANTILE_LABEL, '0.5')
            1 * sampleConsumer.apply(NAME, 750, labelValues1, QUANTILE_LABEL, '0.75')
            1 * sampleConsumer.apply(NAME, 950, labelValues1, QUANTILE_LABEL, '0.95')
            1 * sampleConsumer.apply(NAME, 980, labelValues1, QUANTILE_LABEL, '0.98')
            1 * sampleConsumer.apply(NAME, 990, labelValues1, QUANTILE_LABEL, '0.99')
            1 * sampleConsumer.apply(NAME, 999, labelValues1, QUANTILE_LABEL, '0.999')
            1 * sampleConsumer.apply(SUM_NAME, (1..1000).sum(), labelValues1, null, null)
            1 * sampleConsumer.apply(COUNT_NAME, 1000, labelValues1, null, null)
            1 * sampleConsumer.apply(NAME, 500, labelValues2, QUANTILE_LABEL, '0.5')
            1 * sampleConsumer.apply(NAME, 750, labelValues2, QUANTILE_LABEL, '0.75')
            1 * sampleConsumer.apply(NAME, 950, labelValues2, QUANTILE_LABEL, '0.95')
            1 * sampleConsumer.apply(NAME, 980, labelValues2, QUANTILE_LABEL, '0.98')
            1 * sampleConsumer.apply(NAME, 990, labelValues2, QUANTILE_LABEL, '0.99')
            1 * sampleConsumer.apply(NAME, 999, labelValues2, QUANTILE_LABEL, '0.999')
            1 * sampleConsumer.apply(SUM_NAME, (1..1000).sum(), labelValues2, null, null)
            1 * sampleConsumer.apply(COUNT_NAME, 1000, labelValues2, null, null)
            0 * sampleConsumer.apply(_, _, _, _, _)
    }

    def 'Timer should add one sample for each time it is started and then stopped'() {
        final TestClock clock = new TestClock()
        given:
            final double sum = (1..1000).sum() as double
            final Summary summary = new SummaryBuilder(NAME, HELP).withClock(clock).build()
            //Make a 1000 measurements with values 1, 2, 3, ... 1000
            1.upto(1000, {
                clock.setTick(0)
                final com.outbrain.swinfra.metrics.timing.Timer timer = summary.startTimer()
                clock.setTick(it)
                timer.stop()
            })
        when:
            summary.forEachSample(sampleConsumer)
        then:
            1 * sampleConsumer.apply(NAME, 500, [], QUANTILE_LABEL, '0.5')
            1 * sampleConsumer.apply(NAME, 750, [], QUANTILE_LABEL, '0.75')
            1 * sampleConsumer.apply(NAME, 950, [], QUANTILE_LABEL, '0.95')
            1 * sampleConsumer.apply(NAME, 980, [], QUANTILE_LABEL, '0.98')
            1 * sampleConsumer.apply(NAME, 990, [], QUANTILE_LABEL, '0.99')
            1 * sampleConsumer.apply(NAME, 999, [], QUANTILE_LABEL, '0.999')
            1 * sampleConsumer.apply(SUM_NAME, sum, [], null, null)
            1 * sampleConsumer.apply(COUNT_NAME, 1000, [], null, null)
    }
}
