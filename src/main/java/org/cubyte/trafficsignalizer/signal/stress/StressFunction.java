package org.cubyte.trafficsignalizer.signal.stress;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;

public interface StressFunction {
    double calculateStress(Network network, Signal signal, SignalSystem system, double timeSeconds);

    static Class<? extends StressFunction> byName(String n) {
        switch (n) {
            case "none": return NoStressFunction.class;
            case "car_count": return CarCountStressFunction.class;
            case "time_variant": return TimeVariantStressFunction.class;
            default: return null;
        }
    }
}
