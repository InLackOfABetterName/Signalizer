package org.cubyte.trafficsignalizer.routes;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import java.util.ArrayList;
import java.util.List;

public class Routes {

    public static final String CONFIG_MODULE = "signalizerRoutes";
    public static final String CONFIG_PARAM_INPUT_FILE = "inputRoutesFile";

    private final Network network;
    private final List<Node> startNodes;
    private final List<Node> endNodes;

    protected Routes(Network network) {
        this.network = network;
        startNodes = new ArrayList<>();
        endNodes = new ArrayList<>();
    }

    public Network getNetwork() {
        return network;
    }

    public static Routes load(String filename, Network network) {
        RoutesXmlParser parser = new RoutesXmlParser(network);
        parser.parse(filename);
        return parser.result();
    }

    public void addStartNode(Node node, float weight) {

    }

    public void addEndNode(Node node, float weight) {

    }
}
