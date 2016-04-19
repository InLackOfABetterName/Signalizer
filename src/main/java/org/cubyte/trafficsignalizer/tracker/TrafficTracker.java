package org.cubyte.trafficsignalizer.tracker;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public interface TrafficTracker {
    int carCountAt(Id<Link> link);
}
