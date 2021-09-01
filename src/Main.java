import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    //CONSTANTS
    private static final double PRODUCTION_PER_CIV_FACTORY = 5;
    private static final double PRODUCTION_PER_MIL_FACTORY = 2.25; //half the actual value, approximates production efficiency
    private static final int MAXIMUM_CIV_FACTORIES_PER_PROJECT = 15;
    private static final int MAXIMUM_INFRASTRUCTURE_LEVEL = 10;
    private static final String GAME_DATA_FILE = "stateInformationProcessed.csv";

    //INPUTS
    private static final String testNationName = "Soviet Union";
    private static final int testWarDays = 1999; //days from the beginning of 1936 to barbarossa

    //running total
    private double totalMilProduction = 0;

    //initial values for comparison
    private final int startingMilFactories;
    private final int startingCivFactories;

    //states storage
    private final ArrayList<State> states = new ArrayList<>();

    private Main() {
        String nationName = testNationName;
        nationName = nationName.replaceAll(" ", "");
        loadNation(nationName);
        startingMilFactories = countMilFactories();
        startingCivFactories = countCivFactories();
        for (int i = 0; i < testWarDays; i++) {
            dayLoop();
            System.out.println("Day #" + i + " completed");
        }
        System.out.println("Start Military Factories:  " + startingMilFactories);
        System.out.println("Start Civilian Factories:  " + startingCivFactories);
        System.out.println("========");
        System.out.println("Total Military Production: " + totalMilProduction);
        System.out.println("Total Military Factories:  " + countMilFactories());
        System.out.println("Total Civilian Factories:  " + countCivFactories());
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
        //TODO account for various buffs and de-buffs
        double constructionPoints = civFactories * PRODUCTION_PER_CIV_FACTORY;
        int currentState = 0;
        while (constructionPoints > 0 && currentState != states.size()) {
            if (states.get(currentState).getFreeBuildingSlots() != 0) {
                double constructionBlock;
                if (constructionPoints >= MAXIMUM_CIV_FACTORIES_PER_PROJECT * PRODUCTION_PER_CIV_FACTORY) {
                    constructionBlock = constructionPoints - (MAXIMUM_CIV_FACTORIES_PER_PROJECT * PRODUCTION_PER_CIV_FACTORY);
                } else {
                    constructionBlock = constructionPoints;
                }
                constructionPoints -= constructionBlock;
                states.get(currentState).addCivConstruction(constructionBlock);
            }
            currentState++;
        }
    }
    private int countCivFactories() {
        int civFactories = 0;
        for (State state : states) {
            civFactories += state.getCivFactories();
        }
        return civFactories;
    }
    private int countMilFactories() {
        int milFactories = 0;
        for (State state : states) {
            milFactories += state.getMilFactories();
        }
        return milFactories;
    }
    private void loadNation(String nationName) {
        ArrayList<State> initialStates = new ArrayList<>();
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
                    initialStates.add(new State(infrastructure, buildingSlots, milFactories, dockyards, civFactories));
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
        int lastInfrastructureLevel = MAXIMUM_INFRASTRUCTURE_LEVEL;
        while (initialStates.size() != states.size()) {
            for (State state : initialStates) {
                if (state.getInfrastructureLevel() == lastInfrastructureLevel) {
                    states.add(state);
                }
            }
            lastInfrastructureLevel--;
        }
    }
    public static void main(String[] args) {
        new Main();
    }
}
