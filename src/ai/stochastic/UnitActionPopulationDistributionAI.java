/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.stochastic;

import rts.units.UnitTypeTable;

/**
 *
 * @author santi
 * 
 */
public class UnitActionPopulationDistributionAI extends UnitActionProbabilityDistributionAI {


    public UnitActionPopulationDistributionAI(UnitTypeTable utt) throws Exception {
        super(utt);
    }

    public UnitActionPopulationDistributionAI(UnitActionProbabilityDistribution a_model, UnitTypeTable a_utt, String a_modelName) {
        super(a_model,a_utt,a_modelName);
    }
    @Override
    public void reset() {
        ((UnitActionPopulationDistribution) model).randomize();
    }

}
