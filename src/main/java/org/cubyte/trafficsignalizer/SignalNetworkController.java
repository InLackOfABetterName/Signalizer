package org.cubyte.trafficsignalizer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;

import java.awt.geom.Line2D;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.awt.geom.Line2D.linesIntersect;
import static java.util.Collections.disjoint;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Singleton
public class SignalNetworkController
{
    private final List<SignalizerController> controllers;
    private final Network network;

    @Inject
    public SignalNetworkController(Network network) {
        this.network = network;
        this.controllers = new ArrayList<>();
    }

    public void addController(SignalizerController c) {
        this.controllers.add(c);
    }

    public List<SignalizerController> otherControllers(SignalizerController c) {
        return this.controllers.stream().filter(controller -> controller != c).collect(toList());
    }

    public void updateState(SignalizerController signalizerController, double timeSeconds) {

    }

    public void receivedSystem(SignalizerController signalizerController, SignalSystem system) {
        final Map<Id<SignalGroup>, SignalGroup> groupTable = system.getSignalGroups();
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

        Set<Set<Signal>> minimalGroups = usefulGroups.stream().filter(group -> {
            final Set<Id<Signal>> signalIds = group.stream().map(Signal::getId).collect(toSet());
            for (Set<Id<Signal>> other : usefulIdGroups) {
                if (other.containsAll(signalIds) && !signalIds.containsAll(other)) {
                    return false;
                }
            }
            return true;
        }).collect(toSet());

        System.out.println("Possible groups: " + possibleGroups.size());
        System.out.println("Useful groups: " + usefulGroups.size());
        System.out.println("Minimal groups: " + minimalGroups.size());
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
            return !af.equals(bf) && !at.equals(bt);
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

    public void reset(SignalizerController signalizerController, Integer iterationNumber) {

    }

    public void simulationInitialized(SignalizerController signalizerController, double simStartTimeSeconds) {

    }
}
