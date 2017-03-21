package com.outbrain.swinfra.metrics.format;

import java.io.IOException;

public interface CollectorFormatter {

    void formatTo(Appendable appendable) throws IOException;
}
