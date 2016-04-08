package org.cubyte.trafficsignalizer.traffictracker;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public interface TrafficTracker {
    int getCarCount(Id<Link> link);
}
