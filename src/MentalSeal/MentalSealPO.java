package MentalSeal;

import ai.RandomBiasedAI;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.GameState;
import rts.PartiallyObservableGameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MentalSealPO extends AIWithComputationBudget {
    UnitTypeTable utt;
    boolean started = false;
    AI ai;
    int[] arm = null;

    public MentalSealPO(int timeBudget, int iterationsBudget) {
        super(timeBudget, iterationsBudget);
    }

    public MentalSealPO(UnitTypeTable utt) {
        super(100, -1);
        this.utt = utt;
    }

    @Override
    public void reset() {
        this.started = false;
    }

    @Override
    public PlayerAction getAction(int i, GameState gameState) throws Exception {
        if (gameState.canExecuteAnyAction(i)) {
            if (!started) {
                ai = new GuidedGreedyNaiveMCTS(getTimeBudget(), -1, 100, 100,
                        .3f, 0.0f, .66f, .66f, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(),
                        new AI[]{
                                new OptimizablePORush(utt, new AStarPathFinding(), arm),
                        },
                        true);
                started = true;
            }
            return ai.getAction(i, gameState);
        } else {
            return new PlayerAction();
        }
    }

    @Override
    public AI clone() {
        return new MentalSealPO(utt);
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        return null;
    }

    @Override
    public void preGameAnalysis(GameState gs, long milliseconds, String folder) throws Exception {
        long start = System.currentTimeMillis();
        boolean visualize = false;
        float epsilon_l = .33f;
        float epsilon_g = .0f;
        float epsilon_0 = .4f;
        Random r = new Random();
        List<String> maps = new ArrayList<>();
        maps.add("maps/8x8/basesWorkers8x8A.xml");
        maps.add("maps/16x16/basesWorkers16x16A.xml");
        maps.add("maps/BWDistantResources32x32.xml");
        maps.add("maps/BroodWar/(4)BloodBath.scmB.xml");
        maps.add("maps/8x8/FourBasesWorkers8x8.xml");
        maps.add("maps/16x16/TwoBasesBarracks16x16.xml");
        maps.add("maps/NoWhereToRun9x8.xml");
        maps.add("maps/DoubleGame24x24.xml");
        List<int[]> arms = new ArrayList<>();
        arms.add(new int[]{0, 1, 2, 0, 0});
        arms.add(new int[]{2, 1, 0, 1, 0});
        arms.add(new int[]{1, 1, 1, 1, 0});
        arms.add(new int[]{4, 2, 1, 2, 0});
        arms.add(new int[]{1, 2, 1, 0, 2});
        arms.add(new int[]{2, 0, 0, 1, 0});
        arms.add(new int[]{1, 0, 0, 1, 2});
        arms.add(new int[]{5, 0, 1, 2, 1});
        double kernel[] = {0, 1, 0};
        PhysicalGameState pgs = gs.getPhysicalGameState();
        for (int i = 0; i < maps.size(); i++) {
            if (PhysicalGameState.load(maps.get(i), gs.getUnitTypeTable()).equivalents(gs.getPhysicalGameState())) {
                arm = arms.get(i);
                return;
            }
        }
        if (milliseconds < 10000) {
            int gs_str = gs.toString().hashCode();
            BufferedReader reader = new BufferedReader(new FileReader(folder + "/" + gs_str));
            String arm_str = reader.readLine();
            arm = new int[5];
            for (int i = 0; i < arm_str.split(", ").length; i++) {
                arm[i] = Integer.parseInt(arm_str.split(", ")[i]);
            }
            return;
        }

        int arm_config[] = new int[]{5, 3, 3, 3, 3};
        StandAloneCMAB cmab1 = new StandAloneCMAB(true, arm_config, kernel);
        StandAloneCMAB cmab2 = new StandAloneCMAB(true, arm_config, kernel);
        while (System.currentTimeMillis() - start < milliseconds) {
            float f = (milliseconds - (System.currentTimeMillis() - start)) / (float) milliseconds;
            float actual_epsilon_0 = epsilon_0 * f;
            StandAloneCMAB.MacroArm marm1 = null;
            StandAloneCMAB.MacroArm marm2 = null;
            marm1 = cmab1.pullArm(epsilon_l, epsilon_g, actual_epsilon_0);
            marm2 = cmab2.pullArm(epsilon_l, epsilon_g, actual_epsilon_0);
            ArrayList<Double> rewards = new ArrayList<>();
            ArrayList<Thread> threads = new ArrayList<>();
            int pp1_d[] = new int[marm1.values.length];
            int pp2_d[] = new int[marm2.values.length];
            for (int i = 0; i < marm1.values.length; i++) {
                pp1_d[i] = (marm1.values[i]);
                pp2_d[i] = (marm2.values[i]);
            }
            PhysicalGameState pgs2 = pgs.clone();
            for (int j = 0; j < 4; j++) {
                Runnable runnable =
                        () -> {
                            try {
                                UnitTypeTable utt = new UnitTypeTable();
                                AI ai1 = new OptimizableRush(utt, new AStarPathFinding(), pp1_d);
                                AI ai2 = new OptimizableRush(utt, new AStarPathFinding(), pp2_d);
                                double reward = 0.5;
                                if (r.nextBoolean()) {
                                    GameState endState = runGame(ai1, ai2, pgs2, 6000, visualize, utt);
                                    if (endState.winner() == 0) reward = 1.0;
                                    if (endState.winner() == 1) reward = 0.0;
                                } else {
                                    GameState endState = runGame(ai2, ai1, pgs2, 6000, visualize, utt);
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
            cmab1.receiveReward(reward, marm1);
            cmab2.receiveReward(1 - reward, marm2);
        }
        StandAloneCMAB.MacroArm arm1 = cmab1.getMostVisited();
        StandAloneCMAB.MacroArm arm2 = cmab2.getMostVisited();
        if (arm1.visit_count > arm2.visit_count) {
            arm = arm1.values;
        } else {
            arm = arm2.values;
        }
        int gs_str = gs.toString().hashCode();
        BufferedWriter writer = new BufferedWriter(new FileWriter(folder + "/" + gs_str));
        String arm_str = Arrays.toString(arm);
        writer.write(arm_str.substring(1, arm_str.length() - 1));
        writer.flush();
        writer.close();
    }

    public static GameState runGame(AI ai1, AI ai2, PhysicalGameState pgs, int maxcycles, boolean visualize, UnitTypeTable
            utt) throws Exception {
        GameState gs = new GameState(pgs, utt);
        boolean gameover;
        do {
            PlayerAction pa1 = ai1.getAction(0, gs);
            PlayerAction pa2 = ai2.getAction(1, gs);
            gs.issueSafe(pa1);
            gs.issueSafe(pa2);
            gameover = gs.cycle();
        } while (!gameover && gs.getTime() < maxcycles);
        ai1.gameOver(gs.winner());
        ai2.gameOver(gs.winner());
        return gs;
    }
}
