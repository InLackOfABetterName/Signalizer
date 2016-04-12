package org.cubyte.trafficsignalizer.prediction;

import com.google.inject.Inject;
import org.cubyte.trafficsignalizer.SignalizerParams;
import org.cubyte.trafficsignalizer.SignalizerConfigGroup;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

public class LearnAndMeasureHandler implements IterationEndsListener {

    private final PredictionNetwork predictionNetwork;
    private final NodeTraverseHandler nodeTraverseHandler;
    private final SignalizerConfigGroup config;
    private final SignalizerParams params;

    @Inject
    public LearnAndMeasureHandler(PredictionNetwork predictionNetwork, NodeTraverseHandler nodeTraverseHandler,
                                  SignalizerConfigGroup config, SignalizerParams params) {
        this.predictionNetwork = predictionNetwork;
        this.nodeTraverseHandler = nodeTraverseHandler;
        this.config = config;
        this.params = params;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (params.learn) {
            predictionNetwork.learn(nodeTraverseHandler.getDataSets());
            predictionNetwork.save(config.getNeuralNetworkSaveFolder());
        }
        System.out.println("==========================================================================================\n" +
                "Prediction precision: " + predictionNetwork.measureError(nodeTraverseHandler.getDataSets()) + "\n" +
                "==========================================================================================");
    }
}
