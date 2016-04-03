package org.cubyte.trafficsignalizer;

import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;

public class SignalizerController implements SignalController {

    private final SignalNetworkController masterController;
    private SignalSystem system;

    public SignalizerController(SignalNetworkController masterController) {
        this.masterController = masterController;
    }

    public void updateState(double timeSeconds) {
        System.out.println("Time: " + timeSeconds);
        for (Signal signal : system.getSignals().values()) {
            signal.setState(SignalGroupState.GREEN);
        }
    }

    public void addPlan(SignalPlan plan) {

    }

    public void setSignalSystem(SignalSystem system) {
        this.system = system;
    }

    public void reset(Integer iterationNumber) {

    }

    public void simulationInitialized(double simStartTimeSeconds) {
        System.out.println("Start Time: " + simStartTimeSeconds);
    }
}
