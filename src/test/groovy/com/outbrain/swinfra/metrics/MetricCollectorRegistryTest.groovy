package com.outbrain.swinfra.metrics

import com.outbrain.swinfra.metrics.format.CollectorFormatter
import spock.lang.Specification
import spock.lang.Subject

class MetricCollectorRegistryTest extends Specification {

    @Subject
    MetricCollectorRegistry registry = new MetricCollectorRegistry()

    MetricCollector collector1 = Mock(MetricCollector)
    MetricCollector collector2 = Mock(MetricCollector)
    MetricCollector collector3 = Mock(MetricCollector)

    def 'collector registers to collection'() {
        when:
            registry.register(collector1)
        then:
            registry.iterator().size() == 1
            collector1 == registry.iterator().next()
    }

    def 'collector unregisters from collection'() {
        given:
            registry.register(collector1)
        when:
            registry.unregister(collector1)
        then:
            registry.iterator().size() == 0
    }

    def 'multiple collectors register to collection'() {
        when:
            registry.register(collector1)
            registry.register(collector2)
        then:
            registry.iterator().size() == 2
            [ collector1, collector2 ] as Set == registry.iterator().collect() as Set
    }

    def 'unregister non existing collector does nothing'() {
        given:
            registry.register(collector1)
        when:
            registry.unregister(collector2)
        then:
            registry.iterator().size() == 1
            collector1 == registry.iterator().next()
    }

    def 'iterator iterates over all collector formatters'() {
        given:
            registry.register(collector1)
            registry.register(collector2)
            registry.register(collector3)
            Iterator<CollectorFormatter> iterator = registry.iterator()
            Set<MetricCollector> iterated = [] as Set
        when:
            while (iterator.hasNext()) {
                iterated.add(iterator.next())
            }
        then:
            [collector1, collector2, collector3] as Set == iterated
    }
}
