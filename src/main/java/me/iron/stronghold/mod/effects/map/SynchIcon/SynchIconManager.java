package me.iron.stronghold.mod.effects.map.SynchIcon;

import api.network.packets.PacketUtil;
import api.utils.StarRunnable;
import me.iron.stronghold.mod.ModMain;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.GameServerState;

import java.util.*;

/**
 * core class for the SynchIcon utiltity
 * allows the server to tell clients "draw this icon for that sector on the map for x seconds"
 * does not require any additional logic on the client.
 */
public class SynchIconManager {
    public static final int DEFAULT_ICON = 0;
    public static final int ANIMATION_NONE = 0;
    public static final String[] root = new String[]{"root"};
    public static SynchIconManager instance;
    private LinkedList<SynchMapIcon> icons = new LinkedList<>();
    private SynchIconMapDrawer mapDrawer;
    private boolean flagRedraw;

    public SynchIconManager(boolean onServer) {
        instance = this;
        if (onServer) {
          //
        } else {
            this.mapDrawer = new SynchIconMapDrawer(ModMain.instance);

            new StarRunnable() {
                @Override
                public void run() {
                    RequestIconsFromServer(); //request once on server join
                    new StarRunnable() {
                        @Override
                        public void run() {
                            //localized update for timed icons
                            instance.localUpdate();
                        }
                    }.runTimer(ModMain.instance, 100);
                }
            }.runLater(ModMain.instance, 500);
        }
    }

    public void remoteUpdateEveryone() {
        for (PlayerState p: GameServerState.instance.getPlayerStatesByName().values())
            remoteUpdateFor(p);
    }
    public void remoteUpdateFor(PlayerState playerState) {
        AddIconsToClients(icons.toArray(new SynchMapIcon[0]), Collections.singletonList(playerState));
    }

    private void localUpdate() {
        removeExpiredIcons();

        if (flagRedraw)
            RedrawIcons();
    }

    private void RedrawIcons() {
        mapDrawer.setIcons(icons);
        flagRedraw = false;
    }

    private void removeExpiredIcons() {
        long currentMillis = System.currentTimeMillis();
        Iterator<SynchMapIcon> iterator = icons.iterator();
        while (iterator.hasNext()) {
            SynchMapIcon icon = iterator.next();
            if (icon.expirationUnixTime != -1 && currentMillis > icon.expirationUnixTime) {
                iterator.remove();
                flagRedraw = true;
            }
        }
    }

    private boolean categoryMatch(SynchMapIcon icon1, SynchMapIcon icon2) {
        for (int i= 0; i < Math.min(icon1.category.length, icon2.category.length); i++) {
            if (!Objects.equals(icon1.category[i], icon2.category[i]))
                return false;
        }
        return true;
    }

    //add these icons on this client machine
    public void AddIconsLocal(SynchMapIcon[] icons) {
        for (SynchMapIcon newIcon: icons) {
            //remove all existing entries
            Iterator<SynchMapIcon> iterator = this.icons.iterator();
            while (iterator.hasNext()) {
                SynchMapIcon existingIcon = iterator.next();
                if (categoryMatch(newIcon, existingIcon)) {
                    iterator.remove();
                }
            }
            this.icons.add(newIcon);
        }
        flagRedraw = true;
    }

    /**
     * send these icons to the specifies clients and display them there.
     * serverside
     *
     * @param icons
     */
    public void AddIconsToClients(SynchMapIcon[] icons, Collection<PlayerState> targetClients) {
        SynchIconsPacket packet = new SynchIconsPacket(icons);
        for (PlayerState client : targetClients) {
            PacketUtil.sendPacket(client, packet);
        }
    }

    public void setPublicIcons(LinkedList<SynchMapIcon> icons) {
        this.icons = icons;
    }

    public void RequestIconsFromServer() {
        PacketUtil.sendPacketToServer(new SynchIconsPacket());
    }



}
