package me.iron.stronghold.mod.voidshield;

import api.listener.Listener;
import api.listener.events.player.PlayerChangeSectorEvent;
import api.listener.events.systems.ShieldHitEvent;
import api.mod.StarLoader;
import api.utils.StarRunnable;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.effects.sounds.SoundManager;
import me.iron.stronghold.mod.framework.Stronghold;
import me.iron.stronghold.mod.framework.StrongholdController;
import org.schema.common.util.linAlg.Vector;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.network.server.ServerMessage;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 13.01.2022
 * TIME: 18:29
 */
public class VoidShield {
    public static int getRequiredPointsForShield() {
        return 0;
    }

    public static boolean isStrongholdShielded(Stronghold s) {
        return s.getDefensePoints()>=getRequiredPointsForShield();
    }

    public static boolean isSectorVoidShielded(Vector3i sector) {
        Stronghold hold = StrongholdController.getInstance().getStrongholdFromSector(sector);
   //     ModMain.log(sector+" sector->sys "+hold.getStellarPos());
        return isStrongholdShielded(hold)&& !hold.isStrongpoint(sector);
    }

    public static boolean isObjectVoidShielded(SimpleTransformableSendableObject object) {
        Vector3i sector = object.getSector(new Vector3i());
        Stronghold hold = StrongholdController.getInstance().getStrongholdFromSector(sector);
        return  isSectorVoidShielded(sector)&&
                !object.isHomeBase()&&
                hold.getOwner()!=0&&
                hold.getOwner()==object.getFactionId()&&
                object.getType().equals(SimpleTransformableSendableObject.EntityType.SPACE_STATION);
    }

    public static void initEHs() {
        StarLoader.registerListener(ShieldHitEvent.class, new Listener<ShieldHitEvent>() {
            @Override
            public void onEvent(ShieldHitEvent shieldHitEvent) {
                if (VoidShield.isObjectVoidShielded(shieldHitEvent.getHitController())) {
                    //    ModMain.log("cancel hit");
                    shieldHitEvent.setDamage(0);
                    shieldHitEvent.getShieldHit().hasHit=false;
                    shieldHitEvent.getShieldHit().setDamage(0);
                    shieldHitEvent.setCanceled(true);

                    shieldHitEvent.getShieldHit().damager.sendServerMessage(Lng.astr("system is voidshielded."), ServerMessage.MESSAGE_TYPE_ERROR);
                }
            }
        }, ModMain.instance);


    }

    public static void initClientEHs() {
        StarLoader.registerListener(PlayerChangeSectorEvent.class, new Listener<PlayerChangeSectorEvent>() {
            @Override
            public void onEvent(final PlayerChangeSectorEvent playerChangeSectorEvent) {
                new StarRunnable(){
                    @Override
                    public void run() {
                        Vector3i pos = GameClientState.instance.getPlayer().getCurrentSector();
                        boolean isS = isSectorVoidShielded(pos);
                        if (isSectorVoidShielded(pos))
                            SoundManager.instance.queueSound(SoundManager.Sound.system_shielded);
                    }
                }.runLater(ModMain.instance,10);
            }
        }, ModMain.instance);
    }
}
