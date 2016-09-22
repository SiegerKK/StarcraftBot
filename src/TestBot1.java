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
    private Map<Unit, ArrayList<Unit>> refineries;
    private Map<UnitType, Unit> workersIsComingToBuild;
    private ArrayList<Unit> buildings;
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

        //----------//
        botsUnits = new HashMap<String, Integer>();
        refineries = new HashMap<Unit, ArrayList<Unit>>();
        workersIsComingToBuild = new HashMap<UnitType, Unit>();

        botsUnits.put("SCV", 4);
        botsUnits.put("Marine", 0);
        botsUnits.put("Medic", 0);

        botsUnits.put("Comand Center", 1);
        botsUnits.put("Refinery", 0);
        botsUnits.put("Suply Depot", 0);
        botsUnits.put("Barracks", 0);
        botsUnits.put("Academy", 0);

        workersIsComingToBuild.put(UnitType.Terran_Command_Center, null);
        workersIsComingToBuild.put(UnitType.Terran_Refinery, null);
        workersIsComingToBuild.put(UnitType.Terran_Supply_Depot, null);
        workersIsComingToBuild.put(UnitType.Terran_Barracks, null);
        workersIsComingToBuild.put(UnitType.Terran_Academy, null);

        buildings = new ArrayList<>();
        builders = new ArrayList<>();
        //----------//

        //Use BWTA to analyze map
        //This may take a few minutes if the map is processed first time!
        System.out.println("Analyzing map...");
        BWTA.readMap();
        BWTA.analyze();
        System.out.println("Map data ready");

        //Find enemy base
        for (BaseLocation base : BWTA.getBaseLocations()){
            if(base.isStartLocation()) {
                if(!self.getStartLocation().equals(base.getTilePosition())) {
                    enemyBasePosition = base.getPosition();
                    break;
                }
            }
        }
        //---//

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
        //---------//--Calculate-units--//----------//

        botsUnits.replace("SCV", 0);
        botsUnits.replace("Marine", 0);
        botsUnits.replace("Medic", 0);
        botsUnits.replace("Comand Center", 0);
        botsUnits.replace("Refinery", 0);
        botsUnits.replace("Suply Depot", 0);
        botsUnits.replace("Barracks", 0);
        botsUnits.replace("Academy", 0);

        buildings.clear();
        builders.clear();

        for (UnitType type : workersIsComingToBuild.keySet()){
            if(workersIsComingToBuild.get(type) != null)
                if(workersIsComingToBuild.get(type).isIdle())
                    workersIsComingToBuild.replace(type, null);
        }
        for (Unit refinery : refineries.keySet()){
            for (Unit worker : refineries.get(refinery)){
                if(worker == null)
                    refineries.get(refinery).remove(worker);
            }
        }

        for (Unit myUnit : self.getUnits()) {
            if(myUnit.getType() == UnitType.Terran_SCV) {
                botsUnits.replace("SCV", botsUnits.get("SCV") + 1);

                //if SCV is constructing
                if(myUnit.isConstructing()) {
                    Unit building = myUnit.getBuildUnit();
                    if(building == null){
                        System.out.print("Building - null\n");
                    } else {
                        buildings.add(building);
                        builders.add(myUnit);

                        if(building.getType().equals(UnitType.Terran_Command_Center))
                            workersIsComingToBuild.replace(UnitType.Terran_Command_Center, null);
                        else if(building.getType().equals(UnitType.Terran_Refinery)) {
                            workersIsComingToBuild.replace(UnitType.Terran_Refinery, null);
                            if(!refineries.containsKey(myUnit.getBuildUnit())){
                                refineries.put(myUnit.getBuildUnit(), new ArrayList<>());
                            }
                        }
                        else if(building.getType().equals(UnitType.Terran_Supply_Depot))
                            workersIsComingToBuild.replace(UnitType.Terran_Supply_Depot, null);
                        else if(building.getType().equals(UnitType.Terran_Barracks))
                            workersIsComingToBuild.replace(UnitType.Terran_Barracks, null);
                        else if(building.getType().equals(UnitType.Terran_Academy))
                            workersIsComingToBuild.replace(UnitType.Terran_Academy, null);
                    }
                }

                //----------//
                for (ArrayList<Unit> workers : refineries.values()){
                    for (Unit worker : workers){
                        if (!worker.isGatheringGas() && !worker.isCarryingGas()){
                            workers.remove(worker);
                        }
                    }
                }
                //----------//


            } else if(myUnit.getType() == UnitType.Terran_Marine) {
                botsUnits.replace("Marine", botsUnits.get("Marine") + 1);
            } else if(myUnit.getType() == UnitType.Terran_Medic) {
                botsUnits.replace("Medic", botsUnits.get("Medic") + 1);
            } else if(myUnit.getType() == UnitType.Terran_Command_Center) {
                botsUnits.replace("Comand Center", botsUnits.get("Comand Center") + 1);
            } else if(myUnit.getType() == UnitType.Terran_Refinery) {
                botsUnits.replace("Refinery", botsUnits.get("Refinery") + 1);
            } else if(myUnit.getType() == UnitType.Terran_Supply_Depot) {
                botsUnits.replace("Suply Depot", botsUnits.get("Suply Depot") + 1);
            } else if(myUnit.getType() == UnitType.Terran_Barracks) {
                botsUnits.replace("Barracks", botsUnits.get("Barracks") + 1);
            } else if(myUnit.getType() == UnitType.Terran_Academy) {
                botsUnits.replace("Academy", botsUnits.get("Academy") + 1);
            }
        }

        //---------//
        game.drawTextScreen(10, 10, "Playing as " + self.getName() + " - " + self.getRace());
        game.drawTextScreen(160, 10, "Resources: " + self.minerals() + " minerals | " + self.gas() + " gas | " + self.supplyTotal() + " suplies");

        StringBuilder units = new StringBuilder("Units:\n");
        for(String unitType : botsUnits.keySet()){
            units.append("\t" + unitType + ": " + botsUnits.get(unitType) + "\n");
        }
        game.drawTextScreen(10, 25, units.toString());

        int i = 0;
        game.drawTextScreen(200, 25, "Buildings:");
        for (Unit buildingName : buildings){
            game.drawTextScreen(220, 40 + (i * 15), buildingName.getType() + " - " + builders.get(i).getType());
            i++;
        }
        //---------//


        //---------//-------------------//----------//

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

                //---Comand Center
                //if there's enough minerals, train an SCV
                if ((myUnit.getType() == UnitType.Terran_Command_Center)) {
                    if ((self.supplyTotal() - self.supplyUsed() > 2) && (self.minerals() >= 50) && (!myUnit.canCancelTrainSlot(2))) {
                        if (botsUnits.get("SCV") / botsUnits.get("Comand Center") < 16) {
                            boolean train = myUnit.train(UnitType.Terran_SCV);
                        }
                    }
                }
                //---//

                //---Refinery
                if(myUnit.getType() == UnitType.Terran_Refinery){
                    //nothing
                }
                //---//

                //---Barracks
                //if there's enough minerals, train an Marines
                if ((myUnit.getType() == UnitType.Terran_Barracks)) {
                    boolean train = false;
                    if(!myUnit.canCancelTrainSlot(0)) {
                        if ((self.minerals() >= 50) && (self.gas() >= 25) &&
                                (botsUnits.get("Marine").doubleValue() / botsUnits.get("Medic").doubleValue() > 3) &&
                                (myUnit.canTrain(UnitType.Terran_Medic))){
                            train = myUnit.train(UnitType.Terran_Medic);
                        } else if ((self.minerals() >= 50)) {
                            train = myUnit.train(UnitType.Terran_Marine);
                        }
                    }
                }
                //---//

                //---Academy
                //if there's enough minerals, train an Marines
                if ((myUnit.getType() == UnitType.Terran_Academy)) {
                    if((self.minerals() >= 150) && (self.gas() >= 150) && (!self.isUpgrading(UpgradeType.U_238_Shells))) {
                        boolean research = myUnit.upgrade(UpgradeType.U_238_Shells);
                    }
                }
                //---//

                //---SCV
                if (myUnit.getType() == UnitType.Terran_SCV) {

                    //SCV builds Suply
                    boolean buildingSuply = false;
                    if(workersIsComingToBuild.get(UnitType.Terran_Supply_Depot) != null){
                        buildingSuply = true;
                    } else {
                        for (Unit buildingName : buildings) {
                            if (buildingName.getType().equals(UnitType.Terran_Supply_Depot)) buildingSuply = true;
                        }
                    }
                    if ((!buildingSuply) && (self.supplyTotal() - self.supplyUsed() <= 2) && (self.minerals() >= 100)) {
                        TilePosition buildTile = getBuildTile(myUnit, UnitType.Terran_Supply_Depot, self.getStartLocation());
                        System.out.print("Terran_SCV try build Terran_Suply_Depot - ");
                        boolean result = myUnit.build(UnitType.Terran_Supply_Depot, buildTile);
                        System.out.print(result + " " + buildTile.toString() + "\n");
                        if(result) {
                            workersIsComingToBuild.replace(UnitType.Terran_Supply_Depot, myUnit);
                        }
                    }

                    //SCV builds Barrack
                    boolean buildingBarracks = false;
                    if(workersIsComingToBuild.get(UnitType.Terran_Barracks) != null){
                        buildingBarracks = true;
                    } else {
                        for (Unit buildingName : buildings) {
                            if (buildingName.getType().equals(UnitType.Terran_Barracks)) buildingBarracks = true;
                        }
                    }
                    if (((!botsUnits.containsKey("Barracks")) || (botsUnits.get("Barracks") < 4)) && (!buildingBarracks) && (botsUnits.get("SCV") >= 10) && (self.minerals() >= 150)) {
                        TilePosition buildTile = getBuildTile(myUnit, UnitType.Terran_Barracks, self.getStartLocation());
                        System.out.print("Terran_SCV try build Terran_Barrackss - ");
                        boolean result = myUnit.build(UnitType.Terran_Barracks, buildTile);
                        System.out.print(result + " " + buildTile.toString() + "\n");
                        if(result) {
                            workersIsComingToBuild.replace(UnitType.Terran_Barracks, myUnit);
                        }
                    }

                    //SCV builds Refinery
                    boolean buildingRefinery = false;
                    if(workersIsComingToBuild.get(UnitType.Terran_Refinery) != null){
                        buildingRefinery = true;
                    } else {
                        for (Unit buildingName : buildings) {
                            if (buildingName.getType().equals(UnitType.Terran_Refinery)) buildingRefinery = true;
                        }
                    }
                    if ((botsUnits.get("Refinery") < botsUnits.get("Comand Center")) && (!buildingRefinery) && (botsUnits.get("SCV") >= 10) && (self.minerals() >= 100)) {
                        TilePosition buildTile = getBuildTile(myUnit, UnitType.Terran_Refinery, self.getStartLocation());
                        System.out.print("Terran_SCV try build Terran_Refinery - ");
                        boolean result = myUnit.build(UnitType.Terran_Refinery, buildTile);
                        System.out.print(result + " " + buildTile.toString() + "\n");
                        if(result) {
                            workersIsComingToBuild.replace(UnitType.Terran_Refinery, myUnit);
                        }
                    }

                    //SCV builds Academy
                    boolean buildingAcademy = false;
                    if(workersIsComingToBuild.get(UnitType.Terran_Academy) != null){
                        buildingAcademy = true;
                    } else {
                        for (Unit buildingName : buildings) {
                            if (buildingName.getType().equals(UnitType.Terran_Academy)) buildingAcademy = true;
                        }
                    }
                    if ((myUnit.canBuild(UnitType.Terran_Academy)) && (botsUnits.get("Academy") < 1) && (!buildingAcademy) && (botsUnits.get("SCV") >= 10) && (self.minerals() >= 150)) {
                        TilePosition buildTile = getBuildTile(myUnit, UnitType.Terran_Academy, self.getStartLocation());
                        System.out.print("Terran_SCV try build Terran_Academy - ");
                        boolean result = myUnit.build(UnitType.Terran_Academy, buildTile);
                        System.out.print(result + " " + buildTile.toString() + "\n");
                        if(result) {
                            workersIsComingToBuild.replace(UnitType.Terran_Academy, myUnit);
                        }
                    }

                    //----------//

                    //Chekicng Refineries
                    for (Unit refinery : refineries.keySet()) {
                        System.out.print("Refinery - " + refineries.get(refinery).size() + "\n");
                    }

                    if(myUnit.isIdle() || myUnit.isGatheringMinerals()) {
                        for (Unit refinery : refineries.keySet()) {
                            if (refineries.get(refinery).size() < 3) {
                                //myUnit.canGather(refinery);
                                myUnit.gather(refinery);
                                refineries.get(refinery).add(myUnit);
                                break;
                            }
                        }
                    }

                    //if it's a worker and it's idle, send it to the closest mineral patch
                    if (myUnit.isIdle()) {

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
                //---//

                //---Marine
                if (myUnit.getType() == UnitType.Terran_Marine) {
                    if(botsUnits.get("Marine") >= 36){
                        if(myUnit.isIdle())
                            myUnit.attack(enemyBasePosition);
                    }
                }
                //---//

                //---Medic
                if (myUnit.getType() == UnitType.Terran_Medic) {
                    if(botsUnits.get("Marine") >= 36){
                        if(myUnit.isIdle())
                            myUnit.attack(enemyBasePosition);
                    }
                }
                //---//
            }
        }
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













