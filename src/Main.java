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
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    //data file location
    private static final String GAME_DATA_FILE = "stateInformationProcessed.csv";

    //INPUTS
    //TODO replace with actual inputs instead of taking values in the code
    private static final String testCountryName = "Soviet Union";
    private static final int warDay = 1999; //calculateResults of 1936 to calculateResults of barbarossa


    private Main() {
        //load the country that the user specified from the data file
        String countryName = testCountryName;
        countryName = countryName.replaceAll(" ", "");
        State[] states = loadNation(countryName);
        //create an array to keep track of the instance that cuts off at each day.
        Country[] countryInstances = new Country[warDay];
        for (int cutoffDay = 0; cutoffDay < warDay; cutoffDay++) {
            countryInstances[cutoffDay] = new Country(warDay, cutoffDay, duplicateStateList(states));
        }

        System.out.println("all country instances created... processing now");
        //tell each instance to do the require processing, this may need to be changed to process in parallel for performance
        for (Country countryInstance : countryInstances) {
            countryInstance.calculateResults();
        }
        System.out.println("all country results calculated... graphing now");
        //gather all of the data to graph it
        double[] productionData = new double[warDay];
        double[] civFactoryData = new double[warDay];
        double[] milFactoryData = new double[warDay];
        double[] xAxisData = new double[warDay];
        for (int i = 0; i < countryInstances.length; i++) {
            productionData[i] = countryInstances[i].getMilProduction();
            civFactoryData[i] = countryInstances[i].countCivFactories();
            milFactoryData[i] = countryInstances[i].countMilFactories();
            xAxisData[i] = i;
        }
        //create a window for the graphs to go in and set some basic properties
        JFrame outputWindow = new JFrame();
        outputWindow.setTitle("Simulation Results");
        outputWindow.setPreferredSize(new Dimension(900, 900));
        outputWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        //create a JPanel to hold all of the graphs.
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayout(3, 1));
        outputWindow.add(contentPanel);
        //create all three graphs and set the required information
        XYChart productionGraph = new XYChart(1, 1);
        productionGraph.setTitle("Total Military Production over " + warDay + " days");
        productionGraph.setXAxisTitle("Day switched from civilian to military factories");
        productionGraph.setYAxisTitle("Total military production over " + warDay + " days");
        productionGraph.addSeries("Total military production over " + warDay + " days", xAxisData, productionData);
        contentPanel.add(new XChartPanel<>(productionGraph), 0);

        XYChart civFactoryGraph = new XYChart(1, 1);
        civFactoryGraph.setTitle("Total Civilian Factories after " + warDay + " days");
        civFactoryGraph.setXAxisTitle("Day switched from civilian to military factories");
        civFactoryGraph.setYAxisTitle("Civilian factories after " + warDay + " days");
        civFactoryGraph.addSeries("Civilian factories after " + warDay + " days", xAxisData, civFactoryData);
        contentPanel.add(new XChartPanel<>(civFactoryGraph), 1);

        XYChart milFactoryGraph = new XYChart(1, 1);
        milFactoryGraph.setTitle("Total Military Factories after " + warDay + " days");
        milFactoryGraph.setXAxisTitle("Day switched from civilian to military factories");
        milFactoryGraph.setYAxisTitle("Military factories after " + warDay + " days");
        milFactoryGraph.addSeries("Military factories after " + warDay + " days", xAxisData, milFactoryData);
        contentPanel.add(new XChartPanel<>(milFactoryGraph), 2);

        //show the window once everything has been added
        outputWindow.pack();
        outputWindow.toFront();
        outputWindow.setVisible(true);
    }
    private State[] duplicateStateList(State[] original) {
        State[] newStateList = new State[original.length];
        for (int i = 0; i < original.length; i++) {

            newStateList[i] = new State(original[i].getInfrastructureLevel(), original[i].getBuildingSlots(), original[i].getMilFactories(), original[i].getDockyards(), original[i].getCivFactories());
        }
        return newStateList;
    }
    private State[] loadNation(String nationName) {
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
    public static void main(String[] args) {
        new Main();
    }
}
