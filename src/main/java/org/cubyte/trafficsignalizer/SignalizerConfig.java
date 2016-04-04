package org.cubyte.trafficsignalizer;

public class SignalizerConfig {

    public final boolean learn;
    public final String neuralNetworkSaveFolder;

    public SignalizerConfig(boolean learn, String neuralNetworkSaveFolder) {
        this.learn = learn;
        this.neuralNetworkSaveFolder = neuralNetworkSaveFolder;
    }
}
