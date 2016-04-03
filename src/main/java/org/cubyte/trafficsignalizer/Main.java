package org.cubyte.trafficsignalizer;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;
import static org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists;

public class Main {

    public static void main(String[] args) {
        final Config config = ConfigUtils.loadConfig("./conf/" + (args.length > 0 ? args[0] : "initial") + "/config.xml");
        final SignalSystemsConfigGroup signalsConf = addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);

        final Scenario scenario = ScenarioUtils.loadScenario(config);
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsScenarioLoader(signalsConf).loadSignalsData());
        config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);

        final SignalNetworkController networkController = new SignalNetworkController();
        final Controler c = new Controler(scenario);
        c.addOverridingModule(new OTFVisLiveModule());
        c.addOverridingModule(new SignalsModule());
        c.addOverridingModule(new SignalizerModule(networkController));
        c.getConfig().controler().setOverwriteFileSetting(deleteDirectoryIfExists);
        c.run();
    }
}
