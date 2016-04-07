package org.cubyte.trafficsignalizer.trafficsensors.events;

public class CountingTrafficEvent extends TrafficSensorEvent {

    public final int vehicles;

    public CountingTrafficEvent(double time, int vehicles) {
        super(time);
        this.vehicles = vehicles;
    }

    @Override
    public String getEventType() {
        return "countingTrafficEvent";
    }
}
