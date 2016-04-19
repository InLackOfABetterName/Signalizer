package org.cubyte.trafficsignalizer.signal;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalPlan;

public abstract class AbstractSignalController implements SignalController {
    private final Network network;
    private final SignalNetworkController networkController;

    public AbstractSignalController(Network network, SignalNetworkController networkController) {
        this.network = network;
        this.networkController = networkController;
    }

    public Network getNetwork() {
        return network;
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
}
