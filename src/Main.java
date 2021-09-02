/*
MIT License

Copyright (c) 2021 fat-beaver

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    //general constants
    private static final double PRODUCTION_PER_CIV_FACTORY = 5;
    //half the actual value, approximates production efficiency
    private static final int MAXIMUM_CIV_FACTORIES_PER_PROJECT = 15;
    private static final int MAXIMUM_INFRASTRUCTURE_LEVEL = 10;
    private static final String GAME_DATA_FILE = "stateInformationProcessed.csv";
    private final static double CONSUMER_GOODS_AMOUNT = 0.35;
    private final static double ECONOMY_LAW_CONSTRUCTION = - 0.3;
    //research constants
    private final static int[] CONSTRUCTION_TECHNOLOGY_INCREASES = {0, 365, 1095, 1825, 2555}; //days after 1936 for each
    private final static int[] INDUSTRY_TECHNOLOGY_INCREASES = {170, 365, 1095, 1825, 2555}; //of the technologies
    private final static int[] TOOLS_TECHNOLOGY_INCREASES = {0, 365, 1095, 1825, 2555};
    private final static int[] TOOLS_SPECIAL_INCREASES = {2555};
    private final static int CONSTRUCTION_TECHNOLOGY_RESEARCH_TIME = 170; //days to research each construction tech
    private final static int INDUSTRY_TECHNOLOGY_RESEARCH_TIME = 170; //days to research each industry tech
    private final static int TOOLS_TECHNOLOGY_RESEARCH_TIME = 127;
    private final static int TOOLS_SPECIAL_RESEARCH_TIME = 127;
    private final static double CONSTRUCTION_TECHNOLOGY_INCREMENT = 0.1;
    private final static double INDUSTRY_TECHNOLOGY_SLOT_INCREMENT = 0.2;
    private final static double INDUSTRY_TECHNOLOGY_PRODUCTION_INCREMENT = 0.15;
    private final static double TOOLS_TECHNOLOGY_CAP_INCREMENT = 0.1;
    private final static double TOOLS_SPECIAL_GAIN_INCREMENT = 0.1;

    //INPUTS
    private static final String testNationName = "Soviet Union";
    private static final int testWarDays = 1999; //days from the beginning of 1936 to barbarossa
    private static final int testCutoffDay = 0;

    //running total
    private double totalMilProduction = 0;

    //states storage
    private State[] states;

    //tech progressions
    private int constructionTechLevel = 0;
    private int industryTechLevel = 0;
    private int toolsTechLevel = 0;
    private int toolsSpecialLevel = 0;

    private Main() {
        //TODO replace with an actual input of the name instead of taking a value in the code
        String nationName = testNationName;
        nationName = nationName.replaceAll(" ", "");
        loadNation(nationName);
        //initial values for comparison
        int startingMilFactories = countMilFactories();
        int startingCivFactories = countCivFactories();
        for (int currentDay = 0; currentDay < testWarDays; currentDay++) {
            //check for technology advancement
            checkTechProgress(currentDay);
            //do the actual processing
            dayLoop(currentDay);
        }
        //print out some basic information for comparison
        System.out.println("Start Military Factories:  " + startingMilFactories);
        System.out.println("Start Civilian Factories:  " + startingCivFactories);
        System.out.println("========");
        System.out.println("Total Military Production: " + totalMilProduction);
        System.out.println("Total Military Factories:  " + countMilFactories());
        System.out.println("Total Civilian Factories:  " + countCivFactories());
    }
    private void dayLoop(int currentDay) {
        //add the military production for the dZay to the total
        double milProductionMultiplier = 1 + INDUSTRY_TECHNOLOGY_PRODUCTION_INCREMENT * industryTechLevel;
        for (State state : states) {
            totalMilProduction += (state.getMilProduction() * milProductionMultiplier);
        }
        //calculate how much construction to do
        int effectiveCivFactories = (int) (countCivFactories() - (CONSUMER_GOODS_AMOUNT * (countCivFactories() + countMilFactories())));
        double constructionPoints = effectiveCivFactories * PRODUCTION_PER_CIV_FACTORY;
        //go through the list of states and assign the maximum number of factories to each construction until run out
        int currentState = 0;
        while (constructionPoints > 0 && currentState != states.length) {
            if (states[currentState].getFreeBuildingSlots() != 0) {
                double constructionBlock = Math.min(constructionPoints, MAXIMUM_CIV_FACTORIES_PER_PROJECT * PRODUCTION_PER_CIV_FACTORY);
                constructionPoints -= constructionBlock;
                //account for construction speed from economy law
                constructionBlock *= (1 + ECONOMY_LAW_CONSTRUCTION);
                constructionBlock *= (1 + ECONOMY_LAW_CONSTRUCTION + (CONSTRUCTION_TECHNOLOGY_INCREMENT * constructionTechLevel));
                //choose whether to build civ or mil factories after calculating the size of the construction block
                if (currentDay < testCutoffDay || states[currentState].isCivUnderConstruction()) {
                    states[currentState].addCivConstruction(constructionBlock);
                } else {
                    states[currentState].addMilConstruction(constructionBlock);
                }
            }
            //move on to the next state
            currentState++;
        }
    }
    private void checkTechProgress(int currentDay) {
        //check for construction tech increases
        for (int techIncreaseDay : CONSTRUCTION_TECHNOLOGY_INCREASES) {
            if (currentDay == (techIncreaseDay + CONSTRUCTION_TECHNOLOGY_RESEARCH_TIME)) {
                constructionTechLevel++;
            }
        }
        //check for industry tech increases
        for (int techIncreaseDay : INDUSTRY_TECHNOLOGY_INCREASES) {
            if (currentDay == (techIncreaseDay + INDUSTRY_TECHNOLOGY_RESEARCH_TIME)) {
                industryTechLevel++;
                for (State state : states) {
                    state.setBuildingSlotsBonus(INDUSTRY_TECHNOLOGY_SLOT_INCREMENT * industryTechLevel);
                }
            }
        }
        //check for tool tech increases
        for (int techIncreaseDay : TOOLS_TECHNOLOGY_INCREASES) {
            if (currentDay == (techIncreaseDay + TOOLS_TECHNOLOGY_RESEARCH_TIME)) {
                toolsTechLevel++;
                for (State state : states) {
                    state.setProductionEfficiencyCapBonus(TOOLS_TECHNOLOGY_CAP_INCREMENT * toolsTechLevel);
                }
            }
        }
        for (int techIncreaseDay : TOOLS_SPECIAL_INCREASES) {
            if (currentDay == (techIncreaseDay + TOOLS_SPECIAL_RESEARCH_TIME)) {
                toolsSpecialLevel++;
                for (State state : states) {
                    state.setProductionEfficiencyGainBonus(TOOLS_SPECIAL_GAIN_INCREMENT * toolsSpecialLevel);
                }
            }
        }
    }
    private int countCivFactories() {
        int civFactories = 0;
        //loop through the list of states to count how many civ factories the country has
        for (State state : states) {
            civFactories += state.getCivFactories();
        }
        return civFactories;
    }
    private int countMilFactories() {
        int milFactories = 0;
        //loop through the list of states to count how many mil factories the country has
        for (State state : states) {
            milFactories += state.getMilFactories();
        }
        return milFactories;
    }
    private void loadNation(String nationName) {
        //create a structure to hold all of the states before they get added to the simulation properly
        ArrayList<State> initialStates = new ArrayList<>();
        //read required state and country data from the included data file
        File dataFile = new File(GAME_DATA_FILE);
        try {
            Scanner nationDataReader = new Scanner(dataFile);
            //remove the human-readable headings
            nationDataReader.nextLine();
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
        //sort the states by infrastructure level and then add them to the main arraylist
        int lastInfrastructureLevel = MAXIMUM_INFRASTRUCTURE_LEVEL;
        states = new State[initialStates.size()];
        int currentSlot = 0;
        while (currentSlot < states.length) {
            for (State state : initialStates) {
                if (state.getInfrastructureLevel() == lastInfrastructureLevel) {
                    states[currentSlot] = state;
                    currentSlot++;
                }
            }
            lastInfrastructureLevel--;
        }
    }
    public static void main(String[] args) {
        new Main();
    }
}
