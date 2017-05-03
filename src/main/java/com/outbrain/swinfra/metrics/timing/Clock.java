package com.outbrain.swinfra.metrics.timing;

import java.util.concurrent.TimeUnit;

public interface Clock {
  Clock DEFAULT_CLOCK = new SystemClock();

  long getTick();

  long getTick(TimeUnit timeunit);

  /**
   * A clock that uses System.nanoTime to measure its ticks.
   * Ticks are provided according the to given ticks unit, the default being nanoseconds.
   */
  class SystemClock implements Clock {

    private final double factor;

    /**
     * Creates an instance with the default ticks unit - nanoseconds
     */
    public SystemClock() {
      this(TimeUnit.NANOSECONDS);
    }

    /**
     * Creates a new instance with the given measurement units
     * @param ticksUnit the units in which to provide the ticks
     */
    public SystemClock(final TimeUnit ticksUnit) {
      factor = 1.0 / ticksUnit.toNanos(1);
    }

    @Override
    public long getTick() {
      return (long) (System.nanoTime() * factor);
    }

    @Override
    public long getTick(final TimeUnit ticksUnit) {
      return ticksUnit.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
    }
  }
}
