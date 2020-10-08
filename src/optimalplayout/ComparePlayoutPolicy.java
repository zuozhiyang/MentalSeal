/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package optimalplayout;

import ai.abstraction.WorkerRush;
import ai.core.AI;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.mcts.naivemcts.NaiveMCTS;
import ai.stochastic.UnitActionProbabilityDistributionAI;
import ai.stochastic.UnitActionTypeConstantDistribution;
import gui.PhysicalGameStatePanel;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author santi
 */
public class ComparePlayoutPolicy {

    public static void main(String args[]) throws Exception {
        boolean CONTINUING = false;
        int TIME = -1;
        int MAX_ACTIONS = 100;
        int MAX_PLAYOUTS = 100;
        int PLAYOUT_TIME = 100;
        int MAX_DEPTH = 10;
        int RANDOMIZED_AB_REPEATS = 10;
        float epsilon_l = 0.3f;
        float epsilon_g = 0.0f;
        float epsilon_0 = .5f;
        float ai_epsilon_l = 0.3f;
        float ai_epsilon_g = 0.0f;
        float ai_epsilon_0 = 0.4f;
        int iteration_budget = 100;
        boolean visualize = false;
        List<String> maps = new ArrayList<>();
        List<Integer> maxGameLength = new ArrayList<>();
        maps.add("maps/8x8/basesWorkers8x8A.xml");
        maxGameLength.add(3000);
        maps.add("maps/8x8/FourBasesWorkers8x8.xml");
        maxGameLength.add(3000);
        maps.add("maps/NoWhereToRun9x8.xml");
        maxGameLength.add(3000);
        maps.add("maps/16x16/basesWorkers16x16A.xml");
        maxGameLength.add(4000);
        maps.add("maps/16x16/TwoBasesBarracks16x16.xml");
        maxGameLength.add(4000);

        int m = 0;
        UnitTypeTable utt = new UnitTypeTable();
        double pp1_d[] = new double[]{0, 0, 1, 1, 0, 4};
//        double pp1_d[] = new double[]{0, 0, 0, 3, 0, 5};
//        double pp2_d[] = new double[]{1,1,1,1,1,5};
        ArrayList<double[]> population = new ArrayList<>();
        String mapName;
        if (maps.get(m).split("/").length>2)
            mapName = maps.get(m).split("/")[1]+"-"+maps.get(m).split("/")[2];
        else
            mapName = maps.get(m).split("/")[1];
//        UnitActionPopulationDistribution populationDistribution = new UnitActionPopulationDistribution(utt,"FP-Normalized/history-maps-" + mapName + ".txt");
//        AI pp2 = new RandomBiasedAI(utt);
//        AI pai1 = new UnitActionPopulationDistributionAI(populationDistribution,utt,"popAI");
        AI pai1 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp1_d), utt, "uapdai1");
        AI pai2 = new WorkerRush(utt);
//        UnitActionProbabilityDistributionAI pp2 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp2_d), utt, "uapdai1");
//        AI ai1 = new NaiveMCTS(-1, MAX_PLAYOUTS, 100, 10, 0.3f, 0, 0.4f, pp1, new SimpleSqrtEvaluationFunction3(), true);
//        AI ai2 = new NaiveMCTS(-1, MAX_PLAYOUTS, 100, 10, 0.3f, 0, 0.4f, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), true);
//        bots.add(ai1);
//        bots.add(ai2);
        ArrayList<Double> rewards = new ArrayList<>();
        int ITERATIONS = 20000;
        Random r = new Random();
        int CORES = 10;
        for (int iteration = 0; iteration < ITERATIONS; ) {
            ArrayList<Thread> threads = new ArrayList<>();
            for (int j = 0; j < CORES; j++) {
                Runnable runnable =
                        () -> {
                            try {
//                                AI ai1 = new WorkerRush(utt);

                                AI ai1 = new NaiveMCTS(-1, iteration_budget, 100, 10, ai_epsilon_l, ai_epsilon_g, ai_epsilon_0, pai1, new SimpleSqrtEvaluationFunction3(), true);
                                AI ai2 = new NaiveMCTS(-1, iteration_budget, 100, 10, ai_epsilon_l, ai_epsilon_g, ai_epsilon_0, pai2, new SimpleSqrtEvaluationFunction3(), true);
//                                AI ai2 = new NaiveMCTS(-1, iteration_budget, 100, 10, ai_epsilon_l, ai_epsilon_g, ai_epsilon_0, new WorkerRush(utt), new SimpleSqrtEvaluationFunction3(), true);
//                                double []  ppd = population.get(r.nextInt(population.size()));
//                                AI ai1 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp1_d), utt, "uapdai1");

                                int mapIdx = m;
                                double reward = 0.5;
                                if (r.nextBoolean()) {
                                    GameState endState = runGame(ai1, ai2, maps.get(mapIdx), maxGameLength.get(mapIdx), visualize, utt);
                                    if (endState.winner() == 0) reward = 1.0;
                                    if (endState.winner() == 1) reward = 0.0;
                                } else {
                                    GameState endState = runGame(ai2, ai1, maps.get(mapIdx), maxGameLength.get(mapIdx), visualize, utt);
                                    if (endState.winner() == 0) reward = 0.0;
                                    if (endState.winner() == 1) reward = 1.0;
                                }
                                rewards.add(reward);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        };
                Thread thread = new Thread(runnable);
                threads.add(thread);
            }
            for (Thread thread : threads) {
                thread.start();
                iteration++;
            }
            for (Thread thread : threads) {
                thread.join();
            }
            double reward = 0;
            for (Double rw : rewards) {
                reward += rw;
            }
            reward /= rewards.size();
            System.out.println("iteration: " + iteration + " - " + reward);
        }

    }

    public static GameState runGame(AI ai1, AI ai2, String mapFile, int maxcycles, boolean visualize, UnitTypeTable
            utt) throws Exception {
        PhysicalGameState pgs = PhysicalGameState.load(mapFile, utt);
        GameState gs = new GameState(pgs, utt);

        JFrame w = null;
        if (visualize)
            w = PhysicalGameStatePanel.newVisualizer(gs, 640, 640, false, PhysicalGameStatePanel.COLORSCHEME_BLACK);

        boolean gameover;
        do {
            PlayerAction pa1 = ai1.getAction(0, gs);
            PlayerAction pa2 = ai2.getAction(1, gs);
            gs.issueSafe(pa1);
            gs.issueSafe(pa2);
            gameover = gs.cycle();
            if (visualize) w.repaint();
//            Thread.sleep(20);
            Thread.yield(); // Play nice with the other processes, and do not use 100% of the CPU :)
        } while (!gameover && gs.getTime() < maxcycles);
        ai1.gameOver(gs.winner());
        ai2.gameOver(gs.winner());
        return gs;
    }
}
