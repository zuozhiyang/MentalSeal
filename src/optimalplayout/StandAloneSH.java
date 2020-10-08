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
//public class StandAloneSH {
//
//    public class Arm {
//        public int nValues = 0;
//        public double[] accum_evaluation;
//        public double[] visit_count;
//    }
//
//    static List<String> maps = new ArrayList<>();
//    static List<Integer> maxGameLength = new ArrayList<>();
//
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
//    public ArrayList<ArrayList<Integer>> localArms;
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
//    public List<Arm> armTable;
//    int budget;
//    int curr_budget;
//    int round;
//
//    public StandAloneSH(int b) throws Exception {
//        budget = b;
//        localArms = new ArrayList<>();
//        for (int i = 0; i < 6; i++) {
//            localArms.add(new ArrayList<>());
//            for (int j = 0; j < 8; j++) {
//                localArms.get(i).add(j);
//            }
//        }
//        curr_budget = (int) (budget / (Math.log(8) / Math.log(2)));
//        round = budget / curr_budget;
//        population = new ArrayList<>();
//        for (int i = 0; i < curr_budget; i++) {
//            int[] v = new int[6];
//            for (int j = 0; j < v.length; j++) {
//                v[j] = r.nextInt(8);
//            }
//            population.add(new MacroArm(v));
//        }
//        armTable = new LinkedList<>();
//        for (int nValues : new int[]{8, 8, 8, 8, 8, 8}) {
//            Arm arm = new Arm();
//            arm.nValues = nValues;
//            arm.accum_evaluation = new double[arm.nValues];
//            arm.visit_count = new double[arm.nValues];
//            for (int i = 0; i < arm.nValues; i++) {
//                arm.accum_evaluation[i] = 0;
//                arm.visit_count[i] = 0;
//            }
//            armTable.add(arm);
//        }
//        globalMABarms = new ArrayList<>();
//    }
//
//
//    public void evolve() {
//        List<MacroArm> nextPopulation = new ArrayList<>();
//        for (Arm arm : armTable) {
//            PriorityQueue<double[]> queue = new PriorityQueue<>(new Comparator<double[]>() {
//                @Override
//                public int compare(double[] d1, double[] d2) {
//                    return Double.compare(d1[0], d2[0]);
//                }
//            });
//            int index = armTable.indexOf(arm);
//            for (int i = 0; i < arm.accum_evaluation.length; i++) {
//                if (localArms.get(index).contains(i))
//                    queue.add(new double[]{arm.accum_evaluation[i] / arm.visit_count[i], i});
//            }
//            int size = queue.size();
//            while (queue.size() > size / 2) {
//                double[] next = queue.poll();
////                System.out.println(Arrays.toString(next));
//                localArms.get(index).remove(localArms.get(index).indexOf((int) next[1]));
//            }
//        }
//        for (int i = 0; i < curr_budget; i++) {
//            int[] v = new int[6];
//            for (int j = 0; j < v.length; j++) {
//                v[j] = localArms.get(j).get(r.nextInt(localArms.get(j).size()));
//            }
////            System.out.println(Arrays.toString(v));
////            System.out.println(localArms);
//            if (childrenMap.containsKey(Arrays.toString(v))) {
//                nextPopulation.add(childrenMap.get(Arrays.toString(v)));
//            } else {
//                nextPopulation.add(new MacroArm(v));
//            }
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
//        round--;
//    }
//
//    public void receiveReward(double evaluation, MacroArm choice) {
//        choice.accum_evaluation += evaluation;
//        choice.visit_count++;
//
//        for (int i = 0; i < armTable.size(); i++) {
//            armTable.get(i).accum_evaluation[choice.values[i]] += evaluation;
//            armTable.get(i).visit_count[choice.values[i]] += 1;
//        }
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
//
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
//        double pp1_d[] = new double[6];
//        for (int i = 0; i < UnitAction.NUMBER_OF_ACTION_TYPES; i++) {
//            pp1_d[i] = (double) (arm.values[i] + 1.0);
//        }
//        UnitActionProbabilityDistributionAI pp1 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp1_d), utt, "uapdai1");
////        for (int i = 0; i < 10;) {
//        ArrayList<Thread> threads = new ArrayList<>();
//        for (int j = 0; j < CORES; j++) {
//            Runnable runnable =
//                    () -> {
//                        try {
//                            AI ai1 = pp1;
//                            AI ai2 = new RandomBiasedAI();
//                            double reward = 0.5;
//                            if (r.nextBoolean()) {
//                                GameState endState = runGame(ai1, ai2, maps.get(mapIdx), maxGameLength.get(mapIdx), false, utt, po);
//                                if (endState.winner() == 0) reward = 1.0;
//                                if (endState.winner() == 1) reward = 0.0;
//                            } else {
//                                GameState endState = runGame(ai2, ai1, maps.get(mapIdx), maxGameLength.get(mapIdx), false, utt, po);
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
////                i++;
//        }
//        for (Thread thread : threads) {
//            thread.join();
//        }
////        }
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
//        int iterations = 10000;
//        for (int batch = 0; batch < 10; batch++) {
//            for (int map = 0; map < maps.size(); map++) {
//                StandAloneSH ga = new StandAloneSH(iterations);
//                BufferedWriter writer = new BufferedWriter(new FileWriter("gameplay-results-SH/" + "SH" + "-" +
//                        maps.get(map).replaceAll("/", "-") + "-" + batch + ".txt"));
//                while (ga.round >= 1) {
//                    for (MacroArm arm : ga.population) {
//                        double evaluation = ga.evaluate(arm, map);
//                        ga.receiveReward(evaluation, arm);
//                    }
//                    ga.evolve();
//                    for (int i = 0; i < ga.localArms.size(); i++) {
//                        writer.write(ga.localArms.get(i) + "");
//                        if (i < ga.localArms.size() - 1) {
//                            writer.write("-");
//                        }
//                    }
//                    writer.write("\n");
//                }
//                writer.flush();
//                writer.close();
//            }
////            System.out.println("Iteration " + i + " ------------------ ");
////            System.out.println(ga.globalMABarms.size());
////            ga.PrintMacroArmDebugInfo(true, true, 5);
//        }
//
//    }
//}
