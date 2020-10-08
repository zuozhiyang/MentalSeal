/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package optimalplayout;

import MentalSeal.OptimizablePORush;
import MentalSeal.OptimizableRush;
import ai.abstraction.partialobservability.POHeavyRush;
import ai.abstraction.partialobservability.POLightRush;
import ai.abstraction.partialobservability.PORangedRush;
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
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author santi
 */
public class OptimalScripts {
    public static void main(String args[]) throws Exception {
        int OPTIMIZATION_ITERATIONS = 50000;
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

        boolean po = args[0].equals("po");
        UnitTypeTable utt = new UnitTypeTable();

        int CORES = 10;
        double kernel[] = {0, 1, 0};

        for (int m = 0; m < 8; m++) {

            int arm_config[] = new int[]{6, 6, 6, 3, 3, 3, 3, 3}; // number of possible values for each variable in the CMAB
            long start = System.currentTimeMillis();
            String dir = "opt-script/";
            PrintStream o = new PrintStream(new File(dir + maps.get(m).replaceAll("/", "-") + "-" + po + ".txt"));
            System.setOut(o);
            StandAloneCMAB cmab = new StandAloneCMAB(true, arm_config, kernel);
            for (Integer iteration = 0; iteration < OPTIMIZATION_ITERATIONS; ) {
                StandAloneCMAB.MacroArm marm1 = null;
                marm1 = cmab.pullArm(epsilon_l, epsilon_g, epsilon_0);
                int pp1_d[] = new int[marm1.values.length];
                for (int i = 0; i < marm1.values.length; i++) {
                    pp1_d[i] = (marm1.values[i]);
                }
                ArrayList<Double> rewards = new ArrayList<>();
                ArrayList<Thread> threads = new ArrayList<>();
                for (int j = 0; j < CORES; j++) {
                    int mapIdx = m;
                    Runnable runnable =
                            () -> {
                                try {
                                    List<AI> baselines = new ArrayList<>();
                                    baselines.add(new POWorkerRush(utt));
                                    baselines.add(new POLightRush(utt));
                                    baselines.add(new PORangedRush(utt));
                                    baselines.add(new POHeavyRush(utt));
                                    AI ai1 = new OptimizableRush(utt, new AStarPathFinding(), pp1_d);
                                    if (po) {
                                        ai1 = new OptimizablePORush(utt, new AStarPathFinding(), pp1_d);
                                    }
                                    AI ai2 = baselines.get(r.nextInt(baselines.size()));
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

                if (iteration % 100 == 0) {
                    System.out.println("Iteration " + iteration + " ------------------ ");
                    long seconds = TimeUnit.SECONDS.convert(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
                    System.out.println(seconds);
                    cmab.PrintMacroArmDebugInfo(true, true, 3);
                }

            }

        }

    }

    public static GameState runGame(AI ai1, AI ai2, String mapFile, int maxcycles, boolean visualize, UnitTypeTable
            utt, boolean po) throws Exception {
        PhysicalGameState pgs = PhysicalGameState.load(mapFile, utt);
        GameState gs = new GameState(pgs, utt);

        JFrame w = null;
        if (visualize)
            w = PhysicalGameStatePanel.newVisualizer(gs, 640, 640, false, PhysicalGameStatePanel.COLORSCHEME_BLACK);
        int lastTimeActionIssued = 0;
        int max_inactive_cycles = 200;
        boolean gameover;
        do {
            if (po) {
                PlayerAction pa1 = ai1.getAction(0, new PartiallyObservableGameState(gs, 0));
                PlayerAction pa2 = ai2.getAction(1, new PartiallyObservableGameState(gs, 1));
                if (gs.issueSafe(pa1)) lastTimeActionIssued = gs.getTime();
                if (gs.issueSafe(pa2)) lastTimeActionIssued = gs.getTime();
            } else {
                PlayerAction pa1 = ai1.getAction(0, gs);
                PlayerAction pa2 = ai2.getAction(1, gs);
                if (gs.issueSafe(pa1)) lastTimeActionIssued = gs.getTime();
                if (gs.issueSafe(pa2)) lastTimeActionIssued = gs.getTime();
            }

            gameover = gs.cycle();
        } while (!gameover && gs.getTime() < maxcycles && (gs.getTime() - lastTimeActionIssued < max_inactive_cycles));
        ai1.gameOver(gs.winner());
        ai2.gameOver(gs.winner());
        return gs;
    }

}
