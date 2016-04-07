package org.cubyte.trafficsignalizer.trafficsensors;

import org.cubyte.trafficsignalizer.trafficsensors.events.TrafficSensorEvent;
import org.matsim.api.core.v01.Id;
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
        eventsManager.processEvent(event);
    }
}
