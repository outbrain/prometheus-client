package com.outbrain.swinfra.metrics.exports


import com.outbrain.swinfra.metrics.MetricRegistry
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.lang.management.GarbageCollectorMXBean
import java.util.function.Predicate


class GarbageCollectorMetricsTest extends Specification {

    private GarbageCollectorMXBean gc1 = Mock(GarbageCollectorMXBean)
    private GarbageCollectorMXBean gc2 = Mock(GarbageCollectorMXBean)

    @Subject
    private GarbageCollectorMetric gcMetrics = new GarbageCollectorMetric([gc1, gc2])

    def 'verify metrics registered correctly'() {
        given:
            gc1.getName() >> 'gc1'
            gc1.getCollectionCount() >> 1
            gc1.getCollectionTime() >> 11 * 1000
            gc2.getName() >> 'gc2'
            gc2.getCollectionCount() >> 2
            gc2.getCollectionTime() >> 22 * 1000
            final MetricRegistry registry = new MetricRegistry()
        when:
            gcMetrics.registerMetricsTo(registry)
        then:
            registry.find { it.name == 'jvm_gc_collection_count' }.getValue('gc1') == 1
            registry.find { it.name == 'jvm_gc_collection_count' }.getValue('gc2') == 2
            registry.find { it.name == 'jvm_gc_collection_seconds' }.getValue('gc1') == 11
            registry.find { it.name == 'jvm_gc_collection_seconds' }.getValue('gc2') == 22
    }

    @Unroll
    def 'filter metrics by name expect #expected'() {
        given:
            final MetricRegistry registry = new MetricRegistry()
        when:
            gcMetrics.registerMetricsTo(registry, filter as Predicate)
        then:
            registry.collect {it.name }.sort() == expected
        where:
            filter                                 | expected
                    { name -> false }                             | []
                    { name -> name == 'jvm_gc_collection_count' } | ['jvm_gc_collection_count']
                    { name -> true }                              | ['jvm_gc_collection_count', 'jvm_gc_collection_seconds']
    }
}
