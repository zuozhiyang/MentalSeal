/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.abstraction;

import ai.abstraction.pathfinding.*;
import org.jdom.Element;
import rts.GameState;
import rts.PhysicalGameState;
import rts.ResourceUsage;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;
import util.XMLWriter;

/**
 *
 * @author santi
 */
public abstract class RndAbstractAction {
    
    Unit unit;
    
    public RndAbstractAction(Unit a_unit) {
        unit = a_unit;
    }

    
    public Unit getUnit() {
        return unit;
    }
    
    
    public void setUnit(Unit u) {
        unit = u;
    }
    
    
    public abstract boolean completed(GameState pgs);
    
    
    public UnitAction execute(GameState pgs){
    	return execute(pgs,null);
    };
    
    
    public abstract void toxml(XMLWriter w);
    
    
    public static RndAbstractAction fromXML(Element e, PhysicalGameState gs, UnitTypeTable utt)
    {
        PathFinding pf = null;
        String pfString = e.getAttributeValue("pathfinding");
        if (pfString != null) {
            if (pfString.equals("AStarPathFinding")) pf = new AStarPathFinding();
            if (pfString.equals("BFSPathFinding")) pf = new BFSPathFinding();
            if (pfString.equals("FloodFillPathFinding")) pf = new FloodFillPathFinding();
            if (pfString.equals("GreedyPathFinding")) pf = new GreedyPathFinding();
        }
        switch (e.getName()) {
            case "Attack":
                return new RndAttack(gs.getUnit(Long.parseLong(e.getAttributeValue("unitID"))),
                        gs.getUnit(Long.parseLong(e.getAttributeValue("target"))),
                        pf);
            case "Build":
                return new RndBuild(gs.getUnit(Long.parseLong(e.getAttributeValue("unitID"))),
                        utt.getUnitType(e.getAttributeValue("type")),
                        Integer.parseInt(e.getAttributeValue("x")),
                        Integer.parseInt(e.getAttributeValue("y")),
                        pf);
            case "Harvest":
                return new RndHarvest(gs.getUnit(Long.parseLong(e.getAttributeValue("unitID"))),
                        gs.getUnit(Long.parseLong(e.getAttributeValue("target"))),
                        gs.getUnit(Long.parseLong(e.getAttributeValue("base"))),
                        pf);
            case "Idle":
                return new RndIdle(gs.getUnit(Long.parseLong(e.getAttributeValue("unitID"))));
            case "Move":
                return new RndMove(gs.getUnit(Long.parseLong(e.getAttributeValue("unitID"))),
                        Integer.parseInt(e.getAttributeValue("x")),
                        Integer.parseInt(e.getAttributeValue("y")),
                        pf);
            case "Train":
                return new RndTrain(gs.getUnit(Long.parseLong(e.getAttributeValue("unitID"))),
                        utt.getUnitType(e.getAttributeValue("type")));
            default:
                return null;
        }
    }
    
    
    public abstract UnitAction execute(GameState pgs, ResourceUsage ru);
}
