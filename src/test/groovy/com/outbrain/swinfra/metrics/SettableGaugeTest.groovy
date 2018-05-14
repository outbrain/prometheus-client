package com.outbrain.swinfra.metrics

import com.outbrain.swinfra.metrics.data.MetricDataConsumer
import spock.lang.Specification

import java.util.function.Consumer

import static com.outbrain.swinfra.metrics.SettableGauge.SettableGaugeBuilder

class SettableGaugeTest extends Specification {

    private static final String NAME = "NAME"
    private static final String HELP = "HELP"

    private final MetricDataConsumer metricDataConsumer = Mock(MetricDataConsumer)
    private final Consumer<SettableGauge.SettableDoubleSupplier> consumer = Mock(Consumer)

    def 'consumeGauge will be called by MetricDataConsumer for each child'() {
        given:
            final SettableGauge gauge = new SettableGaugeBuilder(NAME, HELP).withLabels('a').build()
        when:
            gauge.set(1.1D, 'A')
            gauge.set(2.2D, 'B')
            gauge.forEachMetricData(metricDataConsumer)
        then:
            1 * metricDataConsumer.consumeGauge(gauge, ['A'], 1.1D)
            1 * metricDataConsumer.consumeGauge(gauge, ['B'], 2.2D)
            0 * metricDataConsumer._
    }

    def 'SettableGauge should have the correct value'() {
        final double expectedValue = 239487234

        given:
            final SettableGauge settableGauge = new SettableGaugeBuilder(NAME, HELP).build()

        when:
            settableGauge.set(expectedValue)

        then:
            settableGauge.getValue() == expectedValue
    }

    def 'SettableGauge should have the correct value with labels'() {
        final double expectedValue = 239487234
        final String labelValue = "value1"

        given:
            final SettableGauge settableGauge = new SettableGaugeBuilder(NAME, HELP)
                    .withLabels("label1")
                    .build()

        when:
            settableGauge.set(expectedValue, labelValue)

        then:
            settableGauge.getValue(labelValue) == expectedValue
    }

    def "SettableGauge should return empty samples when nothing was set"() {
        given:

            final SettableGauge settableGauge = new SettableGaugeBuilder(NAME, HELP).build()
        when:
            settableGauge.forEachChild(consumer)
        then:
            1 * consumer.accept({ it.metric.getAsDouble() == 0d && it.labelValues == [] })
    }

    def "SettableGauge with labels should return no samples when nothing was set"() {
        given:
            final SettableGauge settableGauge = new SettableGaugeBuilder(NAME, HELP).withLabels("label1", "label2").
                    build()
        when:
            settableGauge.forEachChild(consumer)
        then:
            0 * consumer._
    }

    def "SettableGauge should return the correct value after it was set"() {
        given:
            final SettableGauge settableGauge = new SettableGaugeBuilder(NAME, HELP).build()
            settableGauge.set(1d)
        when:
            settableGauge.forEachChild(consumer)
        then:
            1 * consumer.accept({ it.metric.getAsDouble() == 1d && it.labelValues == [] })

        when:
            settableGauge.set(2d)
            settableGauge.forEachChild(consumer)
        then:
            1 * consumer.accept({it.metric.getAsDouble() == 2d && it.labelValues == [] })

    }

    def "SettableGauge with labels should return the correct value after it was set"() {
        given:
            final SettableGauge settableGauge = new SettableGaugeBuilder(NAME, HELP).withLabels("label1").build()
            settableGauge.set(1d, "value1")
            settableGauge.set(2d, "value2")
        when:
            settableGauge.forEachChild(consumer)
        then:
            1 * consumer.accept({it.metric.getAsDouble() == 1d && it.labelValues == ['value1'] })
            1 * consumer.accept({it.metric.getAsDouble() == 2d && it.labelValues == ['value2'] })
            0 * consumer.accept(_)
    }

    def 'SettableGauge without labels should throw an exception when attempting to set with labels'() {
        given:
        final SettableGauge settableGauge = new SettableGaugeBuilder(NAME, HELP).build()

        when:
        settableGauge.set(0, "labelValue")

        then:
        thrown(IllegalArgumentException.class)
    }

    def 'SettableGauge with labels should throw an exception when attempting to set wrong number of labels'() {
        given:
        final SettableGauge settableGauge = new SettableGaugeBuilder(NAME, HELP).withLabels("label1").build()

        when:
        settableGauge.set(0, "labelValue1", "labelValue2")

        then:
        thrown(IllegalArgumentException.class)
    }

}
