package org.cubyte.trafficsignalizer.sensors.sensors;

import com.google.inject.Inject;
import org.cubyte.trafficsignalizer.sensors.events.CountingTrafficEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;

public class AllKnowingTrafficSensor extends TrafficSensor<CountingTrafficEvent> {

    private int vehicles;

    @Inject
    public AllKnowingTrafficSensor(EventsManager eventsManager, Id<Link> linkId) {
        super(eventsManager, linkId);
        reset(-1);
    }

    @Override
    public void handleLinkEnter(LinkEnterEvent event) {
        vehicles++;
        processEvent(new CountingTrafficEvent(event.getTime(), vehicles));
    }

    @Override
    public void handleLinkLeave(LinkLeaveEvent event) {
        vehicles--;
        processEvent(new CountingTrafficEvent(event.getTime(), vehicles));
    }

    @Override
    public void handlePersonArrival(PersonArrivalEvent event) {
        vehicles--;
        processEvent(new CountingTrafficEvent(event.getTime(), vehicles));
    }

    @Override
    public void handlePersonDeparture(PersonDepartureEvent event) {
        vehicles++;
        processEvent(new CountingTrafficEvent(event.getTime(), vehicles));
    }

    @Override
    public void reset(int iteration) {
        vehicles = 0;
    }
}
