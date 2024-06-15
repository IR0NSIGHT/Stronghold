package me.iron.stronghold.mod.AlienStation;

import api.mod.StarMod;
import api.utils.game.chat.CommandInterface;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import me.iron.stronghold.mod.utility.SimpleTools;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.catalog.CatalogPermission;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.game.common.data.player.faction.FactionRoles;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.network.objects.Sendable;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;

import static me.iron.stronghold.mod.utility.DebugUI.echo;

public class AlienAreaCommand implements CommandInterface {
    @Override
    public String getCommand() {
        return "AlienArea";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"alien"};
    }

    @Override
    public String getDescription() {
        return "/AlienArea create 6 # creates an area with radius 6 sectors around the selected station \n" +
                "/AlienArea list # list all regions of type AlienArea\n" +
                "/AlienArea faction \"<name>\" \"<description>\" \"leadername\"# add a non-public faction with this name, faction leader will be ADMIN_alien\n" +
                "/AlienArea lootFreq <set|get> <minutes> # set/get  the frequency at which loot regenerates in stations to this value\n" +
                "/AlienArea guardian_blueprint_add expression # add all blueprints that match the regex expression";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    public Faction addNonPublicFaction(String name, String description, String leader) {
        if (name.isEmpty()) {
            return null;
        }
        Faction faction = new Faction(GameServerState.instance, FactionManager.getNewId(), name, description);
        faction.setOpenToJoin(false);
        faction.addOrModifyMember("ADMIN", leader, FactionRoles.INDEX_ADMIN_ROLE, System.currentTimeMillis(), GameServerState.instance.getGameState(), false);
        GameServerState.instance.getGameState().getFactionManager().addFaction(faction);
        return faction;
    }

    @Override
    public boolean onCommand(PlayerState playerState, String[] strings) {
        switch (strings[0]) {
            case "create": {
                if (strings.length == 1) {
                    echo("expected more argumetns",playerState);
                    return false;
                }
                Sendable s = SimpleTools.getSelectedObject(playerState);
                if (!(s instanceof SpaceStation)) {
                    echo("selected object must be a space station.", playerState);
                    return false;
                }
                echo("create alien area around station: ", playerState);
                ModMain.areaManager.addChildObject(AlienArea.aroundSpaceStation((SpaceStation) s, Integer.parseInt(strings[1])));
                return true;
            }
            case "list": {
                StringBuilder b = new StringBuilder("all alien areas:");
                for (SendableUpdateable area : ModMain.areaManager.getChildren()) {
                    if (area instanceof AlienArea)
                        b.append(area.toString()).append("\n");
                }

                echo(b.toString(), playerState);
                return true;
            }
            case "faction": {
                if (strings.length != 4) {
                    echo("expect 3 arguments: name, description, leader", playerState);
                    return false;
                }
                Faction faction = addNonPublicFaction(strings[1], strings[2], strings[3]);
                echo("Added faction:" + faction.toString(), playerState);
                return true;
            }
            case "loot": {
                if (!(strings.length == 2 && Objects.equals(strings[1], "get")) && !(strings.length == 3 && Objects.equals(strings[1], "set"))) {
                    echo("incorrect number of arguments", playerState);
                    return false;
                }
                boolean set = strings[1].equals("set");

                if (set) {
                    try {
                        float minutes = Float.parseFloat(strings[2]);
                        AlienArea.lootFrequencyMinutes = minutes;
                    } catch (NumberFormatException ex) {
                        echo(ex.getMessage(), playerState);
                        return false;
                    }
                }
                echo("AlienArea lootFrequencyMinutes = " + AlienArea.lootFrequencyMinutes, playerState);
                return true;
            }
            case "guardian_blueprint_add": {
                String expression = strings[1];

                LinkedList<CatalogPermission> matches = new LinkedList<>();
                Collection<CatalogPermission> bps = GameServerState.instance.getCatalogManager().getCatalog();
                for (CatalogPermission p : bps) {
                    String uid = p.getUid();
                    if (uid.toLowerCase().matches(expression.toLowerCase()))
                        matches.add(p);
                }
                echo("expression matched these blueprints:", playerState);

                AlienGuardian container = AlienGuardian.getInstance();
                container.guardianFaction = 10003;
                GameServerState.instance.getFactionManager().getFaction(container.guardianFaction).setAttackNeutral(false);
                container.catalogNames.clear();
                for (CatalogPermission p : matches) {
                    echo(p.getUid() + " : " + p.description, playerState);
                    container.catalogNames.add(p.getUid());
                }

                if (!container.catalogNames.isEmpty())
                    for (int i = 0; i < 2; i++) {
                        container.spawnGuardian(playerState.getCurrentSector());
                    }


                return true;
            }
            case "peace": {
                GameServerState.instance.getFactionManager().setRelationServer(AlienGuardian.getInstance().guardianFaction, playerState.getFactionId(), FactionRelation.RType.NEUTRAL.code);
                echo("made peace between player and guardian",playerState);
            }
        }

        echo("unknown subcommand.", playerState);
        return false;
    }

    @Override
    public void serverAction(@Nullable PlayerState playerState, String[] strings) {

    }

    @Override
    public StarMod getMod() {
        return ModMain.instance;
    }
}
