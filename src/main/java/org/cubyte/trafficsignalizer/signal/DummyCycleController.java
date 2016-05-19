package org.cubyte.trafficsignalizer.signal;

import org.cubyte.trafficsignalizer.ui.TextObject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DummyCycleController extends AbstractSignalController {

    private final TextObject.Writer textWriter;
    private List<Id<SignalGroup>> groups;
    private int currentGroup = -1;
    private Id<SignalGroup> active = null;
    private static final int PHASE_DURATION = 30; // seconds

    @Inject
    public DummyCycleController(Network network, SignalNetworkController networkController, TextObject.Writer textWriter) {
        super(network, networkController);
        this.textWriter = textWriter;
    }

    @Override
    public void updateState(double v) {
    }

    @Override
    protected void receivedSystem() {
        this.groups = new ArrayList<>(getSystem().getSignalGroups().keySet());
    }

    @Override
    public void simulationInitialized(double timeSeconds) {
        this.currentGroup = 0;
        Collections.shuffle(this.groups);
        final Id<SignalGroup> g = this.groups.get(this.currentGroup);
        getSystem().scheduleOnset(timeSeconds, g);
        super.simulationInitialized(timeSeconds);
    }

    @Override
    public void groupStateChanged(Id<SignalGroup> signalGroupId, SignalGroupState newState, double t) {
        switch (newState) {
            case GREEN:
                getSystem().scheduleDropping(t + PHASE_DURATION, signalGroupId);
                active = signalGroupId;
                break;
            case RED:
                if (signalGroupId.equals(active)) {
                    this.currentGroup = (this.currentGroup + 1) % this.groups.size();
                    getSystem().scheduleOnset(t + 1, this.groups.get(this.currentGroup));
                    drawActiveGroup();
                }
        }
    }

    private void drawActiveGroup() {
        final SignalGroup g = getSystem().getSignalGroups().get(groups.get(currentGroup));
        final Link l = getNetwork().getLinks().get(g.getSignals().values().iterator().next().getLinkId());

        this.textWriter.putMid(getSystem().getId() + "_active_group", g.getId() + "", l);

    }
}
