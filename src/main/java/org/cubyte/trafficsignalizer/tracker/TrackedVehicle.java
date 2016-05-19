package org.cubyte.trafficsignalizer.tracker;

import java.util.ArrayList;
import java.util.List;

public class TrackedVehicle {

    private List<TrackedVehicle> clones;
    private double currentLinkEnteredTime;
    private double probability;

    public TrackedVehicle(double currentLinkEnteredTime) {
        this.clones = new ArrayList<>();
        this.clones.add(this);
        this.currentLinkEnteredTime = currentLinkEnteredTime;
        this.probability = 1.0;
    }

    public TrackedVehicle(List<TrackedVehicle> clones, double currentLinkEnteredTime, double probability) {
        this.clones = clones;
        this.clones.add(this);
        this.currentLinkEnteredTime = currentLinkEnteredTime;
        this.probability = probability;
    }

    public double getCurrentLinkEnteredTime() {
        return currentLinkEnteredTime;
    }

    public void setCurrentLinkEnteredTime(double currentLinkEnteredTime) {
        this.currentLinkEnteredTime = currentLinkEnteredTime;
    }

    /**
     * Splits the vehicle and gives back the set of vehicles ordered like the probabilities
     */
    public List<TrackedVehicle> split(double[] probabilities) {
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] = probabilities[i] * this.probability;
        }
        this.probability = probabilities[0];
        List<TrackedVehicle> vehicles = new ArrayList<>();
        vehicles.add(this);
        for (int i = 1; i < probabilities.length; i++) {
            vehicles.add(new TrackedVehicle(this.clones, this.currentLinkEnteredTime, probabilities[i]));
        }
        return vehicles;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
        if (probability > 1) {
            this.probability = 1;
        } else if (probability < 0) {
            this.probability = 0;
        }
    }
}
