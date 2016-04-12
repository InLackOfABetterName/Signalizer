package org.cubyte.trafficsignalizer.tracker;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubyte.trafficsignalizer.prediction.PredictionNetwork;
import org.cubyte.trafficsignalizer.sensors.TrafficSensorFactory;
import org.cubyte.trafficsignalizer.sensors.events.EnteringTrafficEvent;
import org.cubyte.trafficsignalizer.sensors.handlers.EnteringTrafficHandler;
import org.cubyte.trafficsignalizer.sensors.sensors.EnteringTrafficSensor;
import org.cubyte.trafficsignalizer.signal.SignalNetworkController;
import org.cubyte.trafficsignalizer.ui.TextObject;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.util.*;

import static org.matsim.core.mobsim.qsim.interfaces.SignalGroupState.GREEN;
import static org.matsim.vis.otfvis.data.OTFServerQuadTree.getOTFTransformation;

@Singleton
public class PredictedTrafficTracker implements TrafficTracker, MobsimBeforeSimStepListener {

    private final Network network;
    private final PredictionNetwork predictionNetwork;
    private final SignalNetworkController signalNetworkController;
    private final TextObject.Writer textWriter;
    private final Map<Id<Link>, List<TrackedVehicle>> trackedVehicleQueques;
    private final Random random;
    private double lastSimStepTime;
    private Map<Id<Link>, Double> remainingTime; // Time that remains after vehicles exiting the link.
    // Only an even amount of vehicles can exit a link in a simstep

    @Inject
    public PredictedTrafficTracker(Network network, PredictionNetwork predictionNetwork, TextObject.Writer textWriter,
                                   SignalNetworkController signalNetworkController, EventsManager em,
                                   TrafficSensorFactory trafficSensorFactory) {
        this.network = network;
        this.predictionNetwork = predictionNetwork;
        this.signalNetworkController = signalNetworkController;
        this.textWriter = textWriter;
        this.trackedVehicleQueques = new HashMap<>();
        this.random = new Random();
        this.lastSimStepTime = 0;
        this.remainingTime = new HashMap<>();
        for (Map.Entry<Id<Link>, ? extends Link> link : network.getLinks().entrySet()) {
            this.trackedVehicleQueques.put(link.getKey(), new ArrayList<>());
            this.remainingTime.put(link.getKey(), 0d);
            trafficSensorFactory.createTrafficSensor(EnteringTrafficSensor.class, link.getKey());
        }
        Handler handler = new Handler();
        em.addHandler(handler);
    }

    private void simulate(double simulationTime) {
        Map<Id<Link>, List<TrackedVehicle>> toAdd = new HashMap<>();
        Map<Id<Link>, List<TrackedVehicle>> toRemove = new HashMap<>();
        trackedVehicleQueques.entrySet().stream().forEach(entry -> {
            toAdd.put(entry.getKey(), new ArrayList<>());
            toRemove.put(entry.getKey(), new ArrayList<>());
        });
        double timeSinceLastSimStep = simulationTime - lastSimStepTime;
        for (Map.Entry<Id<Link>, List<TrackedVehicle>> entry : trackedVehicleQueques.entrySet()) {
            Link link = network.getLinks().get(entry.getKey());
            Optional<Set<SignalGroup>> groupsOptional = signalNetworkController.getGroupsAtLink(link.getId());
            if (groupsOptional.isPresent()) {
                Set<SignalGroup> groups = groupsOptional.get();
                if (!groups.stream().anyMatch(group -> group.getState() == GREEN)) {
                    remainingTime.put(link.getId(), 0d);
                    continue;
                }
            }

            /*CoordinateTransformation trans = getOTFTransformation();
            if (trans != null) {
                Coord coord = trans.transform(link.getToNode().getCoord());
                textWriter.put("vehicles_on_link_" + link.getId(), String.valueOf(entry.getValue().size()), coord.getX(), coord.getY(), false);
            }*/

            double travelTime = link.getLength() / link.getFreespeed();
            timeSinceLastSimStep += remainingTime.get(link.getId());
            double capacity = link.getCapacity() * timeSinceLastSimStep / 3600;
            double newRemainingTime;
            if (entry.getValue().size() <= capacity) {
                newRemainingTime = 0;
            } else {
                newRemainingTime = capacity % 1 * (timeSinceLastSimStep / capacity);
            }
            int capacityAsInt = (int) Math.floor(capacity);
            int vehicleCount = entry.getValue().size();
            for (TrackedVehicle vehicle : entry.getValue().subList(0, capacityAsInt > vehicleCount ? vehicleCount : capacityAsInt )) {
                double timeSinceEnteredLink = simulationTime - vehicle.getCurrentLinkEnteredTime();
                if (timeSinceEnteredLink >= travelTime) {
                    List<Id<Link>> toLinks = new ArrayList<>(link.getToNode().getOutLinks().keySet());
                    toLinks.sort(Id<Link>::compareTo);
                    Id<Link> newLinkId;
                    if (toLinks.size() > 0) {
                        double timeWhenEnteredNewLink = simulationTime - (timeSinceEnteredLink - travelTime);
                        if (toLinks.size() > 1) {
                            double[] predictions = predictionNetwork.getPrediction(link.getId(), 1, timeWhenEnteredNewLink);
                            double acc = 0;
                            for (double prediction : predictions) {
                                acc += prediction;
                            }
                            acc /= predictions.length;
                            for (int i = 0; i < predictions.length; i++) {
                                predictions[i] /= acc;
                            }
                            double choice = random.nextDouble();
                            acc = 0;
                            for (int i = 0; i < predictions.length; i++) {
                                acc += predictions[i];
                                if (choice <= acc) {
                                    choice = i;
                                    break;
                                }
                            }
                            newLinkId = toLinks.get((int) choice);
                        } else {
                            newLinkId = toLinks.get(0);
                        }
                        toAdd.get(newLinkId).add(vehicle);
                        toRemove.get(link.getId()).add(vehicle);
                        vehicle.setCurrentLinkEnteredTime(timeWhenEnteredNewLink - remainingTime.get(link.getId()));
                    } else {
                        toRemove.get(link.getId()).add(vehicle);
                    }
                }
            }
            remainingTime.put(link.getId(), newRemainingTime);
        }
        trackedVehicleQueques.entrySet().stream().forEach(entry -> {
            entry.getValue().addAll(toAdd.get(entry.getKey()));
            entry.getValue().removeAll(toRemove.get(entry.getKey()));
        });
        lastSimStepTime = simulationTime;
    }

    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
        simulate(e.getSimulationTime());
    }

    @Override
    public int getCarCount(Id<Link> link) {
        return trackedVehicleQueques.get(link).size();
    }

    private class Handler implements EnteringTrafficHandler {

        @Override
        public void handleEvent(EnteringTrafficEvent event) {
            trackedVehicleQueques.get(event.getLinkId()).add(new TrackedVehicle(event.getTime()));
        }

        @Override
        public void reset(int iteration) {
            trackedVehicleQueques.entrySet().stream().forEach(entry -> entry.getValue().clear());
            lastSimStepTime = 0;
            for (Id<Link> linkId: network.getLinks().keySet()) {
                remainingTime.put(linkId, 0d);
            }
        }
    }
}
