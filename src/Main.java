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

    //the three graphs
    private final XYChart productionGraph;
    private final XYChart civFactoryGraph;
    private final XYChart milFactoryGraph;

    //the window
    private final JFrame window;


    public Main() {
        //create a window and set some basic properties
        window = new JFrame();
        window.setTitle("Hearts of Iron IV Production Calculator");
        window.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        window.setResizable(false);
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        //add the UI elements and specify their sizes & positions using GridBagLayout
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
        goButton.addActionListener(actionEvent -> doSimulation(endDateField.getText(), countryNameField.getText()));
        pane.add(goButton, constraints);

        //get all of the graphs ready
        DecimalFormat graphCursorFormat = new DecimalFormat("#,###.###");
        productionGraph = setGraphVisuals(new XYChart(WINDOW_WIDTH / 2, WINDOW_HEIGHT / 3), graphCursorFormat);
        productionGraph.setXAxisTitle("Time switched from civilian to military factories (years after game start)");
        productionGraph.setYAxisTitle("Total military production");
        productionGraph.addSeries("Total military production", new double[] {0}, new double[] {0});
        productionGraph.getStyler().setCustomCursorYDataFormattingFunction(graphCursorFormat::format);
        XChartPanel<XYChart> productionGraphPanel = new XChartPanel<>(productionGraph);
        productionGraphPanel.removeMouseListener(productionGraphPanel.getMouseListeners()[0]); //remove the right-click menu
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 5;
        pane.add(productionGraphPanel, constraints);

        civFactoryGraph = setGraphVisuals(new XYChart(WINDOW_WIDTH / 2, WINDOW_HEIGHT / 3), graphCursorFormat);
        civFactoryGraph.setXAxisTitle("Time switched from civilian to military factories (years after game start)");
        civFactoryGraph.setYAxisTitle("Civilian factories");
        civFactoryGraph.addSeries("Civilian factories", new double[] {0}, new double[] {0});
        XChartPanel<XYChart> civGraphPanel = new XChartPanel<>(civFactoryGraph);
        civGraphPanel.removeMouseListener(civGraphPanel.getMouseListeners()[0]); //remove the right-click menu
        constraints.gridy = 5;
        pane.add(civGraphPanel, constraints);

        milFactoryGraph = setGraphVisuals(new XYChart(WINDOW_WIDTH / 2, WINDOW_HEIGHT / 3), graphCursorFormat);
        milFactoryGraph.setXAxisTitle("Time switched from civilian to military factories (years after game start)");
        milFactoryGraph.setYAxisTitle("Military factories");
        milFactoryGraph.addSeries("Military factories", new double[] {0}, new double[] {0});
        XChartPanel<XYChart> milGraphPanel = new XChartPanel<>(milFactoryGraph);
        milGraphPanel.removeMouseListener(milGraphPanel.getMouseListeners()[0]); //remove the right-click menu
        constraints.gridy = 10;
        pane.add(milGraphPanel, constraints);

        //show the window once everything has been added
        window.pack();
        window.toFront();
        window.setVisible(true);
        //do a simulation after everything has been set up
        doSimulation(DEFAULT_END_DATE, DEFAULT_COUNTRY_CODE);
    }
    private void doSimulation(String rawDate, String countryName) {
        State[] states = loadDataFile(countryName.toLowerCase());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        int duration;
        try {
            Date gameStart = dateFormat.parse(GAME_START_DATE);
            Date endDate = dateFormat.parse(rawDate);
            long durationMilli = endDate.getTime() - gameStart.getTime();
            duration = (int) TimeUnit.DAYS.convert(durationMilli, TimeUnit.MILLISECONDS);
            Country[] countryInstances = new Country[duration];
            for (int cutoffDay = 0; cutoffDay < duration; cutoffDay++) {
                countryInstances[cutoffDay] = new Country(duration, cutoffDay, duplicateStateList(states));
            }
            SimulationProcessor simProcessor = new SimulationProcessor(countryInstances, 0, countryInstances.length);
            ForkJoinPool processingPool = new ForkJoinPool();
            processingPool.invoke(simProcessor);
            double[] productionData = new double[duration];
            double[] civFactoryData = new double[duration];
            double[] milFactoryData = new double[duration];
            double[] dateData = new double[duration];
            for (int i = 0; i < countryInstances.length; i++) {
                productionData[i] = countryInstances[i].getMilProduction();
                civFactoryData[i] = countryInstances[i].countCivFactories();
                milFactoryData[i] = countryInstances[i].countMilFactories();
                dateData[i] = ((double) i )/ 365;
            }
            productionGraph.updateXYSeries("Total military production", dateData, productionData, null);
            civFactoryGraph.updateXYSeries("Civilian factories", dateData, civFactoryData, null);
            milFactoryGraph.updateXYSeries("Military factories", dateData, milFactoryData, null);
            civFactoryGraph.setTitle("Total Civilian Factories on " + rawDate + " (after " + duration + " days) for " + countryName);
            milFactoryGraph.setTitle("Total Military Factories on " + rawDate + " (after " + duration + " days) for " + countryName);
            productionGraph.setTitle("Total military production from " + GAME_START_DATE + " to " + rawDate + " (" + duration + " days) for " + countryName);
            window.repaint();
        } catch (ParseException ignored) {}
    }
    private State[] duplicateStateList(State[] original) {
        State[] newStateList = new State[original.length];
        for (int i = 0; i < original.length; i++) {
            newStateList[i] = new State(original[i].getInfrastructureLevel(), original[i].getBuildingSlots(), original[i].getMilFactories(), original[i].getDockyards(), original[i].getCivFactories());
        }
        return newStateList;
    }
    private XYChart setGraphVisuals(XYChart graph, DecimalFormat cursorFormat) {
        //set all of the desired visuals for the graphs in one place
        graph.getStyler().setLegendVisible(false);
        graph.getStyler().setMarkerSize(4);
        graph.getStyler().setXAxisMin(0d);
        graph.getStyler().setYAxisMin(0d);
        graph.getStyler().setCursorEnabled(true);
        graph.getStyler().setCustomCursorXDataFormattingFunction(x -> "Cut-off point: " + cursorFormat.format(x) + " years after start");
        return graph;
    }
    private State[] loadDataFile(String nationName) {
        //create a structure to hold all of the states before they get added to the simulation properly
        ArrayList<State> initialStates = new ArrayList<>();
        //read required state and country data from the included data file
        File dataFile = new File(GAME_DATA_FILE);
        try {
            Scanner nationDataReader = new Scanner(dataFile);
            //remove the human-readable headings
            nationDataReader.nextLine();
            while (nationDataReader.hasNextLine()) {
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
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
