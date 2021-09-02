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

public class State {
    //CONSTANTS
    private static final int MILITARY_FACTORY_COST = 7200;
    private static final int CIVILIAN_FACTORY_COST = 10800;
    //basic starting info
    private final int baseInfrastructure;
    private final int baseBuildingSlots;
    private final int baseDockyards; //Only recorded because they take up slots that cannot be used by factories.
    //current info
    private int buildingSlots;
    private int milFactories;
    private int civFactories;

    private boolean civUnderConstruction;
    private double currentCivProduction;
    private boolean milUnderConstruction;
    private double currentMilProduction;

    public State(int newInfrastructure, int newBuildingSlots, int newMilFactories, int newDockyards, int newCivFactories) {
        baseInfrastructure = newInfrastructure;
        baseBuildingSlots = newBuildingSlots;
        buildingSlots = newBuildingSlots;
        milFactories = newMilFactories;
        baseDockyards = newDockyards;
        civFactories = newCivFactories;
    }
    public void changeBuildingSlotsBonus(double newBonus) {
        //increase the concentrated/dispersed industry tech for building slots
        buildingSlots = (int) (baseBuildingSlots * (1 + newBonus));
    }
    public int getMilFactories() {
        return milFactories;
    }
    public int getCivFactories() {
        return civFactories;
    }
    public int getFreeBuildingSlots() {
        return buildingSlots - civFactories - milFactories - baseDockyards;
    }
    public int getInfrastructureLevel() {
        return baseInfrastructure;
    }
    public boolean isCivUnderConstruction() {
        return civUnderConstruction;
    }
    public boolean isMilUnderConstruction() {
        return milUnderConstruction;
    }
    public void addCivConstruction(double productionToAdd) {
        //add the given amount of production to factory construction progress and add a factory if required
        productionToAdd *= (1 + baseInfrastructure * 0.1);
        if (civUnderConstruction) {
            currentCivProduction += productionToAdd;
        } else {
            civUnderConstruction = true;
            currentCivProduction = productionToAdd;
        }
        if (currentCivProduction >= CIVILIAN_FACTORY_COST) {
            currentCivProduction = 0;
            civFactories +=1;
            civUnderConstruction = false;
        }
    }
    public void addMilConstruction(double productionToAdd) {
        //add the given amount of production to factory construction progress and add a factory if required
        productionToAdd *= (1 + baseInfrastructure * 0.1);
        if (milUnderConstruction) {
            currentMilProduction += productionToAdd;
        } else {
            milUnderConstruction = true;
            currentMilProduction = productionToAdd;
        }
        if (currentMilProduction >= MILITARY_FACTORY_COST) {
            currentMilProduction = 0;
            milFactories +=1;
            milUnderConstruction = false;
        }
    }
}
