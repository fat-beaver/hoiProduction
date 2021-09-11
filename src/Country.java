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

import java.util.ArrayList;
import java.util.Collections;

public class Country {
    //general constants
    private static final double PRODUCTION_PER_CIV_FACTORY = 5;
    private static final double BASE_POLITICAL_POWER_GAIN = 1.0;
    private static final int MAXIMUM_CIV_FACTORIES_PER_PROJECT = 15;
    private static final double STABILITY_MAXIMUM_FACTORY_BONUS = 0.2;
    private static final double STABILITY_MAXIMUM_FACTORY_PENALTY = -0.5;

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

    //general properties
    private final String name;
    private final double stability;
    private final double warSupport;
    private final ArrayList<State> states;
    private EconomyTech economyTech = EconomyTech.civilian; //almost all nations start at civilian so this is fine for now
    private int politicalPower = 0;

    //running total
    private double totalMilProduction;
    //tech progressions
    private int constructionTechLevel = 0;
    private int industryTechLevel = 0;
    private int toolsTechLevel = 0;
    private int toolsSpecialLevel = 0;
    private enum EconomyTech {
        civilian(0.00, 150, 0.35, -0.3, -0.3),
        early   (0.15, 150, 0.30, -0.1, -0.1),
        partial (0.25, 150, 0.25, +0.0, +0.1),
        war     (0.50, 150, 0.20, +0.0, +0.2);
        //total mobilisation should go here, but it is ignored because the manpower decrease it causes cannot be considered
        private final double warSupport;
        private final double politicalPowerCost;
        private final double consumerGoods;
        private final double civConstructionBonus;
        private final double milConstructionBonus;
        EconomyTech(double warSupport, double politicalPowerCost, double consumerGoods, double civConstructionBonus, double milConstructionBonus) {
            this.warSupport = warSupport;
            this.politicalPowerCost = politicalPowerCost;
            this.consumerGoods = consumerGoods;
            this.civConstructionBonus = civConstructionBonus;
            this.milConstructionBonus = milConstructionBonus;
        }
    }
    public Country(State[] initialStates, double stability, double warSupport, String name) {
        states = new ArrayList<>();
        Collections.addAll(states, initialStates);
        this.stability = stability;
        this.warSupport = warSupport;
        this.name = name;
    }
    public Country copy() {
        State[] newStateList = new State[states.size()];
        for (int i = 0; i < states.size(); i++) {
            newStateList[i] = new State(states.get(i).getInfrastructureLevel(), states.get(i).getIndustrialLevel(), states.get(i).getDockyards(), states.get(i).getCivFactories(), states.get(i).getMilFactories());
        }
        return new Country(newStateList, stability, warSupport, name);
    }
    public void calculateResults(int cutoffDay, int duration) {
        for (int currentDay = 0; currentDay < duration; currentDay++) {
            //check for technology advancement
            checkTechProgress(currentDay);
            //do the actual processing
            dayLoop(currentDay, cutoffDay);
        }
    }
    public void addState (State toAdd) {
        states.add(toAdd);
    }
    private void dayLoop(int currentDay, int cutoffDay) {
        //calculate the effect of the current stability on factory output
        double stabilityFactoryBonus;
        if (stability >= 0.5) {
            stabilityFactoryBonus = STABILITY_MAXIMUM_FACTORY_BONUS * (0.5 - stability);
        } else {
            stabilityFactoryBonus = STABILITY_MAXIMUM_FACTORY_PENALTY * (0.5 - stability);
        }
        //add the military production for the day to the total
        double milProductionMultiplier = 1 + (INDUSTRY_TECHNOLOGY_PRODUCTION_INCREMENT * industryTechLevel) + stabilityFactoryBonus;
        for (State state : states) {
            totalMilProduction += (state.getMilProduction() * milProductionMultiplier);
        }
        //add political power and change economy law if political power and war support allow for it and not at max already
        politicalPower += BASE_POLITICAL_POWER_GAIN;
        if (economyTech != EconomyTech.war) {
            EconomyTech nextEconomyTech = EconomyTech.values()[economyTech.ordinal() + 1];
            if (nextEconomyTech.politicalPowerCost <= politicalPower && nextEconomyTech.warSupport <= warSupport) {
                economyTech = nextEconomyTech;
            }
        }
        //calculate how much construction to do
        int effectiveCivFactories = (int) (countCivFactories() - (economyTech.consumerGoods * (countCivFactories() + countMilFactories())));
        double constructionPoints = effectiveCivFactories * PRODUCTION_PER_CIV_FACTORY;
        //go through the list of states and assign the maximum number of factories to each construction until run out
        int currentState = 0;
        while (constructionPoints > 0 && currentState != states.size()) {
            if (states.get(currentState).getFreeBuildingSlots() != 0) {
                double constructionBlock = Math.min(constructionPoints, MAXIMUM_CIV_FACTORIES_PER_PROJECT * PRODUCTION_PER_CIV_FACTORY);
                constructionPoints -= constructionBlock;
                //account for construction speed bonus from technology
                constructionBlock *= (1 + (CONSTRUCTION_TECHNOLOGY_INCREMENT * constructionTechLevel));
                //choose whether to build civ or mil factories after calculating the size of the construction block
                if (currentDay < cutoffDay || states.get(currentState).isCivUnderConstruction()) {
                    //use the civ construction bonus
                    constructionBlock *= (1 + economyTech.civConstructionBonus);
                    states.get(currentState).addCivConstruction(constructionBlock);
                } else {
                    //use the mil construction bonus
                    constructionBlock *= (1 + economyTech.milConstructionBonus);
                    states.get(currentState).addMilConstruction(constructionBlock);
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
    public double getMilProduction() {
        return totalMilProduction;
    }
    public int countCivFactories() {
        int civFactories = 0;
        //loop through the list of states to count how many civ factories the country has
        for (State state : states) {
            civFactories += state.getCivFactories();
        }
        return civFactories;
    }
    public int countMilFactories() {
        int milFactories = 0;
        //loop through the list of states to count how many mil factories the country has
        for (State state : states) {
            milFactories += state.getMilFactories();
        }
        return milFactories;
    }
    public String getName() {
        return name;
    }
}
