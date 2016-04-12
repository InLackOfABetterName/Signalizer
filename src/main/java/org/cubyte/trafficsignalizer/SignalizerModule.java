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
import org.cubyte.trafficsignalizer.tracker.PredictedTrafficTracker;
import org.cubyte.trafficsignalizer.tracker.TrafficTracker;
import org.cubyte.trafficsignalizer.ui.OTFVisMobsimListener;
import org.cubyte.trafficsignalizer.ui.TextObject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.builder.SignalModelFactory;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.replanning.ReplanningContext;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;


public class SignalizerModule extends AbstractModule {

    private final SignalizerParams params;

    public SignalizerModule(SignalizerParams params) {
        this.params = params;
    }

    public void install() {
        addOrGetModule(getConfig(), SignalizerConfigGroup.GROUPNAME, SignalizerConfigGroup.class);
        if (!this.params.learn) {
            this.addMobsimListenerBinding().to(OTFVisMobsimListener.class);
        }
        this.bind(SignalizerParams.class).toInstance(this.params);
        this.bind(NodeTraverseHandler.class);
        this.bind(SignalNetworkController.class);
        this.addEventHandlerBinding().to(NodeTraverseHandler.class);
        this.addControlerListenerBinding().to(LearnAndMeasureHandler.class);
        this.bind(StressFunction.class).to(CarCountStressFunction.class);
        this.bind(SignalModelFactory.class).to(SignalizerSignalModelFactory.class);
        this.bind(StressBasedController.class);
        if (this.params.learn) {
            this.bind(TrafficTracker.class).to(AllKnowingTrafficTracker.class);
        } else {
            this.bind(TrafficTracker.class).to(PredictedTrafficTracker.class);
        }
        this.bind(TrafficSensorFactory.class);
        this.bind(TextObject.Writer.class).asEagerSingleton();
        this.bind(AllKnowingTrafficTracker.class);
        this.addMobsimListenerBinding().to(PredictedTrafficTracker.class);
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

}
