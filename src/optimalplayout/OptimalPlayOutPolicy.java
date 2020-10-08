/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
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
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * @author santi
 */
public class OptimalPlayOutPolicy {
    public static void main(String args[]) throws Exception {
        // Results after running 500 iterations:
        // - Best weights so far: {2.0, 1.0, 4.0, 3.0, 8.0, 8.0}
        // Which can be instantiated into an AI, like this:
        // - new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt,new double[]{2.0, 1.0, 4.0, 3.0, 8.0, 8.0}), utt, "playoutai")

        String map = args[0];
        int OPTIMIZATION_ITERATIONS = 10000;
        int iteration_budget = 100;
        boolean visualize = false;
        float epsilon_l = 0.3f;
        float epsilon_g = 0.0f;
        float epsilon_0 = .5f;

        Random r = new Random();
        UnitTypeTable utt = new UnitTypeTable();
        int CORES = 10;

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

        int arm_config[] = new int[]{6, 6, 6, 6, 6, 6}; // number of possible values for each variable in the CMAB
//        PrintStream o = new PrintStream(new File("opponent-opt/" + map + ".txt"));
//        System.setOut(o);
        NormalizedStandAloneCMAB cmab = new NormalizedStandAloneCMAB(true, arm_config, new double[]{0, 1, 0});

        for (Integer iteration = 0; iteration < OPTIMIZATION_ITERATIONS; ) {
            float f = (OPTIMIZATION_ITERATIONS - iteration) / (float) OPTIMIZATION_ITERATIONS;
            float actual_epsilon_0 = epsilon_0 * f;
            NormalizedStandAloneCMAB.MacroArm marm1 = null;
            marm1 = cmab.pullArm(epsilon_l, epsilon_g, actual_epsilon_0);
            ArrayList<Double> rewards = new ArrayList<>();
            ArrayList<Thread> threads = new ArrayList<>();
            double pp1_d[] = new double[marm1.values.length];
            for (int i = 0; i < marm1.values.length; i++) {
                pp1_d[i] = (double) (marm1.values[i]);
            }
            UnitActionProbabilityDistributionAI pp1 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp1_d), utt, "uapdai1");
            for (int j = 0; j < CORES; j++) {
                Runnable runnable =
                        () -> {
                            try {
                                AI ai1 = new NaiveMCTS(-1, 100, 100, 10, .3f, 0, .4f, pp1, new SimpleSqrtEvaluationFunction3(), true);
                                AI ai2 = new WorkerRush(utt);
                                double reward = 0.5;
                                if (r.nextBoolean()) {
                                    GameState endState = runGame(ai1, ai2, maps.get(0), maxGameLength.get(0), visualize, utt);
                                    if (endState.winner() == 0) reward = 1.0;
                                    if (endState.winner() == 1) reward = 0.0;
                                } else {
                                    GameState endState = runGame(ai2, ai1, maps.get(0), maxGameLength.get(0), visualize, utt);
                                    if (endState.winner() == 0) reward = 0.0;
                                    if (endState.winner() == 1) reward = 1.0;
                                }
                                synchronized (rewards) {
                                    rewards.add(reward);
                                }
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
                reward += rw;
            }
            reward /= rewards.size();
            cmab.receiveReward(reward, marm1);
            iteration++;
//                    cmab.PrintMacroArmDebugInfo(true,true,5);
            if (iteration % 50 == 0) {
                System.out.println("Iteration " + iteration + " ------------------ ");
                System.out.println("Reward: " + reward);
                System.out.println("Marm: " + Arrays.toString(marm1.values));
                cmab.PrintMacroArmDebugInfo(true, true, 5);
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
