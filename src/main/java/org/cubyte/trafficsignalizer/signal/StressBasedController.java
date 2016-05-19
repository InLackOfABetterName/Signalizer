package org.cubyte.trafficsignalizer.signal;

import com.google.inject.Inject;
import org.cubyte.trafficsignalizer.signal.stress.StressFunction;
import org.cubyte.trafficsignalizer.tracker.TrafficTracker;
import org.cubyte.trafficsignalizer.ui.TextObject;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.*;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Math.*;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.cubyte.trafficsignalizer.signal.stress.CarCountStressFunction.countCarsAtSignal;
import static org.matsim.vis.otfvis.data.OTFServerQuadTree.getOTFTransformation;

public class StressBasedController extends AbstractSignalController {

    private final StressFunction stressFunction;
    private final TextObject.Writer textWriter;
    private final TrafficTracker tracker;
    private SignalGroup upcomingGroup;
    private SignalGroup activeGroup;
    private SignalGroup expiringGroup;
    private double greenSince;
    private double dropAt;
    private final double greenTimePerCar = 10d;
    private final double maximumGreenTime = 100d;
    private final double minimumGreenTime = 1 * greenTimePerCar;
    private Map<Id<Signal>, Double> redSince = new HashMap<>();

    @Inject
    public StressBasedController(Network network, SignalNetworkController networkController,
                                 StressFunction stressFunction, TextObject.Writer textWriter,
                                 TrafficTracker tracker) {
        super(network, networkController);
        this.stressFunction = stressFunction;
        this.textWriter = textWriter;
        this.tracker = tracker;
    }

    public void simulationInitialized(double timeSeconds) {
        for (Id<SignalGroup> group : getSystem().getSignalGroups().keySet()) {
            getSystem().scheduleDropping(timeSeconds, group);
        }

        this.redSince.clear();
        for (Id<Signal> signalId : getSystem().getSignals().keySet()) {
            this.redSince.put(signalId, timeSeconds);
        }

        super.simulationInitialized(timeSeconds);
    }

    public void updateState(double t) {
        getNetworkController().updateState(this, t);

        Map<Id<Signal>, Double> stressPerSignal = stressTable(t);
        mixInBackPressure(stressPerSignal, t);

        Map<Id<SignalGroup>, Double> stressPerGroup = new HashMap<>();
        Collection<SignalGroup> groups = getSystem().getSignalGroups().values();
        for (SignalGroup g : groups) {
            double stress = g.getSignals().keySet().stream().mapToDouble(stressPerSignal::get).sum();
            stressPerGroup.put(g.getId(), stress);
        }

        for (Signal signal : getSystem().getSignals().values()) {
            drawCurrentStress(signal, stressPerSignal);
        }

        if (activeGroup != null && upcomingGroup == null && expiringGroup == null) {

            if (t >= this.dropAt) {
                getSystem().scheduleDropping(t, activeGroup.getId());
                this.expiringGroup = this.activeGroup;
                this.activeGroup = null;
            }

        } else if (activeGroup == null && upcomingGroup == null && expiringGroup == null) {

            Optional<Map.Entry<Id<SignalGroup>, Double>> mostStressed = stressPerGroup.entrySet().stream().max((a, b) -> Double.compare(a.getValue(), b.getValue()));
            if (mostStressed.isPresent()) {
                SignalGroup group = getSystem().getSignalGroups().get(mostStressed.get().getKey());
                getSystem().scheduleOnset(t, group.getId());
                this.upcomingGroup = group;
            }
        }


    }

    protected Map<Id<Signal>, Double> stressTable(double t) {
        return stressTable(getSystem(), t);
    }

    protected Map<Id<Signal>, Double> stressTable(SignalSystem s, double t) {
        return stressTableFromIds(s, t, s.getSignals().keySet());
    }

    protected Map<Id<Signal>, Double> stressTable(SignalSystem s, double t, Collection<Signal> signals) {
        return stressTable(s, t, signals, Signal::getId);
    }

    protected Map<Id<Signal>, Double> stressTableFromIds(SignalSystem s, double t, Collection<Id<Signal>> signals) {
        return stressTable(s, t, signals, identity());
    }

    protected <A> Map<Id<Signal>, Double> stressTable(SignalSystem s, double t, Collection<A> signals, Function<A, Id<Signal>> id) {
        Map<Id<Signal>, Double> table = new HashMap<>();
        for (A signal : signals) {
            final Id<Signal> sid = id.apply(signal);
            table.put(sid, calculateStress(t, s.getSignals().get(sid)));
        }
        return table;
    }

    protected void mixInBackPressure(Map<Id<Signal>, Double> stress, double t) {
        double maxPressure = 0d;
        Map<Id<Signal>, Double> backPressure = new HashMap<>();
        for (Map.Entry<Id<Signal>, Double> e : stress.entrySet()) {
            Signal s = getSystem().getSignals().get(e.getKey());
            double pressure = calculateBackPressure(t, s) + 1;
            backPressure.put(s.getId(), pressure);
            if (pressure > maxPressure) {
                maxPressure = pressure;
            }
        }

        for (Map.Entry<Id<Signal>, Double> entry : backPressure.entrySet()) {
            double x = 1 - entry.getValue() / maxPressure;
            double b = 0.7;
            double a = 1 - b;

            final Id<Signal> s = entry.getKey();
            stress.put(s, stress.get(s) * (a * x + b));
        }
    }

    protected double calculateBackPressure(double t, Signal s) {
        final Link link = getNetwork().getLinks().get(s.getLinkId());
        final Map<Id<Link>, ? extends Link> outLinks = link.getToNode().getOutLinks();
        return sqrt(outLinks.values().stream().mapToDouble(l -> tracker.carCountAt(l.getId())).map(x -> x * x).sum());
    }

    protected double calculateStress(double t, Signal s) {
        return stressFunction.calculateStress(getNetwork(), s, getSystem(), t - redSince.get(s.getId()));
    }

    protected double calculateGreenTime(Id<SignalGroup> groupId, double t) {
        final SignalGroup signalGroup = getSystem().getSignalGroups().get(groupId);
        final int cars = signalGroup.getSignals().values().stream().map(s -> countCarsAtSignal(tracker, s)).max(Integer::compare).get();
        return clamp(greenTimePerCar * cars + greenTimePerCar * log(cars), minimumGreenTime, maximumGreenTime);
    }

    private static double clamp(double x, double min, double max) {
        return max(min, min(max, x));
    }

    @Override
    public void groupStateChanged(Id<SignalGroup> signalGroupId, SignalGroupState newState, double t) {

        switch (newState) {
            case RED:
                for (Signal signal : getSystem().getSignalGroups().get(signalGroupId).getSignals().values()) {
                    this.redSince.put(signal.getId(), t);
                }
                this.expiringGroup = null;
                break;
            case GREEN:
                this.activeGroup = this.upcomingGroup;
                this.upcomingGroup = null;
                this.greenSince = t;
                final double greenTime = calculateGreenTime(signalGroupId, t);
                this.dropAt = t + greenTime;
                drawGreenTime(greenTime);
                break;
        }
    }

    private void drawGreenTime(double t) {
        if (t == minimumGreenTime) {
            return;
        }
        final List<Coord> coords = getSystem().getSignals().values().stream().map(s -> {
            return getNetwork().getLinks().get(s.getLinkId()).getToNode().getCoord();
        }).collect(toList());
        double x = coords.stream().mapToDouble(Coord::getX).max().getAsDouble() + 100;
        double y = coords.stream().mapToDouble(Coord::getY).max().getAsDouble() + 100;
        textWriter.put(getSystem().getId() + "_green_time", "G: " + round(t), new Coord(x, y));
    }

    private void drawCurrentStress(Signal signal, Map<Id<Signal>, Double> stressPerSignal) {
        final CoordinateTransformation trans = getOTFTransformation();
        if (trans != null) {
            double roundedStress = round(stressPerSignal.get(signal.getId()) * 10) / 10;
            Link link = getNetwork().getLinks().get(signal.getLinkId());
            String id = "signal_stress_" + getSystem().getId() + "_" + signal.getId();
            textWriter.putMid(id, countCarsAtSignal(tracker, signal) + "|" + roundedStress + "", link);
        }
    }
}
