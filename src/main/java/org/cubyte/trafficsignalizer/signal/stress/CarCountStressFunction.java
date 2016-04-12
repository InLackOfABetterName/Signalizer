package org.cubyte.trafficsignalizer.signal.stress;

import org.cubyte.trafficsignalizer.SignalizerConfigGroup;
import org.cubyte.trafficsignalizer.tracker.AllKnowingTrafficTracker;
import org.cubyte.trafficsignalizer.tracker.TrafficTracker;
import org.cubyte.trafficsignalizer.ui.TextObject;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;

import javax.inject.Inject;

import static org.matsim.vis.otfvis.data.OTFServerQuadTree.getOTFTransformation;

public class CarCountStressFunction implements StressFunction {

    private final TrafficTracker trafficTracker;
    private final AllKnowingTrafficTracker allKnowingTrafficTracker;
    private final TextObject.Writer textWriter;
    private final SignalizerConfigGroup signalizerConfigGroup;

    @Inject
    public CarCountStressFunction(TrafficTracker trafficTracker, AllKnowingTrafficTracker allKnowingTrafficTracker,
                                  TextObject.Writer textWriter, SignalizerConfigGroup signalizerConfigGroup) {
        this.trafficTracker = trafficTracker;
        this.allKnowingTrafficTracker = allKnowingTrafficTracker;
        this.textWriter = textWriter;
        this.signalizerConfigGroup = signalizerConfigGroup;
    }

    @Override
    public double calculateStress(Network network, Signal signal, SignalSystem system, double timeSeconds) {
        int actual = allKnowingTrafficTracker.getCarCount(signal.getLinkId());
        int predicted = trafficTracker.getCarCount(signal.getLinkId());

        if (!signalizerConfigGroup.isLearn() && predicted != 0 && actual != 0) {
            Coord coord = getOTFTransformation().transform(network.getLinks().get(signal.getLinkId()).getToNode().getCoord());
            textWriter.put("signal_prediction_" + signal.getId(), predicted + "/" + actual, coord.getX(), coord.getY(), false);
            //System.out.println("predicted: " + predicted + " actual: " + actual);
        }
        return predicted;
    }
}
