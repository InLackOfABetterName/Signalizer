package org.cubyte.trafficsignalizer.signal.stress;

import org.cubyte.trafficsignalizer.tracker.PredictedTrafficTracker;
import org.cubyte.trafficsignalizer.tracker.TrafficTracker;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;

import javax.inject.Inject;

public class CarCountStressFunction implements StressFunction {

    private final TrafficTracker trafficTracker;
    private final PredictedTrafficTracker predictedTrafficTracker;

    @Inject
    public CarCountStressFunction(TrafficTracker trafficTracker, PredictedTrafficTracker predictedTrafficTracker) {
        this.trafficTracker = trafficTracker;
        this.predictedTrafficTracker = predictedTrafficTracker;
    }

    @Override
    public double calculateStress(Network network, Signal signal, SignalSystem system, double timeSeconds) {
        int predicted = predictedTrafficTracker.getCarCount(signal.getLinkId());
        int actual = trafficTracker.getCarCount(signal.getLinkId());
        if (predicted != 0 && actual != 0)
            System.out.println("predicted: " + predicted + " actual: " + actual);
        return predicted;
    }
}
