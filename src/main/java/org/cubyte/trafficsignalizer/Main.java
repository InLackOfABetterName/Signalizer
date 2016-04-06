package org.cubyte.trafficsignalizer;

import org.cubyte.trafficsignalizer.routes.Routes;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;
import static org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists;

public class Main {

    public static void main(String[] args) {
        boolean learn = false;
        String configName = "";

        if (args.length > 0) {
            for (String arg : args) {
                switch (arg) {
                    case "-l":
                        learn = true;
                        break;
                    default:
                        configName = arg;
                }
            }
        }

        final Path base = Paths.get("conf", (!"".equals(configName) ? configName : "initial"));
        final Config config = ConfigUtils.loadConfig(base.resolve("config.xml").toString());
        final SignalSystemsConfigGroup signalsConf = addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
        final SignalizerConfigGroup signalizerConf = addOrGetModule(config, SignalizerConfigGroup.GROUPNAME, SignalizerConfigGroup.class);

        signalizerConf.setLearn(learn);

        File plansFile = new File(config.plans().getInputFile());
        if (!plansFile.exists()) {
            try(FileWriter fileWriter = new FileWriter(plansFile)) {
                fileWriter.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                 "<!DOCTYPE plans SYSTEM \"http://www.matsim.org/files/dtd/plans_v4.dtd\">\n" +
                                 "<plans></plans>");
            } catch (IOException e) {
                // do nothing
            }
        }

        final Scenario scenario = ScenarioUtils.loadScenario(config);
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsScenarioLoader(signalsConf).loadSignalsData());
        config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);

        if (scenario.getPopulation().getPersons().isEmpty()) {
            generatePopulation(scenario.getPopulation(),
                    Routes.load(signalizerConf.getInputRoutesFile(), scenario.getNetwork()));
            new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(config.plans().getInputFile());
        }

        final Controler c = new Controler(scenario);
        //c.addOverridingModule(new OTFVisLiveModule());
        c.addOverridingModule(new SignalsModule());
        c.addOverridingModule(new SignalizerModule(learn));
        c.getConfig().controler().setOverwriteFileSetting(deleteDirectoryIfExists);
        c.run();
    }

    private static void generatePopulation(Population population, Routes routes) {
        final double SIMULATION_START = 0;
        final double SIMULATION_END = 25000;
        Random random = new Random(System.currentTimeMillis());
        PopulationFactory populationFactory = population.getFactory();
        for (int i = 0; i < 30000; i++) {
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
