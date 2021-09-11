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

public class State {
    //CONSTANTS
    private static final int MILITARY_FACTORY_COST = 7200;
    private static final int CIVILIAN_FACTORY_COST = 10800;
    //basic starting info
    private final int baseInfrastructure;
    private final int baseDockyards; //Only recorded because they take up slots that cannot be used by factories.
    private final IndustrialLevel industrialLevel;
    private final int bonusBuildingSlots;
    //current info
    private int buildingSlots;
    private final ArrayList<MilFactory> milFactories;
    private int civFactories;
    //construction info
    private boolean civUnderConstruction;
    private double currentCivConstruction;
    private boolean milUnderConstruction;
    private double currentMilConstruction;
    public enum IndustrialLevel {
        wasteland(0),
        enclave(0),
        tiny_island(0),
        small_island(1),
        pastoral(1),
        rural(2),
        town(4),
        large_town(5),
        city(6),
        large_city(8),
        metropolis(10),
        megalopolis(12);
        private final int buildingSlots;
        IndustrialLevel(int buildingSlots) {
            this.buildingSlots = buildingSlots;
        }
    }
    public State(int infrastructure, IndustrialLevel industrialLevel, int bonusBuildingSlots, int dockyards, int civFactories, int milFactories) {
        baseInfrastructure = infrastructure;
        this.industrialLevel = industrialLevel;
        buildingSlots = industrialLevel.buildingSlots + bonusBuildingSlots;
        this.bonusBuildingSlots = bonusBuildingSlots;
        baseDockyards = dockyards;
        this.civFactories = civFactories;
        this.milFactories = new ArrayList<>();
        for (int i = 0; i < milFactories; i++) {
            this.milFactories.add(new MilFactory(true));
        }
    }
    public void setBuildingSlotsBonus(double newBonus) {
        //increase the concentrated/dispersed industry tech for building slots
        buildingSlots = (int) (industrialLevel.buildingSlots * (1 + newBonus)) + bonusBuildingSlots;
    }
    public void setProductionEfficiencyCapBonus(double capBonus) {
        for (MilFactory milFactory : milFactories) {
            milFactory.setProductionEfficiencyCapBonus(capBonus);
        }
    }
    public void setProductionEfficiencyGainBonus(double gainBonus) {
        for (MilFactory milFactory : milFactories) {
            milFactory.setProductionEfficiencyGainBonus(gainBonus);
        }
    }
    public double getMilProduction() {
        double milProduction = 0;
        for (MilFactory milFactory : milFactories) {
            milProduction += milFactory.doProduction();
        }
        return milProduction;
    }
    public int getMilFactories() {
        return milFactories.size();
    }
    public int getCivFactories() {
        return civFactories;
    }
    public int getFreeBuildingSlots() {
        return buildingSlots - civFactories - milFactories.size() - baseDockyards;
    }
    public int getInfrastructureLevel() {
        return baseInfrastructure;
    }
    public boolean isCivUnderConstruction() {
        return civUnderConstruction;
    }
    public void addCivConstruction(double productionToAdd) {
        //add the given amount of production to factory construction progress and add a factory if required
        productionToAdd *= (1 + baseInfrastructure * 0.1);
        if (civUnderConstruction) {
            currentCivConstruction += productionToAdd;
        } else {
            civUnderConstruction = true;
            currentCivConstruction = productionToAdd;
        }
        if (currentCivConstruction >= CIVILIAN_FACTORY_COST) {
            currentCivConstruction = 0;
            civFactories +=1;
            civUnderConstruction = false;
        }
    }
    public void addMilConstruction(double productionToAdd) {
        //add the given amount of production to factory construction progress and add a factory if required
        productionToAdd *= (1 + baseInfrastructure * 0.1);
        if (milUnderConstruction) {
            currentMilConstruction += productionToAdd;
        } else {
            milUnderConstruction = true;
            currentMilConstruction = productionToAdd;
        }
        if (currentMilConstruction >= MILITARY_FACTORY_COST) {
            currentMilConstruction = 0;
            milFactories.add(new MilFactory(false));
            milUnderConstruction = false;
        }
    }
    public int getDockyards() {
        return baseDockyards;
    }
    public IndustrialLevel getIndustrialLevel() {
        return industrialLevel;
    }
    public int getBonusBuildingSlots() {
        return bonusBuildingSlots;
    }
}
