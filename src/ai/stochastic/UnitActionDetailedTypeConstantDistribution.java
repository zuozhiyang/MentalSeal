/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.stochastic;

import rts.GameState;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;

import java.util.List;

/**
 *
 * @author santi
 * 
 * Action detailed types:
 * 0 None
 * 1 Move-to-enemy-no-resources
 * 2 Move-to-resource-no-resources
 * 3 Move-to-base-resources
 * 4 Move-other
 * 5 Harvest
 * 6 Return
 * 7 Produce
 * 8 Attack
 *
 */
public class UnitActionDetailedTypeConstantDistribution extends UnitActionProbabilityDistribution {

    double m_distribution[];
    int worker_type;
    int barracks_type;
    int base_type;
    
    public UnitActionDetailedTypeConstantDistribution(UnitTypeTable a_utt, double distribution[]) throws Exception {
        super(a_utt);
        
        worker_type = a_utt.getUnitType("Worker").ID;
        barracks_type = a_utt.getUnitType("Barracks").ID;
        base_type = a_utt.getUnitType("Base").ID;
        
        
        if (distribution==null || distribution.length != 9) throw new Exception("distribution does not have the right number of elements!");
        m_distribution = distribution;
    }
    
    
    @Override
    public double[] predictDistribution(Unit u, GameState gs, List<UnitAction> actions) throws Exception
    {
        int nActions = actions.size();
        double distribution[] = new double[nActions];
        double accum = 0;
        
        for(int i = 0;i<nActions;i++) {
            UnitAction ua = actions.get(i);
            int type = ua.getType();
            switch(type) {
                case UnitAction.TYPE_MOVE:
                    {
                        double closest_enemy_d = 0;
                        Unit closest_enemy = null;
                        boolean moving_to_enemy = false;
                        double closest_resource_d = 0;
                        Unit closest_resource = null;
                        boolean moving_to_resource = false;
                        double closest_base_d = 0;
                        Unit closest_base = null;
                        boolean moving_to_base = false;
                        for(Unit u2:gs.getUnits()) {
                            if (u2.getPlayer() == -1) {
                                // resource:
                                double d = Math.abs(u.getX()-u2.getX()) + Math.abs(u.getY()-u2.getY());
                                if (closest_resource == null || d<closest_resource_d) {
                                    closest_resource_d = d;
                                    closest_resource = u2;
                                }
                            } else if (u2.getPlayer() == u.getPlayer()) {
                                // friendly:
                                if (u2.getType().ID == base_type) {
                                    double d = Math.abs(u.getX()-u2.getX()) + Math.abs(u.getY()-u2.getY());
                                    if (closest_base == null || d<closest_base_d) {
                                        closest_base_d = d;
                                        closest_base = u2;
                                    }
                                }
                            } else {
                                // enemy:
                                double d = Math.abs(u.getX()-u2.getX()) + Math.abs(u.getY()-u2.getY());
                                if (closest_enemy == null || d<closest_enemy_d) {
                                    closest_enemy_d = d;
                                    closest_enemy = u2;
                                }
                            }
                        }

                        if (u.getResources() == 0) {
                            // no resources:
                            if (closest_enemy != null &&
                                ((closest_enemy.getX() - u.getX())* UnitAction.DIRECTION_OFFSET_X[ua.getDirection()] > 0 ||
                                 (closest_enemy.getY() - u.getY())* UnitAction.DIRECTION_OFFSET_Y[ua.getDirection()] > 0)) {
                                moving_to_enemy = true;
                            }
                            if (closest_resource != null &&
                                ((closest_resource.getX() - u.getX())* UnitAction.DIRECTION_OFFSET_X[ua.getDirection()] > 0 ||
                                 (closest_resource.getY() - u.getY())* UnitAction.DIRECTION_OFFSET_Y[ua.getDirection()] > 0)) {
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
                                ((closest_base.getX() - u.getX())* UnitAction.DIRECTION_OFFSET_X[ua.getDirection()] > 0 ||
                                 (closest_base.getY() - u.getY())* UnitAction.DIRECTION_OFFSET_Y[ua.getDirection()] > 0)) {
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
        
        if (accum <= 0) {
            // if 0 accum, then just make uniform distribution:
            for(int i = 0;i<nActions;i++) distribution[i] = 1.0/nActions;
        } else {
            for(int i = 0;i<nActions;i++) distribution[i] /= accum;
        }
        
        return distribution;    
    }   
    
}
