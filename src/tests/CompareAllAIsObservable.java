/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import MentalSeal.*;
import ai.abstraction.partialobservability.POLightRush;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ContinuingAI;
import ai.core.PseudoContinuingAI;

import java.io.File;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import rts.PhysicalGameState;
import rts.units.UnitTypeTable;
import ai.core.InterruptibleAI;

/**
 * @author santi
 */
public class CompareAllAIsObservable {

    public static void main(String args[]) throws Exception {
        boolean CONTINUING = false;
        int TIME = 100;
        int MAX_ACTIONS = 100;
        int MAX_PLAYOUTS = -1;
        int PLAYOUT_TIME = 100;
        int MAX_DEPTH = 10;
        int RANDOMIZED_AB_REPEATS = 10;

        List<AI> bots = new LinkedList<>();
        UnitTypeTable utt = new UnitTypeTable();

//        bots.add(new RandomAI(utt));
//        bots.add(new RandomBiasedAI());
//        bots.add(new LightRush(utt, new BFSPathFinding()));
//        bots.add(new RangedRush(utt, new BFSPathFinding()));
//        bots.add(new PortfolioAI(new AI[]{new WorkerRush(utt, new BFSPathFinding()),
//                                          new LightRush(utt, new BFSPathFinding()),
//                                          new RangedRush(utt, new BFSPathFinding()),
//                                          new RandomBiasedAI()},
//                                 new boolean[]{true,true,true,false},
//                                 TIME, MAX_PLAYOUTS, PLAYOUT_TIME*4, new SimpleSqrtEvaluationFunction3()));
//
//        bots.add(new IDRTMinimax(TIME, new SimpleSqrtEvaluationFunction3()));
//        bots.add(new IDRTMinimaxRandomized(TIME, RANDOMIZED_AB_REPEATS, new SimpleSqrtEvaluationFunction3()));
//        bots.add(new IDABCD(TIME, MAX_PLAYOUTS, new LightRush(utt, new GreedyPathFinding()), PLAYOUT_TIME, new SimpleSqrtEvaluationFunction3(), false));
//
//        bots.add(new MonteCarlo(TIME, PLAYOUT_TIME, MAX_PLAYOUTS, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3()));
//        bots.add(new MonteCarlo(TIME, PLAYOUT_TIME, MAX_PLAYOUTS, MAX_ACTIONS, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3()));
//        // by setting "MAX_DEPTH = 1" in the next two bots, this effectively makes them Monte Carlo search, instead of Monte Carlo Tree Search
//        bots.add(new NaiveMCTS(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, 1, 0.33f, 0.0f, 0.75f, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), true));
//        bots.add(new NaiveMCTS(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, 1, 1.00f, 0.0f, 0.25f, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), true));
//
//        bots.add(new UCT(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_DEPTH, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3()));
//        bots.add(new DownsamplingUCT(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_ACTIONS, MAX_DEPTH, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3()));
//        bots.add(new UCTUnitActions(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_DEPTH*10, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3()));
//        bots.add(new NaiveMCTS(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_DEPTH, 0.33f, 0.0f, 0.75f, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), true));
//        bots.add(new NaiveMCTS(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_DEPTH, 1.00f, 0.0f, 0.25f, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), true));
//        bots.add(new NStepGuidedNaiveMCTS(TIME, MAX_PLAYOUTS, 100, 100, 0.3f, 0.0f, 0.4f,
//                new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), new WorkerRush(utt), true));
//        bots.add(new LightRush(utt));
//        bots.add(new GuidedNaiveMCTS(TIME, MAX_PLAYOUTS, 100, 100, 0.3f, 0.0f, 0.4f,
//                new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), new WorkerRush(utt), true));
//        bots.add(new OptimizablePORush(utt,new AStarPathFinding(),new int[]{6, 0, 0, 1, 1, 0, 1, 0}));
//        bots.add(new OptimizableRush(utt,new AStarPathFinding(),new int[]{0, 0, 1, 0, 1, 0, 1, 0}));

//        AI script = new OptimizableRush(utt, new AStarPathFinding(), new int[]{5, 5, 3, 0, 2, 1, 4});
//        bots.add(script);
        bots.add(new MentalSealPO(utt));
        bots.add(new POLightRush(utt));

//        bots.add(new MentalSeal(utt));

//        bots.add(new POWorkerRush(utt));
//        bots.add(new WorkerRush(utt));
//        bots.add(new WorkerRush(utt));
//        bots.add(new GuidedGreedyNaiveMCTS(100, -1, 100, 50,
//                .33f, 0.0f, .8f, .8f, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(),new AI[]{script}, true));
//        bots.add(new MixedBot(utt));
//        bots.add(new MentalSeal(utt));

//        bots.add(new Droplet(utt));

        if (CONTINUING) {
            // Find out which of the bots can be used in "continuing" mode:
            List<AI> bots2 = new LinkedList<>();
            for (AI bot : bots) {
                if (bot instanceof AIWithComputationBudget) {
                    if (bot instanceof InterruptibleAI) {
                        bots2.add(new ContinuingAI(bot));
                    } else {
                        bots2.add(new PseudoContinuingAI((AIWithComputationBudget) bot));
                    }
                } else {
                    bots2.add(bot);
                }
            }
            bots = bots2;
        }

        PrintStream out = new PrintStream(new File("results.txt"));

        // Separate the matchs by map:
        List<PhysicalGameState> maps = new LinkedList<PhysicalGameState>();

        maps.clear();
//        maps.add(PhysicalGameState.load("maps/8x8/basesWorkers8x8A.xml",utt));
//        maps.add(PhysicalGameState.load("maps/8x8/FourBasesWorkers8x8.xml",utt));
//        maps.add(PhysicalGameState.load("maps/NoWhereToRun9x8.xml", utt));
//        maps.add(PhysicalGameState.load("maps/16x16/TwoBasesBarracks16x16.xml",utt));
//        maps.add(PhysicalGameState.load("maps/BroodWar/(4)BloodBath.scmB.xml", utt));
//        maps.add(PhysicalGameState.load("maps/BWDistantResources32x32.xml",utt));
//        maps.add(PhysicalGameState.load("maps/16x16/basesWorkers16x16A.xml", utt));
//        maps.add(PhysicalGameState.load("maps/DoubleGame24x24.xml", utt));
        maps.add(PhysicalGameState.load("maps/melee4x4light2.xml", utt));

//        maps.add(PhysicalGameState.load("maps/chambers32x32.xml", utt));
//        Experimenter.runExperiments(bots, maps, utt, 10, 8000, 300, true, System.out);
        Experimenter.runExperimentsPartiallyObservable(bots, maps, utt, 10, 5000, 300, true, System.out);
//
//        maps.clear();
//        Experimenter.runExperiments(bots, maps, utt, 10, 3000, 300, true, out);
//
//        maps.clear();
//        maps.add(PhysicalGameState.load("maps/8x8/melee8x8mixed6.xml",utt));
//        Experimenter.runExperiments(bots, maps, utt, 10, 3000, 300, true, out);
//
//        maps.clear();
//        maps.add(PhysicalGameState.load("maps/melee4x4light2.xml",utt));
//        Experimenter.runExperiments(bots, maps, utt, 10, 3000, 300, true, out);
    }
}
