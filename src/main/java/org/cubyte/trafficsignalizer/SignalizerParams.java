package org.cubyte.trafficsignalizer;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import static net.sourceforge.argparse4j.impl.Arguments.storeTrue;

public class SignalizerParams {
    public final String scenario;
    public final boolean learn;
    public final boolean refreshGroups;
    public final boolean caculateLinkLengths;
    public final double coordScale;

    public SignalizerParams(String scenario, boolean learn, boolean refreshGroups, boolean caculateLinkLengths, double coordScale) {
        this.scenario = scenario;
        this.learn = learn;
        this.refreshGroups = refreshGroups;
        this.caculateLinkLengths = caculateLinkLengths;
        this.coordScale = coordScale;
    }

    public static SignalizerParams fromNamespace(Namespace ns) {
        return new SignalizerParams(
                ns.getString("scenario"),
                ns.getBoolean("learn"),
                ns.getBoolean("refresh_groups"),
                ns.getBoolean("calculate_length"),
                ns.getDouble("coord_scale")
        );
    }

    public static SignalizerParams fromArgs(String[] args) {

        final ArgumentParser argParser = ArgumentParsers.newArgumentParser("Signalizer");
        argParser.addArgument("scenario").type(String.class).setDefault("initial").help("The scenario folder name in ./conf");
        argParser.addArgument("-l", "--learn").action(storeTrue()).help("This flag starts the simulation in learning mode");
        argParser.addArgument("-r", "--refresh-groups").action(storeTrue()).help("The flag forces the regeneration of controlled signal groups");
        argParser.addArgument("-c", "--calculate-length").action(storeTrue()).help("This flag forces the recalculation of the street lengths");
        argParser.addArgument("-s", "--coord-scale").type(Double.class).metavar("scale").setDefault(1d).help("This flag forces the recalculation of the street lengths");

        try {
            return fromNamespace(argParser.parseArgs(args));
        } catch (ArgumentParserException e) {
            argParser.handleError(e);
            System.exit(1);
            throw new RuntimeException(e); // compiler will not detect termination by System.exit
        }
    }
}
