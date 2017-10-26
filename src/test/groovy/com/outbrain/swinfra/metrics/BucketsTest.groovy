package com.outbrain.swinfra.metrics

import spock.lang.Specification
import spock.lang.Unroll

import static com.outbrain.swinfra.metrics.Histogram.BucketValues
import static com.outbrain.swinfra.metrics.Histogram.Buckets

class BucketsTest extends Specification {

    def 'Buckets should contain a single bucket when initiated with no buckets'() {
        given:
            final Buckets buckets = new Buckets(true)

        expect:
            final BucketValues values = buckets.getValues()
            values.sum == 0
            values.buckets.length == 1
            values.buckets[0] == 0l
    }

    def 'Buckets should contain the correct number of buckets when initiated with several buckets'() {
        given:
            final Buckets buckets = new Buckets(true, 1, 10, 100)

        expect:
            final BucketValues values = buckets.getValues()
            values.sum == 0
            values.buckets.length == 4
            values.buckets[0] == 0l
            values.buckets[1] == 0l
            values.buckets[2] == 0l
            values.buckets[3] == 0l
    }

    @Unroll
    def 'Buckets should contain the correct sum and number of events after observations #observations'() {
        given:
            final Buckets buckets = new Buckets(true)

        when:
            observations.each {buckets.add(it)}
        then:
            buckets.values.sum == observations.sum() as double
            buckets.values.buckets[0] == observations.size() as long

        where:
            observations << [
                [1],
                [1, 2],
                [1, 2, 3, 4, 5, 6]
            ]
    }

    @Unroll
    def 'Buckets should contain the correct sum and number of events after observations #observations and buckets #bucketEvents'() {
        given:
            final Buckets buckets = new Buckets(true, 1, 10, 100)

        when:
            observations.each {buckets.add(it)}

        then:

            buckets.values.sum == observations.sum() as double
            buckets.values.buckets[0] == bucketEvents[0] as long
            buckets.values.buckets[1] == bucketEvents[1] as long
            buckets.values.buckets[2] == bucketEvents[2] as long
            buckets.values.buckets[3] == bucketEvents[3] as long

        where:
            observations               | bucketEvents
            [1, 1, 0, 1, 0]            | [5, 5, 5, 5] //All events fall into the bucket "1"
            [1, 50, 50, 5, 5, 70, 80]  | [1, 3, 7, 7] //One event in bucket "1", two more in bucket "5", three more in "100"
            [100, 110, 110, 110, 1100] | [0, 0, 1, 5] //No events in buckets "1" and "5", one event in "100" and 4 more in the infinity bucket
    }
}
