package me.iron.stronghold.mod.effects.map.SynchIcon;

import api.network.packets.PacketUtil;
import api.utils.StarRunnable;
import me.iron.stronghold.mod.ModMain;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.PlayerState;

import javax.vecmath.Vector4f;
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
            //DEBUG
            setPublicIcons(new LinkedList<>(Arrays.asList(
                    new SynchMapIcon(DEFAULT_ICON,
                            new Vector3i(100, 100, 100),
                            1,
                            new Vector4f(1, 1, 1, 1),
                            ANIMATION_NONE,
                            root,
                            -1,
                            false))));
        } else {
            this.mapDrawer = new SynchIconMapDrawer(ModMain.instance);

            new StarRunnable() {
                @Override
                public void run() {
                    RequestIconsFromServer(); //TODO dont always run
                    new StarRunnable() {
                        @Override
                        public void run() {

                            instance.updateClient();
                        }
                    }.runTimer(ModMain.instance, 100);
                }
            }.runLater(ModMain.instance, 500);
        }
    }

    public void UpdateClient(PlayerState playerState) {
        AddIconsToClients(icons.toArray(new SynchMapIcon[0]), Collections.singletonList(playerState));
    }

    private void updateClient() {
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


    //add these icons on this client machine
    public void AddIconsLocal(SynchMapIcon[] icons) {
        Collections.addAll(this.icons, icons);
        flagRedraw = true;
    }

    /**
     * send these icons to the specifies clients and display them there.
     * serverside
     *
     * @param icons
     */
    public void AddIconsToClients(SynchMapIcon[] icons, Collection<PlayerState> targetClients) {
        AddIconsPacket packet = new AddIconsPacket(icons);
        for (PlayerState client : targetClients) {
            PacketUtil.sendPacket(client, packet);
        }
    }

    public void setPublicIcons(LinkedList<SynchMapIcon> icons) {
        this.icons = icons;
    }

    public void RequestIconsFromServer() {
        PacketUtil.sendPacketToServer(new AddIconsPacket());
    }

}
