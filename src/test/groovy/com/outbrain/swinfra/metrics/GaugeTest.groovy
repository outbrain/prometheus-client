package com.outbrain.swinfra.metrics

import spock.lang.Specification

import java.util.function.DoubleSupplier

import static com.outbrain.swinfra.metrics.Gauge.GaugeBuilder

class GaugeTest extends Specification {

    private static final String NAME = "NAME"
    private static final String HELP = "HELP"


    private final SampleConsumer sampleConsumer = Mock(SampleConsumer)

    def 'Gauge should return the correct samples without labels'() {
        final double expectedValue = 239487234

        given:
            final Gauge gauge = new GaugeBuilder(NAME, HELP)
                .withValueSupplier({ expectedValue } as DoubleSupplier)
                .build()

        when:
            gauge.forEachSample(sampleConsumer)
        then:
            1 * sampleConsumer.apply(NAME, expectedValue, [], null, null)
    }

    def 'Gauge should return the correct samples with labels'() {
        given:
            final String[] labelNames = ["label1", "label2"]
            final String[] labelValues1 = ["val1", "val2"]
            final double expectedValue1 = 239487
            final String[] labelValues2 = ["val2", "val3"]
            final double expectedValue2 = 181239813
            final Gauge gauge = new GaugeBuilder(NAME, HELP)
                .withLabels(labelNames)
                .withValueSupplier({ expectedValue1 } as DoubleSupplier, labelValues1)
                .withValueSupplier({ expectedValue2 } as DoubleSupplier, labelValues2)
                .build()
        when:
            gauge.forEachSample(sampleConsumer)
        then:
            1 * sampleConsumer.apply(NAME, expectedValue1, labelValues1 as List, null, null)
            1 * sampleConsumer.apply(NAME, expectedValue2, labelValues2 as List, null, null)
    }

    def 'GaugeBuilder should throw an exception on null value supplier'() {
        when:
            new GaugeBuilder(NAME, HELP)
                .withValueSupplier(null, "val1", "val2")
                .build()

        then:
            def ex = thrown NullPointerException
            ex.message.contains("value supplier")
    }

    def 'GaugeBuilder should throw an exception on invalid length label values'() {
        when:
            new GaugeBuilder(NAME, HELP)
                .withLabels("label1", "label2")
                .withValueSupplier({ 0 } as DoubleSupplier, "val1", "val2", "extraVal")
                .build()

        then:
            def ex = thrown IllegalArgumentException
            ex.message.contains("does not contain the expected amount 2")
    }

    def 'GaugeBuilder should throw an exception when not all labels are given values'() {
        when:
            new GaugeBuilder(NAME, HELP)
                .withLabels("label1", "label2")
                .withValueSupplier({ 0 } as DoubleSupplier, "val1")
                .build()

        then:
            def ex = thrown IllegalArgumentException
            ex.message.contains("does not contain the expected amount 2")
    }
}
