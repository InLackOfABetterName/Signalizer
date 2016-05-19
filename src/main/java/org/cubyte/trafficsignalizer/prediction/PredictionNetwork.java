package org.cubyte.trafficsignalizer.prediction;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.core.exceptions.NeurophException;
import org.neuroph.core.learning.IterativeLearning;
import org.neuroph.nnet.MultiLayerPerceptron;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PredictionNetwork {
    public final int INPUTLAYER_NODE_COUNT = 2;

    private Network network;
    public Map<Id<Node>, NeuralNetwork> neuralNetworkMap;

    /**
     * Loads the neural networks for the street network nodes (only the ones with multiple outputs) from the folder and
     * creates new multilayer perceptrons for the nodes where no neural network is found.
     */
    public PredictionNetwork(Network network, String folder, boolean learn) {
        this.network = network;
        neuralNetworkMap = new HashMap<>();
        List<Node> nodesWithMultiOut = network.getNodes().values().stream()
                .filter((Node node) -> node.getOutLinks().size() > 1).map(node -> (Node) node)
                .collect(Collectors.toList());
        for (Node node : nodesWithMultiOut) {
            NeuralNetwork neuralNetwork = null;
            if (!learn) {
                try {
                    neuralNetwork = NeuralNetwork.createFromFile(folder + "/node" + node.getId().toString() + ".nnet");
                } catch (NeurophException ex) {
                    // do nothing...
                }
            }
            if (neuralNetwork == null) {
                neuralNetwork = new MultiLayerPerceptron(INPUTLAYER_NODE_COUNT, 3, node.getOutLinks().size());
                // Input:  Hour of the day | Minute of the hour
                // Output: Percentage chance that a vehicle drives down the lane
            }
            ((IterativeLearning)neuralNetwork.getLearningRule()).setMaxIterations(400);
            neuralNetworkMap.put(node.getId(), neuralNetwork);
        }
    }

    /**
     * Each neural network is saved in its own file. Therefore the folder where all the files will be save has to be
     * given.
     */
    public void save(String folder) {
        Path path = Paths.get(folder);
        try {
            Files.createDirectories(path);
            for (Map.Entry<Id<Node>, NeuralNetwork> node : neuralNetworkMap.entrySet()) {
                try {
                    Path file = path.resolve("node" + node.getKey().toString() + ".nnet");

                    node.getValue().save(file.toString());
                } catch (NeurophException ex) {
                    // do something...
                }
            }
        } catch (IOException e) {
            // do something...
        }
    }

    public void learn(Map<Id<Node>, DataSet> dataSet) {
        for (Map.Entry<Id<Node>, DataSet> nodeSet : dataSet.entrySet()) {
            neuralNetworkMap.get(nodeSet.getKey()).learn(nodeSet.getValue());
        }
    }

    public double measureError(Map<Id<Node>, DataSet> dataSet) {
        double error = 0;
        double tests = 0;
        for (Map.Entry<Id<Node>, DataSet> nodeSet : dataSet.entrySet()) {
            NeuralNetwork network = neuralNetworkMap.get(nodeSet.getKey());
            for (DataSetRow row : nodeSet.getValue().getRows()) {
                network.setInput(row.getInput());
                network.calculate();
                double[] calculated = network.getOutput();
                double distanceBetween = 0;
                for (int i = 0; i < calculated.length; i++) {
                    distanceBetween += (calculated[i] - row.getDesiredOutput()[i]) *
                            (calculated[i] - row.getDesiredOutput()[i]);
                }
                distanceBetween = Math.pow(distanceBetween, 1 / calculated.length);
                error += distanceBetween;
                tests++;
            }
        }
        return error / tests;
    }

    /**
     * Returns the prediction for all the outgoing links or {-1} if there is no neural network for the given link
     */
    public double[] getPrediction(Id<Link> fromLinkId, double time) {
        Id<Node> nodeId = network.getLinks().get(fromLinkId).getToNode().getId();
        if (neuralNetworkMap.containsKey(nodeId)) {
            NeuralNetwork neuralNetwork = neuralNetworkMap.get(nodeId);
            double hours = Math.round(time / 60 / 60) % 24;
            double minutes = Math.round(time / 60 / 10) % 6;
            neuralNetwork.setInput(hours, minutes);
            neuralNetwork.calculate();
            return neuralNetwork.getOutput();
        } else {
            return new double[] {-1d};
        }
    }

    /**
     * Returns the prediction for the outgoing link or -1 when the outgoing link or a neural network for the fromlink cannot
     * be found
     */
    public double getPrediction(Id<Link> fromLinkId, Id<Link> toLinkId, double time) {
        List<Id<Link>> outLinks = new ArrayList<>();
        outLinks.addAll(network.getLinks().get(fromLinkId).getToNode().getOutLinks().keySet());
        outLinks.sort(Id<Link>::compareTo);
        int indexOfOutLink = outLinks.indexOf(toLinkId);
        if (indexOfOutLink != -1) {
            double[] predictions = getPrediction(fromLinkId, time);
            if (predictions.length == 1 && predictions[0] == -1) {
                return -1;
            }
            return predictions[indexOfOutLink];
        }
        return -1;
    }

    /**
     * Returns the nodes of the network in a list that have multiple outgoing links
     */
    public static List<Node> getNodesWithMultiOut(Network network) {
        return network.getNodes().values().stream().filter((Node node) -> node.getOutLinks().size() > 1)
                .map(node -> (Node) node).collect(Collectors.toList());
    }
}
