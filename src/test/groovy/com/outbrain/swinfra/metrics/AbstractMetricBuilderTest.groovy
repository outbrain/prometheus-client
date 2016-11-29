package com.outbrain.swinfra.metrics

import spock.lang.Specification
import spock.lang.Unroll

class AbstractMetricBuilderTest extends Specification {

    @Unroll
    def 'builder should throw exception on  name #name help #help and labels #labels'() {
        given:
            AbstractMetricBuilder builder = new MyBuilder(name, help)
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

    private class MyBuilder extends AbstractMetricBuilder {

        MyBuilder(String name, String help) {
            super(name, help)
        }

        @Override
        protected AbstractMetric create(String fullName, String help, String[] labelNames) {
            return null
        }
    }
}
