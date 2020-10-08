/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package optimalplayout;

import ai.core.AI;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.mcts.informedmcts.InformedNaiveMCTS;
import ai.stochastic.UnitActionProbabilityDistributionAI;
import ai.stochastic.UnitActionTypeConstantDistribution;
import gui.PhysicalGameStatePanel;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.UnitAction;
import rts.units.UnitTypeTable;

import javax.swing.*;
import java.io.File;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author santi
 */
public class OptimalRepeatedZSGamePolicyFullMCTS {
    static float[] EPS = new float[]{0, .2f, .4f, .6f, .8f, 1f};
    static int[] LH = new int[]{0, 50, 100, 150, 200, 250};

    public static void main(String args[]) throws Exception {
        // Results after running 500 iterations:
        // - Best weights so far: {2.0, 1.0, 4.0, 3.0, 8.0, 8.0}
        // Which can be instantiated into an AI, like this:
        // - new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt,new double[]{2.0, 1.0, 4.0, 3.0, 8.0, 8.0}), utt, "playoutai")
        int CORES = 20;
        String dir = "Repeat-Normalized-FullMCTS/";
//        String map = args[0];
//        String algo = args[1];
        int NUM_EVAL = 1000;
        int OPTIMIZATION_ITERATIONS = 50000;
        boolean visualize = false;
        float epsilon_l = .33f;
        float epsilon_g = .0f;
        float epsilon_0 = .5f;
        Random r = new Random();
        UnitTypeTable utt = new UnitTypeTable();

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
        DecimalFormat df = new DecimalFormat("0.0");
//        int m = Integer.parseInt(args[0]);
        int m = 0;
        double kernel[] = {0, 1, 0};
        int arm_config[] = new int[]{
                6, 6, 6, 6, 6, 6,
                6, 6, 6, 6, 6, 6,
                6, 6, 6,
                6};
//        BufferedWriter writer0 = new BufferedWriter(new FileWriter(dir + "history-" + maps.get(m).replaceAll("/", "-") + ".txt"));

        PrintStream o = new PrintStream(new File(dir + maps.get(m).replaceAll("/", "-") + ".txt"));
        System.setOut(o);
//        for (String algo : new String[]{"EG-g"}) {
        StandAloneCMAB cmab1 = new StandAloneCMAB(true, arm_config, kernel);
        StandAloneCMAB cmab2 = new StandAloneCMAB(true, arm_config, kernel);
//        float actual_epsilon_0 = epsilon_0;
        for (Integer iteration = 0; iteration < OPTIMIZATION_ITERATIONS; ) {
//            actual_epsilon_0 *= 0.99975;
            float f = (OPTIMIZATION_ITERATIONS - iteration) / (float) OPTIMIZATION_ITERATIONS;
            float actual_epsilon_0 = epsilon_0 * f;
//                float f = (float) (Math.log(iteration) / iteration);
//                float actual_epsilon_0 = f * 200;
            StandAloneCMAB.MacroArm marm1 = null;
            StandAloneCMAB.MacroArm marm2 = null;
            marm1 = cmab1.pullArm(epsilon_l, epsilon_g, actual_epsilon_0);
            marm2 = cmab2.pullArm(epsilon_l, epsilon_g, actual_epsilon_0);
            ArrayList<Double> rewards = new ArrayList<>();
            ArrayList<Thread> threads = new ArrayList<>();
            double pp1_d[] = new double[6];
            double pp2_d[] = new double[6];
            double bs1_d[] = new double[6];
            double bs2_d[] = new double[6];
            for (int i = 0; i < 6; i++) {
                pp1_d[i] = (double) (marm1.values[i]);
                pp2_d[i] = (double) (marm2.values[i]);
                bs1_d[i] = (double) (marm1.values[i + 6]);
                bs2_d[i] = (double) (marm2.values[i + 6]);
            }
            float e11 = EPS[marm1.values[12]];
            float e21 = EPS[marm1.values[13]];
            float e31 = EPS[marm1.values[14]];
            int lh1 = LH[marm1.values[15]];

            float e12 = EPS[marm2.values[12]];
            float e22 = EPS[marm2.values[13]];
            float e32 = EPS[marm2.values[14]];
            int lh2 = LH[marm2.values[15]];
//                UnitActionProbabilityDistributionAI pp1 = new UnitActionProbabilityDistributionAI(new UnitActionDetailedTypeConstantDistribution2(utt, pp1_d), utt, "uapdai1");
//                UnitActionProbabilityDistributionAI pp2 = new UnitActionProbabilityDistributionAI(new UnitActionDetiledTypeConstantDistribution2(utt, pp1_d), utt, "uapdai1");
            UnitActionProbabilityDistributionAI pp1 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp1_d), utt, "uapdai1");
            UnitActionProbabilityDistributionAI pp2 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp2_d), utt, "uapdai1");

            UnitActionTypeConstantDistribution bias1 = new UnitActionTypeConstantDistribution(utt, bs1_d);
            UnitActionTypeConstantDistribution bias2 = new UnitActionTypeConstantDistribution(utt, bs2_d);
            int mapIdx = m;
            for (int j = 0; j < CORES; j++) {
                Runnable runnable =
                        () -> {
                            try {
                                AI ai1 = new InformedNaiveMCTS(-1, 100, lh1, 10, e11, e21, e31, pp1, bias1, new SimpleSqrtEvaluationFunction3(), utt);
                                AI ai2 = new InformedNaiveMCTS(-1, 100, lh2, 10, e12, e22, e32, pp2, bias2, new SimpleSqrtEvaluationFunction3(), utt);
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
            cmab1.receiveReward(reward, marm1);
            cmab2.receiveReward(1 - reward, marm2);
            iteration++;
            if (iteration % 10 == 0) {
                System.out.println(iteration + " " + actual_epsilon_0);
                System.out.println("CMAB 1:");
                cmab1.PrintMacroArmDebugInfo(true, true, 3);
                cmab1.PrintMacroArmDebugInfo(true, false, 3);
                System.out.println("CMAB 2:");
                cmab2.PrintMacroArmDebugInfo(true, true, 3);
                cmab2.PrintMacroArmDebugInfo(true, false, 3);
            }
        }
//        }
//        writer0.flush();
//        writer0.close();
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

    public static double parallelRun(int CORES, int total, double[] values1, ArrayList<double[]> values2, String map, Integer maxGameLength) throws Exception {
        UnitTypeTable utt = new UnitTypeTable();
        Random r = new Random();
        double pp1_d[] = new double[6];
        for (int i = 0; i < UnitAction.NUMBER_OF_ACTION_TYPES; i++) {
            pp1_d[i] = (double) (values1[i]);
        }

        UnitActionProbabilityDistributionAI pp1 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp1_d), utt, "uapdai1");
        ArrayList<Double> rewards = new ArrayList<>();
        for (int k = 0; k < total; ) {
            ArrayList<Thread> threads = new ArrayList<>();
            for (int j = 0; j < CORES; j++) {
                double[] sample;
                if (values2.isEmpty()) {
                    sample = new double[6];
                } else {
                    sample = values2.get(r.nextInt(values2.size()));
                }
                double pp2_d[] = new double[6];
                for (int i = 0; i < UnitAction.NUMBER_OF_ACTION_TYPES; i++) {
                    pp2_d[i] = (double) (sample[i]);
                }
                UnitActionProbabilityDistributionAI pp2 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp2_d), utt, "uapdai2");
                Runnable runnable =
                        () -> {
                            try {
                                AI ai1 = pp1;
                                AI ai2 = pp2;
                                double reward = 0.5;
                                if (r.nextBoolean()) {
                                    GameState endState = runGame(ai1, ai2, map, maxGameLength, false, utt);
                                    if (endState.winner() == 0) reward = 1.0;
                                    if (endState.winner() == 1) reward = 0.0;
                                } else {
                                    GameState endState = runGame(ai2, ai1, map, maxGameLength, false, utt);
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
                k++;
            }
            for (Thread thread : threads) {
                thread.join();
            }
        }
        double reward = 0;
        for (Double rw : rewards) {
            reward += rw;
        }
        reward /= rewards.size();
        return reward;
    }
}
