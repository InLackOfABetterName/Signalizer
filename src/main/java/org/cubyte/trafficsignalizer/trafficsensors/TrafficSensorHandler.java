package org.cubyte.trafficsignalizer.trafficsensors;

import org.cubyte.trafficsignalizer.trafficsensors.events.TrafficSensorEvent;
import org.matsim.core.events.handler.EventHandler;

public interface TrafficSensorHandler<T extends TrafficSensorEvent> extends EventHandler {
    void handleEvent(T event);
}
