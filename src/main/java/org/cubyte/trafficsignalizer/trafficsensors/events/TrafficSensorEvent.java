package org.cubyte.trafficsignalizer.trafficsensors.events;

import org.matsim.api.core.v01.events.Event;

public abstract class TrafficSensorEvent extends Event {
    public TrafficSensorEvent(double time) {
        super(time);
    }
}
