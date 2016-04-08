package org.cubyte.trafficsignalizer.traffictracker;

import com.google.inject.Inject;
import org.cubyte.trafficsignalizer.prediction.PredictionNetwork;
import org.cubyte.trafficsignalizer.trafficsensors.TrafficSensorFactory;
import org.cubyte.trafficsignalizer.trafficsensors.TrafficSensorHandler;
import org.cubyte.trafficsignalizer.trafficsensors.events.EnteringTrafficEvent;
import org.cubyte.trafficsignalizer.trafficsensors.sensors.AllKnowingTrafficSensor;
import org.cubyte.trafficsignalizer.trafficsensors.sensors.EnteringTrafficSensor;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PredictedTrafficTracker implements TrafficTracker {

    private final Network network;
    private final PredictionNetwork predictionNetwork;
    private final List<TrackedVehicle> trackedVehicles;
    private double lastSimulationTime;

    @Inject
    public PredictedTrafficTracker(Network network, PredictionNetwork predictionNetwork,
                                   TrafficSensorFactory trafficSensorFactory, EventsManager em) {
        this.network = network;
        this.predictionNetwork = predictionNetwork;
        this.trackedVehicles = new ArrayList<>();
        for (Map.Entry<Id<Link>, ? extends Link> link : network.getLinks().entrySet()) {
            trafficSensorFactory.createTrafficSensor(EnteringTrafficSensor.class, link.getKey());
        }
        em.addHandler(new Handler());
    }

    private void simulate(double simulationTime) {
        // do something

        lastSimulationTime = simulationTime;
    }

    @Override
    public int getCarCount(Id<Link> link) {
        return 0;
    }

    private class Handler implements TrafficSensorHandler<EnteringTrafficEvent>, MobsimBeforeSimStepListener {

        @Override
        public void handleEvent(EnteringTrafficEvent event) {
            trackedVehicles.add(new TrackedVehicle(event.getLinkId(), event.getTime()));
        }

        @Override
        public void reset(int iteration) {
            trackedVehicles.clear();
        }

        @Override
        public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
            simulate(e.getSimulationTime());
        }
    }
}
