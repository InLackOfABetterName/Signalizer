package org.cubyte.trafficsignalizer.stress;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubyte.trafficsignalizer.trafficsensors.TrafficSensorFactory;
import org.cubyte.trafficsignalizer.trafficsensors.TrafficSensorHandler;
import org.cubyte.trafficsignalizer.trafficsensors.events.CountingTrafficEvent;
import org.cubyte.trafficsignalizer.trafficsensors.sensors.AllKnowingTrafficSensor;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class LinkTrafficTracker {

    private Map<Id<Link>, Integer> linkState = new HashMap<>();

    @Inject
    public LinkTrafficTracker(Network network, TrafficSensorFactory trafficSensorFactory, EventsManager em) {
        this.linkState = new HashMap<>();
        for (Map.Entry<Id<Link>, ? extends Link> link : network.getLinks().entrySet()) {
            trafficSensorFactory.createTrafficSensor(AllKnowingTrafficSensor.class, link.getKey());
        }
        em.addHandler(new Handler());
    }

    public int getCarCount(Id<Link> link) {
        final Integer count = linkState.get(link);
        return count != null ? count : 0;
    }

    private class Handler implements TrafficSensorHandler<CountingTrafficEvent> {

        @Override
        public void handleEvent(CountingTrafficEvent event) {
            setLinkState(event.getLinkId(), event.vehicles);
        }

        private void setLinkState(Id<Link> link, int count) {
            linkState.put(link, count);
        }

        @Override
        public void reset(int iteration) {
            linkState.clear();
        }
    }
}
