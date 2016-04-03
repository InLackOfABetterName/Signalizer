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

    private final SignalNetworkController networkController;

    public SignalizerModule(SignalNetworkController networkController) {
        this.networkController = networkController;
    }

    public void install() {
    }

    private class SignalizerSignalModelFactory implements SignalModelFactory {

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
            } else {
                SignalizerController c = new SignalizerController(networkController);
                networkController.addController(c);
                return c;
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
