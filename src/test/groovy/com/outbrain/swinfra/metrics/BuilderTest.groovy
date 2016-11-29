package com.outbrain.swinfra.metrics

import spock.lang.Specification
import spock.lang.Unroll

import static com.outbrain.swinfra.metrics.Counter.CounterBuilder;

/*
This is a test for the AbstractMetricBuilder class, since all builders should inherit from it
I chose the CounterBuilder as a specimen for the tests
 */
class BuilderTest extends Specification {

    @Unroll
    def 'builder should throw exception on  name #name help #help and labels #labels'() {
        given:
            AbstractMetricBuilder builder = new CounterBuilder(name, help)
            if (labels) {
                builder = builder.withLabels(labels as String[])
            }
        when:
            builder.build()
        then:
            def ex = thrown IllegalArgumentException
            ex.message.contains(expectedInErrorMessage)
        where:
            name        | help        | labels          | expectedInErrorMessage
            null        | "some help" | null            | "name"
            "   "       | "some help" | null            | "name"
            "some name" | null        | null            | "help"
            "some name" | "  "        | null            | "help"
            "some name" | "some help" | ["label", null] | "Label"
            "some name" | "some help" | ["label", "  "] | "Label"

    }
}
