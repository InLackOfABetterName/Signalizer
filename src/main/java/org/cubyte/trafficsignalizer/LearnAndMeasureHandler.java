package org.cubyte.trafficsignalizer;

import com.google.inject.Inject;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

public class LearnAndMeasureHandler implements IterationEndsListener {

    private final PredictionNetwork predictionNetwork;
    private final NodeTraverseHandler nodeTraverseHandler;
    private final SignalizerConfig signalizerConfig;

    @Inject
    public LearnAndMeasureHandler(PredictionNetwork predictionNetwork, NodeTraverseHandler nodeTraverseHandler,
                                  SignalizerConfig signalizerConfig) {
        this.predictionNetwork = predictionNetwork;
        this.nodeTraverseHandler = nodeTraverseHandler;
        this.signalizerConfig = signalizerConfig;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (signalizerConfig.learn) {
            predictionNetwork.learn(nodeTraverseHandler.getDataSets());
            predictionNetwork.save(signalizerConfig.neuralNetworkSaveFolder);
        }
        System.out.println("Network error: " + predictionNetwork.measureError(nodeTraverseHandler.getDataSets()));
    }
}
