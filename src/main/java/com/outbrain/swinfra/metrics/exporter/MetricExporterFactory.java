package com.outbrain.swinfra.metrics.exporter;

import com.outbrain.swinfra.metrics.MetricRegistry;
import com.outbrain.swinfra.metrics.exporter.protobuf.ProtobufFormatter;
import com.outbrain.swinfra.metrics.exporter.text.TextFormatter;

import java.util.Collection;

public enum MetricExporterFactory {

  TEXT_004 {
    @Override
    public MetricExporter create(final Collection<MetricRegistry> registries) {
      return new TextFormatter(registries);
    }
  },

  PROTOBUF {
    @Override
    public MetricExporter create(final Collection<MetricRegistry> registries) {
      return new ProtobufFormatter(registries);
    }
  };

  public abstract MetricExporter create(final Collection<MetricRegistry> registries);
}
