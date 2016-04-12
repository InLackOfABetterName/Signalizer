package org.cubyte.trafficsignalizer.ui;

import org.cubyte.trafficsignalizer.SignalizerParams;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.vis.otfvis.OnTheFlyServer;
import org.matsim.vis.otfvis.caching.SimpleSceneLayer;
import org.matsim.vis.otfvis.data.OTFConnectionManager;

import javax.inject.Inject;

import static org.cubyte.trafficsignalizer.ui.OTFClientVisWithSignalizer.runClient;
import static org.matsim.contrib.signals.otfvis.OTFVisWithSignals.startServerAndRegisterWithQSim;

public class OTFVisMobsimListener implements MobsimInitializedListener {
    @Inject
    private Scenario scenario;

    @Inject
    private EventsManager events;

    @Inject
    private TextObject.Writer writer;

    @Inject
    private SignalizerParams params;

    @Override
    public void notifyMobsimInitialized(MobsimInitializedEvent e) {
        final OnTheFlyServer server = startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, (QSim) e.getQueueSimulation());
        final OTFConnectionManager cm = new OTFConnectionManager();
        cm.connectWriterToReader(TextObject.Writer.class, TextObject.Reader.class);
        cm.connectReaderToReceiver(TextObject.Reader.class, TextObject.Drawer.class);
        cm.connectReceiverToLayer(TextObject.Drawer.class, SimpleSceneLayer.class);
        server.addAdditionalElement(writer);
        runClient(cm, server, scenario.getConfig(), params);
    }
}
