package org.cubyte.trafficsignalizer.trafficsensors;

import com.google.inject.Inject;
import org.cubyte.trafficsignalizer.trafficsensors.events.CountingTrafficEvent;
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
        vehicles = 0;
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {

    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {

    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {

    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {

    }

    @Override
    public void reset(int iteration) {

    }
}
