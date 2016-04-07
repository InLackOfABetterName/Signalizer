package org.cubyte.trafficsignalizer.signal;

import org.cubyte.trafficsignalizer.SignalizerController;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;

import java.util.*;

import static java.awt.geom.Line2D.linesIntersect;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Collections.disjoint;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;

public class SignalGroups {

    public static boolean shouldGenerate(SignalsData signalsData, Id<SignalSystem> system) {
        final SignalSystemControllerData controlData = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(system);
        return controlData != null && controlData.getControllerIdentifier().equals(SignalizerController.class.getName());
    }

    public static Collection<SignalGroupData> determineGroups(Network n, Id<SignalSystem> systemId, Collection<SignalData> signals, SignalGroupsDataFactory factory) {

        final Set<Set<SignalData>> possibleGroups = powerSet(new HashSet<>(signals));

        final Set<Set<SignalData>> usefulGroups = possibleGroups.stream().filter(group -> {
            if (group.isEmpty()) {
                return false;
            } else {

                for (SignalData outer : group) {
                    for (SignalData inner : group) {
                        if (outer == inner) {
                            continue;
                        }
                        if (conflictingLinks(n, inner, outer)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }).collect(toSet());

        final Set<Set<Id<Signal>>> usefulIdGroups = usefulGroups.stream().map(g -> g.stream().map(SignalData::getId).collect(toSet())).collect(toSet());

        final Set<Set<SignalData>> minimalGroups = usefulGroups.stream().filter(group -> {
            final Set<Id<Signal>> signalIds = group.stream().map(SignalData::getId).collect(toSet());
            for (Set<Id<Signal>> other : usefulIdGroups) {
                if (other.containsAll(signalIds) && !signalIds.containsAll(other)) {
                    return false;
                }
            }
            return true;
        }).collect(toSet());

        int i = 0;
        List<SignalGroupData> groups = new ArrayList<>();
        for (Set<SignalData> group : minimalGroups) {
            String id = group.stream().map(s -> s.getId().toString()).sorted(CASE_INSENSITIVE_ORDER).reduce("", String::join);

            final SignalGroupData actualGroup = factory.createSignalGroupData(systemId, Id.create(i++ + "_" + id, SignalGroup.class));
            group.stream().map(SignalData::getId).forEach(actualGroup::addSignalId);
            groups.add(actualGroup);

        }
        return groups;
    }

    private static boolean conflictingLinks(Network n, SignalData a, SignalData b) {
        final Map<Id<Link>, ? extends Link> links = n.getLinks();
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

    private static boolean intersect(Link a, Link b) {
        final Coord af = a.getFromNode().getCoord();
        final Coord at = a.getToNode().getCoord();
        final Coord bf = b.getFromNode().getCoord();
        final Coord bt = b.getToNode().getCoord();

        if (linesIntersect(af.getX(), af.getY(), at.getX(), at.getY(), bf.getX(), bf.getY(), bt.getX(), bt.getY())) {
            return !af.equals(bf);
        }
        return false;
    }

    private static boolean targetSameWay(Node a, Node b) {
        Set<Id<Node>> followA = selectFollowingNodes(singleton(a), 5);
        Set<Id<Node>> followB = selectFollowingNodes(singleton(b), 5);
        return !disjoint(followA, followB);
    }

    private static Set<Id<Node>> selectFollowingNodes(Set<Node> in, int levels) {
        if (levels == 0) {
            return Collections.emptySet();
        } else {
            Set<Node> next = in.stream().flatMap(n -> n.getOutLinks().values().stream().map(Link::getToNode)).collect(toSet());

            Set<Id<Node>> out = next.stream().map(Node::getId).collect(toSet());
            out.addAll(selectFollowingNodes(next, levels - 1));
            return out;
        }
    }

    private static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
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
