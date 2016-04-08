package org.cubyte.trafficsignalizer.traffictracker;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class TrackedVehicle {

    private Id<Link> currentLinkId;
    private double   currentLinkEnteredTime;

    public TrackedVehicle(Id<Link> currentLinkId, double currentLinkEnteredTime) {
        this.currentLinkId = currentLinkId;
        this.currentLinkEnteredTime = currentLinkEnteredTime;
    }

    public Id<Link> getCurrentLinkId() {
        return currentLinkId;
    }

    public void setCurrentLinkId(Id<Link> currentLinkId) {
        this.currentLinkId = currentLinkId;
    }

    public double getCurrentLinkEnteredTime() {
        return currentLinkEnteredTime;
    }

    public void setCurrentLinkEnteredTime(double currentLinkEnteredTime) {
        this.currentLinkEnteredTime = currentLinkEnteredTime;
    }
}
