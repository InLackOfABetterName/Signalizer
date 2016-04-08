package org.cubyte.trafficsignalizer.signal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubyte.trafficsignalizer.stress.StressFunction;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEventHandler;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.api.experimental.events.EventsManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Singleton
public class SignalNetworkController {
    private final List<StressBasedController> controllers;
    private final Network network;
    private final StressFunction stressFunction;

    @Inject
    public SignalNetworkController(Network network, StressFunction stressFunction, EventsManager em) {
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
                    ctrl.groupStateChanged(event.getSignalGroupId(), event.getNewState());
                }
            }

            @Override
            public void reset(int iteration) {
                this.cache.clear();
            }
        });
    }

    public void addController(StressBasedController c) {
        this.controllers.add(c);
    }

    public List<StressBasedController> otherControllers(StressBasedController c) {
        return this.controllers.stream().filter(controller -> controller != c).collect(toList());
    }

    public double stressFor(Signal signal, SignalSystem system) {
        return stressFunction.calculateStress(network, signal, system);
    }

    public void updateState(StressBasedController controller, double timeSeconds) {

    }

    public void controllerReady(StressBasedController controller, SignalSystem system) {
    }


    public void reset(StressBasedController controller, Integer iterationNumber) {

    }

    public void simulationInitialized(StressBasedController controller, double simStartTimeSeconds) {

    }
}
