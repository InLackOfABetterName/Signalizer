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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.network.NetworkImpl;

import java.util.*;

import static org.matsim.core.mobsim.qsim.interfaces.SignalGroupState.GREEN;

@Singleton
public class PredictedTrafficTracker implements TrafficTracker, MobsimBeforeSimStepListener {

    private final Network network;
    private final PredictionNetwork predictionNetwork;
    private final SignalNetworkController signalNetworkController;
    private final TextObject.Writer textWriter;
    private final Map<Id<Link>, TrackedLink> trackedLinks;
    private final Random random;
    private double lastSimStepTime;
    private Map<Id<Link>, Double> remainingTime; // Time that remains after vehicles exiting the link.
    // Only an even amount of vehicles can exit a link in a simstep

    @Inject
    public PredictedTrafficTracker(Network network, PredictionNetwork predictionNetwork, TextObject.Writer textWriter,
                                   SignalNetworkController signalNetworkController, EventsManager em,
                                   TrafficSensorFactory trafficSensorFactory, Scenario scenario) {
        this.network = network;
        this.predictionNetwork = predictionNetwork;
        this.signalNetworkController = signalNetworkController;
        this.textWriter = textWriter;
        this.trackedLinks = new HashMap<>();
        this.random = new Random();
        this.lastSimStepTime = 0;
        this.remainingTime = new HashMap<>();
        for (Map.Entry<Id<Link>, ? extends Link> link : network.getLinks().entrySet()) {
            this.trackedLinks.put(link.getKey(), new TrackedLink(link.getValue(),
                    ((NetworkImpl) network).getEffectiveCellSize(), scenario.getConfig().qsim().getStorageCapFactor()));
            this.remainingTime.put(link.getKey(), 0d);
            trafficSensorFactory.createTrafficSensor(EnteringTrafficSensor.class, link.getKey());
        }
        Handler handler = new Handler();
        em.addHandler(handler);
    }

    private void simulate(double simulationTime) {
        double timeSinceLastSimStep = simulationTime - lastSimStepTime;
        for (TrackedLink trackedLink : trackedLinks.values()) {
            Link link = trackedLink.getLink();
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
            if (trackedLink.getVehicleCount() <= capacity) {
                newRemainingTime = 0;
            } else {
                newRemainingTime = capacity % 1 * (timeSinceLastSimStep / capacity);
            }
            int capacityAsInt = (int) Math.floor(capacity);
            for (TrackedVehicle vehicle : trackedLink.getVehicles(capacityAsInt)) {
                double timeSinceEnteredLink = simulationTime - vehicle.getCurrentLinkEnteredTime();
                if (timeSinceEnteredLink >= travelTime) {
                    double timeWhenEnteredNewLink = simulationTime - (timeSinceEnteredLink - travelTime);

                    // TODO take into account that vehicles can come from multiple links and not all from one can fill the link
                    // TODO take into account that vehicles can leave a link randomly to park or something

                    List<Id<Link>> toLinks = new ArrayList<>(trackedLink.getLink().getToNode().getOutLinks().keySet());
                    toLinks.sort(Id<Link>::compareTo);

                    if (toLinks.size() > 0) {
                        if (toLinks.size() > 1) {
                            double[] predictions = predictionNetwork.getPrediction(trackedLink.getLink().getId(), timeWhenEnteredNewLink);

                            boolean fits = true;

                            for (int i = 0; i < predictions.length; i++) {
                                if (trackedLinks.get(toLinks.get(i)).getFreeStorage() < predictions[i]) {
                                    fits = false;
                                    break;
                                }
                            }

                            if (fits) {
                                vehicle.setCurrentLinkEnteredTime(timeWhenEnteredNewLink - remainingTime.get(link.getId()));
                                trackedLink.removeVehicle(vehicle);
                                List<TrackedVehicle> vehicles = vehicle.split(predictions);
                                for (int i = 0; i < vehicles.size(); i++) {
                                    if (vehicles.get(i).getProbability() > 0.001) {
                                        trackedLinks.get(toLinks.get(i)).addVehicle(vehicles.get(i));
                                    }
                                }
                            } else {
                                break;  // do not let through the vehicles following this or this, because this can not drive
                                        // to the next link
                            }
                        } else {
                            TrackedLink newLink = trackedLinks.get(toLinks.get(0));
                            if (newLink.getFreeStorage() > vehicle.getProbability()) {
                                vehicle.setCurrentLinkEnteredTime(timeWhenEnteredNewLink - remainingTime.get(link.getId()));
                                trackedLink.removeVehicle(vehicle);
                                newLink.addVehicle(vehicle);
                            } else {
                                break;  // do not let through the vehicles following this or this, because this can not drive
                                        // to the next link
                            }
                        }
                    } else {
                        trackedLink.removeVehicle(vehicle);
                    }
                }
            }
            remainingTime.put(link.getId(), newRemainingTime);
        }
        int vehicleCount = 0;
        for (TrackedLink trackedLink : trackedLinks.values()) {
            trackedLink.resolve();
            vehicleCount += trackedLink.getVehicleCount();
        }
        textWriter.put("vehicle_count", "vehicles in network: " + vehicleCount, 10, 10, true);
        lastSimStepTime = simulationTime;
    }

    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
        simulate(e.getSimulationTime());
    }

    @Override
    public double carCountAt(Id<Link> link) {
        return trackedLinks.get(link).getVehicleCount();
    }

    private class Handler implements EnteringTrafficHandler {

        @Override
        public void handleEvent(EnteringTrafficEvent event) {
            trackedLinks.get(event.getLinkId()).addVehicle(new TrackedVehicle(event.getTime())).resolve();
        }

        @Override
        public void reset(int iteration) {
            for (TrackedLink trackedLink : trackedLinks.values()) {
                trackedLink.reset();
            }
            lastSimStepTime = 0;
            for (Id<Link> linkId: network.getLinks().keySet()) {
                remainingTime.put(linkId, 0d);
            }
        }
    }
}
