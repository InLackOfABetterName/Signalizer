package org.cubyte.trafficsignalizer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubyte.trafficsignalizer.stress.StressFunction;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Singleton
public class SignalNetworkController {
    private final List<SignalizerController> controllers;
    private final Network network;
    private final StressFunction stressFunction;

    @Inject
    public SignalNetworkController(Network network, StressFunction stressFunction) {
        this.network = network;
        this.stressFunction = stressFunction;
        this.controllers = new ArrayList<>();
    }

    public void addController(SignalizerController c) {
        this.controllers.add(c);
    }

    public List<SignalizerController> otherControllers(SignalizerController c) {
        return this.controllers.stream().filter(controller -> controller != c).collect(toList());
    }

    public double stressFor(Signal signal, SignalSystem system) {
        return stressFunction.calculateStress(network, signal, system);
    }

    public void updateState(SignalizerController signalizerController, double timeSeconds) {

    }

    public void receivedSystem(SignalizerController signalizerController, SignalSystem system) {
    }


    public void reset(SignalizerController signalizerController, Integer iterationNumber) {

    }

    public void simulationInitialized(SignalizerController signalizerController, double simStartTimeSeconds) {

    }
}
