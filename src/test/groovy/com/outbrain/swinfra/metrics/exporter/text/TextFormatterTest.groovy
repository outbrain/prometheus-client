package com.outbrain.swinfra.metrics.exporter.text

import com.outbrain.swinfra.metrics.*
import spock.lang.Specification
import spock.lang.Subject

import java.util.function.DoubleSupplier

class TextFormatterTest extends Specification {

    @Subject TextFormatter formatter
    private MetricRegistry registry = Mock(MetricRegistry)

    def 'appends counter samples in text format to given output buffer'() {
        given:
            ByteArrayOutputStream output = new ByteArrayOutputStream()
            Counter counter1 = new Counter.CounterBuilder('Counter1', 'help').build()
            counter1.inc(17)
            Counter counter2 = new Counter.CounterBuilder('Counter2', 'help').withLabels('label').build()
            counter2.inc(19, 'labelValue')
            registry.iterator() >> [counter1, counter2].iterator()
            registry.staticLabels >> ['a':'b']

            formatter = new TextFormatter(Collections.singleton(registry))
        when:
            formatter.exportTo(output)
        then:
            output.toString() == '''# HELP Counter1 help
# TYPE Counter1 counter
Counter1{a="b",} 17.0
# HELP Counter2 help
# TYPE Counter2 counter
Counter2{a="b",label="labelValue",} 19.0
'''
    }

    def 'appends a gauge samples in text format to given output buffer'() {
        given:
            ByteArrayOutputStream output = new ByteArrayOutputStream()
            Gauge gauge1 = new Gauge.GaugeBuilder('Gauge1', 'help').
                    withValueSupplier({ 17d } as DoubleSupplier).build()

            Gauge gauge2 = new Gauge.GaugeBuilder('Gauge2', 'help').
                    withLabels('label').
                    withValueSupplier({ 19d } as DoubleSupplier, 'labelValue')build()

            SettableGauge settableGauge = new SettableGauge.SettableGaugeBuilder('SettableGauge', 'help').
                    build()
            settableGauge.set(23d)
            registry.iterator() >> [gauge1, gauge2, settableGauge].iterator()
            registry.staticLabels >> ['a':'b']

            formatter = new TextFormatter(Collections.singleton(registry))
        when:
            formatter.exportTo(output)
        then:
            output.toString() == '''# HELP Gauge1 help
# TYPE Gauge1 gauge
Gauge1{a="b",} 17.0
# HELP Gauge2 help
# TYPE Gauge2 gauge
Gauge2{a="b",label="labelValue",} 19.0
# HELP SettableGauge help
# TYPE SettableGauge gauge
SettableGauge{a="b",} 23.0
'''
    }

    def 'appends summary samples in text format to given output buffer'() {
        given:
            ByteArrayOutputStream output = new ByteArrayOutputStream()
            Summary summary1 = new Summary.SummaryBuilder('Summary1', 'help').build()
            summary1.observe(17)
            Summary summary2 = new Summary.SummaryBuilder('Summary2', 'help').withLabels('label').build()
            summary2.observe(19, 'labelValue')
            registry.iterator() >> [summary1, summary2].iterator()
            registry.staticLabels >> ['a':'b']

            formatter = new TextFormatter(Collections.singleton(registry))
        when:
            formatter.exportTo(output)
        then:
            output.toString() == '''# HELP Summary1 help
# TYPE Summary1 summary
Summary1{a="b",quantile="0.5",} 17.0
Summary1{a="b",quantile="0.75",} 17.0
Summary1{a="b",quantile="0.95",} 17.0
Summary1{a="b",quantile="0.98",} 17.0
Summary1{a="b",quantile="0.99",} 17.0
Summary1{a="b",quantile="0.999",} 17.0
Summary1_count{a="b",} 1.0
Summary1_sum{a="b",} 17.0
# HELP Summary2 help
# TYPE Summary2 summary
Summary2{a="b",label="labelValue",quantile="0.5",} 19.0
Summary2{a="b",label="labelValue",quantile="0.75",} 19.0
Summary2{a="b",label="labelValue",quantile="0.95",} 19.0
Summary2{a="b",label="labelValue",quantile="0.98",} 19.0
Summary2{a="b",label="labelValue",quantile="0.99",} 19.0
Summary2{a="b",label="labelValue",quantile="0.999",} 19.0
Summary2_count{a="b",label="labelValue",} 1.0
Summary2_sum{a="b",label="labelValue",} 19.0
'''
    }

    def 'appends histogram samples in text format to given output buffer'() {
        given:
            ByteArrayOutputStream output = new ByteArrayOutputStream()
            Histogram histogram1 = new Histogram.HistogramBuilder('Histogram1', 'help').build()
            histogram1.observe(0.17)
            histogram1.observe(17)
            Histogram histogram2 = new Histogram.HistogramBuilder('Histogram2', 'help').
                    withLabels('label').withBuckets(1, 10, 100).build()
            histogram2.observe(0.19, 'labelValue')
            histogram2.observe(19, 'labelValue')
            registry.iterator() >> [histogram1, histogram2].iterator()
            registry.staticLabels >> ['a':'b']

            formatter = new TextFormatter(Collections.singleton(registry))
        when:
            formatter.exportTo(output)
        then:
            output.toString() == '''# HELP Histogram1 help
# TYPE Histogram1 histogram
Histogram1_bucket{a="b",le="0.005",} 0.0
Histogram1_bucket{a="b",le="0.01",} 0.0
Histogram1_bucket{a="b",le="0.025",} 0.0
Histogram1_bucket{a="b",le="0.05",} 0.0
Histogram1_bucket{a="b",le="0.075",} 0.0
Histogram1_bucket{a="b",le="0.1",} 0.0
Histogram1_bucket{a="b",le="0.25",} 1.0
Histogram1_bucket{a="b",le="0.5",} 1.0
Histogram1_bucket{a="b",le="0.75",} 1.0
Histogram1_bucket{a="b",le="1.0",} 1.0
Histogram1_bucket{a="b",le="2.5",} 1.0
Histogram1_bucket{a="b",le="5.0",} 1.0
Histogram1_bucket{a="b",le="7.5",} 1.0
Histogram1_bucket{a="b",le="10.0",} 1.0
Histogram1_bucket{a="b",le="+Inf",} 2.0
Histogram1_count{a="b",} 2.0
Histogram1_sum{a="b",} 17.17
# HELP Histogram2 help
# TYPE Histogram2 histogram
Histogram2_bucket{a="b",label="labelValue",le="1.0",} 1.0
Histogram2_bucket{a="b",label="labelValue",le="10.0",} 1.0
Histogram2_bucket{a="b",label="labelValue",le="100.0",} 2.0
Histogram2_bucket{a="b",label="labelValue",le="+Inf",} 2.0
Histogram2_count{a="b",label="labelValue",} 2.0
Histogram2_sum{a="b",label="labelValue",} 19.19
'''
    }
}
