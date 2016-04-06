package org.cubyte.trafficsignalizer;

import com.google.inject.Inject;
import org.cubyte.trafficsignalizer.stress.StressFunction;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.*;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
        this.networkController.updateState(this, timeSeconds);

        Map<Id<Signal>, Double> stressPerSignal = new HashMap<>();
        for (Signal s : this.system.getSignals().values()) {
            stressPerSignal.put(s.getId(), stressForSignal(s));
        }

        Map<Id<SignalGroup>, Double> stressPerGroup = new HashMap<>();
        Collection<SignalGroup> groups = this.system.getSignalGroups().values();
        for (SignalGroup g : groups) {
            double stress = g.getSignals().keySet().stream().map(stressPerSignal::get).mapToDouble(Double::doubleValue).sum();
            stressPerGroup.put(g.getId(), stress);
        }

        //System.out.println("Stress in " + system.getId() + ": " + stressPerSignal.values().stream().mapToDouble(i -> i).sum());

        Optional<Map.Entry<Id<SignalGroup>, Double>> mostStressed = stressPerGroup.entrySet().stream().max((a, b) -> Double.compare(a.getValue(), b.getValue()));
        if (mostStressed.isPresent()) {
            SignalGroup group = system.getSignalGroups().get(mostStressed.get().getKey());
            groups.stream().filter(g -> !g.getId().equals(group.getId())).forEach(g -> g.setState(SignalGroupState.RED));
            group.setState(SignalGroupState.GREEN);
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
