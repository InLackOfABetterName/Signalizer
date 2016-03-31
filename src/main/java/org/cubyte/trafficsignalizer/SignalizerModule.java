package org.cubyte.trafficsignalizer;

import com.google.inject.Provides;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.builder.DefaultSignalModelFactory;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.builder.SignalModelFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.model.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.replanning.ReplanningContext;

public class SignalizerModule extends AbstractModule {
    public void install() {

    }

    private static class SignalizerSignalModelFactory implements SignalModelFactory {

        private final SignalModelFactory defaultImpl = new DefaultSignalModelFactory();

        public SignalSystemsManager createSignalSystemsManager() {
            return defaultImpl.createSignalSystemsManager();
        }

        public SignalSystem createSignalSystem(Id<SignalSystem> id) {
            return defaultImpl.createSignalSystem(id);
        }

        @SuppressWarnings("unchecked")
        public SignalController createSignalSystemController(String controllerIdentifier) {
            if (controllerIdentifier.equals(DefaultPlanbasedSignalSystemController.IDENTIFIER)) {
                return new DefaultPlanbasedSignalSystemController();
            }
            try {
                Class controllerClass = Class.forName(controllerIdentifier);
                if (SignalController.class.isAssignableFrom(controllerClass) && controllerClass.getConstructor() != null) {
                    return (SignalController)controllerClass.newInstance();
                } else {
                    throw new IllegalArgumentException("Controller " + controllerIdentifier + " does not implement " + SignalController.class.getName() + " or does not have a default constructor!");
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Controller " + controllerIdentifier + " not found!");
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("Could not load controller " + controllerIdentifier + ": " + e.getLocalizedMessage());
            }
        }

        public SignalPlan createSignalPlan(SignalPlanData planData) {
            return defaultImpl.createSignalPlan(planData);
        }
    }

    @Provides
    SignalSystemsManager provideSignalSystemsManager(Scenario scenario, EventsManager eventsManager, ReplanningContext replanningContext) {
        FromDataBuilder modelBuilder = new FromDataBuilder(scenario, new SignalizerSignalModelFactory(), eventsManager);
        SignalSystemsManager signalSystemsManager = modelBuilder.createAndInitializeSignalSystemsManager();
        signalSystemsManager.resetModel(replanningContext.getIteration());
        return signalSystemsManager;
    }
}
