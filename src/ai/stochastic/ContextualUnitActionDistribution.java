/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.stochastic;

import ai.machinelearning.bayes.TrainingInstance;
import ai.machinelearning.bayes.featuregeneration.FeatureGenerator;
import ai.machinelearning.bayes.featuregeneration.FeatureGeneratorEmpty;
import optimalplayout.NormalizedStandAloneCMAB;
import optimalplayout.StandAloneContextualCMAB;
import rts.GameState;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author santi
 */
public class ContextualUnitActionDistribution extends UnitActionProbabilityDistribution {

    public StandAloneContextualCMAB contextualCMAB;
    FeatureGenerator fg = new FeatureGeneratorEmpty();
    public boolean LEARNING = false;
    float epsilon_l = 0.3f;
    float epsilon_g = 0.0f;
    float epsilon_0 = .5f;
    List<NormalizedStandAloneCMAB.MacroArm> learningArms = new ArrayList<>();
    List<NormalizedStandAloneCMAB> learningCMAB = new ArrayList<>();
    HashMap<String, NormalizedStandAloneCMAB.MacroArm> currArms = new HashMap<>();

    public ContextualUnitActionDistribution(UnitTypeTable a_utt, StandAloneContextualCMAB ccmab) throws Exception {
        super(a_utt);
        contextualCMAB = ccmab;
    }

    public String gsToFeature(GameState gs, Unit u, UnitAction ua) throws Exception {
        List<Object> features = fg.generateFeatures(new TrainingInstance(gs, u.getID(), ua));
//        System.out.println(features);
        return features.toString();
//        return "same";
    }

    public double[] predictDistribution(Unit u, GameState gs, List<UnitAction> actions) throws Exception {
        String key = gsToFeature(gs, u, new UnitAction(0));
        int[] m_distribution = new int[contextualCMAB.armNValues.length];
        synchronized (contextualCMAB) {
            if (LEARNING) {
                if (!contextualCMAB.cmabMap.containsKey(key)) {
                    contextualCMAB.putKey(key);
                }
                NormalizedStandAloneCMAB cmab;
                if (currArms.containsKey(key)) {
                    m_distribution = currArms.get(key).values;
                } else {
                    cmab = contextualCMAB.cmabMap.get(key);
                    NormalizedStandAloneCMAB.MacroArm marm = cmab.pullArm(epsilon_l, epsilon_g, epsilon_0);
                    learningArms.add(marm);
                    learningCMAB.add(cmab);
                    currArms.put(key, marm);
                }

            } else {
                if (contextualCMAB.cmabMap.containsKey(key)) {
                    m_distribution = contextualCMAB.cmabMap.get(key).getMostVisited().values;
                }
            }
        }
        int nActions = actions.size();
        double distribution[] = new double[nActions];
        double accum = 0;
        int worker_type = utt.getUnitType("Worker").ID;
        int barracks_type = utt.getUnitType("Barracks").ID;
        int base_type = utt.getUnitType("Base").ID;
        if (contextualCMAB.armNValues.length == 6) {
            for (int i = 0; i < nActions; i++) {
                int type = actions.get(i).getType();
                distribution[i] = m_distribution[type];
                accum += distribution[i];
            }
            if (accum <= 0) {
                for (int i = 0; i < nActions; i++) distribution[i] = 1.0 / nActions;
            } else {
                for (int i = 0; i < nActions; i++) distribution[i] /= accum;
            }
        }
        if (contextualCMAB.armNValues.length == 9) {
            for (int i = 0; i < nActions; i++) {
                UnitAction ua = actions.get(i);
                int type = ua.getType();
                switch (type) {
                    case UnitAction.TYPE_MOVE: {
                        double closest_enemy_d = 0;
                        Unit closest_enemy = null;
                        boolean moving_to_enemy = false;
                        double closest_resource_d = 0;
                        Unit closest_resource = null;
                        boolean moving_to_resource = false;
                        double closest_base_d = 0;
                        Unit closest_base = null;
                        boolean moving_to_base = false;
                        for (Unit u2 : gs.getUnits()) {
                            if (u2.getPlayer() == -1) {
                                // resource:
                                double d = Math.abs(u.getX() - u2.getX()) + Math.abs(u.getY() - u2.getY());
                                if (closest_resource == null || d < closest_resource_d) {
                                    closest_resource_d = d;
                                    closest_resource = u2;
                                }
                            } else if (u2.getPlayer() == u.getPlayer()) {
                                // friendly:
                                if (u2.getType().ID == base_type) {
                                    double d = Math.abs(u.getX() - u2.getX()) + Math.abs(u.getY() - u2.getY());
                                    if (closest_base == null || d < closest_base_d) {
                                        closest_base_d = d;
                                        closest_base = u2;
                                    }
                                }
                            } else {
                                // enemy:
                                double d = Math.abs(u.getX() - u2.getX()) + Math.abs(u.getY() - u2.getY());
                                if (closest_enemy == null || d < closest_enemy_d) {
                                    closest_enemy_d = d;
                                    closest_enemy = u2;
                                }
                            }
                        }

                        if (u.getResources() == 0) {
                            // no resources:
                            if (closest_enemy != null &&
                                    ((closest_enemy.getX() - u.getX()) * UnitAction.DIRECTION_OFFSET_X[ua.getDirection()] > 0 ||
                                            (closest_enemy.getY() - u.getY()) * UnitAction.DIRECTION_OFFSET_Y[ua.getDirection()] > 0)) {
                                moving_to_enemy = true;
                            }
                            if (closest_resource != null &&
                                    ((closest_resource.getX() - u.getX()) * UnitAction.DIRECTION_OFFSET_X[ua.getDirection()] > 0 ||
                                            (closest_resource.getY() - u.getY()) * UnitAction.DIRECTION_OFFSET_Y[ua.getDirection()] > 0)) {
                                moving_to_resource = true;
                            }

                            if (moving_to_enemy && moving_to_resource) {
                                if (closest_enemy_d > closest_resource_d) {
                                    moving_to_enemy = false;
                                } else {
                                    moving_to_resource = false;
                                }
                            }

                            if (moving_to_enemy) {
                                distribution[i] = m_distribution[1];
                            } else if (moving_to_resource) {
                                distribution[i] = m_distribution[2];
                            } else {
                                distribution[i] = m_distribution[4];
                            }
                        } else {
                            // resources:
                            if (closest_base != null &&
                                    ((closest_base.getX() - u.getX()) * UnitAction.DIRECTION_OFFSET_X[ua.getDirection()] > 0 ||
                                            (closest_base.getY() - u.getY()) * UnitAction.DIRECTION_OFFSET_Y[ua.getDirection()] > 0)) {
                                moving_to_base = true;
                            }

                            if (moving_to_base) {
                                distribution[i] = m_distribution[3];
                            } else {
                                distribution[i] = m_distribution[4];
                            }
                        }
                    }
                    break;
                    case UnitAction.TYPE_HARVEST:
                        distribution[i] = m_distribution[5];
                        break;
                    case UnitAction.TYPE_RETURN:
                        distribution[i] = m_distribution[6];
                        break;
                    case UnitAction.TYPE_PRODUCE:
                        distribution[i] = m_distribution[7];
                        break;
                    case UnitAction.TYPE_ATTACK_LOCATION:
                        distribution[i] = m_distribution[8];
                        break;
                    default:    // UnitAction.TYPE_NONE
                        distribution[i] = m_distribution[0];
                        break;

                }
                accum += distribution[i];
            }
        }

        if (contextualCMAB.armNValues.length == 12) {
            for (int i = 0; i < nActions; i++) {
                UnitAction ua = actions.get(i);
                int type = ua.getType();
                switch (type) {
                    case UnitAction.TYPE_MOVE: {
                        double closest_enemy_d = 0;
                        Unit closest_enemy = null;
                        boolean moving_to_enemy = false;
                        double closest_resource_d = 0;
                        Unit closest_resource = null;
                        boolean moving_to_resource = false;
                        double closest_base_d = 0;
                        Unit closest_base = null;
                        boolean moving_to_base = false;
                        for (Unit u2 : gs.getUnits()) {
                            if (u2.getPlayer() == -1) {
                                // resource:
                                double d = Math.abs(u.getX() - u2.getX()) + Math.abs(u.getY() - u2.getY());
                                if (closest_resource == null || d < closest_resource_d) {
                                    closest_resource_d = d;
                                    closest_resource = u2;
                                }
                            } else if (u2.getPlayer() == u.getPlayer()) {
                                // friendly:
                                if (u2.getType().ID == base_type) {
                                    double d = Math.abs(u.getX() - u2.getX()) + Math.abs(u.getY() - u2.getY());
                                    if (closest_base == null || d < closest_base_d) {
                                        closest_base_d = d;
                                        closest_base = u2;
                                    }
                                }
                            } else {
                                // enemy:
                                double d = Math.abs(u.getX() - u2.getX()) + Math.abs(u.getY() - u2.getY());
                                if (closest_enemy == null || d < closest_enemy_d) {
                                    closest_enemy_d = d;
                                    closest_enemy = u2;
                                }
                            }
                        }

                        if (u.getResources() == 0) {
                            // no resources:
                            if (closest_enemy != null &&
                                    ((closest_enemy.getX() - u.getX()) * UnitAction.DIRECTION_OFFSET_X[ua.getDirection()] > 0 ||
                                            (closest_enemy.getY() - u.getY()) * UnitAction.DIRECTION_OFFSET_Y[ua.getDirection()] > 0)) {
                                moving_to_enemy = true;
                            }
                            if (closest_resource != null &&
                                    ((closest_resource.getX() - u.getX()) * UnitAction.DIRECTION_OFFSET_X[ua.getDirection()] > 0 ||
                                            (closest_resource.getY() - u.getY()) * UnitAction.DIRECTION_OFFSET_Y[ua.getDirection()] > 0)) {
                                moving_to_resource = true;
                            }

                            if (moving_to_enemy && moving_to_resource) {
                                if (closest_enemy_d > closest_resource_d) {
                                    moving_to_enemy = false;
                                } else {
                                    moving_to_resource = false;
                                }
                            }

                            if (moving_to_enemy) {
                                distribution[i] = m_distribution[1];
                            } else if (moving_to_resource) {
                                distribution[i] = m_distribution[2];
                            } else {
                                distribution[i] = m_distribution[4];
                            }
                        } else {
                            // resources:
                            if (closest_base != null &&
                                    ((closest_base.getX() - u.getX()) * UnitAction.DIRECTION_OFFSET_X[ua.getDirection()] > 0 ||
                                            (closest_base.getY() - u.getY()) * UnitAction.DIRECTION_OFFSET_Y[ua.getDirection()] > 0)) {
                                moving_to_base = true;
                            }

                            if (moving_to_base) {
                                distribution[i] = m_distribution[3];
                            } else {
                                distribution[i] = m_distribution[4];
                            }
                        }
                    }
                    break;
                    case UnitAction.TYPE_HARVEST:
                        distribution[i] = m_distribution[5];
                        break;
                    case UnitAction.TYPE_RETURN:
                        distribution[i] = m_distribution[6];
                        break;
                    case UnitAction.TYPE_PRODUCE:
                        if (ua.getType() == worker_type) distribution[i] = m_distribution[7];
                        else if (ua.getType() == barracks_type) distribution[i] = m_distribution[8];
                        else if (ua.getType() == base_type) distribution[i] = m_distribution[9];
                        else distribution[i] = m_distribution[10];
                        break;
                    case UnitAction.TYPE_ATTACK_LOCATION:
                        distribution[i] = m_distribution[11];
                        break;
                    default:    // UnitAction.TYPE_NONE
                        distribution[i] = m_distribution[0];
                        break;

                }
                accum += distribution[i];
            }

            if (accum <= 0) {
                // if 0 accum, then just make uniform distribution:
                for (int i = 0; i < nActions; i++) distribution[i] = 1.0 / nActions;
            } else {
                for (int i = 0; i < nActions; i++) distribution[i] /= accum;
            }
        }
        return distribution;
    }

    public void receiveRewards(double reward) {
        for (int i = 0; i < learningCMAB.size(); i++) {
            learningCMAB.get(i).receiveReward(reward, learningArms.get(i));
        }
        learningCMAB.clear();
        learningArms.clear();
        currArms.clear();
    }
}
