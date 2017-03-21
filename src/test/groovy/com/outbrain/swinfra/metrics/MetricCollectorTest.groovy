package com.outbrain.swinfra.metrics

import spock.lang.Specification

class MetricCollectorTest extends Specification {

    final MetricCollector metricCollector = createMetricCollector()

    def 'i can iterate over the metrics in a collector'() {
        expect:
            [ 'Counter1', 'Counter2' ] as Set == metricCollector.iterator().collect { it.name } as Set
    }

    /*
        Create a sample MetricCollector for usage in the tests
     */
    private static MetricCollector createMetricCollector() {
        final MetricRegistry metricRegistry = new MetricRegistry()

        metricRegistry.getOrRegister(new Counter.CounterBuilder("Counter1", "help").build())
        metricRegistry.getOrRegister(new Counter.CounterBuilder("Counter2", "help").build())

        return new MetricCollector(metricRegistry)
    }
}
