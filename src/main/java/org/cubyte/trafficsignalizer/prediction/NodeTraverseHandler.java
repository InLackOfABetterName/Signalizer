package org.cubyte.trafficsignalizer.prediction;

import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.vehicles.Vehicle;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class NodeTraverseHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {
    private final Scenario scenario;
    private Map<Id<Vehicle>, Id<Node>> vehicleLeaveMap;
    private Map<Id<Node>, DataSet> dataSets;

    @Inject
    public NodeTraverseHandler(Scenario scenario) {
        this.scenario = scenario;
        init();
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        if (!vehicleLeaveMap.containsKey(event.getVehicleId())) {
            Node node = scenario.getNetwork().getLinks().get(event.getLinkId()).getToNode();
            if (node.getOutLinks().size() > 1) {
                vehicleLeaveMap.put(event.getVehicleId(), node.getId());
            }/* else {
                System.out.println("Node only has one out link");
            }*/
        } else {
            System.err.println("Vehicle was already in the map");
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        if (vehicleLeaveMap.containsKey(event.getVehicleId())) {
            Link link = scenario.getNetwork().getLinks().get(event.getLinkId());
            if (link.getFromNode().getId().equals(vehicleLeaveMap.get(event.getVehicleId()))) {
                List<Id<Link>> outLinks = new ArrayList<>();
                outLinks.addAll(link.getFromNode().getOutLinks().keySet());
                outLinks.sort(Id<Link>::compareTo);
                double hours = Math.round(event.getTime() / 60 / 60) % 24;
                double minutes = Math.round(event.getTime() / 60) % 60;
                DataSetRow row = null;
                for (DataSetRow current : dataSets.get(vehicleLeaveMap.get(event.getVehicleId())).getRows()) {
                    if (current.getInput()[0] == hours && current.getInput()[1] == minutes) {
                        row = current;
                        break;
                    }
                }
                double[] input;
                double[] desiredOutput = new double[outLinks.size()];
                if (row != null) {
                    desiredOutput = row.getDesiredOutput();
                    desiredOutput[outLinks.indexOf(link.getId())]++;
                    row.setDesiredOutput(desiredOutput);
                } else {
                    input = new double[] {hours, minutes};
                    desiredOutput[outLinks.indexOf(link.getId())] = 1;
                    dataSets.get(vehicleLeaveMap.get(event.getVehicleId())).addRow(new DataSetRow(input, desiredOutput));
                }
                vehicleLeaveMap.remove(event.getVehicleId());
            }
        }
    }

    @Override
    public void reset(int iteration) {
        init();
    }

    private void init() {
        vehicleLeaveMap = new HashMap<>();
        dataSets = new HashMap<>();
        for (Node node : PredictionNetwork.getNodesWithMultiOut(scenario.getNetwork())) {
            dataSets.put(node.getId(), new DataSet(2, node.getOutLinks().size()));
        }
    }

    public Map<Id<Node>, DataSet> getDataSets() {
        dataSets.values().stream().map(DataSet::getRows).flatMap(List::stream).forEach(row -> {
            double[] desiredOutput = row.getDesiredOutput();
            double acc = 0;
            for (double n : desiredOutput) {
                acc += n;
            }
            for (int i = 0; i < desiredOutput.length; i++) {
                desiredOutput[i] = desiredOutput[i] / acc;
            }
            row.setDesiredOutput(desiredOutput);
        });
        return dataSets;
    }
}
