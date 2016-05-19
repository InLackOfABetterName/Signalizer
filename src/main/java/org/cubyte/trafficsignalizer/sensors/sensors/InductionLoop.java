package org.cubyte.trafficsignalizer.sensors.sensors;

import org.cubyte.trafficsignalizer.sensors.events.VehiclePresentChangedEvent;
import org.cubyte.trafficsignalizer.tracker.TrackedLink;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;

public class InductionLoop extends TrafficSensor<VehiclePresentChangedEvent> {

    public int vehiclesPresent = 0;

    public InductionLoop(EventsManager eventsManager, Id<Link> linkId) {
        super(eventsManager, linkId);
    }

    @Override
    public void handleLinkEnter(LinkEnterEvent event) {
        if (vehiclesPresent == 0) {
            processEvent(new VehiclePresentChangedEvent(event.getTime(), true));
        }
        vehiclesPresent++;
    }

    @Override
    public void handleLinkLeave(LinkLeaveEvent event) {
        if (vehiclesPresent == 1) {
            processEvent(new VehiclePresentChangedEvent(event.getTime(), false));
        }
        vehiclesPresent--;
        vehiclesPresent = vehiclesPresent < 0 ? 0 : vehiclesPresent;
    }

    @Override
    public void handlePersonArrival(PersonArrivalEvent event) {
        if (vehiclesPresent == 0) {
            processEvent(new VehiclePresentChangedEvent(event.getTime(), true));
        }
        vehiclesPresent++;
    }

    @Override
    public void handlePersonDeparture(PersonDepartureEvent event) {
        if (vehiclesPresent == 1) {
            processEvent(new VehiclePresentChangedEvent(event.getTime(), false));
        }
        vehiclesPresent--;
        vehiclesPresent = vehiclesPresent < 0 ? 0 : vehiclesPresent;
    }

    @Override
    public void correctErrorIfNeeded(TrackedLink link) {
        if (vehiclesPresent > 0 && link.getVehicleCount() == 0) {
            link.raiseVehicleCountTo(1);
        } else if (vehiclesPresent == 0 && link.getVehicleCount() > 0) {
            link.reduceVehicleCountTo(0);
        }
    }

    @Override
    public void reset(int iteration) {

    }
}
