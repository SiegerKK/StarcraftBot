import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TestBot1 extends DefaultBWListener {

    private Mirror mirror = new Mirror();

    private Game game;

    private Player self;

    private Map<String, Integer> botsUnits;
    private ArrayList<String> buildings;
    private ArrayList<Unit> builders;
    private Position enemyBasePosition;

    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame();
    }

    @Override
    public void onUnitCreate(Unit unit) {
        System.out.println("New unit discovered " + unit.getType());
    }

    @Override
    public void onStart() {
        game = mirror.getGame();
        game.enableFlag(1);
        self = game.self();

        //Find enemy base
        for (BaseLocation base : BWTA.getBaseLocations()){
            if(base.isStartLocation())
                enemyBasePosition = base.getPosition();
        }

        //----------//
        botsUnits = new HashMap<String, Integer>();
        botsUnits.put("SCV", 4);
        botsUnits.put("ComandCenter", 4);

        buildings = new ArrayList<>();
        builders = new ArrayList<>();
        //----------//

        //Use BWTA to analyze map
        //This may take a few minutes if the map is processed first time!
        System.out.println("Analyzing map...");
        BWTA.readMap();
        BWTA.analyze();
        System.out.println("Map data ready");
        
        int i = 0;
        for(BaseLocation baseLocation : BWTA.getBaseLocations()){
        	System.out.println("Base location #" + (++i) + ". Printing location's region polygon:");
        	for(Position position : baseLocation.getRegion().getPolygon().getPoints()){
        		System.out.print(position + ", ");
        	}
        	System.out.println();
        }

    }

    @Override
    public void onFrame() {
        //game.setTextSize(10);
        game.drawTextScreen(10, 10, "Playing as " + self.getName() + " - " + self.getRace());
        game.drawTextScreen(200, 10, "Resources: " + self.minerals() + " minerals | " + self.gas() + " gas | " + self.supplyTotal() + " suplies");

        //---------//
        int i = 0;
        game.drawTextScreen(200, 25, "Buildings:");
        for (String buildingName : buildings){
            game.drawTextScreen(220, 40 + (i * 15), buildingName + " - " + builders.get(i));
            i++;
        }
        //---------//

        StringBuilder units = new StringBuilder("My units:\n");

        //iterate through my units
        for (Unit myUnit : self.getUnits()) {
            //If unit is busy for building - don't touch it
            boolean isBuilder = false;
            for (Unit builder : builders){
                if(builder.equals(myUnit))
                    isBuilder = true;
            }
            //---//
            if(!isBuilder) {
                units.append(myUnit.getType()).append(" ").append(myUnit.getTilePosition()).append("\n");

                //Checking buildings
                for (Unit unit : builders) {
                    if (unit.isIdle() || unit.isGatheringMinerals() || unit.isGatheringGas()) {
                        buildings.remove(builders.indexOf(unit));
                        builders.remove(unit);
                    }
                }
                //---//

                //---Comand Center
                //if there's enough minerals, train an SCV
                if ((myUnit.getType() == UnitType.Terran_Command_Center)) {
                    if ((self.supplyTotal() - self.supplyUsed() > 2) && (self.minerals() >= 50)) {
                        if (botsUnits.get("SCV") / botsUnits.get("ComandCenter") < 12) {
                            myUnit.train(UnitType.Terran_SCV);
                            if (botsUnits.containsKey("SCV")) {
                                botsUnits.replace("SCV", botsUnits.get("SCV") + 1);
                            } else {
                                botsUnits.put("SCV", 1);
                            }
                        }
                    }
                }
                //---//

                //---Barracks
                //if there's enough minerals, train an Marines
                if ((myUnit.getType() == UnitType.Terran_Barracks)) {
                    myUnit.train(UnitType.Terran_Marine);
                    if (botsUnits.containsKey("Marine")) {
                        botsUnits.replace("Marine", botsUnits.get("Marine") + 1);
                    } else {
                        botsUnits.put("Marine", 1);
                    }
                }
                //---//

                //---SCV
                if (myUnit.getType() == UnitType.Terran_SCV) {

                    //SCV builds Suply
                    boolean buildingSuply = false;
                    for (String buildingName : buildings) {
                        if (buildingName.equals("Suply Depot")) buildingSuply = true;
                    }
                    if ((!buildingSuply) && (self.supplyTotal() - self.supplyUsed() <= 2) && (self.minerals() >= 100)) {
                        TilePosition buildTile = getBuildTile(myUnit, UnitType.Terran_Supply_Depot, self.getStartLocation());
                        System.out.print("Terran_SCV try build Terran_Suply_Depot - ");
                        boolean result = myUnit.build(UnitType.Terran_Supply_Depot, buildTile);
                        System.out.print(result + " " + buildTile.toString() + "\n");
                        buildings.add("Suply Depot");
                        builders.add(myUnit);

                        if (botsUnits.containsKey("Suply Depot")) {
                            botsUnits.replace("Suply Depot", botsUnits.get("Suply Depot") + 1);
                        } else {
                            botsUnits.put("Suply Depot", 1);
                        }
                    }

                    //SCV builds Barrack
                    boolean buildingBarracks = false;
                    for (String buildingName : buildings) {
                        if (buildingName.equals("Barracks")) buildingBarracks = true;
                    }
                    if ((botsUnits.get("Barracks") < 4) && (!buildingBarracks) && (botsUnits.get("SCV") >= 10) && (self.minerals() >= 150)) {
                        TilePosition buildTile = getBuildTile(myUnit, UnitType.Terran_Barracks, self.getStartLocation());
                        System.out.print("Terran_SCV try build Terran_Barrackss - ");
                        boolean result = myUnit.build(UnitType.Terran_Barracks, buildTile);
                        System.out.print(result + " " + buildTile.toString() + "\n");
                        buildings.add("Barracks");
                        builders.add(myUnit);

                        if (botsUnits.containsKey("Barracks")) {
                            botsUnits.replace("Barracks", botsUnits.get("Barracks") + 1);
                        } else {
                            botsUnits.put("Barracks", 1);
                        }
                    }
                }
                //---//

                //---SCV
                if (myUnit.getType() == UnitType.Terran_Marine) {
                    if(botsUnits.get("Marine") >= 24){
                        myUnit.attack(enemyBasePosition);
                    }
                }
                //---//

                //if it's a worker and it's idle, send it to the closest mineral patch
                if (myUnit.getType().isWorker() && myUnit.isIdle()) {
                    Unit closestMineral = null;

                    //find the closest mineral
                    for (Unit neutralUnit : game.neutral().getUnits()) {
                        if (neutralUnit.getType().isMineralField()) {
                            if (closestMineral == null || myUnit.getDistance(neutralUnit) < myUnit.getDistance(closestMineral)) {
                                closestMineral = neutralUnit;
                            }
                        }
                    }

                    //if a mineral patch was found, send the worker to gather it
                    if (closestMineral != null) {
                        myUnit.gather(closestMineral, false);
                    }
                }
            }
        }

        //draw my units on screen
        game.drawTextScreen(10, 25, units.toString());
    }

    public static void main(String[] args) {
        new TestBot1().run();
    }

    //----------//----------//----------//

    public TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile){
        TilePosition ret = null;
        int maxDist = 3;
        int stopDist = 40;

        //Refinery
        if(buildingType.isRefinery()){
            for (Unit geyser : game.neutral().getUnits()){
                if((geyser.getType() == UnitType.Resource_Vespene_Geyser) &&
                        (geyser.getTilePosition().getX() - aroundTile.getX() < stopDist) &&
                        (geyser.getTilePosition().getY() - aroundTile.getY() < stopDist)){
                    return  geyser.getTilePosition();
                }
            }
        }

        //Other buildings
        while ((maxDist < stopDist) && (ret == null)) {
            for (int i = aroundTile.getX() - maxDist; i <= aroundTile.getX() + maxDist; i++) {
                for (int j = aroundTile.getY() - maxDist; j <= aroundTile.getY() + maxDist; j++) {
                    if (game.canBuildHere(new TilePosition(i, j), buildingType, builder, false)) {
                        // units that are blocking the tile
                        boolean unitsInWay = false;
                        for (Unit u : game.getAllUnits()) {
                            if (u.getID() == builder.getID()) continue;
                            if ((Math.abs(u.getTilePosition().getX() - i) < 4) && (Math.abs(u.getTilePosition().getY() - j) < 4)) {
                                unitsInWay = true;
                            }
                        }
                        if (!unitsInWay) {
                            ret = new TilePosition(i, j);
                        }
                        // creep for Zerg
                        if (buildingType.requiresCreep()) {
                            boolean creepMissing = false;
                            for (int k = i; k <= i + buildingType.tileWidth(); k++) {
                                for (int l = j; l <= j + buildingType.tileHeight(); l++) {
                                    if (!game.hasCreep(k, l)) creepMissing = true;
                                    break;
                                }
                            }
                            if (creepMissing) continue;
                        }
                    }
                }
            }
            maxDist += 2;
        }
        if (ret == null) game.printf("Unable to find suitable build position for " + buildingType.toString());
        return ret;
    }
}













