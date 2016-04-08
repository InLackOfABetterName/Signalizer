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
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.QSim;

import java.util.*;

@Singleton
public class PredictedTrafficTracker implements TrafficTracker, MobsimBeforeSimStepListener {

    private final Network network;
    private final PredictionNetwork predictionNetwork;
    private final List<TrackedVehicle> trackedVehicles;
    private final Random random;

    @Inject
    public PredictedTrafficTracker(Network network, PredictionNetwork predictionNetwork,
                                   TrafficSensorFactory trafficSensorFactory, EventsManager em) {
        this.network = network;
        this.predictionNetwork = predictionNetwork;
        this.trackedVehicles = new ArrayList<>();
        this.random = new Random();
        for (Map.Entry<Id<Link>, ? extends Link> link : network.getLinks().entrySet()) {
            trafficSensorFactory.createTrafficSensor(EnteringTrafficSensor.class, link.getKey());
        }
        Handler handler = new Handler();
        em.addHandler(handler);
    }

    private void simulate(double simulationTime) {
        List<TrackedVehicle> toRemove = new ArrayList<>();
        for (TrackedVehicle vehicle : trackedVehicles) {
            Link link = network.getLinks().get(vehicle.getCurrentLinkId());
            double travelTime = link.getLength() / link.getFreespeed();
            double timeSinceEnteredLink = simulationTime - vehicle.getCurrentLinkEnteredTime();
            if (timeSinceEnteredLink >= travelTime) {
                List<Id<Link>> toLinks = new ArrayList<>(link.getToNode().getOutLinks().keySet());
                toLinks.sort(Id<Link>::compareTo);
                Id<Link> newLink;
                if (toLinks.size() > 0) {
                    double timeWhenEnteredNewLink = simulationTime - (timeSinceEnteredLink - travelTime);
                    if (toLinks.size() > 1) {
                        double[] predictions = predictionNetwork.getPrediction(vehicle.getCurrentLinkId(), 1, timeWhenEnteredNewLink);
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
                        newLink = toLinks.get((int) choice);
                    } else {
                        newLink = toLinks.get(0);
                    }
                    vehicle.setCurrentLinkId(newLink);
                    vehicle.setCurrentLinkEnteredTime(timeWhenEnteredNewLink);
                } else {
                    toRemove.add(vehicle);
                }
            }
        }
        trackedVehicles.removeAll(toRemove);
    }

    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
        simulate(e.getSimulationTime());
    }

    @Override
    public int getCarCount(Id<Link> link) {
        return (int) trackedVehicles.stream().filter((vehicle) -> vehicle.getCurrentLinkId() == link).count();
    }

    private class Handler implements EnteringTrafficHandler {

        @Override
        public void handleEvent(EnteringTrafficEvent event) {
            trackedVehicles.add(new TrackedVehicle(event.getLinkId(), event.getTime()));
        }

        @Override
        public void reset(int iteration) {
            trackedVehicles.clear();
        }
    }
}
