package me.iron.stronghold.mod.AlienStation;

import api.mod.StarMod;
import api.utils.game.chat.CommandInterface;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import me.iron.stronghold.mod.utility.SimpleTools;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.network.objects.Sendable;

import javax.annotation.Nullable;

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
                "/AlienArea list # list all regions of type AlienArea";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public boolean onCommand(PlayerState playerState, String[] strings) {
        switch (strings[0]) {
            case "create": {
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
        }
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
