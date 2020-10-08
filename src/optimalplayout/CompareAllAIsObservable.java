/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package optimalplayout;

import ai.abstraction.WorkerRush;
import ai.core.AI;
import ai.stochastic.UnitActionDetailedTypeConstantDistribution2;
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
public class CompareAllAIsObservable {

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
        double pp1_d[] = new double[]{0, 0, 2, 2, 5, 3};
        double pp2_d[] = new double[]{0, 0, 2, 2, 3, 3};
        ArrayList<double[]> population = new ArrayList<>();
        String mapName;
        if (maps.get(m).split("/").length > 2)
            mapName = maps.get(m).split("/")[1] + "-" + maps.get(m).split("/")[2];
        else
            mapName = maps.get(m).split("/")[1];
//        System.out.println(mapName);
//        File myObj = new File("FP-Normalized/history-maps-" + mapName + ".txt");
//        Scanner myReader = new Scanner(myObj);
//        while (myReader.hasNextLine()) {
//            String[] data = myReader.nextLine().replaceAll("\\[|\\]| ", "").split(",");
//            double[] entry = new double[data.length];
//            for (int i = 0; i < entry.length; i++) {
//                entry[i] = Double.parseDouble(data[i]);
//            }
//            population.add(entry);
//        }
//        myReader.close();
//        AI pp2 = new RandomBiasedAI(utt);
        AI ai = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp1_d), utt, "uapdai1");
//        AI pp2 = new WorkerRush(utt);
//        UnitActionProbabilityDistributionAI pp2 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp2_d), utt, "uapdai1");
//        AI ai1 = new NaiveMCTS(-1, MAX_PLAYOUTS, 100, 10, 0.3f, 0, 0.4f, pp1, new SimpleSqrtEvaluationFunction3(), true);
//        AI ai2 = new NaiveMCTS(-1, MAX_PLAYOUTS, 100, 10, 0.3f, 0, 0.4f, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), true);
//        bots.add(ai1);
//        bots.add(ai2);
        ArrayList<Double> rewards = new ArrayList<>();
        int ITERATIONS = 5000;
        Random r = new Random();
        int CORES = 10;
        double[] prev = new double[]{1, 1, 1, 1, 1, 1};

//        for (int k = 0; k < 20; k++) {

//            double[] curr = opt(m, maps, maxGameLength, utt, prev);
//            System.out.println(Arrays.toString(curr));
        for (int iteration = 0; iteration < ITERATIONS; ) {
            ArrayList<Thread> threads = new ArrayList<>();

            for (int j = 0; j < CORES; j++) {
                Runnable runnable =
                        () -> {
                            try {
//                                    AI ai1 = new UnitActionProbabilityDistributionAI(new UnitActionPopulationDistribution(utt, "FP-Normalized/history-"+maps.get(m).replaceAll("/","-")+".txt"), utt, "");
//                                    AI ai2 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, curr), utt, "uapdai1");
//                                AI ai1 = new NaiveMCTS(-1, iteration_budget, 100, 10, ai_epsilon_l, ai_epsilon_g, ai_epsilon_0,
//                                        new UnitActionProbabilityDistributionAI(new UnitActionPopulationDistribution(utt,"FP-Normalized/history-maps-8x8-FourBasesWorkers8x8.xml.txt"),utt,""), new SimpleSqrtEvaluationFunction3(), true);
//                                AI ai2 = new NaiveMCTS(-1, iteration_budget, 100, 10, ai_epsilon_l, ai_epsilon_g, ai_epsilon_0,
//                                        new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, new double[]{1,1,1,1,1,1}), utt, "uapdai1"), new SimpleSqrtEvaluationFunction3(), true);


                                //                                AI ai2 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, new double[]{1, 1, 1, 1, 1, 1}), utt, "uapdai1");
//                                    AI ai1 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, new double[]{0, 0, 2, 2, 5, 3}), utt, "uapdai1");
                                    AI ai1 = new UnitActionProbabilityDistributionAI(new UnitActionDetailedTypeConstantDistribution2(utt, new double[]{0, 1, 0, 2, 0, 2, 2, 0, 1, 0, 0, 2}), utt, "uapdai1");
//                                    AI ai2 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, new double[]{0, 0, 2, 2, 3, 3}), utt, "uapdai1");
                                //                                double []  ppd = population.get(r.nextInt(population.size()));
//                                AI ai1 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp1_d), utt, "uapdai1");
//                                AI ai1 = new NaiveMCTS(-1, iteration_budget, 100, 10, ai_epsilon_l, ai_epsilon_g, ai_epsilon_0,
//                                        ai, new SimpleSqrtEvaluationFunction3(), true);
//
                                AI ai2 = new WorkerRush(utt);

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
        double reward = 0;
        for (Double rw : rewards) {
            reward += rw;
        }
        reward /= rewards.size();
        System.out.println(reward);
//            prev = curr;

//        }
    }

    public static double[] opt(int mapIdx, List<String> maps, List<Integer> maxGameLength, UnitTypeTable utt, double[] prev) throws Exception {
        NormalizedStandAloneCMAB cmab = new NormalizedStandAloneCMAB(true, new int[]{6, 6, 6, 6, 6, 6}, new double[]{0, 1, 0});

        for (Integer iter = 0; iter < 5000; ) {
            NormalizedStandAloneCMAB.MacroArm marm1 = null;

            marm1 = cmab.pullArm(0.3f, 0, 0.4f);

            ArrayList<Double> rewards = new ArrayList<>();
            ArrayList<Thread> threads = new ArrayList<>();
            double pp1_d[] = new double[marm1.values.length];
            for (int i = 0; i < marm1.values.length; i++) {
                pp1_d[i] = (double) (marm1.values[i]);
            }

            double pp2_d[] = new double[marm1.values.length];
            for (int i = 0; i < marm1.values.length; i++) {
                pp2_d[i] = (double) (prev[i]);
            }
            UnitActionProbabilityDistributionAI pp1 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp1_d), utt, "uapdai1");
            UnitActionProbabilityDistributionAI pp2 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp2_d), utt, "uapdai1");
//                    UnitActionProbabilityDistributionAI pp1 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp1_d), utt, "uapdai1");
//            int mapIdx = m;
            for (int j = 0; j < 10; j++) {
                Runnable runnable =
                        () -> {
                            try {
                                AI ai1 = pp1;
                                AI ai2 = pp2;
                                double reward = 0.5;
                                if (new Random().nextBoolean()) {
                                    GameState endState = runGame(ai1, ai2, maps.get(mapIdx), maxGameLength.get(mapIdx), false, utt);
                                    if (endState.winner() == 0) reward = 1.0;
                                    if (endState.winner() == 1) reward = 0.0;
                                } else {
                                    GameState endState = runGame(ai2, ai1, maps.get(mapIdx), maxGameLength.get(mapIdx), false, utt);
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
//                        System.out.println(rewards);
            cmab.receiveReward(reward, marm1);
            iter++;
//            if (iter % 100 == 0) {
//                System.out.println("Iteration " + iter + " ------------------ ");
//                System.out.println("Reward: " + reward);
//                System.out.println("Marm: " + Arrays.toString(marm1.values));
//                cmab.PrintMacroArmDebugInfo(true, true, 5);
//                NormalizedStandAloneCMAB.MacroArm macroArm = cmab.getMostVisited();
////                            writer.write(iteration + "," + Arrays.toString(macroArm.values) + "\n");
////                            writer.flush();
//            }
        }
        double[] result = new double[cmab.getMostVisited().values.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = cmab.getMostVisited().values[i];
        }
        return result;
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
