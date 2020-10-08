/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MentalSeal;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

import java.util.*;


public class OptimizableRush extends RndAbstractionLayerAI {
    SimpleSqrtEvaluationFunction3 eval = new SimpleSqrtEvaluationFunction3();
    Random r = new Random();
    protected UnitTypeTable utt;
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType rangedType;
    UnitType lightType;
    UnitType heavyType;


    int resourcesUsed;
    int threshold_value = 0;

    int[] arm;

    int numHarvestWorker = 6;
    int numBase = 1;
    int numBarracks = 1;
    double[] weights = new double[3];
    double waving_threshold = 5;


    // If we have any unit for attack: send it to attack to the nearest enemy unit
    // If we have a base: train worker until we have 4 workers per base. The 4ª unit send to build a new base.
    // If we have a barracks: train light, Ranged and Heavy in order
    // If we have a worker: go to resources closest, build barracks, build new base closest harvest resources
    public OptimizableRush(UnitTypeTable a_utt) {
        this(a_utt, new AStarPathFinding());
    }

    public OptimizableRush(UnitTypeTable a_utt, PathFinding a_pf) {
        super(a_pf);
        reset(a_utt);
    }

    public OptimizableRush(UnitTypeTable a_utt, PathFinding a_pf, int[] arm) {
        super(a_pf);
        reset(a_utt);
        this.arm = arm;
        numHarvestWorker = arm[0] + 1;
        waving_threshold = arm[1] * 10;
        numBase = arm[2];
        numBarracks = arm[3];
        double light = arm[4];
        weights[0] = light / 2.0;
        weights[1] = 1 - weights[0];
    }

    public void reset() {
        super.reset();
    }

    public void reset(UnitTypeTable a_utt) {
        utt = a_utt;
        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        rangedType = utt.getUnitType("Ranged");
        lightType = utt.getUnitType("Light");
        heavyType = utt.getUnitType("Heavy");
    }

    @Override
    public PlayerAction getAction(int player, GameState gs) throws Exception {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
        resourcesUsed = gs.getResourceUsage().getResourcesUsed(player);
        threshold_value = 0;
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() == player) {
                if (u.getType().canAttack && !u.getType().canHarvest) {
                    threshold_value += u.getType().hp;
                }
            }
        }
        // behavior of bases:
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == baseType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
                baseBehavior(u, p, pgs);
            }
        }

        // behavior of barracks:
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == barracksType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
                barracksBehavior(u, p, pgs);
            }
        }

        // behavior of workers:
        List<Unit> workers = new ArrayList<Unit>();
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canHarvest
                    && u.getPlayer() == player
                    && u.getType() == workerType
//                    && gs.getActionAssignment(u) == null
            ) {
                workers.add(u);
            }
        }
        workersBehavior(workers, p, pgs);

        // behavior of melee units:
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canAttack && !u.getType().canHarvest
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
                if (threshold_value < waving_threshold ) {
                    defenceUnitBehavior(u, p, pgs);
                } else {
                    attackUnitBehavior(u, p, pgs);
                }
            }
        }

        return translateActions(player, gs);
    }

    @Override
    public AI clone() {
        return new OptimizableRush(utt, pf, arm);
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList<>();

        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));

        return parameters;
    }

    public void baseBehavior(Unit u, Player p, PhysicalGameState pgs) {
        int nworkers = 0;
        int nbases = 0;
        int nbarracks = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == workerType
                    && u2.getPlayer() == p.getID()) {
                nworkers++;
            }
            if (u2.getType() == baseType
                    && u2.getPlayer() == p.getID()) {
                nbases++;
            }
            if (u2.getType() == barracksType
                    && u2.getPlayer() == p.getID()) {
                nbarracks++;
            }
        }
//        System.out.println(numHarvestWorker+" "+nworkers);
        if ((nworkers < numHarvestWorker && p.getResources() >= (workerType.cost + resourcesUsed))
                || (numBarracks <= nbarracks && numBase <= nbases && p.getResources() >= (workerType.cost + resourcesUsed + 5))
                || (numBarracks == 0 && p.getResources() >= (workerType.cost + resourcesUsed))
        ) {
            train(u, workerType);
            resourcesUsed += workerType.cost;
        }
    }

    public void barracksBehavior(Unit u, Player p, PhysicalGameState pgs) {
        int nbases = 0;
        int nbarracks = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == baseType
                    && u2.getPlayer() == p.getID()) {
                nbases++;
            }
            if (u2.getType() == barracksType
                    && u2.getPlayer() == p.getID()) {
                nbarracks++;
            }
        }
        if (numBarracks <= nbarracks || numBase <= nbases) {
            double sample = r.nextDouble();
            double sum = 0;
            for (int i = 0; i < weights.length; i++) {
                sum += weights[i];
                if (sample < sum) {
                    if (i == 0) {
                        if (p.getResources() >= (rangedType.cost + resourcesUsed)) {
                            train(u, rangedType);
                            resourcesUsed += rangedType.cost;
                        }
                        break;
                    } else if (i == 1) {
                        if (p.getResources() >= (lightType.cost + resourcesUsed)) {
                            train(u, lightType);
                            resourcesUsed += lightType.cost;
                        }
                        break;
                    }
                }
            }
        }
    }

    public void attackUnitBehavior(Unit u, Player p, PhysicalGameState pgs) {
        Unit closestEnemy = null;
        int closestDistance = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy == null || d < closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestEnemy != null) {
            attack(u, closestEnemy);
        } else {
            move(u, u.getX() + r.nextInt(3) - 1, u.getY() + r.nextInt(3) - 1);
        }
    }

    public void defenceUnitBehavior(Unit u, Player p, PhysicalGameState pgs) {
        Unit closestEnemy = null;
        int closestDistance = 0;
        int mybase = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy == null || d < closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
            else if (u2.getPlayer() == p.getID() && u2.getType() == baseType) {
                mybase = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
            }
        }
        if (closestEnemy != null && (closestDistance < pgs.getHeight() / 3) ||  mybase < closestDistance/3) {
            attack(u, closestEnemy);
        } else {
            move(u, u.getX() + r.nextInt(3) - 1, u.getY() + r.nextInt(3) - 1);
        }
    }

    public void workersBehavior(List<Unit> workers, Player p, PhysicalGameState pgs) {
        int nbases = 0;
        int nbarracks = 0;
        List<Unit> freeWorkers = new ArrayList<Unit>();
        freeWorkers.addAll(workers);

        freeWorkers.sort((Unit u1, Unit u2) -> Long.signum(u1.getID() - (u2.getID())));

        if (workers.isEmpty()) {
            return;
        }
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == baseType
                    && u2.getPlayer() == p.getID()) {
                nbases++;
            }
            if (u2.getType() == barracksType
                    && u2.getPlayer() == p.getID()) {
                nbarracks++;
            }
        }


        List<Integer> reservedPositions = new ArrayList<Integer>();
        if (nbases < numBase && !freeWorkers.isEmpty()) {
            // build a base:
            if (p.getResources() >= baseType.cost + resourcesUsed) {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u, baseType, u.getX(), u.getY(), reservedPositions, p, pgs);
                resourcesUsed += baseType.cost;
            }
        }

        if (nbarracks < numBarracks && !freeWorkers.isEmpty()) {
            // build a barracks:
            if (p.getResources() >= barracksType.cost + resourcesUsed) {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u, barracksType, u.getX(), u.getY(), reservedPositions, p, pgs);
                resourcesUsed += barracksType.cost;
            }
        }
        List<Unit> harvestWorkers = new ArrayList<Unit>();
        while (harvestWorkers.size() < numHarvestWorker && !freeWorkers.isEmpty()) {
            Unit u = freeWorkers.remove(0);
            harvestWorkers.add(u);
        }
        harvestWorkers(harvestWorkers, p, pgs);
        for (Unit u : freeWorkers) {
            attackUnitBehavior(u, p, pgs);
        }

    }

    protected List<Unit> otherResourcePoint(Player p, PhysicalGameState pgs) {

        List<Unit> bases = getMyBases(p, pgs);
        Set<Unit> myResources = new HashSet<>();
        Set<Unit> otherResources = new HashSet<>();

        for (Unit base : bases) {
            List<Unit> closestUnits = new ArrayList<>(pgs.getUnitsAround(base.getX(), base.getY(), 10));
            for (Unit closestUnit : closestUnits) {
                if (closestUnit.getType().isResource) {
                    myResources.add(closestUnit);
                }
            }
        }

        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType().isResource) {
                if (!myResources.contains(u2)) {
                    otherResources.add(u2);
                }
            }
        }
        if (!bases.isEmpty()) {
            return getOrderedResources(new ArrayList<>(otherResources), bases.get(0));
        } else {
            return new ArrayList<>(otherResources);
        }
    }

    protected List<Unit> getOrderedResources(List<Unit> resources, Unit base) {
        List<Unit> resReturn = new ArrayList<Unit>();

        HashMap<Integer, ArrayList<Unit>> map = new HashMap<>();
        for (Unit res : resources) {
            int d = Math.abs(res.getX() - base.getX()) + Math.abs(res.getY() - base.getY());
            if (map.containsKey(d)) {
                ArrayList<Unit> nResourc = map.get(d);
                nResourc.add(res);
            } else {
                ArrayList<Unit> nResourc = new ArrayList<>();
                nResourc.add(res);
                map.put(d, nResourc);
            }
        }
        ArrayList<Integer> keysOrdered = new ArrayList<>(map.keySet());
        Collections.sort(keysOrdered);

        for (Integer key : keysOrdered) {
            for (Unit uTemp : map.get(key)) {
                resReturn.add(uTemp);
            }

        }

        return resReturn;

    }

    protected List<Unit> getMyBases(Player p, PhysicalGameState pgs) {

        List<Unit> bases = new ArrayList<>();
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == baseType
                    && u2.getPlayer() == p.getID()) {
                bases.add(u2);
            }
        }
        return bases;
    }

    protected void harvestWorkers(List<Unit> freeWorkers, Player p, PhysicalGameState pgs) {
        for (Unit u : freeWorkers) {
            Unit closestBase = null;
            Unit closestResource = null;
            int closestDistance = 0;
            for (Unit u2 : pgs.getUnits()) {
                if (u2.getType().isResource) {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestResource == null || d < closestDistance) {
                        closestResource = u2;
                        closestDistance = d;
                    }
                }
            }
            closestDistance = 0;
            for (Unit u2 : pgs.getUnits()) {
                if (u2.getType().isStockpile && u2.getPlayer() == p.getID()) {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestBase == null || d < closestDistance) {
                        closestBase = u2;
                        closestDistance = d;
                    }
                }
            }
            if (closestResource != null && closestBase != null) {
                RndAbstractAction aa = getAbstractAction(u);
                if (aa instanceof RndHarvest) {
                    RndHarvest h_aa = (RndHarvest) aa;
                    if (h_aa.getTarget() != closestResource || h_aa.getBase() != closestBase) {
                        harvest(u, closestResource, closestBase);
                    }
                } else {
                    harvest(u, closestResource, closestBase);
                }
            } else {
                attackUnitBehavior(u, p, pgs);
            }
        }
    }

}
