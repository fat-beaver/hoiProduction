public class MilFactory {
    //constants
    private final static double PRODUCTION_PER_MIL_FACTORY = 4.5;
    private final static double MINIMUM_PRODUCTION_EFFICIENCY = 0.1;
    private final static double DEFAULT_PRODUCTION_EFFICIENCY_CAP = 0.5;
    private final static double PRODUCTION_EFFICIENCY_GAIN_MULTIPLIER = 0.001;

    //working variables
    private double productionEfficiencyCap = DEFAULT_PRODUCTION_EFFICIENCY_CAP;
    private double productionEfficiency;

    public MilFactory(boolean gameStart) {
        if (gameStart) {
            productionEfficiency = productionEfficiencyCap;
        } else {
            productionEfficiency = MINIMUM_PRODUCTION_EFFICIENCY;
        }
    }
    public double doProduction() {
        //calculate a new production efficiency if needed
        if (productionEfficiency != productionEfficiencyCap) {
            double productionEfficiencyGain = Math.pow(productionEfficiencyCap, 2) / productionEfficiency;
            productionEfficiency += productionEfficiencyGain * PRODUCTION_EFFICIENCY_GAIN_MULTIPLIER;
        }
        return PRODUCTION_PER_MIL_FACTORY * productionEfficiency;
    }
    public void setProductionEfficiencyCap(double newValue) {
        productionEfficiencyCap = DEFAULT_PRODUCTION_EFFICIENCY_CAP + newValue;
        productionEfficiency = Math.min(productionEfficiency, productionEfficiencyCap);
    }
}
