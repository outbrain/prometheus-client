package com.outbrain.swinfra.metrics.exports

import com.outbrain.swinfra.metrics.MetricRegistry
import com.sun.management.UnixOperatingSystemMXBean
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.lang.management.RuntimeMXBean
import java.util.function.Predicate

class StandardMetricsTest extends Specification {


    @Subject
    StandardMetric standardMetrics

    StandardMetric.StatusReader statusReader = Mock(StandardMetric.StatusReader)
    UnixOperatingSystemMXBean osBean = Mock(UnixOperatingSystemMXBean)
    RuntimeMXBean runtimeBean = Mock(RuntimeMXBean)

    def setup() {
        osBean.getName() >> 'Linux'
        standardMetrics = new StandardMetric(statusReader, osBean, runtimeBean)
    }

    def 'verify metrics registered correctly'() {
        given:
            osBean.getProcessCpuTime() >> 123L * 1.0E9
            osBean.getOpenFileDescriptorCount() >> 10L
            osBean.getMaxFileDescriptorCount() >> 20L
            runtimeBean.getStartTime() >> 456L * 1.0E3
            statusReader.readProcSelfStatus() >> new StandardMetric.StatusParser('Name:   cat\nVmSize:\t5900 kB\nVmRSS:\t   360 kB\n')
            final MetricRegistry registry = new MetricRegistry()
        when:
            standardMetrics.registerMetricsTo(registry)
        then:
            registry.find { it.name == 'process_cpu_seconds_total' }.getValue() == 123
            registry.find { it.name == 'process_open_fds' }.getValue() == 10
            registry.find { it.name == 'process_max_fds' }.getValue() == 20
            registry.find { it.name == 'process_start_time_seconds' }.getValue() == 456
            registry.find { it.name == 'process_virtual_memory_bytes' }.getValue() == 5900 * 1024
            registry.find { it.name == 'process_resident_memory_bytes' }.getValue() == 360 * 1024
    }

    def 'broken proc status fails gracefully'() {
        given:
            osBean.getProcessCpuTime() >> 123L * 1.0E9
            statusReader.readProcSelfStatus() >> new StandardMetric.StatusParser('Name:   cat\nVmSize:\n')
            final MetricRegistry registry = new MetricRegistry()
        when:
            standardMetrics.registerMetricsTo(registry)
        then:
            registry.find { it.name == 'process_cpu_seconds_total' }.getValue() == 123
            registry.find { it.name == 'process_virtual_memory_bytes' }.getValue() == 0
            registry.find { it.name == 'process_resident_memory_bytes' }.getValue() == 0
    }

    @Unroll
    def 'filter metrics by name expect #expected'() {
        given:
            final MetricRegistry registry = new MetricRegistry()
        when:
            standardMetrics.registerMetricsTo(registry, filter as Predicate)
        then:
            registry.collect {it.name }.sort() == expected
        where:
            filter                                  | expected
            { name -> false }                       | []
            { name -> name == 'process_max_fds' }   | ['process_max_fds']
            { name -> true }                        | ['process_cpu_seconds_total', 'process_max_fds', 'process_open_fds', 'process_resident_memory_bytes', 'process_start_time_seconds', 'process_virtual_memory_bytes']
    }
}
