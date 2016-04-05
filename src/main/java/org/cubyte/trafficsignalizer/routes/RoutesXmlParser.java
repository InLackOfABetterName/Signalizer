package org.cubyte.trafficsignalizer.routes;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Map;
import java.util.Stack;

public class RoutesXmlParser extends MatsimXmlParser{

    private final Network network;
    private Routes routes;

    private enum State {
        START_LINKS,
        END_LINKS,
        NONE
    }

    private State state;

    public RoutesXmlParser(Network network) {
        this.network = network;
        state = State.NONE;
    }

    @Override
    public void startTag(String name, Attributes atts, Stack<String> context) {
        Map<Id<Link>, ? extends Link> links = network.getLinks();
        switch (name) {
            case "routes":
                routes = new Routes(network);
                break;
            case "startLinks":
                state = State.START_LINKS;
                break;
            case "endLinks":
                state = State.END_LINKS;
                break;
            case "link":
                Link link = links.get(Id.createLinkId(atts.getValue("id")));
                Float weight = Float.parseFloat(atts.getValue("weight"));
                if (link != null) {
                    if (state == State.START_LINKS) {
                        routes.addStartLink(link, weight);
                    } else if (state == State.END_LINKS) {
                        routes.addEndLink(link, weight);
                    }
                } else {
                    System.err.println("Link with id " + atts.getValue("id") + " does not exist. It could not be inserted " +
                            "as link for the route generation.");
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
