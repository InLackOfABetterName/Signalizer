package org.cubyte.trafficsignalizer.stress;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.mobsim.qsim.QSim;

import javax.inject.Inject;

public class CarCountStressFunction implements StressFunction {

    private final LinkTrafficTracker trafficTracker;

    @Inject
    public CarCountStressFunction(LinkTrafficTracker trafficTracker) {
        this.trafficTracker = trafficTracker;
    }

    @Override
    public double calculateStress(Network network, Signal signal, SignalSystem system) {
        return trafficTracker.getCarCount(signal.getLinkId());
    }
}
