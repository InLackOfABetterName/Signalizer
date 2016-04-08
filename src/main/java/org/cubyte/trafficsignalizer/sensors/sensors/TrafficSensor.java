package org.cubyte.trafficsignalizer.sensors.sensors;

import org.cubyte.trafficsignalizer.sensors.events.TrafficSensorEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;

public abstract class TrafficSensor<T extends TrafficSensorEvent> implements LinkEnterEventHandler,
        LinkLeaveEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler {

    private EventsManager eventsManager;
    protected Id<Link> linkId;

    public TrafficSensor(EventsManager eventsManager, Id<Link> linkId) {
        this.eventsManager = eventsManager;
        this.linkId = linkId;
    }

    protected void processEvent(T event) {
        event.setLinkId(linkId);
        eventsManager.processEvent(event);
    }

    public abstract void handleLinkEnter(LinkEnterEvent event);
    public abstract void handleLinkLeave(LinkLeaveEvent event);
    public abstract void handlePersonArrival(PersonArrivalEvent event);
    public abstract void handlePersonDeparture(PersonDepartureEvent event);

    @Override
    public void handleEvent(LinkEnterEvent event) {
        if (linkId.equals(event.getLinkId())) {
            handleLinkEnter(event);
        }
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        if (linkId.equals(event.getLinkId())) {
            handleLinkLeave(event);
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (linkId.equals(event.getLinkId())) {
            handlePersonArrival(event);
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (linkId.equals(event.getLinkId())) {
            handlePersonDeparture(event);
        }
    }
}
