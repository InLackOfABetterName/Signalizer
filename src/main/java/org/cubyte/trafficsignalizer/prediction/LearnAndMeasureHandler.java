package org.cubyte.trafficsignalizer.prediction;

import com.google.inject.Inject;
import org.cubyte.trafficsignalizer.SignalizerConfigGroup;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

public class LearnAndMeasureHandler implements IterationEndsListener {

    private final PredictionNetwork predictionNetwork;
    private final NodeTraverseHandler nodeTraverseHandler;
    private final SignalizerConfigGroup signalizerConfig;

    @Inject
    public LearnAndMeasureHandler(PredictionNetwork predictionNetwork, NodeTraverseHandler nodeTraverseHandler,
                                  SignalizerConfigGroup signalizerConfig) {
        this.predictionNetwork = predictionNetwork;
        this.nodeTraverseHandler = nodeTraverseHandler;
        this.signalizerConfig = signalizerConfig;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (signalizerConfig.isLearn()) {
            predictionNetwork.learn(nodeTraverseHandler.getDataSets());
            predictionNetwork.save(signalizerConfig.getNeuralNetworkSaveFolder());
        }
        System.out.println("Network error: " + predictionNetwork.measureError(nodeTraverseHandler.getDataSets()));
    }
}
