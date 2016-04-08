package org.cubyte.trafficsignalizer.sensors.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;

public abstract class TrafficSensorEvent extends Event {

    private Id<Link> linkId;

    public TrafficSensorEvent(double time) {
        super(time);
    }

    public void setLinkId(Id<Link> linkId) {
        this.linkId = linkId;
    }

    public Id<Link> getLinkId() {
        return linkId;
    }
}
