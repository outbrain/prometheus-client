package com.outbrain.swinfra.metrics

import spock.lang.Specification
import spock.lang.Unroll

import static com.outbrain.swinfra.metrics.Counter.CounterBuilder

class CounterTest extends Specification {

    private static final String NAME = "NAME"
    private static final String HELP = "HELP"


    private final SampleConsumer sampleConsumer = Mock(SampleConsumer)

    @Unroll
    def 'Counter should return #expectedValue after incrementing #increment times'() {
        given:
            final Counter counter = new CounterBuilder(NAME, HELP).build()

        when:
            if (increment > 0) {
                1.upto(increment, { counter.inc() })
            }

        then:
            counter.getValue() == expectedValue

        where:
            increment | expectedValue
            0         | 0
            1         | 1
            10        | 10
    }

    @Unroll
    def 'Counter should return #expectedValue after incrementing by #incrementBy 10 times'() {
        given:
            final Counter counter = new CounterBuilder(NAME, HELP).build()

        when:
            if (incrementBy > 0) {
                1.upto(10, { counter.inc(incrementBy) })
            }

        then:
            counter.getValue() == expectedValue

        where:
            incrementBy | expectedValue
            0           | 0
            1           | 10
            10          | 100
    }

    def 'Counter should return the correct samples with labels defined'() {
        final String[] labelValues1 = ["val1", "val2"]
        final String[] labelValues2 = ["val3", "val4"]

        given:
            final Counter counter = new CounterBuilder(NAME, HELP)
                .withLabels("label1", "label2")
                .build()

            counter.inc(5, labelValues1)
            counter.inc(6, labelValues2)
        when:
            counter.forEachSample(sampleConsumer)
        then:
            1 * sampleConsumer.apply(NAME, 5, labelValues1 as List, null, null)
            1 * sampleConsumer.apply(NAME, 6, labelValues2 as List, null, null)
    }

    def 'Counter should return the correct samples with subsystem defined'() {
        final String subsystem = "myNamespace"
        final String fullName = subsystem + "_" + NAME

        given:
            final Counter counter = new CounterBuilder(NAME, HELP)
                .withSubsystem(subsystem)
                .build()

        when:
            counter.forEachSample(sampleConsumer)
        then:
            1 * sampleConsumer.apply(fullName, 0, [], null, null)
    }

    def 'Counter should return the correct samples with namespace and subsystem defined'() {
        final String namespace = "myNamespace"
        final String subsystem = "mySubsystem"
        final String fullName = namespace + "_" + subsystem + "_" + NAME

        given:
            final Counter counter = new CounterBuilder(NAME, HELP)
                .withNamespace(namespace)
                .withSubsystem(subsystem)
                .build()
        when:
            counter.forEachSample(sampleConsumer)
        then:
            1 * sampleConsumer.apply(fullName, 0, [], null, null)
    }

    def 'Counter should return the correct samples with namespace defined'() {
        final String namespace = "mySubsystem"
        final String fullName = namespace + "_" + NAME

        given:
            final Counter counter = new CounterBuilder(NAME, HELP)
                .withNamespace(namespace)
                .build()
        when:
            counter.forEachSample(sampleConsumer)
        then:
            1 * sampleConsumer.apply(fullName, 0, [], null, null)
    }
}
