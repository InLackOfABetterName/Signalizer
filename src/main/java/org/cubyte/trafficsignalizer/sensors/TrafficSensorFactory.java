package org.cubyte.trafficsignalizer.sensors;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubyte.trafficsignalizer.sensors.events.TrafficSensorEvent;
import org.cubyte.trafficsignalizer.sensors.sensors.TrafficSensor;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;

import java.lang.reflect.InvocationTargetException;

@Singleton
public class TrafficSensorFactory {

    private EventsManager eventsManager;

    @Inject
    public TrafficSensorFactory(EventsManager eventsManager) {
        this.eventsManager = eventsManager;
    }

    public <T extends TrafficSensorEvent> TrafficSensor<T> createTrafficSensor(Class<? extends TrafficSensor<T>> clazz,
                                                                               Id<Link> linkId) {
        try {
            TrafficSensor<T> sensor = clazz.getConstructor(EventsManager.class, Id.class).newInstance(eventsManager, linkId);
            eventsManager.addHandler(sensor);
            return sensor;
        } catch (NoSuchMethodException e) {
            System.err.println("Could not create traffic sensor." +
                    "The constructor of the TrafficSensor should not be overriden with another default constructor " +
                    "with different arguments.");
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            // do nothing
        }
        return null;
    }
}
