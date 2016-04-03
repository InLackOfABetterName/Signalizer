package org.cubyte.trafficsignalizer;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;

import javax.inject.Inject;

public class NodeTraverseHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {
    @Inject
    public NodeTraverseHandler(EventsManager eventsManager) {
        eventsManager.addHandler(this);
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {

    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {

    }

    @Override
    public void reset(int iteration) {

    }
}
