package org.cubyte.trafficsignalizer;

import com.google.inject.Provides;
import org.cubyte.trafficsignalizer.prediction.LearnAndMeasureHandler;
import org.cubyte.trafficsignalizer.prediction.NodeTraverseHandler;
import org.cubyte.trafficsignalizer.prediction.PredictionNetwork;
import org.cubyte.trafficsignalizer.sensors.TrafficSensorFactory;
import org.cubyte.trafficsignalizer.signal.SignalNetworkController;
import org.cubyte.trafficsignalizer.signal.SignalizerSignalModelFactory;
import org.cubyte.trafficsignalizer.signal.StressBasedController;
import org.cubyte.trafficsignalizer.signal.stress.CarCountStressFunction;
import org.cubyte.trafficsignalizer.signal.stress.StressFunction;
import org.cubyte.trafficsignalizer.tracker.AllKnowingTrafficTracker;
import org.cubyte.trafficsignalizer.tracker.TrafficTracker;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.builder.SignalModelFactory;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.contrib.signals.otfvis.OTFClientLiveWithSignals;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignals;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.vis.otfvis.OnTheFlyServer;

import javax.inject.Inject;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;


public class SignalizerModule extends AbstractModule {

    private final boolean learn;

    public SignalizerModule(boolean learn) {
        this.learn = learn;
    }

    public void install() {
        addOrGetModule(getConfig(), SignalizerConfigGroup.GROUPNAME, SignalizerConfigGroup.class).setLearn(this.learn);
        if (!this.learn) {
            this.addMobsimListenerBinding().to(OTFVisMobsimListener.class);
        }
        this.bind(NodeTraverseHandler.class);
        this.bind(SignalNetworkController.class);
        this.addEventHandlerBinding().to(NodeTraverseHandler.class);
        this.addControlerListenerBinding().to(LearnAndMeasureHandler.class);
        this.bind(StressFunction.class).to(CarCountStressFunction.class);
        this.bind(SignalModelFactory.class).to(SignalizerSignalModelFactory.class);
        this.bind(StressBasedController.class);
        this.bind(TrafficTracker.class).to(AllKnowingTrafficTracker.class).asEagerSingleton();
        this.bind(TrafficSensorFactory.class);
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
            OnTheFlyServer server = OTFVisWithSignals.startServerAndRegisterWithQSim(s.getConfig(), s, events, qSim);
            OTFClientLiveWithSignals.run(s.getConfig(), server);
        }
    }
}
