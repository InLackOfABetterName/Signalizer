package org.cubyte.trafficsignalizer;

import com.google.inject.Inject;
import org.cubyte.trafficsignalizer.stress.StressFunction;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;

import java.util.stream.Collectors;

public class SignalizerController implements SignalController {

    private final Network network;
    private final SignalNetworkController networkController;
    private final StressFunction stressFunction;
    private SignalSystem system;

    @Inject
    public SignalizerController(Network network, SignalNetworkController networkController, StressFunction stressFunction) {
        this.network = network;
        this.networkController = networkController;
        this.stressFunction = stressFunction;
    }

    public void updateState(double timeSeconds) {
        //this.system.getSignals().values().stream().map().collect(Collectors.toMap(x -> (x)))
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


    protected double stressForSignal(Signal s) {
        return stressFunction.calculateStress(network, s, system);
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
