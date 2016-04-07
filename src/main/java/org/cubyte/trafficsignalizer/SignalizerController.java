package org.cubyte.trafficsignalizer;

import com.google.inject.Inject;
import org.cubyte.trafficsignalizer.stress.StressFunction;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.model.*;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;

import java.util.*;

import static java.awt.geom.Line2D.linesIntersect;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Collections.disjoint;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;

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
        final Map<Id<SignalGroup>, SignalGroup> groupTable = system.getSignalGroups();
        if (groupTable.size() == 1 && groupTable.values().iterator().next().getId().toString().equals("generate_me")) {
            groupTable.clear();
            determineGroups(system).forEach(system::addSignalGroup);
        }
        this.networkController.receivedSystem(this, system);
    }

    public void reset(Integer iterationNumber) {
        this.networkController.reset(this, iterationNumber);
    }

    public void simulationInitialized(double simStartTimeSeconds) {
        System.out.println("Start Time: " + simStartTimeSeconds);
        this.networkController.simulationInitialized(this, simStartTimeSeconds);
    }

    private Collection<SignalGroup> determineGroups(SignalSystem system) {
        final Map<Id<Signal>, Signal> signalTable = system.getSignals();

        final Set<Signal> signals = new HashSet<>(signalTable.values());
        final Set<Set<Signal>> possibleGroups = powerSet(signals);

        final Set<Set<Signal>> usefulGroups = possibleGroups.stream().filter(group -> {
            if (group.isEmpty()) {
                return false;
            } else {

                for (Signal outer : group) {
                    for (Signal inner : group) {
                        if (outer == inner) {
                            continue;
                        }
                        if (conflictingLinks(inner, outer)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }).collect(toSet());

        final Set<Set<Id<Signal>>> usefulIdGroups = usefulGroups.stream().map(g -> g.stream().map(Signal::getId).collect(toSet())).collect(toSet());

        final Set<Set<Signal>> minimalGroups = usefulGroups.stream().filter(group -> {
            final Set<Id<Signal>> signalIds = group.stream().map(Signal::getId).collect(toSet());
            for (Set<Id<Signal>> other : usefulIdGroups) {
                if (other.containsAll(signalIds) && !signalIds.containsAll(other)) {
                    return false;
                }
            }
            return true;
        }).collect(toSet());

        int i = 0;
        List<SignalGroup> groups = new ArrayList<>();
        for (Set<Signal> group : minimalGroups) {
            groups.add(signalsToGroup(group, i++));
        }
        return groups;
    }

    private SignalGroup signalsToGroup(Set<Signal> signals, int i) {
        String id = signals.stream().map(s -> s.getId().toString()).sorted(CASE_INSENSITIVE_ORDER).reduce("", String::join);
        return new SignalGroupImpl(Id.create(i + "_" + id, SignalGroup.class));
    }

    private boolean conflictingLinks(Signal a, Signal b) {
        final Map<Id<Link>, ? extends Link> links = network.getLinks();
        final Collection<? extends Link> linkA = links.get(a.getLinkId()).getToNode().getOutLinks().values();
        final Collection<? extends Link> linkB = links.get(b.getLinkId()).getToNode().getOutLinks().values();

        for (Link outer : linkA) {
            for (Link inner : linkB) {
                if (inner == outer) {
                    continue;
                }
                if (intersect(inner, outer)) {
                    return true;
                }

                if (targetSameWay(inner.getToNode(), outer.getToNode())) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean intersect(Link a, Link b) {
        final Coord af = a.getFromNode().getCoord();
        final Coord at = a.getToNode().getCoord();
        final Coord bf = b.getFromNode().getCoord();
        final Coord bt = b.getToNode().getCoord();

        if (linesIntersect(af.getX(), af.getY(), at.getX(), at.getY(), bf.getX(), bf.getY(), bt.getX(), bt.getY())) {
            return !af.equals(bf);
        }
        return false;
    }

    public static boolean targetSameWay(Node a, Node b) {
        Set<Id<Node>> followA = selectFollowingNodes(singleton(a), 5);
        Set<Id<Node>> followB = selectFollowingNodes(singleton(b), 5);
        return !disjoint(followA, followB);
    }

    public static Set<Id<Node>> selectFollowingNodes(Set<Node> in, int levels) {
        if (levels == 0) {
            return Collections.emptySet();
        } else {
            Set<Node> next = in.stream().flatMap(n -> n.getOutLinks().values().stream().map(Link::getToNode)).collect(toSet());

            Set<Id<Node>> out = next.stream().map(Node::getId).collect(toSet());
            out.addAll(selectFollowingNodes(next, levels - 1));
            return out;
        }
    }

    public static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
        Set<Set<T>> sets = new HashSet<>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<>());
            return sets;
        }
        List<T> list = new ArrayList<>(originalSet);
        T head = list.get(0);
        Set<T> rest = new HashSet<>(list.subList(1, list.size()));
        for (Set<T> set : powerSet(rest)) {
            Set<T> newSet = new HashSet<>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }
}
