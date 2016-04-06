package org.cubyte.trafficsignalizer.stress;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class LinkTrafficTracker {

    private Map<Id<Link>, Integer> linkState = new HashMap<>();

    @Inject
    public LinkTrafficTracker(EventsManager em) {
        this.linkState = new HashMap<>();
        em.addHandler(new Handler());
    }

    public int getCarCount(Id<Link> link) {
        final Integer count = linkState.get(link);
        return count != null ? count : 0;
    }

    private class Handler implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler {

        @Override
        public void handleEvent(LinkEnterEvent event) {
            initializeOrIncrease(event.getLinkId());
        }

        private void initializeOrIncrease(Id<Link> link) {
            final Integer count = linkState.get(link);
            if (count != null) {
                linkState.put(link, count + 1);
            } else {
                linkState.put(link, 1);
            }
        }

        private void decrease(Id<Link> link) {
            final Integer count = linkState.get(link);
            linkState.put(link, count - 1);
        }

        @Override
        public void handleEvent(LinkLeaveEvent event) {
            decrease(event.getLinkId());
        }

        @Override
        public void handleEvent(PersonDepartureEvent event) {
            initializeOrIncrease(event.getLinkId());
        }

        @Override
        public void reset(int iteration) {
            linkState.clear();
        }
    }
}
