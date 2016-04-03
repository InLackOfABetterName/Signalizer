package org.cubyte.trafficsignalizer;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.SignalSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignalNetworkController
{
    private final List<SignalizerController> controllers;

    public SignalNetworkController() {
        this.controllers = new ArrayList<>();
    }

    public void addController(SignalizerController c) {
        this.controllers.add(c);
    }

    public List<SignalizerController> otherControllers(SignalizerController c) {
        List<SignalizerController> others = new ArrayList<>();
        for (SignalizerController controller : this.controllers) {
            if (controller != c) {
                others.add(controller);
            }
        }
        return others;
    }
}
