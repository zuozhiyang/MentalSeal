/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MentalSeal;

//import org.apache.commons.math3.distribution.BetaDistribution;
import util.Sampler;
import java.io.BufferedWriter;
import java.math.BigInteger;
import java.util.*;

/**
 * @author santi
 */
public class StandAloneCMAB {

    public class Arm {
        public int nValues = 0;
        public double[] accum_evaluation;
        public double[] visit_count;
        public double[] alpha;
        public double[] beta;
    }

    public class MacroArm {
        public MacroArm(int v[], BigInteger c) {
            values = v;
            code = c;
        }

        public int values[];
        public BigInteger code;
        public double accum_evaluation = 0;
        public int visit_count = 0;
    }

    static public int DEBUG = 0;

    Random r = new Random();
    boolean forceExplorationOfNonSampledActions = true;
    boolean hasMoreActions = true;
    HashMap<BigInteger, MacroArm> childrenMap = new LinkedHashMap<>();    // associates action codes with macro arms
    public List<Arm> armTable;
    public List<MacroArm> globalMABarms;
    public BigInteger multipliers[];
    double kernel[];


    public StandAloneCMAB(boolean fensa, int armNValues[], double k[]) throws Exception {
        forceExplorationOfNonSampledActions = fensa;

        armTable = new LinkedList<>();
        multipliers = new BigInteger[armNValues.length];
        kernel = k;
        if ((kernel.length % 2 != 1)) throw new Exception("Kernel should have an odd size");
        BigInteger baseMultiplier = BigInteger.ONE;
        int idx = 0;
        for (int nValues : armNValues) {
            Arm arm = new Arm();
            arm.nValues = nValues;
            arm.accum_evaluation = new double[arm.nValues];
            arm.visit_count = new double[arm.nValues];
            arm.alpha = new double[arm.nValues];
            arm.beta = new double[arm.nValues];
            for (int i = 0; i < arm.nValues; i++) {
                arm.accum_evaluation[i] = 0;
                arm.visit_count[i] = 0;
                arm.alpha[i] = 1;
                arm.beta[i] = 1;
            }
            armTable.add(arm);
            multipliers[idx] = baseMultiplier;
            baseMultiplier = baseMultiplier.multiply(BigInteger.valueOf(arm.nValues));
            idx++;
        }
        globalMABarms = new ArrayList<>();
    }


    // Naive Sampling:
    public MacroArm pullArm(float epsilon_l, float epsilon_g, float epsilon_0) throws Exception {
        if (globalMABarms.size() > 0 && r.nextFloat() >= epsilon_0) {
            // sample from the global MAB:
            return selectFromAlreadySampledEpsilonGreedy(epsilon_g);
        } else {
            // sample from the local MABs (this might recursively call "selectLeaf" internally):
            return selectLeafUsingLocalMABs(epsilon_l, epsilon_g, epsilon_0);
        }
    }

//    public MacroArm pullTSArm(float epsilon_l, float epsilon_g, float epsilon_0) throws Exception {
//        if (globalMABarms.size() > 0 && r.nextFloat() >= epsilon_0) {
//            // sample from the global MAB:
//            return selectFromAlreadySampledEpsilonGreedy(epsilon_g);
//        } else {
//            // sample from the local MABs (this might recursively call "selectLeaf" internally):
//            return selectLeafUsingLocalTSMABs(epsilon_l, epsilon_g, epsilon_0);
//        }
//
//    }


    public MacroArm selectFromAlreadySampledEpsilonGreedy(float epsilon_g) throws Exception {
        if (r.nextFloat() >= epsilon_g) {
            MacroArm best = null;
            for (MacroArm pate : globalMABarms) {
                // max node:
                if (best == null || (pate.accum_evaluation / pate.visit_count) > (best.accum_evaluation / best.visit_count)) {
                    best = pate;
                }
            }
            return best;
        } else {
            // choose one at random from the ones seen so far:
            MacroArm best = globalMABarms.get(r.nextInt(globalMABarms.size()));
            return best;
        }
    }


//    public MacroArm selectLeafUsingLocalTSMABs(float epsilon_l, float epsilon_g, float epsilon_0) throws Exception {
//        BigInteger actionCode;
//
//        // For each unit, rank the unitActions according to preference:
//        List<double[]> distributions = new LinkedList<>();
//        List<Integer> notSampledYet = new LinkedList<>();
//        for (Arm ate : armTable) {
//            double[] dist = new double[ate.nValues];
//            int bestIdx = -1;
//            double bestEvaluation = -100;
////            double visits = 0;
//            for (int i = 0; i < ate.nValues; i++) {
//                // max node:
//                if (bestIdx == -1) {
//                    bestIdx = i;
////                        System.out.println(ate.alpha[i] + " " + ate.beta[i]);
////                        bestEvaluation = new NormalDistribution(ate.alpha[i], ate.beta[i]).sample();
//                    bestEvaluation = new BetaDistribution(ate.alpha[i], ate.beta[i]).sample();
//                } else {
////                        System.out.println(ate.alpha[i]+" "+ ate.beta[i]+" "+ ate.visit_count[i]);
////                        double sample = new NormalDistribution(ate.alpha[i], ate.beta[i]).sample();
//                    double sample = new BetaDistribution(ate.alpha[i], ate.beta[i]).sample();
////                    System.out.println(ate.alpha[i] + "," + ate.beta[i] + "," + sample);
//
//                    if (sample > bestEvaluation) {
//                        bestIdx = i;
//                        bestEvaluation = sample;
////                            System.out.println(bestEvaluation);
//                    }
//                }
//
////                dist[i] = epsilon_l / ate.nValues;
//            }
//            dist[bestIdx] = 1;
////            if (ate.visit_count[bestIdx] != 0) {
////                dist[bestIdx] = (1 - epsilon_l) + (epsilon_l / ate.nValues);
////            } else {
////                if (forceExplorationOfNonSampledActions) {
////                    for (int j = 0; j < dist.length; j++)
////                        if (ate.visit_count[j] > 0) dist[j] = 0;
////                }
////            }
//
////            if (DEBUG >= 3) {
////                System.out.print("[ ");
////                for (int i = 0; i < ate.nValues; i++)
////                    System.out.print("(" + ate.visit_count[i] + "," + ate.accum_evaluation[i] / ate.visit_count[i] + ")");
////                System.out.println("]");
////                System.out.print("[ ");
////                for (double v : dist) System.out.print(v + " ");
////                System.out.println("]");
////            }
//
//            notSampledYet.add(distributions.size());
//            distributions.add(dist);
//        }
//
//        int values[] = new int[armTable.size()];
//        actionCode = BigInteger.ZERO;
//        while (!notSampledYet.isEmpty()) {
//            int i = notSampledYet.remove(r.nextInt(notSampledYet.size()));
//            double[] distribution = distributions.get(i);
//            int value = Sampler.weighted(distribution);
//            values[i] = value;
//            actionCode = actionCode.add(BigInteger.valueOf(value).multiply(multipliers[i]));
//        }
//
//        MacroArm pate = childrenMap.get(actionCode);
//        if (pate == null) {
//            pate = new MacroArm(values, actionCode);
//            globalMABarms.add(pate);
//            childrenMap.put(actionCode, pate);
//        }
//
//        return pate;
//    }

    public MacroArm selectLeafUsingLocalMABs(float epsilon_l, float epsilon_g, float epsilon_0) throws Exception {
        BigInteger actionCode;

        // For each unit, rank the unitActions according to preference:
        List<double[]> distributions = new LinkedList<>();
        List<Integer> notSampledYet = new LinkedList<>();
        for (Arm ate : armTable) {
            double[] dist = new double[ate.nValues];
            int bestIdx = -1;
            double bestEvaluation = 0;
            double visits = 0;
            for (int i = 0; i < ate.nValues; i++) {
                // max node:
                if (bestIdx == -1 ||
                        (visits != 0 && ate.visit_count[i] == 0) ||
                        (visits != 0 && (ate.accum_evaluation[i] / ate.visit_count[i]) > bestEvaluation)) {
                    bestIdx = i;
                    if (ate.visit_count[i] > 0) bestEvaluation = (ate.accum_evaluation[i] / ate.visit_count[i]);
                    else bestEvaluation = 0;
                    visits = ate.visit_count[i];
                }
                dist[i] = epsilon_l / ate.nValues;
            }
            if (ate.visit_count[bestIdx] != 0) {
                dist[bestIdx] = (1 - epsilon_l) + (epsilon_l / ate.nValues);
            } else {
                if (forceExplorationOfNonSampledActions) {
                    for (int j = 0; j < dist.length; j++)
                        if (ate.visit_count[j] > 0) dist[j] = 0;
                }
            }

            if (DEBUG >= 3) {
                System.out.print("[ ");
                for (int i = 0; i < ate.nValues; i++)
                    System.out.print("(" + ate.visit_count[i] + "," + ate.accum_evaluation[i] / ate.visit_count[i] + ")");
                System.out.println("]");
                System.out.print("[ ");
                for (double v : dist) System.out.print(v + " ");
                System.out.println("]");
            }

            notSampledYet.add(distributions.size());
            distributions.add(dist);
        }

        int values[] = new int[armTable.size()];
        actionCode = BigInteger.ZERO;
        while (!notSampledYet.isEmpty()) {
            int i = notSampledYet.remove(r.nextInt(notSampledYet.size()));
            double[] distribution = distributions.get(i);
            int value = Sampler.weighted(distribution);
            values[i] = value;
            actionCode = actionCode.add(BigInteger.valueOf(value).multiply(multipliers[i]));
        }

        MacroArm pate = childrenMap.get(actionCode);
        if (pate == null) {
            pate = new MacroArm(values, actionCode);
            globalMABarms.add(pate);
            childrenMap.put(actionCode, pate);
        }

        return pate;
    }


    public void receiveReward(double evaluation, MacroArm choice) {
        choice.accum_evaluation += evaluation;
        choice.visit_count++;
        double scaled_eval = r.nextDouble() > evaluation ? 0 : 1;

        // update the arm table:
        for (int i = 0; i < armTable.size(); i++) {
            int kernel_center = kernel.length / 2;
            for (int j = 0; j < kernel.length; j++) {
                int v = choice.values[i] + j - kernel_center;
                if (v >= 0 && v < armTable.get(i).accum_evaluation.length) {
                    armTable.get(i).accum_evaluation[v] += evaluation * kernel[j];
                    armTable.get(i).visit_count[v] += kernel[j];
                }
            }
            armTable.get(i).alpha[choice.values[i]] += scaled_eval;
            armTable.get(i).beta[choice.values[i]] += 1 - scaled_eval;
        }
    }

    public MacroArm getMostVisited() {
        MacroArm best = null;
        for (MacroArm globalMABarm : globalMABarms) {
            if (best == null || globalMABarm.visit_count > best.visit_count ||
                    (globalMABarm.visit_count == best.visit_count && globalMABarm.accum_evaluation / globalMABarm.visit_count > best.accum_evaluation / best.visit_count)) {
                best = globalMABarm;
            }
        }
        return best;
    }

    public void PrintMacroArmDebugInfo(boolean sorted, boolean sortByvisitCount, int topN) {
        List<MacroArm> toDisplay = globalMABarms;
        if (sorted) {
            toDisplay = new ArrayList<>();
            toDisplay.addAll(globalMABarms);
            if (sortByvisitCount) {
                Collections.sort(toDisplay, new Comparator<MacroArm>() {
                    @Override
                    public int compare(MacroArm o1, MacroArm o2) {
                        // compare them in reverse, so that they are sorted from larger to smaller
                        if (o2.visit_count == o1.visit_count) {
                            // resolve ties by score:
                            return Double.compare(o2.accum_evaluation / o2.visit_count, o1.accum_evaluation / o1.visit_count);
                        } else {
                            return Double.compare(o2.visit_count, o1.visit_count);
                        }
                    }
                });
            } else {
                Collections.sort(toDisplay, new Comparator<MacroArm>() {
                    @Override
                    public int compare(MacroArm o1, MacroArm o2) {
                        // compare them in reverse, so that they are sorted from larger to smaller
                        return Double.compare(o2.accum_evaluation / o2.visit_count, o1.accum_evaluation / o1.visit_count);
                    }
                });
            }
        }

        if (topN > 0) {
            List<MacroArm> macroArms = toDisplay.subList(0, Math.min(topN, toDisplay.size()));
            if (sortByvisitCount) {
                for (MacroArm marm : macroArms) {
                    System.out.println("best visit " + (macroArms.indexOf(marm) + 1) + ": " + (marm.accum_evaluation / marm.visit_count) + " - " + marm.visit_count + " --> " + Arrays.toString(marm.values));
                }
            } else {
                for (MacroArm marm : macroArms) {
                    System.out.println("best score " + (macroArms.indexOf(marm) + 1) + ": " + (marm.accum_evaluation / marm.visit_count) + " - " + marm.visit_count + " --> " + Arrays.toString(marm.values));
                }
            }
        } else {
            for (MacroArm marm : toDisplay) {
                System.out.println((marm.accum_evaluation / marm.visit_count) + " - " + marm.visit_count + " --> " + Arrays.toString(marm.values));
            }
        }
    }

    public void PrintMacroArmDebugInfo(boolean sorted, boolean sortByvisitCount, int topN, BufferedWriter writer) throws Exception {
        List<MacroArm> toDisplay = globalMABarms;
        if (sorted) {
            toDisplay = new ArrayList<>();
            toDisplay.addAll(globalMABarms);
            if (sortByvisitCount) {
                Collections.sort(toDisplay, new Comparator<MacroArm>() {
                    @Override
                    public int compare(MacroArm o1, MacroArm o2) {
                        // compare them in reverse, so that they are sorted from larger to smaller
                        if (o2.visit_count == o1.visit_count) {
                            // resolve ties by score:
                            return Double.compare(o2.accum_evaluation / o2.visit_count, o1.accum_evaluation / o1.visit_count);
                        } else {
                            return Double.compare(o2.visit_count, o1.visit_count);
                        }
                    }
                });
            } else {
                Collections.sort(toDisplay, new Comparator<MacroArm>() {
                    @Override
                    public int compare(MacroArm o1, MacroArm o2) {
                        // compare them in reverse, so that they are sorted from larger to smaller
                        return Double.compare(o2.accum_evaluation / o2.visit_count, o1.accum_evaluation / o1.visit_count);
                    }
                });
            }
        }

        if (topN > 0) {
            List<MacroArm> macroArms = toDisplay.subList(0, Math.min(5, toDisplay.size()));
            if (sortByvisitCount) {
                for (MacroArm marm : macroArms) {
                    writer.write("best visit " + (macroArms.indexOf(marm) + 1) + ": " + (marm.accum_evaluation / marm.visit_count) + " - " + marm.visit_count + " --> " + Arrays.toString(marm.values) + "\n");
                }
            } else {
                for (MacroArm marm : macroArms) {
                    writer.write("best score " + (macroArms.indexOf(marm) + 1) + ": " + (marm.accum_evaluation / marm.visit_count) + " - " + marm.visit_count + " --> " + Arrays.toString(marm.values) + "\n");
                }
            }
        } else {
            for (MacroArm marm : toDisplay) {
                writer.write((marm.accum_evaluation / marm.visit_count) + " - " + marm.visit_count + " --> " + Arrays.toString(marm.values) + "\n");
            }
        }
    }
}
