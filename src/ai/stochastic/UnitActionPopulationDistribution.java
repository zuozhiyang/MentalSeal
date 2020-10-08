/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.stochastic;

import rts.GameState;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * @author santi
 */
public class UnitActionPopulationDistribution extends UnitActionProbabilityDistribution {

    double m_distribution[];
    ArrayList<double[]> population = new ArrayList<>();
    Random r = new Random();

    public UnitActionPopulationDistribution(UnitTypeTable a_utt, String filename) throws Exception {
        super(a_utt);
        File myObj = new File(filename);
        Scanner myReader = new Scanner(myObj);
        while (myReader.hasNextLine()) {
            String[] data = myReader.nextLine().replaceAll("\\[|\\]| ", "").split(",");
            double[] entry = new double[data.length];
            for (int i = 0; i < entry.length; i++) {
                entry[i] = Double.parseDouble(data[i]);
            }
            population.add(entry);
        }
        myReader.close();
        m_distribution = population.get(r.nextInt(population.size()));
    }

    public void randomize() {
        m_distribution = population.get(r.nextInt(population.size()));
    }


    public double[] predictDistribution(Unit u, GameState gs, List<UnitAction> actions) throws Exception {
        int nActions = actions.size();
        double d[] = new double[nActions];
        double accum = 0;
        for (int i = 0; i < nActions; i++) {
            int type = actions.get(i).getType();
            d[i] = m_distribution[type];
            accum += d[i];
        }

        if (accum <= 0) {
            // if 0 accum, then just make uniform distribution:
            for (int i = 0; i < nActions; i++) d[i] = 1.0 / nActions;
        } else {
            for (int i = 0; i < nActions; i++) d[i] /= accum;
        }

        return d;
    }

}
