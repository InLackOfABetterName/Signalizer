package org.cubyte.trafficsignalizer.tracker;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubyte.trafficsignalizer.sensors.TrafficSensorFactory;
import org.cubyte.trafficsignalizer.sensors.handlers.CountingTrafficHandler;
import org.cubyte.trafficsignalizer.sensors.events.CountingTrafficEvent;
import org.cubyte.trafficsignalizer.sensors.sensors.AllKnowingTrafficSensor;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class AllKnowingTrafficTracker implements TrafficTracker {

    private Map<Id<Link>, Integer> linkState = new HashMap<>();

    @Inject
    public AllKnowingTrafficTracker(Network network, TrafficSensorFactory trafficSensorFactory, EventsManager em) {
        this.linkState = new HashMap<>();
        for (Map.Entry<Id<Link>, ? extends Link> link : network.getLinks().entrySet()) {
            trafficSensorFactory.createTrafficSensor(AllKnowingTrafficSensor.class, link.getKey());
        }
        em.addHandler(new Handler());
    }

    public double carCountAt(Id<Link> link) {
        final Integer count = linkState.get(link);
        return count != null ? count : 0;
    }

    private class Handler implements CountingTrafficHandler {

        @Override
        public void handleEvent(CountingTrafficEvent event) {
            linkState.put(event.getLinkId(), event.vehicles);
        }

        @Override
        public void reset(int iteration) {
            linkState.clear();
        }
    }
}
