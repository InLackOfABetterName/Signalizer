package org.cubyte.trafficsignalizer;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.*;
import org.cubyte.trafficsignalizer.signal.AbstractSignalController;
import org.cubyte.trafficsignalizer.signal.DummyCycleController;
import org.cubyte.trafficsignalizer.signal.StressBasedController;
import org.cubyte.trafficsignalizer.signal.stress.StressFunction;
import org.cubyte.trafficsignalizer.signal.stress.TimeVariantStressFunction;

import static net.sourceforge.argparse4j.impl.Arguments.storeTrue;

public class SignalizerParams {
    public final String scenario;
    public final boolean learn;
    public final boolean refreshGroups;
    public final boolean caculateLinkLengths;
    public final double coordScale;
    public final int timeStepDelay;
    public final int populationSize;
    public final Class<? extends StressFunction> stressFunction;
    public final Class<? extends AbstractSignalController> signalController;

    public SignalizerParams(String scenario, boolean learn, boolean refreshGroups, boolean caculateLinkLengths, double coordScale, int timeStepDelay, int populationSize, Class<? extends StressFunction> stressFunction, Class<? extends AbstractSignalController> signalController) {
        this.scenario = scenario;
        this.learn = learn;
        this.refreshGroups = refreshGroups;
        this.caculateLinkLengths = caculateLinkLengths;
        this.coordScale = coordScale;
        this.timeStepDelay = timeStepDelay;
        this.populationSize = populationSize;
        this.stressFunction = stressFunction;
        this.signalController = signalController;
    }

    public static SignalizerParams fromNamespace(Namespace ns) {
        return new SignalizerParams(
                ns.getString("scenario"),
                ns.getBoolean("learn"),
                ns.getBoolean("refresh_groups"),
                ns.getBoolean("calculate_length"),
                ns.getDouble("coord_scale"),
                ns.getInt("timestep_delay"),
                ns.getInt("population_size"),
                ns.get("stress_function"),
                ns.get("controller"));
    }

    public static SignalizerParams fromArgs(String[] args) {

        final ArgumentParser argParser = ArgumentParsers.newArgumentParser("Signalizer");
        argParser.addArgument("scenario").type(String.class).setDefault("initial").help("The scenario folder name in ./conf");
        argParser.addArgument("-l", "--learn").action(storeTrue()).help("This flag starts the simulation in learning mode");
        argParser.addArgument("-r", "--refresh-groups").action(storeTrue()).help("The flag forces the regeneration of controlled signal groups");
        argParser.addArgument("-c", "--calculate-length").action(storeTrue()).help("This flag forces the recalculation of the street lengths");
        argParser.addArgument("-s", "--coord-scale").type(Double.class).metavar("scale").setDefault(1d).help("This parameter specifies a scale factor to scala the node coords in the network");
        argParser.addArgument("--timestep-delay").type(Integer.class).metavar("delay").setDefault(1).help("This parameter indirectly specifies the visual simulation speed by giving the timestep delay in milliseconds");
        argParser.addArgument("-p", "--population-size").required(true).type(Integer.class).metavar("population").help("This value is used when new plans are generated");

        argParser.addArgument("--stress-function").type((ArgumentType<Class<? extends StressFunction>>) (parser, arg, value) -> {
            final Class<? extends StressFunction> c = StressFunction.byName(value);
            if (c == null) {
                throw new ArgumentParserException("Unknown stress function", parser);
            }
            return c;
        }).setDefault(TimeVariantStressFunction.class).metavar("name").help("Set the stress function to be used");

        argParser.addArgument("--controller").type((ArgumentType<Class<? extends AbstractSignalController>>) (parser, arg, value) -> {
            try {
                return (Class<? extends AbstractSignalController>) Class.forName(value);
            } catch (ClassNotFoundException e) {
                throw new ArgumentParserException("Unknown stress function", parser);
            }
        }).setDefault(StressBasedController.class).metavar("class name").help("Set the signal controller to be used");

        try {
            return fromNamespace(argParser.parseArgs(args));
        } catch (ArgumentParserException e) {
            argParser.handleError(e);
            System.exit(1);
            throw new RuntimeException(e); // compiler will not detect termination by System.exit
        }
    }
}
