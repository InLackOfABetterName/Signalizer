package org.cubyte.trafficsignalizer.signal;

import com.google.inject.Inject;
import org.cubyte.trafficsignalizer.signal.stress.StressFunction;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.*;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StressBasedController implements SignalController {

    private final Network network;
    private final SignalNetworkController networkController;
    private final StressFunction stressFunction;
    private SignalSystem system;
    private SignalGroup upcomingGroup;
    private SignalGroup activeGroup;
    private SignalGroup expiringGroup;
    private double greenSince;
    private Map<Id<Signal>, Double> redSince = new HashMap<>();

    @Inject
    public StressBasedController(Network network, SignalNetworkController networkController, StressFunction stressFunction) {
        this.network = network;
        this.networkController = networkController;
        this.stressFunction = stressFunction;
    }

    public void setSignalSystem(SignalSystem system) {
        this.system = system;
        this.networkController.controllerReady(this, system);
    }

    public SignalSystem getSystem() {
        return system;
    }

    public void simulationInitialized(double timeSeconds) {
        for (Id<SignalGroup> group : system.getSignalGroups().keySet()) {
            system.scheduleDropping(timeSeconds, group);
        }

        this.redSince.clear();
        for (Id<Signal> signalId : system.getSignals().keySet()) {
            this.redSince.put(signalId, timeSeconds);
        }

        this.networkController.controllerInitialized(this, timeSeconds);
    }

    public void updateState(double timeSeconds) {
        this.networkController.updateState(this, timeSeconds);

        Map<Id<Signal>, Double> stressPerSignal = new HashMap<>();
        for (Signal s : this.system.getSignals().values()) {
            stressPerSignal.put(s.getId(), calculateStress(timeSeconds, s));
        }

        Map<Id<SignalGroup>, Double> stressPerGroup = new HashMap<>();
        Collection<SignalGroup> groups = this.system.getSignalGroups().values();
        for (SignalGroup g : groups) {
            final double stress = g.getSignals().keySet().stream().mapToDouble(stressPerSignal::get).sum();
            stressPerGroup.put(g.getId(), stress);
        }

        //System.out.println("Stress in " + system.getId() + ": " + stressPerSignal.values().stream().mapToDouble(i -> i).sum());

        if (activeGroup != null && upcomingGroup == null && expiringGroup == null) {

            if ((timeSeconds - greenSince) > 30) {
                system.scheduleDropping(timeSeconds, activeGroup.getId());
                this.expiringGroup = this.activeGroup;
                this.activeGroup = null;
            }

        } else if (activeGroup == null && upcomingGroup == null && expiringGroup == null) {

            Optional<Map.Entry<Id<SignalGroup>, Double>> mostStressed = stressPerGroup.entrySet().stream().max((a, b) -> Double.compare(a.getValue(), b.getValue()));
            if (mostStressed.isPresent()) {
                SignalGroup group = system.getSignalGroups().get(mostStressed.get().getKey());
                //double stress = mostStressed.get().getValue();
                system.scheduleOnset(timeSeconds, group.getId());
                this.upcomingGroup = group;
            }
        }


    }

    protected double calculateStress(double timeSeconds, Signal s) {
        return stressFunction.calculateStress(network, s, system, timeSeconds - redSince.get(s.getId()));
    }


    public void addPlan(SignalPlan plan) {

    }

    public void reset(Integer iterationNumber) {
        this.networkController.controllerReset(this, iterationNumber);
    }

    public void groupStateChanged(Id<SignalGroup> signalGroupId, SignalGroupState newState, double time) {
        switch (newState) {
            case RED:
                for (Signal signal : this.system.getSignalGroups().get(signalGroupId).getSignals().values()) {
                    this.redSince.put(signal.getId(), time);
                }
                this.expiringGroup = null;
                break;
            case GREEN:
                this.activeGroup = this.upcomingGroup;
                this.upcomingGroup = null;
                this.greenSince = time;
                break;
        }
    }
}
