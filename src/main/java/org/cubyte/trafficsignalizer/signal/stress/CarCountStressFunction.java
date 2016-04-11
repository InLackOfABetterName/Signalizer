package org.cubyte.trafficsignalizer.signal.stress;

import org.cubyte.trafficsignalizer.tracker.PredictedTrafficTracker;
import org.cubyte.trafficsignalizer.tracker.TrafficTracker;
import org.cubyte.trafficsignalizer.ui.TextObject;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;

import javax.inject.Inject;

import static org.matsim.vis.otfvis.data.OTFServerQuadTree.getOTFTransformation;

public class CarCountStressFunction implements StressFunction {

    private final TrafficTracker trafficTracker;
    private final PredictedTrafficTracker predictedTrafficTracker;
    private final TextObject.Writer textWriter;

    @Inject
    public CarCountStressFunction(TrafficTracker trafficTracker, PredictedTrafficTracker predictedTrafficTracker, TextObject.Writer textWriter) {
        this.trafficTracker = trafficTracker;
        this.predictedTrafficTracker = predictedTrafficTracker;
        this.textWriter = textWriter;
    }

    @Override
    public double calculateStress(Network network, Signal signal, SignalSystem system, double timeSeconds) {
        int predicted = predictedTrafficTracker.getCarCount(signal.getLinkId());
        int actual = trafficTracker.getCarCount(signal.getLinkId());

        if (predicted != 0 && actual != 0) {
            Coord coord = getOTFTransformation().transform(network.getLinks().get(signal.getLinkId()).getToNode().getCoord());
            textWriter.put("signal_prediction_" + signal.getId(), predicted + "/" + actual, coord.getX(), coord.getY(), false);
            //System.out.println("predicted: " + predicted + " actual: " + actual);
        }
        return predicted;
    }
}
