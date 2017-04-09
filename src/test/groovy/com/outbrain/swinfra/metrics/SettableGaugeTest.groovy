package com.outbrain.swinfra.metrics

import com.outbrain.swinfra.metrics.samples.SampleCreator
import com.outbrain.swinfra.metrics.samples.StaticLablesSampleCreator
import io.prometheus.client.Collector.MetricFamilySamples
import io.prometheus.client.Collector.MetricFamilySamples.Sample
import spock.lang.Specification

import static com.outbrain.swinfra.metrics.SettableGauge.SettableGaugeBuilder
import static io.prometheus.client.Collector.Type.GAUGE

class SettableGaugeTest extends Specification {

    private static final String NAME = "NAME"
    private static final String HELP = "HELP"
    private static final SampleCreator sampleCreator = new StaticLablesSampleCreator([:])

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
            final List<Sample> samples = [new Sample(NAME, [], [], 0d)]
            final MetricFamilySamples metricFamilySamples = new MetricFamilySamples(NAME, GAUGE, HELP, samples)

        when:
            final SettableGauge settableGauge = new SettableGaugeBuilder(NAME, HELP).build()

        then:
            settableGauge.getSample(sampleCreator) == metricFamilySamples
    }

    def "SettableGauge with labels should return no samples when nothing was set"() {
        given:
            final MetricFamilySamples metricFamilySamples = new MetricFamilySamples(NAME, GAUGE, HELP, [])

        when:
            final SettableGauge settableGauge = new SettableGaugeBuilder(NAME, HELP).withLabels("label1", "label2").
                    build()

        then:
            settableGauge.getSample(sampleCreator) == metricFamilySamples
    }

    def "SettableGauge should return the correct value after it was set"() {
        given:
            final List<Sample> samples1 = [new Sample(NAME, [], [], 1d)]
            final MetricFamilySamples metricFamilySamples1 = new MetricFamilySamples(NAME, GAUGE, HELP, samples1)

            final List<Sample> samples2 = [new Sample(NAME, [], [], 2d)]
            final MetricFamilySamples metricFamilySamples2 = new MetricFamilySamples(NAME, GAUGE, HELP, samples2)

        when:
            final SettableGauge settableGauge = new SettableGaugeBuilder(NAME, HELP).build()
            settableGauge.set(1d)

        then:
            settableGauge.getSample(sampleCreator) == metricFamilySamples1

        when:
            settableGauge.set(2d)

        then:
            settableGauge.getSample(sampleCreator) == metricFamilySamples2
    }

    def "SettableGauge with labels should return the correct value after it was set"() {
        given:
            final List<Sample> samples = [new Sample(NAME, ["label1"], ["value1"], 2d),
                                          new Sample(NAME, ["label1"], ["value2"], 2d)]
            final MetricFamilySamples metricFamilySamples = new MetricFamilySamples(NAME, GAUGE, HELP, samples)

        when:
            final SettableGauge settableGauge = new SettableGaugeBuilder(NAME, HELP).withLabels("label1").build()
            settableGauge.set(2d, "value1")
            settableGauge.set(2d, "value2")

        then:
            final MetricFamilySamples actualMetricFamilySamples = settableGauge.getSample(sampleCreator)
            actualMetricFamilySamples.samples.sort() == metricFamilySamples.samples.sort()
            actualMetricFamilySamples.name == metricFamilySamples.name
            actualMetricFamilySamples.help == metricFamilySamples.help
            actualMetricFamilySamples.type == metricFamilySamples.type

    }
}
