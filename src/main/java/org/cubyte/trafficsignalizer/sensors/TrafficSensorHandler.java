package org.cubyte.trafficsignalizer.sensors;

import org.cubyte.trafficsignalizer.sensors.events.TrafficSensorEvent;
import org.matsim.core.events.handler.EventHandler;

public interface TrafficSensorHandler<T extends TrafficSensorEvent> extends EventHandler {
    void handleEvent(T event);
}
