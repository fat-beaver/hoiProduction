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

import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class Main {
    //data file location
    private static final String GAME_DATA_FILE = "stateInformationProcessed.csv";

    //the date the game starts at
    private static final String GAME_START_DATE = "01-01-1936";

    public static void main(String[] args) {
        //ask the user which nation they want to calculate for
        State[] states;
        String countryName = "";
        Scanner keyboardInput = new Scanner(System.in);
        do {
            System.out.println("Please enter a country code (e.g. CAN) OR a *full* country name (e.g. dominion of canada)");
            String nameInput = keyboardInput.nextLine();
            states = loadDataFile(nameInput.replaceAll(" ", "").toLowerCase());
            if (states.length == 0) {
                System.out.println("Invalid name");
            } else {
                countryName = nameInput;
            }
        } while (states.length == 0);
        //ask the user what point they want to reach maximum production at (generally the start of the war)
        int duration = 0;
        String rawDate = null;
        do {
            try {
                System.out.println("Please enter the date to end at in the format DD-MM-YYYY (e.g. 22-06-1941)");
                rawDate = keyboardInput.nextLine();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                Date gameStart = dateFormat.parse(GAME_START_DATE);
                Date endDate = dateFormat.parse(rawDate);
                long durationMilli = endDate.getTime() - gameStart.getTime();
                duration = (int) TimeUnit.DAYS.convert(durationMilli, TimeUnit.MILLISECONDS);
                if (duration < 0) {
                    duration = 0;
                }
            } catch (NumberFormatException | ParseException e) {
                System.out.println("invalid date");
            }

        } while (duration == 0);

        //create an array to keep track of the instance that cuts off at each day.
        Country[] countryInstances = new Country[duration];
        for (int cutoffDay = 0; cutoffDay < duration; cutoffDay++) {
            countryInstances[cutoffDay] = new Country(duration, cutoffDay, duplicateStateList(states));
        }

        System.out.println("all country instances created... processing now");
        //create a fork-join pool to split up the task of calculating what happens in each country instance
        SimulationProcessor simProcessor = new SimulationProcessor(countryInstances, 0, countryInstances.length);
        ForkJoinPool processingPool = new ForkJoinPool();
        processingPool.invoke(simProcessor);

        System.out.println("all country results calculated... graphing now");
        //gather all of the data to graph it
        double[] productionData = new double[duration];
        double[] civFactoryData = new double[duration];
        double[] milFactoryData = new double[duration];
        double[] xAxisData = new double[duration];
        for (int i = 0; i < countryInstances.length; i++) {
            productionData[i] = countryInstances[i].getMilProduction();
            civFactoryData[i] = countryInstances[i].countCivFactories();
            milFactoryData[i] = countryInstances[i].countMilFactories();
            xAxisData[i] = ((double) i )/ 365;
        }
        //create a window for the graphs to go in and set some basic properties
        JFrame outputWindow = new JFrame();
        outputWindow.setTitle("Simulation Results for " + countryName);
        outputWindow.setPreferredSize(new Dimension(900, 900));
        outputWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        //create a JPanel to hold all of the graphs.
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayout(3, 1));
        outputWindow.add(contentPanel);
        //create all three graphs and set the required information
        XYChart productionGraph = new XYChart(1, 1);
        productionGraph.setTitle("Total military production from " + GAME_START_DATE + " to " + rawDate + " (" + duration + " days) for " + countryName);
        productionGraph.setXAxisTitle("Time switched from civilian to military factories (years after game start)");
        productionGraph.setYAxisTitle("Total military production");
        productionGraph.addSeries("Total military production", xAxisData, productionData);
        contentPanel.add(new XChartPanel<>(productionGraph), 0);

        XYChart civFactoryGraph = new XYChart(1, 1);
        civFactoryGraph.setTitle("Total Civilian Factories on " + rawDate + " (after " + duration + " days) for " + countryName);
        civFactoryGraph.setXAxisTitle("Time switched from civilian to military factories (years after game start)");
        civFactoryGraph.setYAxisTitle("Civilian factories");
        civFactoryGraph.addSeries("Civilian factories", xAxisData, civFactoryData);
        contentPanel.add(new XChartPanel<>(civFactoryGraph), 1);

        XYChart milFactoryGraph = new XYChart(1, 1);
        milFactoryGraph.setTitle("Total Military Factories on " + rawDate + " (after " + duration + " days) for " + countryName);
        milFactoryGraph.setXAxisTitle("Time switched from civilian to military factories (years after game start)");
        milFactoryGraph.setYAxisTitle("Military factories");
        milFactoryGraph.addSeries("Military factories", xAxisData, milFactoryData);
        contentPanel.add(new XChartPanel<>(milFactoryGraph), 2);

        //show the window once everything has been added
        outputWindow.pack();
        outputWindow.toFront();
        outputWindow.setVisible(true);
    }
    private static State[] duplicateStateList(State[] original) {
        State[] newStateList = new State[original.length];
        for (int i = 0; i < original.length; i++) {

            newStateList[i] = new State(original[i].getInfrastructureLevel(), original[i].getBuildingSlots(), original[i].getMilFactories(), original[i].getDockyards(), original[i].getCivFactories());
        }
        return newStateList;
    }
    private static State[] loadDataFile(String nationName) {
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
                if (stateInfo[0].toLowerCase().equals(nationName) || stateInfo[1].toLowerCase().equals(nationName)) {
                    int infrastructure = Integer.parseInt(stateInfo[2]);
                    int buildingSlots= Integer.parseInt(stateInfo[3]);
                    int milFactories = Integer.parseInt(stateInfo[4]);
                    int dockyards = Integer.parseInt(stateInfo[5]);
                    int civFactories = Integer.parseInt(stateInfo[6]);
                    initialStates.add(new State(infrastructure, buildingSlots, milFactories, dockyards, civFactories));
                }
                if (stateInfo[0].equals("0")) {
                    reachedEnd = true;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("data file could not be opened");
            System.exit(1);
        } catch (NumberFormatException e) {
            System.out.println("data file appears to have errors");
            System.exit(2);
        }
        //sort the states by infrastructure level and then add them to the main array
        State[] states = new State[initialStates.size()];
        int lastInfrastructureLevel = 0;
        int currentSlot = states.length - 1;
        while (currentSlot >= 0) {
            for (State state : initialStates) {
                if (state.getInfrastructureLevel() == lastInfrastructureLevel) {
                    states[currentSlot] = state;
                    currentSlot--;
                }
            }
            lastInfrastructureLevel++;
        }
        return states;
    }
}
