/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package optimalplayout;

import ai.RandomAI;
import ai.RandomBiasedAI;
import ai.abstraction.WorkerRush;
import ai.core.AI;
import ai.stochastic.ContextualUnitActionDistribution;
import ai.stochastic.UnitActionProbabilityDistributionAI;
import ai.stochastic.UnitActionTypeConstantDistribution;
import gui.PhysicalGameStatePanel;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.UnitAction;
import rts.units.UnitTypeTable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class StandAloneContextualCMAB {

    Random r = new Random();
    public HashMap<String, NormalizedStandAloneCMAB> cmabMap;
    public NormalizedStandAloneCMAB publicCmab;

    public boolean fensa;
    public int armNValues[];
    public double kernel[];

    public StandAloneContextualCMAB(boolean f, int a[], double k[]) throws Exception {
        fensa = f;
        armNValues = a;
        kernel = k;
        publicCmab = new NormalizedStandAloneCMAB(fensa, armNValues, kernel);
        cmabMap = new HashMap<>();

    }

    public void putKey(String key) throws Exception {
        cmabMap.put(key, new NormalizedStandAloneCMAB(fensa, armNValues, kernel));
    }

    public static void main(String[] args) throws Exception {
        int OPTIMIZATION_ITERATIONS = 500000;
        boolean visualize = false;
        float epsilon_l = 0.3f;
        float epsilon_g = 0.0f;
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

//        List<AI> ais = new ArrayList<>();
//        ais.add(new WorkerRush(utt));
//        ais.add(new RandomBiasedAI(utt));
//        ais.add(new RandomAI(utt));

        int m = 0;
        double kernel[] = {0, 1, 0};
//        NormalizedStandAloneCMAB.gamma = 0.01;
//        int arm_config[] = new int[]{6, 6, 6, 6, 6, 6};
//        int arm_config[] = new int[]{3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3};
//        int arm_config[] = new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2};
//        int arm_config[] = new int[]{6, 6, 6, 6, 6, 6, 6, 6, 6};
        int arm_config[] = new int[]{2,2,2,2,2,2,2,2,2};
        int CORES = 10;
        ContextualUnitActionDistribution ctx = new ContextualUnitActionDistribution(utt, new StandAloneContextualCMAB(true, arm_config, kernel));
        UnitActionProbabilityDistributionAI pp1 = new UnitActionProbabilityDistributionAI(ctx, utt, "uapdai1");

        for (Integer iteration = 0; iteration < OPTIMIZATION_ITERATIONS; ) {

            ArrayList<Double> rewards = new ArrayList<>();
            ArrayList<Thread> threads = new ArrayList<>();
            ctx.LEARNING = true;

            int mapIdx = m;
            for (int j = 0; j < CORES; j++) {
                Runnable runnable =
                        () -> {
                            try {
                                AI ai1 = pp1;
                                List<AI> ais = new ArrayList<>();
                                ais.add(new WorkerRush(utt));
                                ais.add(new RandomBiasedAI(utt));
                                ais.add(new RandomAI(utt));
                                AI ai2 = ais.get(r.nextInt(ais.size()));
//                                AI ai2 = new RandomAI();
//                                AI ai2 = new RandomBiasedAI();
//                                AI ai2 = new WorkerRush(utt);
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
//            System.out.println(rewards);
            ctx.receiveRewards(reward);
            iteration++;


            if (iteration % 1000 == 0) {
                System.out.println("Iteration " + iteration + " ------------------ ");

//                for (String s : ctx.contextualCMAB.cmabMap.keySet()) {
//                    ctx.contextualCMAB.cmabMap.get(s).PrintMacroArmDebugInfo(true, true, 1);
//                    System.out.println();
//                }


                ctx.LEARNING = false;
                int total = 1000;
                ArrayList<Double> rs = new ArrayList<>();
                for (int k = 0; k < total; ) {
                    ArrayList<Thread> ths = new ArrayList<>();
                    for (int j = 0; j < CORES; j++) {
                        Runnable runnable =
                                () -> {
                                    try {
                                        AI ai1 = pp1;
                                        AI ai2 = new WorkerRush(utt);
//                                        AI ai2 = new RandomBiasedAI();
                                        double rr = 0.5;
                                        if (r.nextBoolean()) {
                                            GameState endState = runGame(ai1, ai2, maps.get(m), maxGameLength.get(m), false, utt);
                                            if (endState.winner() == 0) rr = 1.0;
                                            if (endState.winner() == 1) rr = 0.0;
                                        } else {
                                            GameState endState = runGame(ai2, ai1, maps.get(m), maxGameLength.get(m), false, utt);
                                            if (endState.winner() == 0) rr = 0.0;
                                            if (endState.winner() == 1) rr = 1.0;
                                        }
                                        synchronized (rs) {
                                            rs.add(rr);
                                        }
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                };
                        Thread thread = new Thread(runnable);
                        ths.add(thread);
                    }
                    for (Thread thread : ths) {
                        thread.start();
                        k++;
                    }
                    for (Thread thread : ths) {
                        thread.join();
                    }
                }
                double avg = 0;
                for (Double rw : rs) {
                    avg += rw;
                }
                avg /= rs.size();
                System.out.println("evaluation: " + avg);
                ctx.LEARNING = true;

                ctx.LEARNING = false;
                ArrayList<Double> rs2 = new ArrayList<>();
                for (int k = 0; k < total; ) {
                    ArrayList<Thread> ths = new ArrayList<>();
                    for (int j = 0; j < CORES; j++) {
                        Runnable runnable =
                                () -> {
                                    try {
                                        AI ai1 = pp1;
                                        AI ai2 = new RandomAI();
                                        double rr = 0.5;
                                        if (r.nextBoolean()) {
                                            GameState endState = runGame(ai1, ai2, maps.get(m), maxGameLength.get(m), false, utt);
                                            if (endState.winner() == 0) rr = 1.0;
                                            if (endState.winner() == 1) rr = 0.0;
                                        } else {
                                            GameState endState = runGame(ai2, ai1, maps.get(m), maxGameLength.get(m), false, utt);
                                            if (endState.winner() == 0) rr = 0.0;
                                            if (endState.winner() == 1) rr = 1.0;
                                        }
                                        synchronized (rs2) {
                                            rs2.add(rr);
                                        }
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                };
                        Thread thread = new Thread(runnable);
                        ths.add(thread);
                    }
                    for (Thread thread : ths) {
                        thread.start();
                        k++;
                    }
                    for (Thread thread : ths) {
                        thread.join();
                    }
                }
                avg = 0;
                for (Double rw : rs2) {
                    avg += rw;
                }
                avg /= rs2.size();
                System.out.println("evaluation: " + avg);
                ctx.LEARNING = true;

                ctx.LEARNING = false;
                ArrayList<Double> rs3 = new ArrayList<>();
                for (int k = 0; k < total; ) {
                    ArrayList<Thread> ths = new ArrayList<>();
                    for (int j = 0; j < CORES; j++) {
                        Runnable runnable =
                                () -> {
                                    try {
                                        AI ai1 = pp1;
                                        AI ai2 = new RandomBiasedAI();
                                        double rr = 0.5;
                                        if (r.nextBoolean()) {
                                            GameState endState = runGame(ai1, ai2, maps.get(m), maxGameLength.get(m), false, utt);
                                            if (endState.winner() == 0) rr = 1.0;
                                            if (endState.winner() == 1) rr = 0.0;
                                        } else {
                                            GameState endState = runGame(ai2, ai1, maps.get(m), maxGameLength.get(m), false, utt);
                                            if (endState.winner() == 0) rr = 0.0;
                                            if (endState.winner() == 1) rr = 1.0;
                                        }
                                        synchronized (rs3) {
                                            rs3.add(rr);
                                        }
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                };
                        Thread thread = new Thread(runnable);
                        ths.add(thread);
                    }
                    for (Thread thread : ths) {
                        thread.start();
                        k++;
                    }
                    for (Thread thread : ths) {
                        thread.join();
                    }
                }
                avg = 0;
                for (Double rw : rs3) {
                    avg += rw;
                }
                avg /= rs3.size();
                System.out.println("evaluation: " + avg);
                ctx.LEARNING = true;




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
//            PlayerAction pa1 = ai1.getAction(0, new PartiallyObservableGameState(gs, 0));
//            PlayerAction pa2 = ai2.getAction(1, new PartiallyObservableGameState(gs, 1));
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

    public static double parallelRun(int CORES, int total, int[] values, String map, Integer maxGameLength) throws Exception {
        UnitTypeTable utt = new UnitTypeTable();
        Random r = new Random();
        double pp1_d[] = new double[6];
        for (int i = 0; i < UnitAction.NUMBER_OF_ACTION_TYPES; i++) {
            pp1_d[i] = (double) (values[i]);
        }
        UnitActionProbabilityDistributionAI pp1 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp1_d), utt, "uapdai1");
        ArrayList<Double> rewards = new ArrayList<>();
        for (int k = 0; k < total; ) {
            ArrayList<Thread> threads = new ArrayList<>();
            for (int j = 0; j < CORES; j++) {
                Runnable runnable =
                        () -> {
                            try {
                                AI ai1 = pp1;
                                AI ai2 = new WorkerRush(utt);
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
