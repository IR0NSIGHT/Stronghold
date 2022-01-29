package me.iron.stronghold.mod.voidshield;

import api.utils.StarRunnable;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.effects.sounds.SoundManager;
import me.iron.stronghold.mod.framework.Stronghold;
import me.iron.stronghold.mod.framework.StrongholdController;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.controller.EditableSendableSegmentController;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.damage.DamageDealerType;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.controller.damage.HitType;
import org.schema.game.common.controller.damage.effects.InterEffectSet;
import org.schema.game.common.controller.elements.ShieldAddOn;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.data.GameServerState;

import javax.vecmath.Vector3f;
import java.util.LinkedList;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 13.01.2022
 * TIME: 18:29
 */
public class VoidShieldController {
    public static LinkedList<IVoidShieldEvent> eventlisteners = new LinkedList<>();
    public static double handleShieldHit(ShieldAddOn hitShield, Damager damager, InterEffectSet defenseSet, Vector3f hitPoint, int projectileSectorId, DamageDealerType damageType, HitType hitType, double damage, long weaponId) {
        if (isObjectVoidShielded(hitShield.getSegmentController())) {
            Stronghold h = StrongholdController.getInstance().getStrongholdFromSector(hitShield.getSegmentController().getSector(new Vector3i()));
            for (IVoidShieldEvent e: eventlisteners) {
                e.onShieldHit(h, hitShield.getSegmentController(), damager);
            }
            damage = 0;
        }

        return damage;
    }

    public static int getRequiredPointsForShield() {
        return 0;
    }

    public static boolean isStrongholdShielded(Stronghold s) {
        return s.getDefensePoints()>=getRequiredPointsForShield();
    }

    public static boolean isSectorVoidShielded(Vector3i sector) {
        Stronghold hold = StrongholdController.getInstance().getStrongholdFromSector(sector);
        return isStrongholdShielded(hold) && !hold.isStrongpoint(sector);
    }

    public static boolean isObjectVoidShielded(SegmentController sc) {
        if (!(sc instanceof EditableSendableSegmentController))
            return false;

        EditableSendableSegmentController object = (EditableSendableSegmentController) sc;
        if (sc.railController.isDocked()) {
            sc = object.railController.getRoot();
            return isObjectVoidShielded(sc);
        }

        Vector3i sector = object.getSector(new Vector3i());
        Stronghold hold = StrongholdController.getInstance().getStrongholdFromSector(sector);
        boolean isOwner = hold.getOwner() == object.getFactionId(), isAlly;
        isAlly = ((GameServerState.instance!=null && GameServerState.instance.getFactionManager().isFriend(hold.getOwner(), object.getFactionId())) ||
                (GameClientState.instance != null && GameClientState.instance.getFactionManager().isFriend(hold.getOwner(), object.getFactionId())));

        return  isSectorVoidShielded(sector)&&
                !object.isHomeBase()&&
                hold.getOwner()!=0&&
                hold.getOwner()==object.getFactionId()||isAlly&&
                object.getType().equals(SimpleTransformableSendableObject.EntityType.SPACE_STATION);
    }

    public static void initClientEHs() {
        new StarRunnable(){
            Vector3i oldSec = new Vector3i();
            Vector3i oldSys = new Vector3i();
            @Override
            public void run() {
                if (GameClientState.instance == null || GameClientState.instance.getPlayer() == null)
                    return;

                Vector3i pos = GameClientState.instance.getPlayer().getCurrentSector();
                if (!pos.equals(oldSec))
                    onSectorChange(pos);
                oldSec.set(pos);

                Vector3i newSys = new Vector3i(pos);
                StrongholdController.mutateSectorToSystem(newSys);
                if (!newSys.equals(oldSys)) {
                    onSystemChange(newSys);
                    oldSys.set(newSys);
                }

            }

            private void onSectorChange(Vector3i pos) {
                //new sector, is strongpoint
                if (StrongholdController.getInstance().getStrongholdFromSector(pos).isStrongpoint(pos)) {
                    SoundManager.instance.queueSound(SoundManager.Sound.strongpoint_sector);
                }
            }

            private void onSystemChange(Vector3i pos) {
                //new system, has voidshield
                if (isSectorVoidShielded(pos))
                    SoundManager.instance.queueSound(SoundManager.Sound.system_shielded);

            }
        }.runTimer(ModMain.instance,10);

        VoidShieldInternalHandler eh = new VoidShieldInternalHandler();
        StrongholdController.getInstance().addStrongholdEventListener(eh);
        StrongholdController.getInstance().addStrongpointEventListener(eh);
    }
}
