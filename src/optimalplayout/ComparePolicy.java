/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package optimalplayout;

import ai.core.AI;
import ai.stochastic.UnitActionProbabilityDistributionAI;
import ai.stochastic.UnitActionTypeConstantDistribution;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;
import tests.Experimenter;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author santi
 */
public class ComparePolicy {

    public static void main(String args[]) throws Exception {
        boolean CONTINUING = false;
        int TIME = -1;
        int MAX_ACTIONS = 100;
        int MAX_PLAYOUTS = 100;
        int PLAYOUT_TIME = 100;
        int MAX_DEPTH = 10;
        int RANDOMIZED_AB_REPEATS = 10;

        List<AI> bots = new LinkedList<>();
        UnitTypeTable utt = new UnitTypeTable();
        List<String> maps = new ArrayList<>();
        List<Integer> maxGameLength = new ArrayList<>();
        maps.add("maps/8x8/basesWorkers8x8A.xml");
//        maxGameLength.add(3000);
        maps.add("maps/8x8/FourBasesWorkers8x8.xml");
//        maxGameLength.add(3000);
        maps.add("maps/NoWhereToRun9x8.xml");
//        maxGameLength.add(3000);
//        maps.add("maps/16x16/basesWorkers16x16A.xml");
//        maxGameLength.add(4000);
//        maps.add("maps/16x16/TwoBasesBarracks16x16.xml");
//        maxGameLength.add(4000);


        int m = 0;

        double rand[] = new double[]{1, 1, 1, 1, 1, 1};
        double randbias[] = new double[]{1, 1, 5, 5, 1, 5};
        double[][] gameplay = new double[][]{
                new double[]{0, 0, 4, 2, 4, 3},
                new double[]{0, 0, 0, 1, 0, 3},
                new double[]{0, 0, 1, 4, 4, 5},
                new double[]{0, 0, 1, 2, 0, 5},
        };

        double[][] playout = new double[][]{
                new double[]{1, 0, 0, 1, 2, 5},
                new double[]{4, 0, 0, 1, 1, 0},
                new double[]{1, 0, 1, 3, 0, 5},
        };


//        PrintStream out = new PrintStream(new File("policy-comparison/gameplay-map-" + m + ".txt"));
        PrintStream out = new PrintStream(new File("policy-comparison/playout-map-" + m + ".txt"));

        // Separate the matchs by map:
        ArrayList<PhysicalGameState> map = new ArrayList<>();
        map.add(PhysicalGameState.load(maps.get(m), utt));


        AI ai1 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, rand), utt, "uapdai1");
        AI ai2 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, randbias), utt, "uapdai1");
        AI ai3 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, gameplay[m]), utt, "uapdai1");
        AI ai4 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, playout[m]), utt, "uapdai1");
        bots.add(ai1);
        bots.add(ai2);
        bots.add(ai3);
        bots.add(ai4);
//        bots.add(new NaiveMCTS(-1, 100, 100, 10, .4f, 0f, .3f,ai1, new SimpleSqrtEvaluationFunction3(), true));
//        bots.add(new NaiveMCTS(-1, 100, 100, 10, .4f, 0f, .3f,ai2, new SimpleSqrtEvaluationFunction3(), true));
//        bots.add(new NaiveMCTS(-1, 100, 100, 10, .4f, 0f, .3f,ai3, new SimpleSqrtEvaluationFunction3(), true));
//        bots.add(new NaiveMCTS(-1, 100, 100, 10, .4f, 0f, .3f,ai4, new SimpleSqrtEvaluationFunction3(), true));


        Experimenter.runExperiments(bots, map, utt, 50, 3000, 300, false, out, -1, true, false);

    }
}
