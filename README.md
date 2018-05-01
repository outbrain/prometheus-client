# Prometheus Client
[![Build Status](https://travis-ci.org/outbrain/prometheus-client.svg?branch=master)](https://travis-ci.org/outbrain/prometheus-client)
[![Bintray](https://img.shields.io/bintray/v/outbrain/OutbrainOSS/prometheus-client.svg)](https://bintray.com/outbrain/OutbrainOSS/prometheus-client)

A Prometheus-compliant Java client, exposing all metric types: Counter, Gauge, Summary and Histogram.

This is not the official Prometheus client, which can be found here: [https://github.com/prometheus/client_java](https://github.com/prometheus/client_java)

## Table of Contents
* [Background](#background)
* [Getting Started](#getting-started)
* [Measuring](#measuring)
    * [Registering a Metric](#registering-a-metric)
    * [Counter](#counter)
    * [Gauge](#gauge)
    * [Settable Gauge](#settable-gauge)
    * [Summary](#summary)
    * [Histogram](#histogram)
    * [Timer](#timer)
* [Advanced Usage](#advanced-usage)
    * [Gauge](#gauge---advanced)
    * [Summary](#summary---advanced)
    * [Histogram](#histogram---advanced)
    * [Timer](#timer---advanced)

## Background
When we began migrating to Prometheus, the support for labels was needed - so we went to check the official client.

At [Outbrain](http://techblog.outbrain.com/) metrics are heavily used in all our micro-services and in a multi-threaded 
environment. Thread-safety is key and the performance impact of using metrics must be negligible.

When reviewing the official Prometheus client we discovered a several *synchronized* blocks that in the *Summary* and *Gauge*
metric types. In order to avoid the [contention](http://www.ibm.com/developerworks/library/j-threads2/) we decided to write our own client, 
wrapping HdrHistogram and dropwizard-metrics internally, with a custom Bucket-based 
[Histogram](https://github.com/outbrain/prometheus-client/blob/master/src/main/java/com/outbrain/swinfra/metrics/Histogram.java) implementation.

## Getting Started
*prometheus-client* is hosted on [Bintray](#https://bintray.com/outbrain/OutbrainOSS/prometheus-client#)

Add a maven/gradle dependency:

**Maven**
```xml
<dependency>
    <groupId>com.outbrain.swinfra</groupId>
    <artifactId>prometheus-client</artifactId>
    <version>0.x</version>
</dependency>
```

**Gradle**
```
com.outbrain.swinfra:prometheus-client:0.x
```

## Measuring

### Registering a Metric
All metrics should be stored inside a MetricRegistry. The registry is a collection that contains all of
your metric objects and performs validation that no two metrics exist with the same name.
```java
MetricRegistry registry = new MetricRegistry();
```

### Counter
```java
//No labels
Counter counter = registry.getOrRegister(new CounterBuilder("name", "help").build());
counter.inc();

//With labels
Counter counter = registry.getOrRegister(new CounterBuilder("name", "help").withLabels("label1")
                                                                           .build());
counter.inc("value1");
```

### Gauge
The gauge takes after the DropWizard's gauge in the sense that it uses a supplier for its value.
This is a gauge that always returns the value "1".

Note that for gauges the label values are determined statically.
```java
//No labels
registry.getOrRegister(new GaugeBuilder("name", "help").withValueSupplier(() -> 1d).build());

//With labels
registry.getOrRegister(new GaugeBuilder("name", "help").withLabels("label1")
                                                       .withValueSupplier(() -> 1d, "value1")
                                                       .build());
```

### Settable Gauge
A gauge whose value is set from an outside source, along with the label values if relevant.
This is different from the regular gauge where the label values are set statically.
```java
//No labels
SettableGauge settableGauge = registry.getOrRegister(new SettableGaugeBuilder("name", "help")
                                                            .build());
settableGauge.set(5);

//With labels
SettableGauge settableGauge = registry.getOrRegister(new SettableGaugeBuilder("name", "help")
                                                            .withLabels("label1")
                                                            .build());
settableGauge.set(5, "value1");
```

### Summary
```java
//No labels
Summary summary = registry.getOrRegister(new SummaryBuilder(NAME, HELP).build());
summary.observe(10);

//With labels
Summary summary = registry.getOrRegister(new SummaryBuilder(NAME, HELP).withLabels("label1")
                                                                       .build());
summary.observe(10, "value1");
```

### Histogram
```java
//No labels
Histogram histogram = registry.getOrRegister(new HistogramBuilder(NAME, HELP).build());
histogram.observe(10);

//With labels
Histogram histogram = registry.getOrRegister(new HistogramBuilder(NAME, HELP).withLabels("label1")
                                                                             .build());
histogram.observe(10, "value1");
```

### Timer
```java
//A Summary timer
Summary summaryTimer = registry.getOrRegister(new SummaryBuilder(NAME, HELP).build());
Timer timer = summaeryTimer.startTimer();
//run some code
timer.stop();

//A Histogram timer
Summary histTimer = registry.getOrRegister(new HistogramBuilder(NAME, HELP).build());
Timer timer = histTimer.startTimer();
//run some code
timer.stop();
```

### Reporting to Prometheus
Prometheus uses the *Collector* class to allow a process to expose its metrics. This client implements
a Prometheus *Collector* that serves exactly this purpose. The *MetricCollector* class is instantiated with
a *MetricRegistry*.

```java
//Create a MetricCollector and pass your *MetricRegistry* to it
MetricCollector collector = new MetricCollector(registry);

//The collector may be registered in the Prometheus CollectorRegistry
CollectorRegistry.defaultRegistry.register(collector);

//The default registry can then be exposed using Prometheus' MetricsServlet for example
```

## Advanced Usage
### Gauge - Advanced
```java
registry.getOrRegister(new GaugeBuilder("name", "help").withLabels("label1")
                                                       .withValueSupplier(() -> 1, "value1")
                                                       .withValueSupplier(() -> 2, "value2")
                                                       .build());
```

### Summary - Advanced
*Summary* supports different precisions.

```java
Summary summary = registry.getOrRegister(new SummaryBuilder("name", "help")
                                              .withNumberOfSignificantValueDigits(3)
                                              .build());
```

### Histogram - Advanced
*Histogram* can be configured with custom buckets or with equal width buckets at a given range.
```java
//Custom buckets
Histogram histo = registry.getOrRegister(new HistogramBuilder("name", "help")
                                                .withBuckets(0.1, 0.2, 0.5, 1.0)
                                                .build());

//Equal width buckets - creating five buckets with the width 0.2 and starting with 0.1
// 0.1, 0.3, 0.5, 0.7, 0.9
Histogram histo = registry.getOrRegister(new HistogramBuilder("name", "help").withEqualWidthBuckets(0.1, 0.2, 5)
                                                                             .build());
```

### Timer - Advanced
*Timer* supports custom clocks, with the default being the system clock which measures intervals
according to *System.nanoTime()*.

This example shows a timer over a *Histogram* metric, but it's also applicable to the *Summary* metric
```java
//Using an instance of MyClock instead of the default
Histogram timer = registry.getOrRegister(new HistogramBuilder("name", "help")
                                                .withClock(new MyClock())
                                                .build()); 
```

## License
prometheus-client is released under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).
