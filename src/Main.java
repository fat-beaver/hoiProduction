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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class Main {
    //constants for visual output
    private static final int WINDOW_WIDTH = 1600;
    private static final int WINDOW_HEIGHT = 900;

    //data file location
    private static final String GAME_DATA_FILE = "stateInformationProcessed.csv";

    //the date the game starts at
    private static final String GAME_START_DATE = "01-01-1936";
    //some constants for the simulation which runs by default
    private static final String DEFAULT_COUNTRY_CODE = "SOV";
    private static final String DEFAULT_END_DATE = "22-06-1941";


    public static void main(String[] args) {
        String countryName = DEFAULT_COUNTRY_CODE;
        State[] states = loadDataFile(countryName.toLowerCase());
        int duration = 0;
        String rawDate = DEFAULT_END_DATE;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            Date gameStart = dateFormat.parse(GAME_START_DATE);
            Date endDate = dateFormat.parse(rawDate);
            long durationMilli = endDate.getTime() - gameStart.getTime();
            duration = (int) TimeUnit.DAYS.convert(durationMilli, TimeUnit.MILLISECONDS);
        } catch (ParseException e) {
            System.out.println("Could not parse default values, this is a bug");
            System.exit(3);
        }
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
        JFrame window = new JFrame();
        window.setTitle("Simulation Results for " + countryName);
        window.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        window.setResizable(false);
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // JPanel for all of the buttons and stuff
        Container pane = window.getContentPane();
        GridBagConstraints constraints = new GridBagConstraints();
        pane.setLayout(new GridBagLayout());
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.5;
        constraints.gridx = 0;
        constraints.gridy = 0;
        pane.add(new JLabel("Country Code/Name"), constraints);
        constraints.gridx = 1;
        constraints.gridy = 0;
        JTextField countryNameField = new JTextField(DEFAULT_COUNTRY_CODE);
        pane.add(countryNameField, constraints);
        constraints.gridx = 0;
        constraints.gridy = 1;
        pane.add(new JLabel("Target Date"), constraints);
        constraints.gridx = 1;
        constraints.gridy = 1;
        JTextField endDateField = new JTextField(DEFAULT_END_DATE);
        pane.add(endDateField, constraints);
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        JButton goButton = new JButton("GO");
        pane.add(goButton, constraints);

        //create all three graphs and set the required information
        DecimalFormat cursorFormat = new DecimalFormat("#,###.###");
        XYChart productionGraph = new XYChart(WINDOW_WIDTH / 2, WINDOW_HEIGHT / 3);
        productionGraph.setTitle("Total military production from " + GAME_START_DATE + " to " + rawDate + " (" + duration + " days) for " + countryName);
        productionGraph.setXAxisTitle("Time switched from civilian to military factories (years after game start)");
        productionGraph.setYAxisTitle("Total military production");
        productionGraph.addSeries("Total military production", xAxisData, productionData);
        productionGraph.getStyler().setLegendVisible(false);
        productionGraph.getStyler().setMarkerSize(4);
        productionGraph.getStyler().setXAxisMin(0d);
        productionGraph.getStyler().setYAxisMin(0d);
        productionGraph.getStyler().setCursorEnabled(true);
        productionGraph.getStyler().setCustomCursorXDataFormattingFunction(x -> "Cut-off point: " + cursorFormat.format(x) + " years after start");
        productionGraph.getStyler().setCustomCursorYDataFormattingFunction(cursorFormat::format);
        XChartPanel<XYChart> productionGraphPanel = new XChartPanel<>(productionGraph);
        productionGraphPanel.removeMouseListener(productionGraphPanel.getMouseListeners()[0]); //remove the right-click menu
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 5;
        pane.add(productionGraphPanel, constraints);

        XYChart civFactoryGraph = new XYChart(WINDOW_WIDTH / 2, WINDOW_HEIGHT / 3);
        civFactoryGraph.setTitle("Total Civilian Factories on " + rawDate + " (after " + duration + " days) for " + countryName);
        civFactoryGraph.setXAxisTitle("Time switched from civilian to military factories (years after game start)");
        civFactoryGraph.setYAxisTitle("Civilian factories");
        civFactoryGraph.addSeries("Civilian factories", xAxisData, civFactoryData);
        civFactoryGraph.getStyler().setLegendVisible(false);
        civFactoryGraph.getStyler().setMarkerSize(4);
        civFactoryGraph.getStyler().setXAxisMin(0d);
        civFactoryGraph.getStyler().setYAxisMin(0d);
        civFactoryGraph.getStyler().setCursorEnabled(true);
        civFactoryGraph.getStyler().setCustomCursorXDataFormattingFunction(x -> "Cut-off point: " + cursorFormat.format(x) + " years after start");
        XChartPanel<XYChart> civGraphPanel = new XChartPanel<>(civFactoryGraph);
        civGraphPanel.removeMouseListener(civGraphPanel.getMouseListeners()[0]); //remove the right-click menu
        constraints.gridx = 2;
        constraints.gridy = 5;
        pane.add(civGraphPanel, constraints);

        XYChart milFactoryGraph = new XYChart(WINDOW_WIDTH / 2, WINDOW_HEIGHT / 3);
        milFactoryGraph.setTitle("Total Military Factories on " + rawDate + " (after " + duration + " days) for " + countryName);
        milFactoryGraph.setXAxisTitle("Time switched from civilian to military factories (years after game start)");
        milFactoryGraph.setYAxisTitle("Military factories");
        milFactoryGraph.addSeries("Military factories", xAxisData, milFactoryData);
        milFactoryGraph.getStyler().setLegendVisible(false);
        milFactoryGraph.getStyler().setMarkerSize(4);
        milFactoryGraph.getStyler().setXAxisMin(0d);
        milFactoryGraph.getStyler().setYAxisMin(0d);
        milFactoryGraph.getStyler().setCursorEnabled(true);
        milFactoryGraph.getStyler().setCustomCursorXDataFormattingFunction(x -> "Cut-off point: " + cursorFormat.format(x) + " years after start");
        XChartPanel<XYChart> milGraphPanel = new XChartPanel<>(milFactoryGraph);
        milGraphPanel.removeMouseListener(milGraphPanel.getMouseListeners()[0]); //remove the right-click menu
        constraints.gridx = 2;
        constraints.gridy = 10;
        pane.add(milGraphPanel, constraints);

        goButton.addActionListener(actionEvent -> {
            String countryName1 = countryNameField.getText();
            State[] states1 = loadDataFile(countryName1.toLowerCase());
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            int duration1;
            try {
                Date gameStart = dateFormat.parse(GAME_START_DATE);
                String rawDate1 = endDateField.getText();
                Date endDate = dateFormat.parse(rawDate1);
                long durationMilli = endDate.getTime() - gameStart.getTime();
                duration1 = (int) TimeUnit.DAYS.convert(durationMilli, TimeUnit.MILLISECONDS);
                Country[] countryInstances1 = new Country[duration1];
                for (int cutoffDay = 0; cutoffDay < duration1; cutoffDay++) {
                    countryInstances1[cutoffDay] = new Country(duration1, cutoffDay, duplicateStateList(states1));
                }
                SimulationProcessor simProcessor1 = new SimulationProcessor(countryInstances1, 0, countryInstances1.length);
                ForkJoinPool processingPool1 = new ForkJoinPool();
                processingPool1.invoke(simProcessor1);
                double[] productionData1 = new double[duration1];
                double[] civFactoryData1 = new double[duration1];
                double[] milFactoryData1 = new double[duration1];
                double[] xAxisData1 = new double[duration1];
                for (int i = 0; i < countryInstances1.length; i++) {
                    productionData1[i] = countryInstances1[i].getMilProduction();
                    civFactoryData1[i] = countryInstances1[i].countCivFactories();
                    milFactoryData1[i] = countryInstances1[i].countMilFactories();
                    xAxisData1[i] = ((double) i )/ 365;
                }
                productionGraph.updateXYSeries("Total military production", xAxisData1, productionData1, null);
                civFactoryGraph.updateXYSeries("Civilian factories", xAxisData1, civFactoryData1, null);
                milFactoryGraph.updateXYSeries("Military factories", xAxisData1, milFactoryData1, null);
                civFactoryGraph.setTitle("Total Civilian Factories on " + rawDate1 + " (after " + duration1 + " days) for " + countryName1);
                milFactoryGraph.setTitle("Total Military Factories on " + rawDate1 + " (after " + duration1 + " days) for " + countryName1);
                productionGraph.setTitle("Total military production from " + GAME_START_DATE + " to " + rawDate1 + " (" + duration1 + " days) for " + countryName1);
                window.repaint();
            } catch (ParseException e) {
                endDateField.setBackground(Color.RED);
            }

        });

        //show the window once everything has been added
        window.pack();
        window.toFront();
        window.setVisible(true);
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
