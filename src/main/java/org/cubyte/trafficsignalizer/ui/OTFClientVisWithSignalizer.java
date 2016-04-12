/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * OTFClientLive.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.cubyte.trafficsignalizer.ui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jogamp.opengl.GLAutoDrawable;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import org.jdesktop.swingx.mapviewer.wms.WMSService;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.otfvis.*;
import org.matsim.core.config.Config;
import org.matsim.vis.otfvis.OTFClient;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.caching.SimpleSceneLayer;
import org.matsim.vis.otfvis.data.OTFClientQuadTree;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.data.fileio.SettingsSaver;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.OTFQueryControl;
import org.matsim.vis.otfvis.gui.OTFQueryControlToolBar;
import org.matsim.vis.otfvis.handler.FacilityDrawer;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.interfaces.OTFServer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleQuadDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleStaticNetLayer;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;

public class OTFClientVisWithSignalizer {


    public static void runClient(OTFConnectionManager cm, OTFServer server, Config config) {
        SwingUtilities.invokeLater(() -> {
            final OTFVisConfigGroup otfVisConf = addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
            final SignalSystemsConfigGroup signalConf = addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
            cm.connectLinkToWriter(OTFLinkAgentsHandler.Writer.class);
            cm.connectWriterToReader(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
            cm.connectReaderToReceiver(OTFLinkAgentsHandler.class, OGLSimpleQuadDrawer.class);
            cm.connectReceiverToLayer(OGLSimpleQuadDrawer.class, OGLSimpleStaticNetLayer.class);
            cm.connectWriterToReader(OTFAgentsListHandler.Writer.class, OTFAgentsListHandler.class);

            if (config.transit().isUseTransit()) {
                cm.connectWriterToReader(FacilityDrawer.Writer.class, FacilityDrawer.Reader.class);
                cm.connectReaderToReceiver(FacilityDrawer.Reader.class, FacilityDrawer.DataDrawer.class);
                cm.connectReceiverToLayer(FacilityDrawer.DataDrawer.class, SimpleSceneLayer.class);
            }

            if ((config.qsim().isUseLanes()) && (!signalConf.isUseSignalSystems())) {
                cm.connectWriterToReader(OTFLaneWriter.class, OTFLaneReader.class);
                cm.connectReaderToReceiver(OTFLaneReader.class, OTFLaneSignalDrawer.class);
                cm.connectReceiverToLayer(OTFLaneSignalDrawer.class, SimpleSceneLayer.class);
            } else if (signalConf.isUseSignalSystems()) {
                cm.connectWriterToReader(OTFSignalWriter.class, OTFSignalReader.class);
                cm.connectReaderToReceiver(OTFSignalReader.class, OTFLaneSignalDrawer.class);
                cm.connectReceiverToLayer(OTFLaneSignalDrawer.class, SimpleSceneLayer.class);
            }
            GLAutoDrawable canvas = OTFOGLDrawer.createGLCanvas(otfVisConf);
            OTFClient otfClient = new OTFClient(canvas);
            otfClient.setServer(server);
            SettingsSaver saver = new SettingsSaver("otfsettings");
            OTFVisConfigGroup visconf = saver.tryToReadSettingsFile();
            if (visconf == null) {
                visconf = server.getOTFVisConfig();
            }
            visconf.setDelay_ms(1);
            OTFClientControl.getInstance().setOTFVisConfig(visconf); // has to be set before OTFClientQuadTree.getConstData() is invoked!
            OTFServerQuadTree serverQuadTree = server.getQuad(cm);
            OTFClientQuadTree clientQuadTree = serverQuadTree.convertToClient(server, cm);
            clientQuadTree.getConstData();
            OTFHostControlBar hostControlBar = otfClient.getHostControlBar();
            OTFOGLDrawer mainDrawer = new OTFOGLDrawer(clientQuadTree, hostControlBar, otfVisConf, canvas);
            OTFQueryControl queryControl = new OTFQueryControl(server, hostControlBar, visconf);
            OTFQueryControlToolBar queryControlBar = new OTFQueryControlToolBar(queryControl, visconf);
            queryControl.setQueryTextField(queryControlBar.getTextField());
            otfClient.getContentPane().add(queryControlBar, BorderLayout.SOUTH);
            mainDrawer.setQueryHandler(queryControl);
            otfClient.addDrawerAndInitialize(mainDrawer, saver);
            if (otfVisConf.isMapOverlayMode()) {
                TileFactory tf;
                if (otfVisConf.getMapBaseURL().isEmpty()) {
                    assertZoomLevel17(otfVisConf);
                    tf = osmTileFactory();
                } else {
                    WMSService wms = new WMSService(otfVisConf.getMapBaseURL(), otfVisConf.getMapLayer());
                    tf = new OTFVisWMSTileFactory(wms, otfVisConf.getMaximumZoom());
                }
                otfClient.addMapViewer(tf);
            }
            otfClient.pack();
            otfClient.setVisible(true);
        });
    }

    private static void assertZoomLevel17(OTFVisConfigGroup otfVisConf) {
        if (otfVisConf.getMaximumZoom() != 17) {
            throw new RuntimeException("The OSM layer only works with maximumZoomLevel = 17. Please adjust your config.");
        }
    }

    private static TileFactory osmTileFactory() {
        final int max = 17;
        TileFactoryInfo info = new TileFactoryInfo(0, 17, 17,
                256, true, true,
                "http://tile.openstreetmap.org",
                "x", "y", "z") {
            @Override
            public String getTileUrl(int x, int y, int zoom) {
                zoom = max - zoom;
                String url = this.baseURL + "/" + zoom + "/" + x + "/" + y + ".png";
                return url;
            }

        };
        TileFactory tf = new DefaultTileFactory(info);
        return tf;
    }

    private static class OTFVisWMSTileFactory extends DefaultTileFactory {
        public OTFVisWMSTileFactory(final WMSService wms, final int maxZoom) {
            super(new TileFactoryInfo(0, maxZoom, maxZoom,
                    256, true, true, // tile size and x/y orientation is r2l & t2b
                    "", "x", "y", "zoom") {
                @Override
                public String getTileUrl(int x, int y, int zoom) {
                    int zz = maxZoom - zoom;
                    int z = (int) Math.pow(2, (double) zz - 1);
                    return wms.toWMSURL(x - z, z - 1 - y, zz, getTileSize(zoom));
                }

            });
        }
    }

}
