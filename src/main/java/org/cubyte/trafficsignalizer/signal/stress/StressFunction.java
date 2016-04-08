package org.cubyte.trafficsignalizer.signal.stress;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;

public interface StressFunction {
    double calculateStress(Network network, Signal signal, SignalSystem system, double timeSeconds);
}
