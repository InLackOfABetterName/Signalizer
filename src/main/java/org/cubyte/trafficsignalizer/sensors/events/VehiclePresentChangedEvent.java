package org.cubyte.trafficsignalizer.sensors.events;

public class VehiclePresentChangedEvent extends TrafficSensorEvent {

    public final boolean vehiclePresent;

    public VehiclePresentChangedEvent(double time, boolean vehiclePresent) {
        super(time);
        this.vehiclePresent = vehiclePresent;
    }

    @Override
    public String getEventType() {
        return "vehiclePresentChangedEvent";
    }
}
