package com.outbrain.swinfra.metrics

import com.outbrain.swinfra.metrics.timing.Clock

import java.util.concurrent.TimeUnit

class TestClock extends com.codahale.metrics.Clock implements Clock {

    private long tick = 0

    long setTick(final long value) {
        this.tick = value
    }

    @Override
    long getTick() {
        return tick
    }

    @Override
    long getTick(final TimeUnit timeunit) {
        return tick
    }

    @Override
    long getTime() {
        return tick
    }


}
