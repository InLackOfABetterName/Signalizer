package org.cubyte.trafficsignalizer.routes;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.util.*;

public class Routes {

    private final Random random;
    private final Network network;
    private final List<Link> startLinks;
    private final List<Link> endLinks;
    private final Map<Id<Link>, Float> startWeights;
    private final Map<Id<Link>, Float> endWeights;

    protected Routes(Network network) {
        random = new Random();
        this.network = network;
        startLinks = new ArrayList<>();
        endLinks = new ArrayList<>();
        startWeights = new HashMap<>();
        endWeights = new HashMap<>();

    }

    public Network getNetwork() {
        return network;
    }

    public static Routes load(String filename, Network network) {
        RoutesXmlParser parser = new RoutesXmlParser(network);
        parser.parse(filename);
        return parser.result();
    }

    public void addStartLink(Link link, float weight) {
        startLinks.add(link);
        startWeights.put(link.getId(), weight);
    }

    public void addEndLink(Link link, float weight) {
        endLinks.add(link);
        endWeights.put(link.getId(), weight);
    }

    /**
     * Returns random start link with taking the weights of the links into consideration
     */
    public Link getRandomStartLink() {
        return getRandomLink(startLinks, startWeights);
    }

    /**
     * Returns random start link with taking the weights of the links into consideration
     */
    public Link getRandomEndLink() {
        return getRandomLink(endLinks, endWeights);
    }

    private Link getRandomLink(List<Link> links, Map<Id<Link>, Float> weights) {
        double weightTotal = weights.values().stream().mapToDouble(weight -> weight).sum();
        double linkChoice = random.nextDouble() * weightTotal;
        return links.stream().reduce(null, (link1, link2) -> {
            if (link1 != null)
                return link1;
            double weightTillNow = links.stream().filter((link) -> links.indexOf(link) < links.indexOf(link2))
                    .mapToDouble((link) -> weights.get(link.getId())).sum();
            if (linkChoice >= weightTillNow && linkChoice <= weightTillNow + weights.get(link2.getId()))
                return link2;
            return null;
        });
    }
}
