import org.matsim.run.Controler;

public class Main {
    private static Controler controler;

    public static void main(String[] args) {
        controler = new Controler("./config.xml");
        controler.run();
    }
}
