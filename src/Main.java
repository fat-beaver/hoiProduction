import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    //CONSTANTS
    private static final double PRODUCTION_PER_CIV_FACTORY = 5;
    private static final double PRODUCTION_PER_MIL_FACTORY = 2.25; //this is half of the actual value, but is used as an
    // approximation of production efficiency until that is implemented.
    private static final int MILITARY_FACTORY_COST = 7200;
    private static final int CIVILIAN_FACTORY_COST = 10800;
    private static final String GAME_DATA_FILE = "stateInformationProcessed.csv";

    //INPUTS
    private static final String testNationName = "Soviet Union";
    private static final int testWarDays = 1999; //days from the beginning of 1936 to barbarossa

    //running totals
    private double totalMilProduction = 0;
    private double currentCivProduction = 0;

    //states storage
    private final ArrayList<State> states = new ArrayList<>();

    private Main() {
        String nationName = testNationName;
        nationName = nationName.replaceAll(" ", "");
        loadNation(nationName);
        for (int i = 0; i < testWarDays; i++) {
            dayLoop();
        }
    }
    private void dayLoop() {
        int milFactories = 0;
        int civFactories = 0;
        for (State state : states) {
            milFactories += state.getMilFactories();
            civFactories += state.getCivFactories();
        }
        //TODO implement proper production efficiency
        totalMilProduction += milFactories * PRODUCTION_PER_MIL_FACTORY;
    }
    private void loadNation(String nationName) {
        File dataFile = new File(GAME_DATA_FILE);
        try {
            Scanner nationDataReader = new Scanner(dataFile);
            nationDataReader.nextLine(); //remove headings

            boolean reachedEnd = false;
            while (nationDataReader.hasNextLine() && !reachedEnd) {
                String readLine = nationDataReader.nextLine();
                String[] stateInfo = readLine.split(",");
                if (stateInfo[0].equals(nationName)) {
                    int infrastructure = Integer.parseInt(stateInfo[1]);
                    int buildingSlots= Integer.parseInt(stateInfo[2]);
                    int milFactories = Integer.parseInt(stateInfo[3]);
                    int dockyards = Integer.parseInt(stateInfo[4]);
                    int civFactories = Integer.parseInt(stateInfo[5]);
                    states.add(new State(infrastructure, buildingSlots, milFactories, dockyards, civFactories));
                }
                if (stateInfo[0].equals("0")) {
                    reachedEnd = true;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("data file could not be opened");
        } catch (NumberFormatException e) {
            System.out.println("data file appears to have errors");
        }
    }
    public static void main(String[] args) {
        new Main();
    }
}
