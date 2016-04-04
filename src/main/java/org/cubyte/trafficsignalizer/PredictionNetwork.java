package org.cubyte.trafficsignalizer;

import org.matsim.api.core.v01.Id;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PredictionNetwork {
    private Network network;
    public Map<Id<Node>, NeuralNetwork> neuralNetworkMap;

    /**
     * Loads the neural networks for the street network nodes (only the ones with multiple outputs) from the folder and
     * creates new multilayer perceptrons for the nodes where no neural network is found.
     */
    public PredictionNetwork(Network network, String folder) {
        this.network = network;
        neuralNetworkMap = new HashMap<>();
        List<Node> nodesWithMultiOut = network.getNodes().values().stream()
                .filter((Node node) -> node.getOutLinks().size() > 1).map(node -> (Node) node)
                .collect(Collectors.toList());
        for (Node node : nodesWithMultiOut) {
            NeuralNetwork neuralNetwork;
            try {
                neuralNetwork = NeuralNetwork.createFromFile(folder + "/node" + node.getId().toString() + ".nnet");
            } catch (NeurophException ex) {
                neuralNetwork = new MultiLayerPerceptron(3, node.getOutLinks().size());
                // Input: Vehicle count that came in aggregated over one minute | Hour of the day | Minute of the hour
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
            System.out.println("test");
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
                double actualLength = 0;
                for (int i = 0; i < calculated.length; i++) {
                    distanceBetween += (calculated[i] - row.getDesiredOutput()[i]) *
                            (calculated[i] - row.getDesiredOutput()[i]);
                    actualLength += row.getDesiredOutput()[i] * row.getDesiredOutput()[i];
                }
                distanceBetween /= calculated.length;
                actualLength /= calculated.length;
                error += distanceBetween / actualLength;
                tests++;
            }
        }
        return error / tests;
    }

    /**
     * Returns the nodes of the network in a list that have multiple outgoing links
     */
    public static List<Node> getNodesWithMultiOut(Network network) {
        return network.getNodes().values().stream().filter((Node node) -> node.getOutLinks().size() > 1)
                .map(node -> (Node) node).collect(Collectors.toList());
    }
}
