package org.cubyte.trafficsignalizer.sensors.events;

public class EnteringTrafficEvent extends TrafficSensorEvent {

    public EnteringTrafficEvent(double time) {
        super(time);
    }

    @Override
    public String getEventType() {
        return "enteringTrafficEvent";
    }
}
