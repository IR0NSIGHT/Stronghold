package me.iron.stronghold.mod.utility;

import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import me.iron.stronghold.mod.implementation.*;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.world.VoidSystem;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

public class DebugUI implements CommandInterface {
    HashSet<String> classNames = new HashSet<>();
    public DebugUI() {
        //effects
        classNames.add(SelectiveVoidShield.class.getName());
        classNames.add(LongRangeScannerEffect.class.getName());
        classNames.add(PveShield.class.getName());
        classNames.add(WelcomeMessageEffect.class.getName());
        classNames.add(VoidShield.class.getName());

        //areas
        classNames.add(SemiProtectedArea.class.getName());
        classNames.add(StrongholdArea.class.getName());
        classNames.add(PveArea.class.getName());
        classNames.add(ControlZoneArea.class.getName());
    }

    @Override
    public String getCommand() {
        return "stronghold_debug";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"stdb"};
    }

    @Override
    public String getDescription() {
        return "debug command:" +
                "\n<pve/hold> [system/surround] 10 10 10 <name> (create pve protected area with x sectors/systems and name)" +
                "\nremove <UID> (remove/delete area/object with this UID)" +
                "\nclasses: prints all classes available to 'add' commands"+
                "\nadd <Classname simple> <UID> [params]: add new Object of this class as child to UID" +
                "\nprint (print all areas)" +
                "\nget_area (print what area i am in right now)" +
                "\nsave" +
                "\nload" +
                "\nclear (clear all)";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public boolean onCommand(PlayerState playerState, String[] strings) {
        //get_area
        if (strings.length==1) {
            if (strings[0].equalsIgnoreCase("get_area")) {
                echo("get area",playerState);
                StringBuilder out = new StringBuilder("Player " + playerState.getName() + " in sector " + playerState.getCurrentSector() + " in areas:\n");
                LinkedList<StellarControllableArea> as = ModMain.areaManager.getAreaFromSector(playerState.getCurrentSector());
                for (StellarControllableArea a: as)
                    out.append(a.getName()).append("\n");
                echo(out.toString(),playerState);
                return true;
            }
        }
        if (strings.length==1&&strings[0].equalsIgnoreCase("classes")) {
            //print all classes available to "add" command


        }

        if (strings.length>=3 && strings[0].equalsIgnoreCase("add")) {
            LinkedList<String> args = new LinkedList<>(Arrays.asList(strings));
            args.removeFirst();args.removeFirst();args.removeFirst();//remove idx 0,1,3 (add classname UID)
            try {
                boolean success = addChild(strings[1],Long.parseLong(strings[2]),args);
                if (success) {
                    echo("instantiated object of class "+ strings[1]+ " as child of UID "+strings[2],playerState);
                    return true;
                }else {
                    echo("instantiation failed without an error.",playerState);
                    return false;
                }
            }catch (Exception e) {
                echo("Error for instantiation: "+ e.getMessage()+"\ncause:"+e.getCause(),playerState);
                return false;
            }

        }

        //pve system 10 10 10 "my home" //pve sector -10 1 3
        if (strings.length==6 && (strings[0].equalsIgnoreCase("pve")||strings[0].equalsIgnoreCase("hold"))) {
            int multiply = 1;
            Vector3i start = new Vector3i(playerState.getCurrentSector());
            int x = Integer.parseInt(strings[2]), y =Integer.parseInt(strings[3]), z = Integer.parseInt(strings[4]);
            Vector3i end = new Vector3i(x,y,z);

            if (strings[1].equalsIgnoreCase("system"))
                multiply = VoidSystem.SYSTEM_SIZE;
            else if (strings[1].equalsIgnoreCase("surround")) {
                multiply = 2;
                start.sub(end); //expand in both directions.
            }
            end.scale(multiply);
            end.add(start);
            String name = strings[5];
            echo("create area from sector "+start+" to "+end+" with name "+ name,playerState);
            SendableUpdateable a = null;
            if (strings[0].equalsIgnoreCase("pve")) {
                a = new PveArea(start,end,name);
            } else if (strings[0].equalsIgnoreCase("hold")) {
                a = new StrongholdArea(start,end);
            }
            if (a != null) {
                ModMain.areaManager.addChildObject(a);
                echo("area "+a,playerState);
                return true;
            } else {
                echo("error, idk :(",playerState);
                return false;
            }

        }
        if (strings.length==2 && strings[0].equalsIgnoreCase("remove")) {
            long UID = Long.parseLong(strings[1]);
            echo("removing object with UID:" + ModMain.areaManager.printObject(UID),playerState);
            //remove
            ModMain.areaManager.removeObject(UID);
            return true;
        }
        //print_all
        if (strings.length==1&&strings[0].contains("print")) {
            String o = ModMain.areaManager.printObject(ModMain.areaManager);
            o = o.replace("\t","__");
            echo("\n"+o,playerState);
            return true;
        }
        if (strings.length==1&&strings[0].equalsIgnoreCase("save")) {
            ModMain.areaManager.save();
            echo("saving sthold areas.",playerState);
            return true;
        }
        if (strings.length==1&&strings[0].equalsIgnoreCase("load")) {
            ModMain.areaManager.getChildren().clear();
            ModMain.areaManager.load();
            echo("loaded sthold areas.",playerState);
            return true;
        }
        if (strings.length==1 && strings[0].equalsIgnoreCase("clear")) {
            ModMain.areaManager.clear();
            echo("clearing area manager: "+ModMain.areaManager,playerState);
            return true;
        }
        if (strings.length==4 && strings[0].equalsIgnoreCase("conquer")) {
            //conquer UID faction cascade => conquer 4 -1 true
            try {
                long UID = Long.parseLong(strings[1]);
                int faction = Integer.parseInt(strings[2]);
                boolean cascade = Boolean.parseBoolean(strings[3]);
                echo("conquer area " +UID+" for faction "+ faction+" cascade: "+ cascade,playerState);
                return conquerArea(UID,faction,cascade);
            } catch (Exception e) {
                echo("Execption caught:"+e.getCause()+"\n"+e.getMessage(),playerState);
            }
        }
        return false;
    }

    private boolean conquerArea(long UID, int faction, boolean cascade) {
        SendableUpdateable su = ModMain.areaManager.getObjectFromUID(UID);
        if (su instanceof AbstractControllableArea) {
            if (cascade)
                for (SendableUpdateable c: ((AbstractControllableArea) su).getChildren()) {
                    conquerArea(c.getUID(), faction, true);
                }
            ((AbstractControllableArea) su).setOwnerFaction(faction);
            return true;
        }
        return false;
    }

    private Collection<String> getClasses() {

        return new LinkedList<String>(Arrays.asList(SelectiveVoidShield.class.getSimpleName()));
    }

    private boolean addChild(String className, long UID, Collection<String> args) {
        return false;
    }

    @Override
    public void serverAction(@Nullable PlayerState playerState, String[] strings) {

    }

    @Override
    public StarMod getMod() {
        return null;
    }

    private void echo(String mssg, PlayerState p) {
        //System.out.println("[TO:"+p.getName()+"]"+ mssg);
        PlayerUtils.sendMessage(p,mssg);
    }
}
