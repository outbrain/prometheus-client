package com.outbrain.swinfra.metrics.format.text

import com.outbrain.swinfra.metrics.Counter
import com.outbrain.swinfra.metrics.MetricCollector
import spock.lang.Specification
import spock.lang.Subject

class TextFormatterTest extends Specification {

    @Subject TextFormatter formatter

    def 'appends a collector metric samples in text format to given output buffer'() {
        given:
            StringBuilder output = new StringBuilder()
            MetricCollector collector = Mock(MetricCollector)
            Counter counter1 = new Counter.CounterBuilder('Counter1', 'help').build()
            counter1.inc(17)
            Counter counter2 = new Counter.CounterBuilder('Counter2', 'help').withLabels('label')build()
            counter2.inc(19, 'labelValue')
            collector.iterator() >> [counter1, counter2].iterator()
            collector.staticLabels >> ['a':'b']

            formatter = new TextFormatter(collector)
        when:
            formatter.formatTo(output)
        then:
            '''# HELP Counter1 help
# TYPE Counter1 counter
Counter1{a="b",} 17.0
# HELP Counter2 help
# TYPE Counter2 counter
Counter2{a="b",label="labelValue",} 19.0
''' == output.toString()
    }
}
