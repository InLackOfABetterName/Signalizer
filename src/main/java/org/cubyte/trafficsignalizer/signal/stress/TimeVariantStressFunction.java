package org.cubyte.trafficsignalizer.signal.stress;

import org.cubyte.trafficsignalizer.SignalizerConfigGroup;
import org.cubyte.trafficsignalizer.tracker.TrafficTracker;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;

import javax.inject.Inject;

public class TimeVariantStressFunction implements StressFunction {

    private final TrafficTracker trafficTracker;
    private final SignalizerConfigGroup config;

    @Inject
    public TimeVariantStressFunction(TrafficTracker trafficTracker, SignalizerConfigGroup config) {
        this.trafficTracker = trafficTracker;
        this.config = config;
    }

    @Override
    public double calculateStress(Network network, Signal signal, SignalSystem system, double t) {
        final double n = trafficTracker.getCarCount(signal.getLinkId());

        return (t + n) * (n + 1);
    }
}
