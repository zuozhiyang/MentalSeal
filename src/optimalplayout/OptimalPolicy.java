/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package optimalplayout;

import ai.RandomBiasedAI;
import ai.core.AI;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.mcts.naivemcts.NaiveMCTS;
import ai.stochastic.UnitActionProbabilityDistributionAI;
import ai.stochastic.UnitActionTypeConstantDistribution;
import gui.PhysicalGameStatePanel;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.UnitAction;
import rts.units.UnitTypeTable;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * @author santi
 */
public class OptimalPolicy {
    public static void main(String args[]) throws Exception {
        // Results after running 500 iterations:
        // - Best weights so far: {2.0, 1.0, 4.0, 3.0, 8.0, 8.0}
        // Which can be instantiated into an AI, like this:
        // - new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt,new double[]{2.0, 1.0, 4.0, 3.0, 8.0, 8.0}), utt, "playoutai")

        String map = args[0];
        String algo = args[1];
        int OPTIMIZATION_ITERATIONS = 2000;
        int iteration_budget = 100;
        boolean visualize = false;
        float epsilon_l = 0.3f;
        float epsilon_g = 0.0f;
        float epsilon_0 = .4f;
        float ai_epsilon_l = 0.3f;
        float ai_epsilon_g = 0.0f;
        float ai_epsilon_0 = 0.4f;
        Random r = new Random();
        UnitTypeTable utt = new UnitTypeTable();

        // maps:
        List<String> maps = new ArrayList<>();
        List<Integer> maxGameLength = new ArrayList<>();

        switch (map) {
            case ("8x8-1"):
                maps.add("maps/8x8/basesWorkers8x8A.xml");
                maxGameLength.add(3000);
                break;
            case ("8x8-4"):
                maps.add("maps/8x8/FourBasesWorkers8x8.xml");
                maxGameLength.add(3000);
                break;
            case ("9x8"):
                maps.add("maps/NoWhereToRun9x8.xml");
                maxGameLength.add(3000);
                break;
        }
        EvaluationFunction e = new SimpleSqrtEvaluationFunction3();
//        EvaluationFunction e = new SimpleEvaluationFunction();
//        StandAloneCMAB cmab = new StandAloneCMAB(true, new int[]{8, 8, 8, 8, 8, 8}); // number of possible values for each variable in the CMAB
        double kernel[] = {0.1577, 0.6845, 0.1577};
        if (args[2].equals("0")) {
            kernel = new double[]{0, 1, 0};
        }
        int arm_config[] = new int[]{8, 8, 8, 8, 8, 8}; // number of possible values for each variable in the CMAB
        for (int iter = 0; iter < 3; iter++) {
            BufferedWriter writer = new BufferedWriter(new FileWriter("bandit-results-2000/"+algo + "-" + map + "-" + args[2] +"-" + iter + ".txt"));
            PrintStream o = new PrintStream(new File("bandit-results-2000/console-"+ algo + "-" + map +"-" + args[2] + "-" + iter + ".txt"));
            System.setOut(o);
            StandAloneCMAB cmab = new StandAloneCMAB(true, arm_config, kernel);
            int CORES = 5;

            for (Integer iteration = 0; iteration < OPTIMIZATION_ITERATIONS; ) {
//            ArrayList<StandAloneCMAB.MacroArm> macroArms = new ArrayList<>();
//            for (int i = 0; i < CORES; i++) {
//                macroArms.add(cmab.pullArm(epsilon_l, epsilon_g, epsilon_0));
                StandAloneCMAB.MacroArm marm1 = null;

              if (algo.equals("EG"))
                    marm1 = cmab.pullArm(epsilon_l, epsilon_g, epsilon_0);
//            }
                ArrayList<Double> rewards = new ArrayList<>();
                ArrayList<Thread> threads = new ArrayList<>();
                double pp1_d[] = new double[6];
                for (int i = 0; i < UnitAction.NUMBER_OF_ACTION_TYPES; i++) {
                    pp1_d[i] = (double) (marm1.values[i] + 1.0);
                }
                final UnitActionProbabilityDistributionAI pp1 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp1_d), utt, "uapdai1");
                for (int j = 0; j < CORES; j++) {
                    Runnable runnable =
                            () -> {
                                try {
                                    AI ai1 = new NaiveMCTS(-1, iteration_budget, 100, 10, ai_epsilon_l, ai_epsilon_g, ai_epsilon_0, pp1, new SimpleSqrtEvaluationFunction3(), true);
                                    AI ai2 = new NaiveMCTS(-1, iteration_budget, 100, 10, ai_epsilon_l, ai_epsilon_g, ai_epsilon_0, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), true);
                                    int mapIdx = r.nextInt(maps.size());
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
                }
                for (Thread thread : threads) {
                    thread.join();
                }
                double reward = 0;
                for (Double rw : rewards) {
                    reward+=rw;
                }
                reward/=rewards.size();
                cmab.receiveReward(reward, marm1);
                System.out.println("Iteration " + iteration++ + " ------------------ ");
                System.out.println("Reward: " + reward);
                System.out.println("Marm: " + Arrays.toString(marm1.values));
                cmab.PrintMacroArmDebugInfo(true, true, 5);
                if (iteration % 10 == 0) {
//                    threads.clear();
//                    ArrayList<Double> evals = new ArrayList<>();
                    StandAloneCMAB.MacroArm macroArm = cmab.getMostVisited();
//                    double dist[] = new double[6];
//                    for (int i = 0; i < UnitAction.NUMBER_OF_ACTION_TYPES; i++) {
//                        dist[i] = (double) (macroArm.values[i] + 1.0);
//                    }
//                    UnitActionProbabilityDistributionAI pp2 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, dist), utt, "uapdai1");
//                    int k = 0;
//                    int NUM_EVALS = 200;
//                    while (k < NUM_EVALS) {
//                        for (int j = 0; j < CORES; j++) {
//                            Runnable runnable =
//                                    () -> {
//                                        try {
//                                            AI ai1 = new NaiveMCTS(-1, iteration_budget, 100, 10, ai_epsilon_l, ai_epsilon_g, ai_epsilon_0, pp2, new SimpleSqrtEvaluationFunction3(), true);
//                                            AI ai2 = new NaiveMCTS(-1, iteration_budget, 100, 10, ai_epsilon_l, ai_epsilon_g, ai_epsilon_0, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), true);
//                                            int mapIdx = r.nextInt(maps.size());
//                                            double rw = 0.5;
//                                            if (r.nextBoolean()) {
//                                                GameState endState = runGame(ai1, ai2, maps.get(mapIdx), maxGameLength.get(mapIdx), visualize, utt);
//                                                if (endState.winner() == 0) rw = 1.0;
//                                                if (endState.winner() == 1) rw = 0.0;
//                                            } else {
//                                                GameState endState = runGame(ai2, ai1, maps.get(mapIdx), maxGameLength.get(mapIdx), visualize, utt);
//                                                if (endState.winner() == 0) rw = 0.0;
//                                                if (endState.winner() == 1) rw = 1.0;
//                                            }
//                                            evals.add(rw);
//                                        } catch (Exception ex) {
//                                            ex.printStackTrace();
//                                        }
//                                    };
//                            Thread thread = new Thread(runnable);
//                            threads.add(thread);
//                        }
//                        for (Thread thread : threads) {
//                            thread.start();
//                            k++;
//                        }
//
//                        for (Thread thread : threads) {
//                            thread.join();
//                        }
//                        threads.clear();
//
//                    }
//                    double avg = 0;
//                    for (Double eval : evals) {
//                        avg += eval;
//                    }
//                    writer.write(iteration + "," + avg / NUM_EVALS + "\n");
                    writer.write(iteration + "," + Arrays.toString(macroArm.values)+ "\n");
                    writer.flush();
                }

            }
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
            Thread.yield(); // Play nice with the other processes, and do not use 100% of the CPU :)
        } while (!gameover && gs.getTime() < maxcycles);
        ai1.gameOver(gs.winner());
        ai2.gameOver(gs.winner());
        return gs;
    }
}
