package org.cubyte.trafficsignalizer;

import org.cubyte.trafficsignalizer.routes.Routes;
import org.cubyte.trafficsignalizer.signal.SignalGroups;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsWriter20;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.cubyte.trafficsignalizer.SignalizerParams.fromArgs;
import static org.cubyte.trafficsignalizer.signal.SignalGroups.determineGroups;
import static org.matsim.core.config.ConfigUtils.addOrGetModule;
import static org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists;

public class Main {

    public static void main(String[] args) {

        SignalizerParams params = fromArgs(args);

        final Path base = Paths.get("conf", params.scenario);
        final Config config = ConfigUtils.loadConfig(base.resolve("config.xml").toString());
        final SignalSystemsConfigGroup signalsConf = addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
        final SignalizerConfigGroup signalizerConf = addOrGetModule(config, SignalizerConfigGroup.GROUPNAME, SignalizerConfigGroup.class);
        final NetworkConfigGroup networkConf = addOrGetModule(config, NetworkConfigGroup.GROUP_NAME, NetworkConfigGroup.class);

        File plansFile = new File(config.plans().getInputFile());
        if (!plansFile.exists()) {
            try (FileWriter fileWriter = new FileWriter(plansFile)) {
                fileWriter.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<!DOCTYPE plans SYSTEM \"http://www.matsim.org/files/dtd/plans_v4.dtd\">\n" +
                        "<plans></plans>");
            } catch (IOException e) {
                // do nothing
            }
        }

        recalculateLengths(networkConf, params.coordScale, params.caculateLinkLengths);

        final Scenario scenario = ScenarioUtils.loadScenario(config);
        final SignalsData signalsData = new SignalsScenarioLoader(signalsConf).loadSignalsData();
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, signalsData);

        generateSignalGroups(scenario.getNetwork(), signalsConf, signalsData, params.refreshGroups);

        if (scenario.getPopulation().getPersons().isEmpty()) {
            generatePopulation(scenario.getPopulation(),
                    Routes.load(signalizerConf.getInputRoutesFile(), scenario.getNetwork()), params.populationSize);
            new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(config.plans().getInputFile());
        }

        final Controler c = new Controler(scenario);
        //c.addOverridingModule(new OTFVisLiveModule());
        c.addOverridingModule(new SignalsModule());
        c.addOverridingModule(new SignalizerModule(params));
        c.getConfig().controler().setOverwriteFileSetting(deleteDirectoryIfExists);
        c.run();
    }

    private static void generateSignalGroups(Network network, SignalSystemsConfigGroup signalsConf, SignalsData signalsData, boolean refresh) {
        final SignalGroupsData groupsData = signalsData.getSignalGroupsData();
        for (SignalSystemData system : signalsData.getSignalSystemsData().getSignalSystemData().values()) {
            if (SignalGroups.shouldGenerate(signalsData, system.getId())) {
                final Map<Id<SignalGroup>, SignalGroupData> existingGroups = groupsData.getSignalGroupDataBySystemId(system.getId());
                if (refresh || existingGroups.size() == 1 && existingGroups.values().iterator().next().getId().toString().equals("generate_me")) {
                    existingGroups.clear();
                    final Collection<SignalGroupData> groups = determineGroups(network, system.getId(), system.getSignalData().values(), groupsData.getFactory());
                    groups.stream().forEach(groupsData::addSignalGroupData);
                }
            }
        }
        new SignalGroupsWriter20(groupsData).write(signalsConf.getSignalGroupsFile());
    }

    private static void recalculateLengths(NetworkConfigGroup networkConfigGroup, double scale, boolean recalculate) {
        if (recalculate) {
            Network network = NetworkUtils.createNetwork();
            new MatsimNetworkReader(network).parse(networkConfigGroup.getInputFile());
            NetworkFactory networkFactory = network.getFactory();
            List<Link> links = new ArrayList<>(network.getLinks().values());
            for (Node node : new ArrayList<>(network.getNodes().values())) {
                Node newNode = networkFactory.createNode(node.getId(),
                        new Coord(node.getCoord().getX() / scale, node.getCoord().getY() / scale));
                network.removeNode(node.getId());
                network.addNode(newNode);
                for (Link link : node.getInLinks().values()) {
                    link.setToNode(newNode);
                }
                for (Link link : node.getOutLinks().values()) {
                    link.setFromNode(newNode);
                }
            }
            for (Link link : links) {
                link.setLength(CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), link.getToNode().getCoord()) / 10);
                link.setFreespeed(50f / 3.6f);
                network.addLink(link);
            }
            new NetworkWriter(network).write(networkConfigGroup.getInputFile());
        }
    }

    private static void generatePopulation(Population population, Routes routes, int populationSize) {
        final double SIMULATION_START = 0;
        final double SIMULATION_END = 60000;
        Random random = new Random(System.currentTimeMillis());
        PopulationFactory populationFactory = population.getFactory();
        for (int i = 0; i < populationSize; i++) {
            Person person = populationFactory.createPerson(Id.createPersonId(i));
            for (int n = 0; n < random.nextInt(3) + 1; n++) {
                Plan plan = populationFactory.createPlan();
                Activity activity = populationFactory.createActivityFromLinkId("from", routes.getRandomStartLink().getId());
                activity.setEndTime(random.nextDouble() * (SIMULATION_END - SIMULATION_START) + SIMULATION_START);
                plan.addActivity(activity);
                Leg leg = populationFactory.createLeg("car");
                plan.addLeg(leg);
                Link interLink = routes.getRandomInterLink();
                double startActivityEndTime = activity.getEndTime();
                if (interLink != null) {
                    activity = populationFactory.createActivityFromLinkId("inter", interLink.getId());
                    activity.setEndTime(random.nextDouble() * (SIMULATION_END - startActivityEndTime)
                            + startActivityEndTime);
                    plan.addActivity(activity);
                    leg = populationFactory.createLeg("car");
                    plan.addLeg(leg);
                }
                activity = populationFactory.createActivityFromLinkId("to", routes.getRandomEndLink().getId());
                plan.addActivity(activity);
                person.addPlan(plan);
            }
            population.addPerson(person);
        }
    }
}
