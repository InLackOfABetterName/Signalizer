package org.cubyte.trafficsignalizer.signal;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;

public abstract class AbstractSignalController implements SignalController {
    private SignalSystem system = null;
    private final Network network;
    private final SignalNetworkController networkController;

    public AbstractSignalController(Network network, SignalNetworkController networkController) {
        this.network = network;
        this.networkController = networkController;
    }

    public Network getNetwork() {
        return network;
    }

    public final SignalSystem getSystem() {
        return system;
    }

    @Override
    public final void setSignalSystem(SignalSystem signalSystem) {
        this.system = signalSystem;
        this.receivedSystem();
        this.networkController.controllerReady(this, signalSystem);
    }

    protected void receivedSystem() {

    }

    public SignalNetworkController getNetworkController() {
        return networkController;
    }

    @Override
    public void addPlan(SignalPlan plan) {

    }

    @Override
    public void reset(Integer iterationNumber) {
        getNetworkController().controllerReset(this, iterationNumber);
    }

    @Override
    public void simulationInitialized(double timeSeconds) {
        getNetworkController().controllerInitialized(this, timeSeconds);
    }

    public void groupStateChanged(Id<SignalGroup> signalGroupId, SignalGroupState newState, double t) {

    }
}
