package com.outbrain.swinfra.metrics.exporter;

import java.io.IOException;
import java.io.OutputStream;

public interface MetricExporter {

    void exportTo(OutputStream stream) throws IOException;
}
