package org.cubyte.trafficsignalizer.signal.stress;

import org.cubyte.trafficsignalizer.SignalizerConfigGroup;
import org.cubyte.trafficsignalizer.tracker.TrafficTracker;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;

import javax.inject.Inject;

import static org.cubyte.trafficsignalizer.signal.stress.CarCountStressFunction.countCarsAtSignal;

public class TimeVariantStressFunction implements StressFunction {

    private final TrafficTracker trafficTracker;
    private final SignalizerConfigGroup config;
    private static final double CAR_SHIFT = 0.1;

    @Inject
    public TimeVariantStressFunction(TrafficTracker trafficTracker, SignalizerConfigGroup config) {
        this.trafficTracker = trafficTracker;
        this.config = config;
    }

    public static double f(double n, double t) {
        return (t + n) * (n + CAR_SHIFT);
    }

    @Override
    public double calculateStress(Network network, Signal signal, SignalSystem system, double t) {
        final double n = countCarsAtSignal(trafficTracker, signal);
        return f(n, t);
    }
}
