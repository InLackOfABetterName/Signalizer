package org.cubyte.trafficsignalizer.sensors.handlers;

import org.cubyte.trafficsignalizer.sensors.events.CountingTrafficEvent;
import org.matsim.core.events.handler.EventHandler;

public interface CountingTrafficHandler extends EventHandler {
    void handleEvent(CountingTrafficEvent event);
}
