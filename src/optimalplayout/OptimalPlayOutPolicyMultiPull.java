/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package optimalplayout;

import ai.RandomBiasedAI;
import ai.core.AI;
import ai.stochastic.UnitActionProbabilityDistributionAI;
import ai.stochastic.UnitActionTypeConstantDistribution;
import gui.PhysicalGameStatePanel;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * @author santi
 */
public class OptimalPlayOutPolicyMultiPull {
    public static void main(String args[]) throws Exception {
        // Results after running 500 iterations:
        // - Best weights so far: {2.0, 1.0, 4.0, 3.0, 8.0, 8.0}
        // Which can be instantiated into an AI, like this:
        // - new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt,new double[]{2.0, 1.0, 4.0, 3.0, 8.0, 8.0}), utt, "playoutai")

//        String map = args[0];
//        String algo = args[1];
        int OPTIMIZATION_ITERATIONS = 5000;
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
        int CORES = 20;


        int arm_config[] = new int[]{9, 9, 9, 9, 9, 9};
        for (int b = 0; b < CORES; b++) {
            int finalB = b;
            Runnable runnable =
                    () -> {
                        try {
                            int batch = finalB;
                            double kernel[] = {0.1577, 0.6845, 0.1577};
                            for (String algo : new String[]{"EG-lk", "EG-gk", "EG-l", "EG-g", "TS-g", "TS-l"}) {
                                for (int m = 0; m < maps.size(); m++) {
                                    BufferedWriter writer = new BufferedWriter(new FileWriter("gameplay-results-multi/" + algo + "-" + maps.get(m).replaceAll("/", "-") + "-" + algo + "-" + batch + ".txt"));
                                    BufferedWriter console = new BufferedWriter(new FileWriter("gameplay-results-multi/console-" + algo + "-" + maps.get(m).replaceAll("/", "-") + "-" + algo + "-" + batch + ".txt"));
                                    StandAloneCMAB cmab = new StandAloneCMAB(true, arm_config, kernel);

                                    for (Integer iteration = 0; iteration < OPTIMIZATION_ITERATIONS; ) {
                                        StandAloneCMAB.MacroArm marm1 = null;
                                        if (algo.equals("EG-g")) {
                                            marm1 = cmab.pullArm(epsilon_l, epsilon_g, epsilon_0);
                                            kernel = new double[]{0, 1, 0};
                                        } else if (algo.equals("EG-l")) {
                                            marm1 = cmab.pullArm(epsilon_l, epsilon_g, 1);
                                            kernel = new double[]{0, 1, 0};
                                        } else if (algo.equals("EG-lk")) {
                                            marm1 = cmab.pullArm(epsilon_l, epsilon_g, 1);
                                        } else {
                                            marm1 = cmab.pullArm(epsilon_l, epsilon_g, epsilon_0);
                                        }
                                        double pp1_d[] = new double[marm1.values.length];
                                        for (int i = 0; i < marm1.values.length; i++) {
                                            pp1_d[i] = (double) (marm1.values[i]);
                                        }
                                        UnitActionProbabilityDistributionAI pp1 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp1_d), utt, "uapdai1");
                                        int mapIdx = m;
                                        AI ai1 = pp1;
                                        AI ai2 = new RandomBiasedAI();
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
                                        cmab.receiveReward(reward, marm1);
                                        iteration++;
                                        if (iteration % 50 == 0) {
                                            console.write("Iteration " + iteration + " ------------------ \n");
                                            console.write("Reward: " + reward + "\n");
                                            console.write("Marm: " + Arrays.toString(marm1.values) + "\n");
                                            cmab.PrintMacroArmDebugInfo(true, true, 5, console);
                                            StandAloneCMAB.MacroArm macroArm = cmab.getMostVisited();
                                            writer.write(iteration + "," + Arrays.toString(macroArm.values) + "\n");
                                            writer.flush();
                                            console.flush();
                                        }
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    };
            Thread thread = new Thread(runnable);
            thread.start();
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
