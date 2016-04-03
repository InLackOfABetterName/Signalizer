package org.cubyte.trafficsignalizer;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.exceptions.NeurophException;
import org.neuroph.nnet.MultiLayerPerceptron;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PredictionNetwork {
    private Network network;
    public Map<Id<Node>, NeuralNetwork> neuralNetworkMap;

    public PredictionNetwork(Network network) {
        this.network = network;
        List<Node> nodesWithMultiOut = network.getNodes().values().stream()
                .filter((Node node) -> node.getOutLinks().size() > 1).map(node -> (Node) node)
                .collect(Collectors.toList());
        for (Node node : nodesWithMultiOut) {
            neuralNetworkMap.put(node.getId(), new MultiLayerPerceptron(3, 5, node.getOutLinks().size()));
        }
    }

    /**
     * Loads the neural networks for the street network nodes (only the ones with multiple outputs) from the folder and
     * creates new multilayer perceptrons for the nodes where no neural network is found.
     */
    public PredictionNetwork(Network network, String folder) {
        this.network = network;
        List<Node> nodesWithMultiOut = network.getNodes().values().stream()
                .filter((Node node) -> node.getOutLinks().size() > 1).map(node -> (Node) node)
                .collect(Collectors.toList());
        for (Node node : nodesWithMultiOut) {
            NeuralNetwork neuralNetwork;
            try {
                neuralNetwork = NeuralNetwork.createFromFile(folder + "/node" + node.getId().toString() + ".nnet");
            } catch (NeurophException ex) {
                neuralNetwork = new MultiLayerPerceptron(3, 5, node.getOutLinks().size());
            }
            neuralNetworkMap.put(node.getId(), neuralNetwork);
        }
    }

    /**
     * Each neural network is saved in its own file. Therefore the folder where all the files will be save has to be
     * given.
     */
    public void save(String folder) {
        for (Map.Entry<Id<Node>, NeuralNetwork> node : neuralNetworkMap.entrySet()) {
            try {
                node.getValue().save(folder + "/node" + node.getKey().toString() + ".nnet");
            } catch (NeurophException ex) {
                // do something...
            }
        }
    }
}
