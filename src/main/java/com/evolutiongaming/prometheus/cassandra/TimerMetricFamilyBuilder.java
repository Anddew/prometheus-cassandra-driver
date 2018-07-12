package com.evolutiongaming.prometheus.cassandra;

import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/* package */class TimerMetricFamilyBuilder {
  private static final long NS_IN_SEC = TimeUnit.SECONDS.toNanos(1);

  private final String name;
  private final String help;
  private final List<String> labelNames;

  private final List<Sample> quantileSamples = new ArrayList<>();
  private final List<Sample> countSamples = new ArrayList<>();
  private final List<Sample> meanSamples = new ArrayList<>();

  TimerMetricFamilyBuilder(String name, String help, List<String> labelNames) {
    this.name = name;
    this.help = help;
    this.labelNames = labelNames;
  }

  void addTimerMetricSample(List<String> labelValues, Timer timer) {
    long count = timer.getCount();
    Snapshot snapshot = timer.getSnapshot();

    addCountMetric(labelValues, count);
    addMeanMetric(labelValues, nsToSec(snapshot.getMean()));

    addQuantileMetric(labelValues, "0", nsToSec(snapshot.getMin()));
    addQuantileMetric(labelValues, "0.5", nsToSec(snapshot.getMedian()));
    addQuantileMetric(labelValues, "0.75", nsToSec(snapshot.get75thPercentile()));
    addQuantileMetric(labelValues, "0.95", nsToSec(snapshot.get95thPercentile()));
    addQuantileMetric(labelValues, "0.98", nsToSec(snapshot.get98thPercentile()));
    addQuantileMetric(labelValues, "0.99", nsToSec(snapshot.get99thPercentile()));
    addQuantileMetric(labelValues, "0.999", nsToSec(snapshot.get999thPercentile()));
    addQuantileMetric(labelValues, "1", nsToSec(snapshot.getMax()));
  }

  private void addQuantileMetric(List<String> labelValues, String quantile, double value) {
    List<String> quantileLabelNames = new ArrayList<>(labelNames);
    quantileLabelNames.add("quantile");
    List<String> quantileLabelValues = new ArrayList<>(labelValues);
    quantileLabelValues.add(quantile);
    quantileSamples.add(new Collector.MetricFamilySamples.Sample(
        name, quantileLabelNames, quantileLabelValues, value
    ));
  }

  private void addCountMetric(List<String> labelValues, double value) {
    countSamples.add(new Collector.MetricFamilySamples.Sample(
        name + "_count", labelNames, labelValues, value
    ));
  }

  private void addMeanMetric(List<String> labelValues, double value) {
    meanSamples.add(new Collector.MetricFamilySamples.Sample(
        name + "_mean", labelNames, labelValues, value
    ));
  }

  private double nsToSec(double nanos) {
    return nanos / NS_IN_SEC;
  }

  Collector.MetricFamilySamples build() {
    List<Sample> samples = new ArrayList<>(quantileSamples);
    samples.addAll(countSamples);
    samples.addAll(meanSamples);
    return new Collector.MetricFamilySamples(
        name,
        Collector.Type.UNTYPED,
        help,
        samples
    );
  }
}
