package org.cubyte.trafficsignalizer.sensors.sensors;

import org.cubyte.trafficsignalizer.sensors.events.EnteringTrafficEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;

public class EnteringTrafficSensor extends TrafficSensor<EnteringTrafficEvent> {

    public EnteringTrafficSensor(EventsManager eventsManager, Id<Link> linkId) {
        super(eventsManager, linkId);
    }

    @Override
    public void handleLinkEnter(LinkEnterEvent event) {
    }

    @Override
    public void handleLinkLeave(LinkLeaveEvent event) {
    }

    @Override
    public void handlePersonArrival(PersonArrivalEvent event) {
    }

    @Override
    public void handlePersonDeparture(PersonDepartureEvent event) {
        processEvent(new EnteringTrafficEvent(event.getTime()));
    }

    @Override
    public void reset(int iteration) {
    }
}
