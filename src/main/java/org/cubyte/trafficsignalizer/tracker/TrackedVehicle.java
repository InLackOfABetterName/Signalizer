package org.cubyte.trafficsignalizer.tracker;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class TrackedVehicle {

    private double   currentLinkEnteredTime;

    public TrackedVehicle(double currentLinkEnteredTime) {
        this.currentLinkEnteredTime = currentLinkEnteredTime;
    }

    public double getCurrentLinkEnteredTime() {
        return currentLinkEnteredTime;
    }

    public void setCurrentLinkEnteredTime(double currentLinkEnteredTime) {
        this.currentLinkEnteredTime = currentLinkEnteredTime;
    }
}
