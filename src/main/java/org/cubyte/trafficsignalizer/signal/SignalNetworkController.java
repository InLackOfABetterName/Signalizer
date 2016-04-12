package org.cubyte.trafficsignalizer.signal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubyte.trafficsignalizer.signal.stress.StressFunction;
import org.cubyte.trafficsignalizer.ui.TextObject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEventHandler;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.api.experimental.events.EventsManager;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Singleton
public class SignalNetworkController {
    private final List<StressBasedController> controllers;
    private final Network network;
    private final StressFunction stressFunction;
    private final Map<Id<Link>, Signal> linkToSignalTable = new HashMap<>();
    private final Map<Id<Link>, Set<SignalGroup>> linkToGroups = new HashMap<>();

    @Inject
    public SignalNetworkController(Network network, StressFunction stressFunction, EventsManager em, TextObject.Writer textWriter) {
        this.network = network;
        this.stressFunction = stressFunction;
        this.controllers = new ArrayList<>();
        em.addHandler(new SignalGroupStateChangedEventHandler() {
            private final Map<Id<SignalSystem>, StressBasedController> cache = new HashMap<>();

            @Override
            public void handleEvent(SignalGroupStateChangedEvent event) {
                final Id<SignalSystem> system = event.getSignalSystemId();
                StressBasedController ctrl = cache.get(system);
                if (ctrl == null) {
                    for (StressBasedController c : controllers) {
                        final SignalSystem s = c.getSystem();
                        if (s != null && s.getId().equals(system)) {
                            cache.put(system, c);
                            ctrl = c;
                            break;
                        }
                    }
                }
                if (ctrl != null) {
                    ctrl.groupStateChanged(event.getSignalGroupId(), event.getNewState(), event.getTime());
                }
            }

            @Override
            public void reset(int iteration) {
                this.cache.clear();
            }
        });
    }

    public Optional<Signal> getSignalByLink(Id<Link> link) {
        return Optional.ofNullable(this.linkToSignalTable.get(link));
    }

    public Optional<Set<SignalGroup>> getGroupsAtLink(Id<Link> link) {
        return Optional.ofNullable(this.linkToGroups.get(link));
    }

    public void addController(StressBasedController c) {
        this.controllers.add(c);
    }

    public List<StressBasedController> otherControllers(StressBasedController c) {
        return this.controllers.stream().filter(controller -> controller != c).collect(toList());
    }

    public void updateState(StressBasedController controller, double timeSeconds) {

    }

    public void controllerReady(StressBasedController controller, SignalSystem system) {
        for (Signal signal : system.getSignals().values()) {
            this.linkToSignalTable.put(signal.getLinkId(), signal);
        }

        for (Signal signal : system.getSignals().values()) {
            Set<SignalGroup> groups = this.linkToGroups.get(signal.getLinkId());
            if (groups == null) {
                groups = new HashSet<>();
                this.linkToGroups.put(signal.getLinkId(), groups);
            }

            groups.addAll(system.getSignalGroups().values().stream().filter(g -> g.getSignals().containsKey(signal.getId())).collect(toSet()));
        }
    }


    public void controllerReset(StressBasedController controller, Integer iterationNumber) {

    }

    public void controllerInitialized(StressBasedController controller, double simStartTimeSeconds) {

    }
}
