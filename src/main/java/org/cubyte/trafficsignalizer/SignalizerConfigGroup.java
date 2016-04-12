package org.cubyte.trafficsignalizer;

import org.matsim.core.config.ReflectiveConfigGroup;

public class SignalizerConfigGroup extends ReflectiveConfigGroup {

    public static final String GROUPNAME = "signalizer";
    public static final String INPUT_ROUTES_FILE = "routesInputFile";
    public static final String NEURAL_SAVE_FOLDER = "neuralSaveFolder";

    private String inputRoutesFile;
    private String neuralNetworkSaveFolder;

    public SignalizerConfigGroup() {
        super(GROUPNAME);
    }

    @StringGetter( INPUT_ROUTES_FILE )
    public String getInputRoutesFile() {
        return inputRoutesFile;
    }

    @StringSetter( INPUT_ROUTES_FILE )
    public void setInputRoutesFile(String inputRoutesFile) {
        this.inputRoutesFile = inputRoutesFile;
    }

    @StringGetter( NEURAL_SAVE_FOLDER )
    public String getNeuralNetworkSaveFolder() {
        return neuralNetworkSaveFolder;
    }

    @StringSetter( NEURAL_SAVE_FOLDER )
    public void setNeuralNetworkSaveFolder(String neuralNetworkSaveFolder) {
        this.neuralNetworkSaveFolder = neuralNetworkSaveFolder;
    }
}
