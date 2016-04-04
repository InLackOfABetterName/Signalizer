package org.cubyte.trafficsignalizer.routes;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Stack;

public class RoutesXmlParser extends MatsimXmlParser{

    private final Network network;
    private Routes routes;

    private enum State {
        startNodes,
        endNodes
    }

    private State state;

    public RoutesXmlParser(Network network) {
        this.network = network;
    }

    @Override
    public void startTag(String name, Attributes atts, Stack<String> context) {
        switch (name) {
            case "routes":
                routes = new Routes(network);
                break;
            case "startNodes":
                state = State.startNodes;
                break;
            case "endNodes":
                state = State.endNodes;
                break;
            case "node":
                if (state == State.startNodes) {
                    //routes.addStartNode();
                } else {
                    //routes.addEndNode();
                }
                break;
        }
    }

    @Override
    public void endTag(String name, String content, Stack<String> context) {
    }

    public Routes result() {
        return routes;
    }
}
