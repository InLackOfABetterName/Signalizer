package org.cubyte.trafficsignalizer.signal;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.builder.DefaultSignalModelFactory;
import org.matsim.contrib.signals.builder.SignalModelFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.model.*;

import javax.inject.Inject;

public class SignalizerSignalModelFactory implements SignalModelFactory {

    private final SignalModelFactory defaultImpl = new DefaultSignalModelFactory();
    private final SignalNetworkController networkController;
    private final Provider<AbstractSignalController> signalController;

    @Inject
    public SignalizerSignalModelFactory(SignalNetworkController c, Provider<AbstractSignalController> signalController) {
        networkController = c;
        this.signalController = signalController;
    }

    public SignalSystemsManager createSignalSystemsManager() {
        return defaultImpl.createSignalSystemsManager();
    }

    public SignalSystem createSignalSystem(Id<SignalSystem> id) {
        return defaultImpl.createSignalSystem(id);
    }

    @SuppressWarnings("unchecked")
    public SignalController createSignalSystemController(String controllerIdentifier) {
        if (controllerIdentifier.equals(DefaultPlanbasedSignalSystemController.IDENTIFIER)) {
            return new DefaultPlanbasedSignalSystemController();
        } else {
            AbstractSignalController c = signalController.get();
            networkController.addController(c);
            return c;
        }
    }

    public SignalPlan createSignalPlan(SignalPlanData planData) {
        return defaultImpl.createSignalPlan(planData);
    }
}
