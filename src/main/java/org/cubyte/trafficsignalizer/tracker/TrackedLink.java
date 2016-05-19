package org.cubyte.trafficsignalizer.tracker;

import org.matsim.api.core.v01.network.Link;

import java.util.ArrayList;
import java.util.List;

public class TrackedLink {
    private int storageCapacity;
    private List<TrackedVehicle> vehicleQueue;
    private Link link;
    private List<TrackedVehicle> toAdd;
    private List<TrackedVehicle> toRemove;

    public TrackedLink(Link link, double effectiveCellSize, double cellStorageCapFactor) {
        //TODO: maybe take into account the flow capacity here like in the QueueWithBuffer class
        this.storageCapacity = (int) Math.ceil(link.getLength() * link.getNumberOfLanes() / effectiveCellSize * cellStorageCapFactor);
        this.vehicleQueue = new ArrayList<>();
        this.toAdd = new ArrayList<>();
        this.toRemove = new ArrayList<>();
        this.link = link;
    }

    /**
     * Adds the vehicle to a list whose entries are added to the actual vehicle queue when calling resolve
     */
    public TrackedLink addVehicle(TrackedVehicle vehicle) {
        toAdd.add(vehicle);
        return this;
    }

    /**
     * Adds the vehicle to a list whose entries are removed from the actual vehicle queue when calling resolve
     */
    public TrackedLink removeVehicle(TrackedVehicle vehicle) {
        toRemove.add(vehicle);
        return this;
    }

    /**
     * Adds the list of added vehicles and removes the list of removed vehicles from the actual vehicle queue
     */
    public void resolve() {
        vehicleQueue.removeAll(toRemove);
        toRemove.clear();
        vehicleQueue.addAll(toAdd);
        toAdd.clear();
    }

    public double getVehicleCount() {
        double count = 0;
        for (TrackedVehicle vehicle : vehicleQueue) {
            count += vehicle.getProbability();
        }
        return count;
    }

    public void reset() {
        vehicleQueue.clear();
    }

    public Link getLink() {
        return link;
    }

    /**
     * Gets the sublist of the vehicle queue from the vehicle standing in front to count (exclusive)
     */
    public List<TrackedVehicle> getVehicles(int count) {
        List<TrackedVehicle> vehicles = new ArrayList<>();
        double countTillNow = 0;
        for (TrackedVehicle vehicle : vehicleQueue) {
            countTillNow += vehicle.getProbability();
            if (countTillNow > count)
                break;
            vehicles.add(vehicle);
        }
        return vehicles;
    }

    public double getFreeStorage() {
        return storageCapacity - ((getVehicleCount() - toRemove.size()) + toAdd.size());
    }

    public void raiseVehicleCountTo(double vehicleCount) {

    }

    public void reduceVehicleCountTo(double vehicleCount) {

    }
}
