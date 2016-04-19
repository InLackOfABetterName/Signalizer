package org.cubyte.trafficsignalizer.signal;

import com.google.inject.Inject;
import org.cubyte.trafficsignalizer.signal.stress.StressFunction;
import org.cubyte.trafficsignalizer.ui.TextObject;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.*;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.matsim.vis.otfvis.data.OTFServerQuadTree.getOTFTransformation;

public class StressBasedController implements SignalController {

    private final Network network;
    private final SignalNetworkController networkController;
    private final StressFunction stressFunction;
    private final TextObject.Writer textWriter;
    private SignalSystem system;
    private SignalGroup upcomingGroup;
    private SignalGroup activeGroup;
    private SignalGroup expiringGroup;
    private double greenSince;
    private Map<Id<Signal>, Double> redSince = new HashMap<>();

    @Inject
    public StressBasedController(Network network, SignalNetworkController networkController, StressFunction stressFunction, TextObject.Writer textWriter) {
        this.network = network;
        this.networkController = networkController;
        this.stressFunction = stressFunction;
        this.textWriter = textWriter;
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

        for (Signal signal : system.getSignals().values()) {
            drawCurrentStress(signal, stressPerSignal);
        }

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

    protected double calculateBackPressure(double t, Signal s) {
        final Link link = network.getLinks().get(s.getLinkId());
        final Map<Id<Link>, ? extends Link> outLinks = link.getToNode().getOutLinks();
        return 1d;
    }

    protected double calculateStress(double t, Signal s) {
        return stressFunction.calculateStress(network, s, system, t - redSince.get(s.getId()));
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

    private void drawCurrentStress(Signal signal, Map<Id<Signal>, Double> stressPerSignal) {
        final CoordinateTransformation trans = getOTFTransformation();
        if (trans != null) {
            final double stress = stressPerSignal.get(signal.getId());
            Link link = network.getLinks().get(signal.getLinkId());
            Coord to = link.getToNode().getCoord();
            Coord from = link.getFromNode().getCoord();
            Coord transformedCoord = trans.transform(new Coord(from.getX() + (to.getX() - from.getX()) / 2d, from.getY() + (to.getY() - from.getY()) / 2d));
            textWriter.put("signal_stress_" + system.getId() + "_" + signal.getId(), stress + "", transformedCoord.getX(), transformedCoord.getY(), false);
        }
    }
}
