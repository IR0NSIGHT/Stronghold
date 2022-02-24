package me.iron.stronghold.mod.utility;

import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import me.iron.stronghold.mod.implementation.PveArea;
import me.iron.stronghold.mod.implementation.StellarControllableArea;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.world.VoidSystem;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedList;

public class DebugUI implements CommandInterface {
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
                "\npve [system] 10 10 10 <name> (create pve protected area with x sectors/systems and name)" +
                "\nremove <UID> (remove/delete area/object with this UID)" +
                "\nprint (print all areas)" +
                "\nget_area (print what area i am in right now)" +
                "\nsave" +
                "\nload" +
                "\nclear (clear all)";
    }

    @Override
    public boolean isAdminOnly() {
        return false;
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
        //pve system 10 10 10 "my home" //pve sector -10 1 3
        if (strings.length==6 && strings[0].equalsIgnoreCase("pve")) {
            int multiply = 1;
            if (strings[1].equalsIgnoreCase("system"))
                multiply = VoidSystem.SYSTEM_SIZE;
            Vector3i start = new Vector3i(playerState.getCurrentSector());
            int x = Integer.parseInt(strings[2]), y =Integer.parseInt(strings[3]), z = Integer.parseInt(strings[4]);
            Vector3i end = new Vector3i(x,y,z);
            end.scale(multiply);
            end.add(start);
            String name = strings[5];
            echo("create PVE area from sector "+start+" to "+end+" with name "+ name,playerState);
            PveArea a = new PveArea(start,end,name);
            ModMain.areaManager.addChildObject(a);
            echo("area "+a,playerState);
            return true;
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
        PlayerUtils.sendMessage(p,mssg);
    }
}
