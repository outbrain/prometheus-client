package com.outbrain.swinfra.metrics;

import java.io.IOException;
import java.util.List;

@FunctionalInterface
public interface SampleConsumer {

    void apply(String name, double value, List<String> labelValues, String additionalLabelName, String additionalLabelValue) throws IOException;
}
