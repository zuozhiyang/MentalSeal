/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package optimalplayout;

import ai.core.AI;
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
import java.io.File;
import java.io.PrintStream;
import java.util.*;

/**
 * @author santi
 */
public class OptimalSinglePlayoutGamePolicy {
    public static void main(String args[]) throws Exception {
        // Results after running 500 iterations:
        // - Best weights so far: {2.0, 1.0, 4.0, 3.0, 8.0, 8.0}
        // Which can be instantiated into an AI, like this:
        // - new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt,new double[]{2.0, 1.0, 4.0, 3.0, 8.0, 8.0}), utt, "playoutai")
        int CORES = 10;
//        int m = Integer.parseInt(args[0]);
//        String algo = args[1];
        int OPTIMIZATION_ITERATIONS = 10000;
        boolean visualize = false;
        float epsilon_l = .33f;
        float epsilon_g = .0f;
        float epsilon_0 = .5f;
        Random r = new Random();
        UnitTypeTable utt = new UnitTypeTable();

        List<String> maps = new ArrayList<>();
        List<Integer> maxGameLength = new ArrayList<>();
        int m = 1;
        maps.add("maps/8x8/basesWorkers8x8A.xml");
        maxGameLength.add(3000);
        maps.add("maps/8x8/FourBasesWorkers8x8.xml");
        maxGameLength.add(3000);
        maps.add("maps/NoWhereToRun9x8.xml");
        maxGameLength.add(3000);
//        maps.add("maps/16x16/basesWorkers16x16A.xml");
//        maxGameLength.add(4000);
//        maps.add("maps/16x16/TwoBasesBarracks16x16.xml");
//        maxGameLength.add(4000);
//        double[] best_sum1 = new double[6];
//        double numUpdate1 = 0;
//
//        double[] best_sum0 = new double[6];
//        double numUpdate0 = 0;
//        HashMap<String, Integer> convergence = new HashMap<>();
//        ArrayList<double[]> history0 = new ArrayList<>();
//        ArrayList<double[]> history1 = new ArrayList<>();

//        DecimalFormat df = new DecimalFormat("0.0000");
//        int m = 2;


        double kernel[] = {0.1577, 0.6845, 0.1577};
        int arm_config[] = new int[]{6, 6, 6, 6, 6, 6}; // number of possible values for each variable in the CMAB
//        for (int m = 0; m < maps.size(); m++) {
        ArrayList<double[]> population = new ArrayList<>();
        String mapName;
        if (maps.get(m).split("/").length > 2)
            mapName = maps.get(m).split("/")[1] + "-" + maps.get(m).split("/")[2];
        else
            mapName = maps.get(m).split("/")[1];
        System.out.println(mapName);
        File myObj = new File("FP-Normalized-playout/history-maps-" + mapName + ".txt");
        Scanner myReader = new Scanner(myObj);
        while (myReader.hasNextLine()) {
            String[] data = myReader.nextLine().replaceAll("\\[|\\]| ", "").split(",");
            double[] entry = new double[data.length];
            for (int i = 0; i < entry.length; i++) {
                entry[i] = Double.parseDouble(data[i]);
            }
            population.add(entry);
        }
        myReader.close();
        PrintStream o = new PrintStream(new File("FP-Normalized-playout/best-arm-" + m + ".txt"));
        System.setOut(o);
//        long time = System.currentTimeMillis();
//        for (int batch = 0; batch < 1000; batch++) {
//            System.out.println("Iteration: " + (batch + 1));
//            time = System.currentTimeMillis();
//            for (String algo : new String[]{"EG-lk", "EG-gk", "EG-l", "EG-g", "TS-g", "TS-l"}) {
        for (String algo : new String[]{"EG-g"}) {
            NormalizedStandAloneCMAB cmab = new NormalizedStandAloneCMAB(true, arm_config, kernel);
            for (Integer iteration = 0; iteration < OPTIMIZATION_ITERATIONS; ) {
                float f = (OPTIMIZATION_ITERATIONS - iteration) / (float) OPTIMIZATION_ITERATIONS;
                float actual_epsilon_0 = epsilon_0 * f;
                NormalizedStandAloneCMAB.MacroArm marm1 = null;
                if (algo.equals("EG-g")) {
                    marm1 = cmab.pullArm(epsilon_l, epsilon_g, actual_epsilon_0);
                    kernel = new double[]{0, 1, 0};
                } else if (algo.equals("EG-l")) {
                    marm1 = cmab.pullArm(epsilon_l, epsilon_g, 1);
                    kernel = new double[]{0, 1, 0};
                } else if (algo.equals("EG-lk")) {
                    marm1 = cmab.pullArm(epsilon_l, epsilon_g, 1);
                } else {
                    marm1 = cmab.pullArm(epsilon_l, epsilon_g, actual_epsilon_0);
                }
                ArrayList<Double> rewards = new ArrayList<>();
                ArrayList<Thread> threads = new ArrayList<>();
                double pp1_d[] = new double[marm1.values.length];
                for (int i = 0; i < marm1.values.length; i++) {
                    pp1_d[i] = (double) (marm1.values[i]);
                }
                UnitActionProbabilityDistributionAI pp1 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp1_d), utt, "uapdai1");
                int mapIdx = m;
                for (int j = 0; j < CORES; j++) {
                    double[] sample;
                    sample = population.get(r.nextInt(population.size()));
                    Runnable runnable =
                            () -> {
                                try {
//                                        AI ai1 = pp1;
//                                        AI ai2 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, sample), utt, "uapdai1");
                                    AI ai1 = new NaiveMCTS(-1, 100, 100, 10, .3f, 0, .4f, pp1, new SimpleSqrtEvaluationFunction3(), true);
                                    AI ai2 = new NaiveMCTS(-1, 100, 100, 10, .3f, 0, .4f, new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, sample), utt, "uapdai1"), new SimpleSqrtEvaluationFunction3(), true);

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
                cmab.receiveReward(reward, marm1);
                iteration++;

                if (iteration % 50 == 0) {
                    System.out.println("Iteration " + iteration + " ------------------ ");
                    System.out.println("Reward: " + reward);
                    System.out.println("Marm: " + Arrays.toString(marm1.values));
                    cmab.PrintMacroArmDebugInfo(true, true, 5);
//                    StandAloneCMAB.MacroArm macroArm = cmab.getMostVisited();
//                            writer.write(iteration + "," + Arrays.toString(macroArm.values) + "\n");
//                            writer.flush();
                }
            }
            System.out.println(Arrays.toString(cmab.getMostVisited().values));
        }
    }
//    }

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
