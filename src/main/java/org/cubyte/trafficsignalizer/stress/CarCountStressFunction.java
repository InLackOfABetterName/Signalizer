package org.cubyte.trafficsignalizer.stress;

import org.cubyte.trafficsignalizer.tracker.TrafficTracker;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;

import javax.inject.Inject;

public class CarCountStressFunction implements StressFunction {

    private final TrafficTracker trafficTracker;

    @Inject
    public CarCountStressFunction(TrafficTracker trafficTracker) {
        this.trafficTracker = trafficTracker;
    }

    @Override
    public double calculateStress(Network network, Signal signal, SignalSystem system) {
        return trafficTracker.getCarCount(signal.getLinkId());
    }
}
