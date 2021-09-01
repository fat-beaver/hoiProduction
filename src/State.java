public class State {
    //CONSTANTS
    private static final int MILITARY_FACTORY_COST = 7200;
    private static final int CIVILIAN_FACTORY_COST = 10800;
    //basic starting info
    private final int baseInfrastructure;
    private final int baseBuildingSlots;
    private final int baseMilFactories;
    private final int baseCivFactories;
    private final int baseDockyards; //Only recorded because they take up slots that cannot be used by factories.
    //current info
    private int buildingSlots;
    private int milFactories;
    private int civFactories;
    private int civTechLevel = 0;

    private boolean civUnderConstruction;
    private double currentCivProduction;
    private boolean milUnderConstruction;
    private double currentMilProduction;

    public State(int newInfrastructure, int newBuildingSlots, int newMilFactories, int newDockyards, int newCivFactories) {
        baseInfrastructure = newInfrastructure;
        baseBuildingSlots = newBuildingSlots;
        buildingSlots = newBuildingSlots;
        baseMilFactories = newMilFactories;
        milFactories = newMilFactories;
        baseDockyards = newDockyards;
        baseCivFactories = newCivFactories;
        civFactories = newCivFactories;
    }
    public void upCivTechLevel() {
        civTechLevel++;
        buildingSlots = (int) (baseBuildingSlots * Math.pow(1.2, civTechLevel));
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
