package org.cubyte.trafficsignalizer;

import com.google.inject.Provides;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.contrib.signals.builder.DefaultSignalModelFactory;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.builder.SignalModelFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.model.*;
import org.matsim.contrib.signals.otfvis.OTFClientLiveWithSignals;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.vis.otfvis.OnTheFlyServer;

import javax.inject.Inject;


public class SignalizerModule extends AbstractModule {

    private final boolean learn;

    public SignalizerModule(boolean learn) {
        this.learn = learn;
    }

    public void install() {
        this.addMobsimListenerBinding().to(OTFVisMobsimListener.class);
        this.bind(NodeTraverseHandler.class);
        this.bind(SignalNetworkController.class);
        this.addEventHandlerBinding().to(NodeTraverseHandler.class);
        this.addControlerListenerBinding().to(LearnAndMeasureHandler.class);
    }

    public static class SignalizerSignalModelFactory implements SignalModelFactory {

        private final SignalModelFactory defaultImpl = new DefaultSignalModelFactory();
        private final SignalNetworkController networkController;

        public SignalizerSignalModelFactory(SignalNetworkController c) {
            networkController = c;
        }

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
    SignalModelFactory provideSignalizerSignalModelFactory(SignalNetworkController c) {
        return new SignalizerSignalModelFactory(c);
    }

    @Provides
    FromDataBuilder provideFromDataBuilder(Scenario scenario, SignalModelFactory modelFactory, EventsManager eventsManager) {
        return new FromDataBuilder(scenario, modelFactory, eventsManager);
    }

    @Provides
    SignalSystemsManager provideSignalSystemsManager(FromDataBuilder modelBuilder, ReplanningContext replanningContext) {
        SignalSystemsManager signalSystemsManager = modelBuilder.createAndInitializeSignalSystemsManager();
        signalSystemsManager.resetModel(replanningContext.getIteration());
        return signalSystemsManager;
    }

    @Provides
    PredictionNetwork providePredictionNetwork(Network network, SignalizerConfigGroup signalizerConfig) {
        PredictionNetwork predictionNetwork;
        if (signalizerConfig.getNeuralNetworkSaveFolder() != null) {
            predictionNetwork = new PredictionNetwork(network, signalizerConfig.getNeuralNetworkSaveFolder());
        } else {
            predictionNetwork = new PredictionNetwork(network, "");
        }
        return predictionNetwork;
    }

    public static class OTFVisMobsimListener implements MobsimInitializedListener {
        @Inject
        private Scenario s;

        @Inject
        private EventsManager events;

        @Override
        public void notifyMobsimInitialized(MobsimInitializedEvent e) {
            QSim qSim = (QSim) e.getQueueSimulation();
            OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(s.getConfig(), s, events, qSim);
            OTFClientLiveWithSignals.run(s.getConfig(), server);
        }
    }
}
