package com.outbrain.swinfra.metrics.exports

import com.outbrain.swinfra.metrics.MetricRegistry
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.lang.management.ThreadMXBean
import java.util.function.Predicate

class ThreadMetricsTest extends Specification {

    private ThreadMXBean threadsBean = Mock(ThreadMXBean)

    @Subject
    private ThreadMetric threadMetrics = new ThreadMetric(threadsBean)

    def 'verify metrics registered correctly'() {
        given:
            threadsBean.getThreadCount() >> 300
            threadsBean.getDaemonThreadCount() >> 200
            threadsBean.getPeakThreadCount() >> 301
            threadsBean.getTotalStartedThreadCount() >> 503L
            threadsBean.findDeadlockedThreads() >> ([11L, 22L, 33L] as long[])
            threadsBean.findMonitorDeadlockedThreads() >> ([12L, 13L, 14L] as long[])
            final MetricRegistry registry = new MetricRegistry()
        when:
            threadMetrics.registerMetricsTo(registry)
        then:
            registry.find { it.name == 'jvm_threads_current' }.getValue() == 300
            registry.find { it.name == 'jvm_threads_daemon' }.getValue() == 200
            registry.find { it.name == 'jvm_threads_peak' }.getValue() == 301
            registry.find { it.name == 'jvm_threads_started_total' }.getValue() == 503
            registry.find { it.name == 'jvm_threads_deadlocked' }.getValue() == 3
            registry.find { it.name == 'jvm_threads_deadlocked_monitor' }.getValue() == 3
    }

    @Unroll
    def 'filter metrics by name expect #expected'() {
        given:
            final MetricRegistry registry = new MetricRegistry()
        when:
            threadMetrics.registerMetricsTo(registry, filter as Predicate)
        then:
            registry.collect {it.name }.sort() == expected
        where:
            filter                                 | expected
            { name -> false }                      | []
            { name -> name == 'jvm_threads_peak' } | ['jvm_threads_peak']
            { name -> true }                       | ['jvm_threads_current', 'jvm_threads_daemon', 'jvm_threads_deadlocked', 'jvm_threads_deadlocked_monitor', 'jvm_threads_peak', 'jvm_threads_started_total']
    }
}
