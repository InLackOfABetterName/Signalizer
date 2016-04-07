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

import static org.cubyte.trafficsignalizer.signal.SignalGroups.determineGroups;
import static org.matsim.core.mobsim.qsim.interfaces.SignalGroupState.GREEN;
import static org.matsim.core.mobsim.qsim.interfaces.SignalGroupState.RED;

public class SignalizerController implements SignalController {

    private final Network network;
    private final SignalNetworkController networkController;
    private final StressFunction stressFunction;
    private SignalSystem system;
    private SignalGroup activeGroup;
    private SignalGroup upcomingGroup;
    private SignalGroupState activeGroupState;
    private SignalGroupState upcomingGroupState;

    @Inject
    public SignalizerController(Network network, SignalNetworkController networkController, StressFunction stressFunction) {
        this.network = network;
        this.networkController = networkController;
        this.stressFunction = stressFunction;
    }

    public SignalSystem getSystem() {
        return system;
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
            final double stress = g.getSignals().keySet().stream().mapToDouble(stressPerSignal::get).sum();
            stressPerGroup.put(g.getId(), stress);
        }

        //System.out.println("Stress in " + system.getId() + ": " + stressPerSignal.values().stream().mapToDouble(i -> i).sum());

        Optional<Map.Entry<Id<SignalGroup>, Double>> mostStressed = stressPerGroup.entrySet().stream().max((a, b) -> Double.compare(a.getValue(), b.getValue()));
        if (mostStressed.isPresent()) {
            SignalGroup group = system.getSignalGroups().get(mostStressed.get().getKey());
            double stress = mostStressed.get().getValue();
            if (this.activeGroup != group && stress > 0.0) {
                if (this.activeGroup != null) {
                    this.activeGroup.setState(RED);
                }
                this.activeGroup = group;
                group.setState(GREEN);
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
        final Map<Id<SignalGroup>, SignalGroup> groupTable = system.getSignalGroups();
        if (groupTable.size() == 1 && groupTable.values().iterator().next().getId().toString().equals("generate_me")) {
            groupTable.clear();
            determineGroups(network, system).forEach(system::addSignalGroup);
        }

        this.networkController.controllerReady(this, system);
    }

    public void reset(Integer iterationNumber) {
        this.networkController.reset(this, iterationNumber);
    }

    public void simulationInitialized(double simStartTimeSeconds) {
        System.out.println("Start Time: " + simStartTimeSeconds);
        this.networkController.simulationInitialized(this, simStartTimeSeconds);
    }

    public void groupStateChanged(Id<SignalGroup> signalGroupId, SignalGroupState newState) {
        System.out.println(system.getId() + " -> " + signalGroupId + " -> " + newState);
    }
}
