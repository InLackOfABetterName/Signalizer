package org.cubyte.trafficsignalizer.tracker;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubyte.trafficsignalizer.prediction.PredictionNetwork;
import org.cubyte.trafficsignalizer.sensors.TrafficSensorFactory;
import org.cubyte.trafficsignalizer.sensors.events.EnteringTrafficEvent;
import org.cubyte.trafficsignalizer.sensors.handlers.EnteringTrafficHandler;
import org.cubyte.trafficsignalizer.sensors.sensors.EnteringTrafficSensor;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

import java.util.*;

@Singleton
public class PredictedTrafficTracker implements TrafficTracker, MobsimBeforeSimStepListener {

    private final Network network;
    private final PredictionNetwork predictionNetwork;
    private final Map<Id<Link>, List<TrackedVehicle>> trackedVehicleQueques;
    private final Random random;

    @Inject
    public PredictedTrafficTracker(Network network, PredictionNetwork predictionNetwork,
                                   TrafficSensorFactory trafficSensorFactory, EventsManager em) {
        this.network = network;
        this.predictionNetwork = predictionNetwork;
        this.trackedVehicleQueques = new HashMap<>();
        this.random = new Random();
        for (Map.Entry<Id<Link>, ? extends Link> link : network.getLinks().entrySet()) {
            this.trackedVehicleQueques.put(link.getKey(), new ArrayList<>());
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
        for (Map.Entry<Id<Link>, List<TrackedVehicle>> entry : trackedVehicleQueques.entrySet()) {
            Link link = network.getLinks().get(entry.getKey());
            double travelTime = link.getLength() / link.getFreespeed();
            for (TrackedVehicle vehicle : entry.getValue()) {
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
                        vehicle.setCurrentLinkEnteredTime(timeWhenEnteredNewLink);
                    } else {
                        toRemove.get(link.getId()).add(vehicle);
                    }
                }
            }
        }
        trackedVehicleQueques.entrySet().stream().forEach(entry -> {
            entry.getValue().addAll(toAdd.get(entry.getKey()));
            entry.getValue().removeAll(toRemove.get(entry.getKey()));
        });
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
        }
    }
}
