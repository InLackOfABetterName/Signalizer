package org.cubyte.trafficsignalizer.tracker;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public interface TrafficTracker {
    double carCountAt(Id<Link> link);
}
