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

public class MilFactory {
    //constants
    private final static double PRODUCTION_PER_MIL_FACTORY = 4.5;
    private final static double MINIMUM_PRODUCTION_EFFICIENCY = 0.1;
    private final static double BASE_PRODUCTION_EFFICIENCY_CAP = 0.5;
    private final static double BASE_PRODUCTION_EFFICIENCY_GAIN_MULTIPLIER = 0.001;

    //working variables
    private double productionEfficiencyCap = BASE_PRODUCTION_EFFICIENCY_CAP;
    private double productionEfficiency;
    private double efficiencyGainBonus = 1;

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
            productionEfficiency += productionEfficiencyGain * BASE_PRODUCTION_EFFICIENCY_GAIN_MULTIPLIER * efficiencyGainBonus;
        }
        return PRODUCTION_PER_MIL_FACTORY * productionEfficiency;
    }
    public void setProductionEfficiencyCapBonus(double newBonus) {
        productionEfficiencyCap = BASE_PRODUCTION_EFFICIENCY_CAP + newBonus;
        productionEfficiency = Math.min(productionEfficiency, productionEfficiencyCap);
    }
    public void setProductionEfficiencyGainBonus(double newBonus) {
        efficiencyGainBonus = 1 + newBonus;
    }
}
