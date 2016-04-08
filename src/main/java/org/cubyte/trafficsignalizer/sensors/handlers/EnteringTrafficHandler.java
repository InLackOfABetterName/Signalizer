package org.cubyte.trafficsignalizer.sensors.handlers;

import org.cubyte.trafficsignalizer.sensors.events.EnteringTrafficEvent;
import org.matsim.core.events.handler.EventHandler;

public interface EnteringTrafficHandler extends EventHandler {
    void handleEvent(EnteringTrafficEvent event);
}
