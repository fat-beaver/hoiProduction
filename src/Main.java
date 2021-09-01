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
    //CONSTANTS
    private static final double PRODUCTION_PER_CIV_FACTORY = 5;
    private static final double PRODUCTION_PER_MIL_FACTORY = 2.25; //half the actual value, approximates production efficiency
    private static final int MAXIMUM_CIV_FACTORIES_PER_PROJECT = 15;
    private static final int MAXIMUM_INFRASTRUCTURE_LEVEL = 10;
    private static final String GAME_DATA_FILE = "stateInformationProcessed.csv";

    //INPUTS
    private static final String testNationName = "Soviet Union";
    private static final int testWarDays = 1999; //days from the beginning of 1936 to barbarossa
        private static final int testCutoffDay = 1000;

    //running total
    private double totalMilProduction = 0;

    //states storage
    private final ArrayList<State> states = new ArrayList<>();

    private Main() {
        String nationName = testNationName;
        nationName = nationName.replaceAll(" ", "");
        loadNation(nationName);
        //initial values for comparison
        int startingMilFactories = countMilFactories();
        int startingCivFactories = countCivFactories();
        for (int i = 0; i < testWarDays; i++) {
            dayLoop(i);
        }
        System.out.println("Start Military Factories:  " + startingMilFactories);
        System.out.println("Start Civilian Factories:  " + startingCivFactories);
        System.out.println("========");
        System.out.println("Total Military Production: " + totalMilProduction);
        System.out.println("Total Military Factories:  " + countMilFactories());
        System.out.println("Total Civilian Factories:  " + countCivFactories());
    }
    private void dayLoop(int currentDay) {
        //TODO implement proper production efficiency
        totalMilProduction += countMilFactories() * PRODUCTION_PER_MIL_FACTORY;
        //TODO account for various buffs and de-buffs
        double constructionPoints = countCivFactories() * PRODUCTION_PER_CIV_FACTORY;
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
                if (currentDay < testCutoffDay || states.get(currentState).isCivUnderConstruction()) {
                    states.get(currentState).addCivConstruction(constructionBlock);
                } else {
                    states.get(currentState).addMilConstruction(constructionBlock);
                }
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
