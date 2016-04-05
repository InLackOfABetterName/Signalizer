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
        LINKS,
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
        Link link;
        switch (name) {
            case "routes":
                routes = new Routes(network);
                break;
            case "links":
                state = State.LINKS;
                break;
            case "start":
            case "inter":
            case "end":
                link = links.get(Id.createLinkId(atts.getValue("id")));
                float weight = Float.parseFloat(atts.getValue("weight"));
                if (link != null) {
                    if ("start".equals(name)) {
                        routes.addStartLink(link, weight);
                    } else if ("inter".equals(name)) {
                        routes.addInterLink(link, weight);
                    } else if ("end".equals(name)) {
                        routes.addEndLink(link, weight);
                    }
                } else {
                    System.err.println("Link with id " + atts.getValue("id") + " does not exist. It could not be inserted " +
                            "as link for the route generation.");
                }
                break;
            case "nointer":
                routes.setNoInter(Float.parseFloat(atts.getValue("weight")));
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
