///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package optimalplayout;
//
//import ai.RandomBiasedAI;
//import ai.core.AI;
//import ai.stochastic.UnitActionProbabilityDistributionAI;
//import ai.stochastic.UnitActionTypeConstantDistribution;
//import rts.GameState;
//import rts.UnitAction;
//import rts.units.UnitTypeTable;
//
//import java.io.BufferedWriter;
//import java.io.FileWriter;
//import java.util.*;
//
//import static optimalplayout.OptimalGamePolicy.runGame;
//
///**
// * @author santi
// */
//public class StandAloneGA {
//    static List<String> maps = new ArrayList<>();
//    static List<Integer> maxGameLength = new ArrayList<>();
//    int OPTIMIZATION_ITERATIONS = 10000;
//    boolean visualize = false;
//    Random r = new Random();
//    UnitTypeTable utt = new UnitTypeTable();
//
//    static {
//
//        maps.add("maps/8x8/basesWorkers8x8A.xml");
//        maxGameLength.add(3000);
//        maps.add("maps/8x8/FourBasesWorkers8x8.xml");
//        maxGameLength.add(3000);
//        maps.add("maps/NoWhereToRun9x8.xml");
//        maxGameLength.add(3000);
//        maps.add("maps/16x16/basesWorkers16x16A.xml");
//        maxGameLength.add(4000);
//        maps.add("maps/16x16/TwoBasesBarracks16x16.xml");
//        maxGameLength.add(4000);
//    }
//
//
//    public class MacroArm {
//        public MacroArm(int v[]) {
//            values = v;
//            key = Arrays.toString(values);
//        }
//
//        public String key;
//        public int values[];
//        public double accum_evaluation = 0;
//        public int visit_count = 0;
//    }
//
//    static public int DEBUG = 0;
//    static float mutationRate = 0.1f;
//    static Comparator<MacroArm> comparator = new Comparator<MacroArm>() {
//        @Override
//        public int compare(MacroArm o1, MacroArm o2) {
//            return Double.compare(o2.accum_evaluation / o2.visit_count, o1.accum_evaluation / o1.visit_count);
//        }
//    };
//
//    HashMap<String, MacroArm> childrenMap = new LinkedHashMap<>();    // associates action codes with macro arms
//    public List<MacroArm> globalMABarms;
//    public List<MacroArm> population;
//    int np;
//
//    public StandAloneGA(int p) throws Exception {
//        population = new ArrayList<>();
//        np = p;
//        for (int i = 0; i < np; i++) {
//            int[] v = new int[6];
//            for (int j = 0; j < v.length; j++) {
//                v[j] = r.nextInt(8);
//            }
//            population.add(new MacroArm(v));
//        }
//        globalMABarms = new ArrayList<>();
//    }
//
//    public int sample(double[] dist) {
//        double sample = r.nextDouble();
//        double sum = 0;
//        int i = 0;
//        while (sum < sample) {
//            sum += dist[i];
//            i++;
//        }
//        return i - 1;
//    }
//
//    public void evolve() {
//        List<MacroArm> nextPopulation = new ArrayList<>();
//        double[] dist = new double[np];
//        double sum = 0;
//        for (int i = 0; i < population.size(); i++) {
//            double avg = population.get(i).accum_evaluation / population.get(i).visit_count;
//            dist[i] = avg;
//            sum += dist[i];
//        }
//        for (int i = 0; i < population.size(); i++) {
//            dist[i] /= sum;
//        }
//
//        for (int i = 0; i < population.size(); i++) {
//            MacroArm p1 = population.get(sample(dist));
//            MacroArm p2 = population.get(sample(dist));
//            nextPopulation.add(mate(p1, p2));
//        }
//        population = nextPopulation;
//        for (MacroArm macroArm : population) {
//            if (!globalMABarms.contains(macroArm)) {
//                globalMABarms.add(macroArm);
//            }
//            if (!childrenMap.containsKey(macroArm.key)) {
//                childrenMap.put(macroArm.key, macroArm);
//            }
//        }
//    }
//
//    public MacroArm mate(MacroArm p1, MacroArm p2) {
//        int[] v1 = p1.values;
//        int[] v2 = p2.values;
//        int[] v = new int[v1.length];
//        int partition = r.nextInt(9);
//        for (int i = 0; i < v1.length; i++) {
//            if (r.nextDouble() < mutationRate)
//                v[i] = r.nextInt(8);
//            else {
//                if (i < partition) {
//                    v[i] = v1[i];
//                } else {
//                    v[i] = v2[i];
//                }
//            }
//        }
//        if (childrenMap.containsKey(Arrays.toString(v))) {
//            return childrenMap.get(Arrays.toString(v));
//        }
//        return new MacroArm(v);
//    }
//
//    public void receiveReward(double evaluation, MacroArm choice) {
//        choice.accum_evaluation += evaluation;
//        choice.visit_count++;
//    }
//
//    public MacroArm getMostVisited() {
//        MacroArm best = null;
//        for (MacroArm globalMABarm : globalMABarms) {
//            if (best == null || globalMABarm.visit_count > best.visit_count ||
//                    (globalMABarm.visit_count == best.visit_count && globalMABarm.accum_evaluation / globalMABarm.visit_count > best.accum_evaluation / best.visit_count)) {
//                best = globalMABarm;
//            }
//        }
//        return best;
//    }
//
//    public void PrintMacroArmDebugInfo(boolean sorted, boolean sortByvisitCount, int topN) {
//        List<MacroArm> toDisplay = globalMABarms;
//        if (sorted) {
//            toDisplay = new ArrayList<>();
//            toDisplay.addAll(globalMABarms);
//            if (sortByvisitCount) {
//                Collections.sort(toDisplay, new Comparator<MacroArm>() {
//                    @Override
//                    public int compare(MacroArm o1, MacroArm o2) {
//                        // compare them in reverse, so that they are sorted from larger to smaller
//                        if (o2.visit_count == o1.visit_count) {
//                            // resolve ties by score:
//                            return Double.compare(o2.accum_evaluation / o2.visit_count, o1.accum_evaluation / o1.visit_count);
//                        } else {
//                            return Double.compare(o2.visit_count, o1.visit_count);
//                        }
//                    }
//                });
//            } else {
//                Collections.sort(toDisplay, new Comparator<MacroArm>() {
//                    @Override
//                    public int compare(MacroArm o1, MacroArm o2) {
//                        // compare them in reverse, so that they are sorted from larger to smaller
//                        return Double.compare(o2.accum_evaluation / o2.visit_count, o1.accum_evaluation / o1.visit_count);
//                    }
//                });
//            }
//        }
//        if (topN > 0) {
//            List<MacroArm> macroArms = toDisplay.subList(0, Math.min(5, toDisplay.size()));
//            if (sortByvisitCount) {
//                for (MacroArm marm : macroArms) {
//                    System.out.println("best visit " + (macroArms.indexOf(marm) + 1) + ": " + (marm.accum_evaluation / marm.visit_count) + " - " + marm.visit_count + " --> " + Arrays.toString(marm.values));
//                }
//            } else {
//                for (MacroArm marm : macroArms) {
//                    System.out.println("best score " + (macroArms.indexOf(marm) + 1) + ": " + (marm.accum_evaluation / marm.visit_count) + " - " + marm.visit_count + " --> " + Arrays.toString(marm.values));
//                }
//            }
//        } else {
//            for (MacroArm marm : toDisplay) {
//                System.out.println((marm.accum_evaluation / marm.visit_count) + " - " + marm.visit_count + " --> " + Arrays.toString(marm.values));
//            }
//        }
//    }
//
//    public double evaluate(MacroArm arm, int mapIdx) throws Exception {
//        ArrayList<Double> rewards = new ArrayList<>();
//        ArrayList<Thread> threads = new ArrayList<>();
//        double pp1_d[] = new double[6];
//        for (int i = 0; i < UnitAction.NUMBER_OF_ACTION_TYPES; i++) {
//            pp1_d[i] = (double) (arm.values[i] + 1.0);
//        }
//        UnitActionProbabilityDistributionAI pp1 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp1_d), utt, "uapdai1");
////        for (int i = 0; i < 100;) {
//        for (int j = 0; j < CORES; j++) {
//            Runnable runnable =
//                    () -> {
//                        try {
//                            AI ai1 = pp1;
//                            AI ai2 = new RandomBiasedAI();
//                            double reward = 0.5;
//                            if (r.nextBoolean()) {
//                                GameState endState = runGame(ai1, ai2, maps.get(mapIdx), maxGameLength.get(mapIdx), visualize, utt, po);
//                                if (endState.winner() == 0) reward = 1.0;
//                                if (endState.winner() == 1) reward = 0.0;
//                            } else {
//                                GameState endState = runGame(ai2, ai1, maps.get(mapIdx), maxGameLength.get(mapIdx), visualize, utt, po);
//                                if (endState.winner() == 0) reward = 0.0;
//                                if (endState.winner() == 1) reward = 1.0;
//                            }
//                            synchronized (rewards) {
//                                rewards.add(reward);
//                            }
//                        } catch (Exception ex) {
//                            ex.printStackTrace();
//                        }
//                    };
//            Thread thread = new Thread(runnable);
//            threads.add(thread);
//        }
//        for (Thread thread : threads) {
//            thread.start();
//        }
//        for (Thread thread : threads) {
//            thread.join();
//        }
//        double reward = 0;
//        for (Double rw : rewards) {
//            reward += rw;
//        }
//        reward /= rewards.size();
//        return reward;
//    }
//
//    static int CORES = 20;
//
//    public static void main(String[] args) throws Exception {
//        int p = 50;
//        int iterations = 10000;
//        int map = 0;
//        for (int batch = 0; batch < 10; batch++) {
//            for (int j = 0; j < maps.size(); j++) {
//                BufferedWriter writer = new BufferedWriter(new FileWriter("gameplay-results-GA/" + "GA" + "-" + maps.get(j).replaceAll("/", "-") + "-" + batch + ".txt"));
//                StandAloneGA ga = new StandAloneGA(p);
//                for (int i = 0; i < iterations; ) {
//                    for (MacroArm arm : ga.population) {
//                        double evaluation = ga.evaluate(arm, map);
//                        ga.receiveReward(evaluation, arm);
//                        i++;
//                    }
//                    ga.evolve();
//                    MacroArm macroArm = ga.getMostVisited();
//                    writer.write(i + "," + Arrays.toString(macroArm.values) + "\n");
//                    writer.flush();
//                }
//                writer.close();
//            }
//        }
//    }
//}
