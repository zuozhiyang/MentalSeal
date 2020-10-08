/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package optimalplayout;

import MentalSeal.OptimizablePORush;
import MentalSeal.OptimizableRush;
import ai.abstraction.WorkerRush;
import ai.abstraction.partialobservability.POWorkerRush;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import gui.PhysicalGameStatePanel;
import rts.GameState;
import rts.PartiallyObservableGameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author santi
 */
public class OptimalGamePolicy {
    public static void main(String args[]) throws Exception {
        int OPTIMIZATION_ITERATIONS = 10000;
        boolean visualize = false;
        float epsilon_l = .3f;
        float epsilon_g = .0f;
        float epsilon_0 = .4f;
        Random r = new Random();

        List<String> maps = new ArrayList<>();
        List<Integer> maxGameLength = new ArrayList<>();

        maps.add("maps/8x8/basesWorkers8x8A.xml");
        maxGameLength.add(3000);
        maps.add("maps/16x16/basesWorkers16x16A.xml");
        maxGameLength.add(4000);
        maps.add("maps/BWDistantResources32x32.xml");
        maxGameLength.add(6000);
        maps.add("maps/BroodWar/(4)BloodBath.scmB.xml");
        maxGameLength.add(8000);
        maps.add("maps/8x8/FourBasesWorkers8x8.xml");
        maxGameLength.add(3000);
        maps.add("maps/16x16/TwoBasesBarracks16x16.xml");
        maxGameLength.add(4000);
        maps.add("maps/NoWhereToRun9x8.xml");
        maxGameLength.add(3000);
        maps.add("maps/DoubleGame24x24.xml");
        maxGameLength.add(5000);

        boolean po = true;

        int CORES = 10;
        int time_budget = 3600 * 3;
        int m = 3;
        int long_bandit_factor = 5;
        double kernel[] = {0, 1, 0};
        ArrayList<int[]> history = new ArrayList<>();
        history.add(new int[]{0, 0, 0, 0, 0, 0, 0, 0});
        int arm_config[] = new int[]{5, 5, 5, 5, 5, 5, 5, 5}; // number of possible values for each variable in the CMAB
        long start = System.currentTimeMillis();
        int FP_iter = 50;
//        String dir = "opt-script/";
//        PrintStream o = new PrintStream(new File(dir + maps.get(m).replaceAll("/", "-") + "-" + po + ".txt"));
//        System.setOut(o);
        for (int batch = 0; batch < FP_iter; batch++) {
            System.out.println("Batch: " + batch);
            StandAloneCMAB cmab = new StandAloneCMAB(true, arm_config, kernel);
            for (Integer iteration = 0; iteration < OPTIMIZATION_ITERATIONS; ) {
                StandAloneCMAB.MacroArm marm1 = null;
                marm1 = cmab.pullArm(epsilon_l, epsilon_g, epsilon_0);
                kernel = new double[]{0, 1, 0};
                int pp1_d[] = new int[marm1.values.length];
                for (int i = 0; i < marm1.values.length; i++) {
                    pp1_d[i] = (marm1.values[i]);
                }
                ArrayList<Double> rewards = new ArrayList<>();
                ArrayList<Thread> threads = new ArrayList<>();
                for (int j = 0; j < CORES; j++) {
                    int mapIdx = m;
                    int[] sample;
                    if (history.isEmpty()) {
                        sample = null;
                    } else {
                        sample = history.get(r.nextInt(history.size()));
                    }
                    Runnable runnable =
                            () -> {
                                try {
                                    UnitTypeTable utt = new UnitTypeTable();

                                    AI ai1 = new OptimizableRush(utt, new AStarPathFinding(), pp1_d);
                                    if (po) {
                                        ai1 = new OptimizablePORush(utt, new AStarPathFinding(), pp1_d);
                                    }
                                    AI ai2;
                                    if (sample != null) {
                                        ai2 = new OptimizableRush(utt, new AStarPathFinding(), sample);
                                        if (po) {
                                            ai2 = new OptimizablePORush(utt, new AStarPathFinding(), sample);
                                        }
                                    } else {
                                        ai2 = new POWorkerRush(utt);
                                    }
                                    double reward = 0.5;
                                    if (r.nextBoolean()) {
                                        GameState endState = runGame(ai1, ai2, maps.get(mapIdx), maxGameLength.get(mapIdx), visualize, utt, po);
                                        if (endState.winner() == 0) reward = 1.0;
                                        if (endState.winner() == 1) reward = 0.0;
                                    } else {
                                        GameState endState = runGame(ai2, ai1, maps.get(mapIdx), maxGameLength.get(mapIdx), visualize, utt, po);
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

                if (iteration % 10 == 0) {
                    System.out.println("Iteration " + iteration + " ------------------ ");
                    long seconds = TimeUnit.SECONDS.convert(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
                    System.out.println(seconds);
                    cmab.PrintMacroArmDebugInfo(true, true, 3);
                }

            }
            int pp_d[] = new int[cmab.getMostVisited().values.length];
            for (int i = 0; i < cmab.getMostVisited().values.length; i++) {
                pp_d[i] = (cmab.getMostVisited().values[i]);
            }
            history.add(pp_d);
            for (int i = 0; i < history.size(); i++) {
                System.out.println(Arrays.toString(history.get(i)));
            }

            long seconds = TimeUnit.SECONDS.convert(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
            FP_iter = time_budget * (batch + 1) / ((int) seconds) - long_bandit_factor;
            System.out.println("estimated total FP iteration: " + FP_iter);
        }

        StandAloneCMAB cmab = new StandAloneCMAB(true, arm_config, kernel);
        for (Integer iteration = 0; iteration < OPTIMIZATION_ITERATIONS * long_bandit_factor; ) {
            StandAloneCMAB.MacroArm marm1 = null;
            marm1 = cmab.pullArm(epsilon_l, epsilon_g, epsilon_0);
            kernel = new double[]{0, 1, 0};
            int pp1_d[] = new int[marm1.values.length];
            for (int i = 0; i < marm1.values.length; i++) {
                pp1_d[i] = (marm1.values[i]);
            }
            ArrayList<Double> rewards = new ArrayList<>();
            ArrayList<Thread> threads = new ArrayList<>();
            for (int j = 0; j < CORES; j++) {
                int mapIdx = m;
                int[] sample;
                if (history.isEmpty()) {
                    sample = null;
                } else {
                    sample = history.get(r.nextInt(history.size()));
                }
                Runnable runnable =
                        () -> {
                            try {
                                UnitTypeTable utt = new UnitTypeTable();
                                OptimizableRush ai1 = new OptimizableRush(utt, new AStarPathFinding(), pp1_d);
                                AI ai2;
                                if (sample != null) {
                                    ai2 = new OptimizableRush(utt, new AStarPathFinding(), sample);
                                } else {
                                    ai2 = new WorkerRush(utt);
                                }
                                double reward = 0.5;
                                if (r.nextBoolean()) {
                                    GameState endState = runGame(ai1, ai2, maps.get(mapIdx), maxGameLength.get(mapIdx), visualize, utt, po);
                                    if (endState.winner() == 0) reward = 1.0;
                                    if (endState.winner() == 1) reward = 0.0;
                                } else {
                                    GameState endState = runGame(ai2, ai1, maps.get(mapIdx), maxGameLength.get(mapIdx), visualize, utt, po);
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
        }
        System.out.println(Arrays.toString(cmab.getMostVisited().values));
    }

    public static GameState runGame(AI ai1, AI ai2, String mapFile, int maxcycles, boolean visualize, UnitTypeTable
            utt, boolean po) throws Exception {
        PhysicalGameState pgs = PhysicalGameState.load(mapFile, utt);
        GameState gs = new GameState(pgs, utt);

        JFrame w = null;
        if (visualize)
            w = PhysicalGameStatePanel.newVisualizer(gs, 640, 640, false, PhysicalGameStatePanel.COLORSCHEME_BLACK);

        boolean gameover;
        do {
            if (po) {
                PlayerAction pa1 = ai1.getAction(0, new PartiallyObservableGameState(gs, 0));
                PlayerAction pa2 = ai2.getAction(1, new PartiallyObservableGameState(gs, 1));
                gs.issueSafe(pa1);
                gs.issueSafe(pa2);
            } else {
                PlayerAction pa1 = ai1.getAction(0, gs);
                PlayerAction pa2 = ai2.getAction(1, gs);
                gs.issueSafe(pa1);
                gs.issueSafe(pa2);
            }

            gameover = gs.cycle();
        } while (!gameover && gs.getTime() < maxcycles);
        ai1.gameOver(gs.winner());
        ai2.gameOver(gs.winner());
        return gs;
    }

//
//    public static double parallelRun(int CORES, int total, int[] values, String map, Integer maxGameLength) throws Exception {
//        UnitTypeTable utt = new UnitTypeTable();
//        Random r = new Random();
//        double pp1_d[] = new double[6];
//        for (int i = 0; i < UnitAction.NUMBER_OF_ACTION_TYPES; i++) {
//            pp1_d[i] = (double) (values[i]);
//        }
//        UnitActionProbabilityDistributionAI pp1 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp1_d), utt, "uapdai1");
//        ArrayList<Double> rewards = new ArrayList<>();
//        for (int k = 0; k < total; ) {
//            ArrayList<Thread> threads = new ArrayList<>();
//            for (int j = 0; j < CORES; j++) {
//                Runnable runnable =
//                        () -> {
//                            try {
//                                AI ai1 = pp1;
//                                AI ai2 = new RandomBiasedAI();
//                                double reward = 0.5;
//                                if (r.nextBoolean()) {
//                                    GameState endState = runGame(ai1, ai2, map, maxGameLength, false, utt);
//                                    if (endState.winner() == 0) reward = 1.0;
//                                    if (endState.winner() == 1) reward = 0.0;
//                                } else {
//                                    GameState endState = runGame(ai2, ai1, map, maxGameLength, false, utt);
//                                    if (endState.winner() == 0) reward = 0.0;
//                                    if (endState.winner() == 1) reward = 1.0;
//                                }
//                                synchronized (rewards) {
//                                    rewards.add(reward);
//                                }
//                            } catch (Exception ex) {
//                                ex.printStackTrace();
//                            }
//                        };
//                Thread thread = new Thread(runnable);
//                threads.add(thread);
//            }
//            for (Thread thread : threads) {
//                thread.start();
//                k++;
//            }
//            for (Thread thread : threads) {
//                thread.join();
//            }
//        }
//        double reward = 0;
//        for (Double rw : rewards) {
//            reward += rw;
//        }
//        reward /= rewards.size();
//        return reward;
//    }
}
