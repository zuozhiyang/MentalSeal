//package optimalplayout;
//
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
//import java.io.File;
//import java.io.FileWriter;
//import java.util.*;
//
//import static optimalplayout.OptimalGamePolicy.runGame;
//
//public class ConvergenceCurveSH {
//    public static void main(String[] args) throws Exception {
//        List<String> maps = new ArrayList<>();
//        List<Integer> maxGameLength = new ArrayList<>();
//        Random r = new Random();
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
//        String dir = "gameplay-results-SH";
//        String[] pathnames;
//
//        File f = new File(dir);
//
//        pathnames = f.list();
//        BufferedWriter writer = new BufferedWriter(new FileWriter("evaluation-results/evaluations-SH.txt"));
//        BufferedWriter name = new BufferedWriter(new FileWriter("evaluation-results/name-SH.txt"));
//        HashMap<String, double[]> lookup = new HashMap<>();
//        for (String pathname : pathnames) {
//            if (!pathname.startsWith("console") && pathname.startsWith("SH")) {
//                System.out.println(pathname);
//                File myObj = new File(dir + "/" + pathname);
//                Scanner myReader = new Scanner(myObj);
//                String[] s = pathname.split("-");
//
//                String map = s[3];
////                String key = s[0]+"-"+s[3];
////                System.out.println(key);
////                System.out.println(Arrays.toString(s));
////                String key = s[0]+s[1]+"-"+(s[4].length()>5?s[4]:s[3]);
//                String key = s[0]+"-"+(s[3].length()>5?s[3]:s[2]);
////                System.out.println(key);
//                int m = 0;
//                for (int i = 0; i < maps.size(); i++) {
//                    if (maps.get(i).contains(map)) {
//                        m = i;
//                        break;
//                    }
//                }
//                HashMap<String, Double> history = new HashMap<>();
//                int j = 0;
//                double[] value = lookup.getOrDefault(key, new double[3]);
//                lookup.put(key,value);
//                while (myReader.hasNextLine()) {
//                    String[] data = myReader.nextLine().replaceAll("\\[|\\]| ", "").split("-");
//                    int[][] bandits = new int[6][];
//                    for (int i = 0; i < data.length; i++) {
//                        String[] row = data[i].split(",");
//                        bandits[i] = new int[row.length];
//                        for (int k = 0; k < row.length; k++) {
//                            bandits[i][k] = Integer.parseInt(row[k]);
//                        }
//                    }
//                    String historyKey = "";
//                    for (int i = 0; i < bandits.length; i++) {
//                        historyKey+= Arrays.toString(bandits[i]);
//                    }
//                    double evaluation = 0;
//                    if (history.containsKey(historyKey)) {
//                        evaluation = history.get(historyKey);
//                    } else {
//                        evaluation = parallelRun(20, 2000, bandits, maps.get(m), maxGameLength.get(m));
//                        history.put(historyKey, evaluation);
//                    }
//                    value[j] += evaluation;
//                    j++;
//                }
//            }
//        }
//        for (String key : lookup.keySet()) {
//            name.write(key + "\n");
//            for (Double value : lookup.get(key)) {
//                    writer.write((value / 10) + ",");
//            }
//            writer.write("\n");
//        }
//        writer.flush();
//        name.flush();
//        writer.close();
//        name.close();
//    }
//    public static double parallelRun(int CORES, int total, int[][] bandits, String map, Integer maxGameLength ) throws Exception {
//        UnitTypeTable utt = new UnitTypeTable();
//        Random r = new Random();
//        int[] values = new int[6];
//        ArrayList<Double> rewards = new ArrayList<>();
//        for (int k = 0; k < total; ) {
//            ArrayList<Thread> threads = new ArrayList<>();
//            for (int j = 0; j < CORES; j++) {
//                Runnable runnable =
//                        () -> {
//                            try {
//                                for (int i = 0; i < values.length; i++) {
//                                    values[i] = bandits[i][r.nextInt(bandits[i].length)];
//                                }
//                                double pp1_d[] = new double[6];
//                                for (int i = 0; i < UnitAction.NUMBER_OF_ACTION_TYPES; i++) {
//                                    pp1_d[i] = (double) (values[i] + 1.0);
//                                }
//                                UnitActionProbabilityDistributionAI pp1 = new UnitActionProbabilityDistributionAI(new UnitActionTypeConstantDistribution(utt, pp1_d), utt, "uapdai1");
//                                AI ai1 = pp1;
//                                AI ai2 = new RandomBiasedAI();
//                                double reward = 0.5;
//                                if (r.nextBoolean()) {
//                                    GameState endState = runGame(ai1, ai2, map, maxGameLength, false, utt, po);
//                                    if (endState.winner() == 0) reward = 1.0;
//                                    if (endState.winner() == 1) reward = 0.0;
//                                } else {
//                                    GameState endState = runGame(ai2, ai1, map, maxGameLength, false, utt, po);
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
//}
