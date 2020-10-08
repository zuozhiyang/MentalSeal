///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package optimalplayout;
//
//import GNS.OptimizablePORush;
//import GNS.OptimizableRush;
//import ai.abstraction.pathfinding.AStarPathFinding;
//import ai.core.AI;
//import gui.PhysicalGameStatePanel;
//import rts.GameState;
//import rts.PartiallyObservableGameState;
//import rts.PhysicalGameState;
//import rts.PlayerAction;
//import rts.units.UnitTypeTable;
//
//import javax.swing.*;
//import java.io.File;
//import java.io.PrintStream;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//
///**
// * @author santi
// */
//public class OptimalRepeatedZSGamePolicyUtil {
//    static int CORES = 4;
//
//    public static AI[] optimizeScript() throws Exception {
//        int OPTIMIZATION_ITERATIONS = 50000;
//        boolean visualize = false;
//        float epsilon_l = .33f;
//        float epsilon_g = .0f;
//        float epsilon_0 = .4f;
//        Random r = new Random();
//
//        List<String> maps = new ArrayList<>();
//        List<Integer> maxGameLength = new ArrayList<>();
//
//        maps.add("maps/8x8/basesWorkers8x8A.xml");
//        maxGameLength.add(3000);
//        maps.add("maps/16x16/basesWorkers16x16A.xml");
//        maxGameLength.add(4000);
//        maps.add("maps/BWDistantResources32x32.xml");
//        maxGameLength.add(6000);
//        maps.add("maps/BroodWar/(4)BloodBath.scmB.xml");
//        maxGameLength.add(8000);
//        maps.add("maps/8x8/FourBasesWorkers8x8.xml");
//        maxGameLength.add(3000);
//        maps.add("maps/16x16/TwoBasesBarracks16x16.xml");
//        maxGameLength.add(4000);
//        maps.add("maps/NoWhereToRun9x8.xml");
//        maxGameLength.add(3000);
//        maps.add("maps/DoubleGame24x24.xml");
//        maxGameLength.add(5000);
//        double kernel[] = {0, 1, 0};
//        int arm_config[] = new int[]{5, 3, 3, 3, 3, 3}; // number of possible values for each variable in the CMAB
//        boolean po = false;
//        StandAloneCMAB cmab1 = new StandAloneCMAB(true, arm_config, kernel);
//        StandAloneCMAB cmab2 = new StandAloneCMAB(true, arm_config, kernel);
//        for (Integer iteration = 0; iteration < OPTIMIZATION_ITERATIONS; ) {
//            float f = (OPTIMIZATION_ITERATIONS - iteration) / (float) OPTIMIZATION_ITERATIONS;
//            float actual_epsilon_0 = epsilon_0 * f;
//            StandAloneCMAB.MacroArm marm1 = null;
//            StandAloneCMAB.MacroArm marm2 = null;
//            marm1 = cmab1.pullArm(epsilon_l, epsilon_g, actual_epsilon_0);
//            marm2 = cmab2.pullArm(epsilon_l, epsilon_g, actual_epsilon_0);
//            ArrayList<Double> rewards = new ArrayList<>();
//            ArrayList<Thread> threads = new ArrayList<>();
//            int pp1_d[] = new int[marm1.values.length];
//            int pp2_d[] = new int[marm2.values.length];
//            for (int i = 0; i < marm1.values.length; i++) {
//                pp1_d[i] = (marm1.values[i]);
//                pp2_d[i] = (marm2.values[i]);
//            }
//            int mapIdx = m;
//
//            for (int j = 0; j < CORES; j++) {
//                Runnable runnable =
//                        () -> {
//                            try {
//                                UnitTypeTable utt = new UnitTypeTable();
//                                AI ai1 = new OptimizableRush(utt, new AStarPathFinding(), pp1_d);
//                                AI ai2 = new OptimizableRush(utt, new AStarPathFinding(), pp2_d);
//                                if (po) {
//                                    ai1 = new OptimizablePORush(utt, new AStarPathFinding(), pp1_d);
//                                    ai2 = new OptimizablePORush(utt, new AStarPathFinding(), pp2_d);
//                                }
//                                double reward = 0.5;
//                                if (r.nextBoolean()) {
//                                    GameState endState = runGame(ai1, ai2, maps.get(mapIdx), maxGameLength.get(mapIdx), visualize, utt, po);
//                                    if (endState.winner() == 0) reward = 1.0;
//                                    if (endState.winner() == 1) reward = 0.0;
//                                } else {
//                                    GameState endState = runGame(ai2, ai1, maps.get(mapIdx), maxGameLength.get(mapIdx), visualize, utt, po);
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
//            }
//            for (Thread thread : threads) {
//                thread.join();
//            }
//
//            double reward = 0;
//            for (Double rw : rewards) {
//                reward += rw;
//            }
//            reward /= rewards.size();
//            cmab1.receiveReward(reward, marm1);
//            cmab2.receiveReward(1 - reward, marm2);
//            iteration++;
//            if (iteration % 1000 == 0) {
//                System.out.println(iteration + " " + actual_epsilon_0);
//                System.out.println("CMAB 1:");
//                cmab1.PrintMacroArmDebugInfo(true, true, 5);
//                System.out.println("CMAB 2:");
//                cmab2.PrintMacroArmDebugInfo(true, true, 5);
//            }
//        }
//    }
////    }
//
//    public static GameState runGame(AI ai1, AI ai2, String mapFile, int maxcycles, boolean visualize, UnitTypeTable
//            utt, boolean po) throws Exception {
//        PhysicalGameState pgs = PhysicalGameState.load(mapFile, utt);
//        GameState gs = new GameState(pgs, utt);
//        JFrame w = null;
//        if (visualize)
//            w = PhysicalGameStatePanel.newVisualizer(gs, 640, 640, false, PhysicalGameStatePanel.COLORSCHEME_BLACK);
//        boolean gameover;
//        do {
//            if (po) {
//                PlayerAction pa1 = ai1.getAction(0, new PartiallyObservableGameState(gs, 0));
//                PlayerAction pa2 = ai2.getAction(1, new PartiallyObservableGameState(gs, 1));
//                gs.issueSafe(pa1);
//                gs.issueSafe(pa2);
//            } else {
//                PlayerAction pa1 = ai1.getAction(0, gs);
//                PlayerAction pa2 = ai2.getAction(1, gs);
//                gs.issueSafe(pa1);
//                gs.issueSafe(pa2);
//            }
//            gameover = gs.cycle();
//            if (visualize) w.repaint();
//            Thread.yield(); // Play nice with the other processes, and do not use 100% of the CPU :)
//        } while (!gameover && gs.getTime() < maxcycles);
//        ai1.gameOver(gs.winner());
//        ai2.gameOver(gs.winner());
//        return gs;
//    }
//}
