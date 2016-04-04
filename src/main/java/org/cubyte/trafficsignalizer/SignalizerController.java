package org.cubyte.trafficsignalizer;

import com.google.inject.Inject;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;

public class SignalizerController implements SignalController {

    private final SignalNetworkController networkController;
    private SignalSystem system;

    @Inject
    public SignalizerController(SignalNetworkController networkController) {
        this.networkController = networkController;
    }

    public void updateState(double timeSeconds) {
        this.networkController.updateState(this, timeSeconds);
        for (Signal signal : system.getSignals().values()) {
            int time = (int) timeSeconds;
            if (time % 10 == 0) {
                signal.setState(SignalGroupState.RED);
            } else if (time % 5 == 0) {
                signal.setState(SignalGroupState.GREEN);
            }
        }
    }

    public void addPlan(SignalPlan plan) {

    }

    public void setSignalSystem(SignalSystem system) {
        this.system = system;
        this.networkController.receivedSystem(this, system);
    }

    public void reset(Integer iterationNumber) {
        this.networkController.reset(this, iterationNumber);
    }

    public void simulationInitialized(double simStartTimeSeconds) {
        System.out.println("Start Time: " + simStartTimeSeconds);
        this.networkController.simulationInitialized(this, simStartTimeSeconds);
    }
}
