import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignals;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.SignalSystemsConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import static org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists;

public class Main {

    public static void main(String[] args) {
        final Config config = ConfigUtils.loadConfig("./conf/config.xml");
        final SignalSystemsConfigGroup signalSystemsModule = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
        final Scenario scenario = ScenarioUtils.loadScenario(config);
        config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsScenarioLoader(signalSystemsModule).loadSignalsData());



        signalSystemsModule.setSignalSystemFile("./conf/signalSystems.xml");
        signalSystemsModule.setSignalGroupsFile("./conf/signalGroups.xml");
        signalSystemsModule.setSignalControlFile("./conf/signalControl.xml");

        Controler c = new Controler(scenario);
        c.addOverridingModule(new SignalsModule());
        c.getConfig().controler().setOverwriteFileSetting(deleteDirectoryIfExists);
        //OTFVisWithSignals.playScenario(scenario);
        c.run();
    }
}
