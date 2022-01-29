package me.iron.stronghold.mod.voidshield;

import api.utils.StarRunnable;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.effects.sounds.SoundManager;
import me.iron.stronghold.mod.events.IStrongholdEvent;
import me.iron.stronghold.mod.events.IStrongpointEvent;
import me.iron.stronghold.mod.framework.Stronghold;
import me.iron.stronghold.mod.framework.StrongholdController;
import me.iron.stronghold.mod.framework.Strongpoint;
import org.lwjgl.Sys;
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
import java.util.HashMap;
import java.util.LinkedList;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 13.01.2022
 * TIME: 18:29
 */
public class VoidShieldController implements IVoidShieldEvent, IStrongpointEvent, IStrongholdEvent {
    private static VoidShieldController instance;
    public static VoidShieldController getInstance() {
        return instance;
    }

    private VoidShieldContainer c = new VoidShieldContainer();
    private long CP_lockdownAfterConquer = 9000;
    private LinkedList<IVoidShieldEvent> eventlisteners = new LinkedList<>();
    public void addListener(IVoidShieldEvent listener) {
        eventlisteners.add(listener);

    }
    public boolean removeListener(IVoidShieldEvent listener) {
        return eventlisteners.remove(listener);
    }

    public VoidShieldController(boolean isServer) {
        instance = this;
    }

    public void init() {
        //add self as listener to StrongholdController

        if (StrongholdController.getInstance() != null) {
            StrongholdController.getInstance().addStrongholdEventListener(this);
            StrongholdController.getInstance().addStrongpointEventListener(this);
        }
    }
    public void onShutdown() {
        eventlisteners.clear();
        instance = null;
    }


    public double handleShieldHit(ShieldAddOn hitShield, Damager damager, InterEffectSet defenseSet, Vector3f hitPoint, int projectileSectorId, DamageDealerType damageType, HitType hitType, double damage, long weaponId) {
        if (isObjectVoidShielded(hitShield.getSegmentController())) {
            Stronghold h = StrongholdController.getInstance().getStrongholdFromSector(hitShield.getSegmentController().getSector(new Vector3i()));
            onShieldHit(h, hitShield.getSegmentController(), damager);
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
        if (isStrongholdShielded(hold) && hold.isStrongpoint(sector)) {
            Strongpoint p = hold.getStrongpointFromSector(sector);
            if (p.equals(hold.getNextEnemyStrongpoint())||p.equals(hold.getNextOwnStrongpoint())) { //only attackable CPs
                //test if this CP is locked down
                if (getInstance() != null) {
                    long lastConquered = getInstance().c.getCPConqueredAt(hold.getStrongpointFromSector(new Vector3i()));
                    long timeSinceConquer = System.currentTimeMillis()-lastConquered;
                    ModMain.log("hit CP is next in line, , last conquer was last conquered at "+lastConquered+" =>" +timeSinceConquer/1000+" seconds ago, wait another "+ ((getInstance().CP_lockdownAfterConquer-timeSinceConquer)/1000)+" seconds");
                    if (timeSinceConquer> getInstance().CP_lockdownAfterConquer) {
                        ModMain.log("hit controlpoint "+p.getSector()+" is free for attack");
                        //the shot strongpoint was last conquered just some time ago and is still on lockdown. voidshield remains active
                    } else {
                        ModMain.log("hit controlpoint "+p.getSector()+"is under lockdown.");
                        return false; //strongpoint was conquered long ago, is not under lockdown anymore.
                    }
                }
            } else {
                ModMain.log("hit controlpoint is not the next one to conquer. voidshield active.");
            }
            //random CP that is currently not attackable was shot. protect it.
        }
        return isStrongholdShielded(hold);// && !hold.isStrongpoint(sector);
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
        boolean isAlly = ((GameServerState.instance!=null && GameServerState.instance.getFactionManager().isFriend(hold.getOwner(), object.getFactionId())) ||
                (GameClientState.instance != null && GameClientState.instance.getFactionManager().isFriend(hold.getOwner(), object.getFactionId())));

        return isSectorVoidShielded(sector) && !object.isHomeBase() && object.getFactionId() != 0 && (hold.getOwner() == object.getFactionId() && isAlly) && object.getType().equals(SimpleTransformableSendableObject.EntityType.SPACE_STATION);
    }

    public static long controlPointLockDownTill(Strongpoint p) {
        if (getInstance() == null)
            return -1;
        return getInstance().c.getCPConqueredAt(p)+ getInstance().CP_lockdownAfterConquer;
    }

    public void initClientEHs() {
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

    }

    //Eventhandler methods for outgoing event firing

    @Override
    public void onShieldHit(Stronghold h, SegmentController hitObject, Damager d) {
        for (IVoidShieldEvent e: eventlisteners) {
            e.onShieldHit(h,hitObject,d);
        }
    }

    @Override
    public void onShieldActivate(Stronghold h) {
        ModMain.log("[Voidshieldcontroller]voidshield activated for "+h.getName());
        for (IVoidShieldEvent e: eventlisteners) {
            e.onShieldActivate(h);
        }
    }

    @Override
    public void onShieldDeactivate(Stronghold h) {
        ModMain.log("[Voidshieldcontroller]voidshield deactivated for "+h.getName());
        for (IVoidShieldEvent e: eventlisteners) {
            e.onShieldDeactivate(h);
        }
    }

    //foreign event methods for internal use
    @Override
    public void onStrongpointOwnerChanged(Strongpoint p, int newOwner) {
        //log the change with time
        c.setCPConquered(p, System.currentTimeMillis());
    }

    @Override
    public void onDefensepointsChanged(Stronghold h, int newPoints) {
        boolean shieldGotActive =h.getDefensePoints()< VoidShieldController.getRequiredPointsForShield() && VoidShieldController.getRequiredPointsForShield()<= newPoints;
        boolean shieldGotInactive = h.getDefensePoints()>= VoidShieldController.getRequiredPointsForShield() && VoidShieldController.getRequiredPointsForShield()>newPoints;
        if (shieldGotActive) {
            onShieldActivate(h);
        }
        if (shieldGotInactive) {
            onShieldDeactivate(h);
        }
    }

    @Override
    public void onStrongholdOwnerChanged(Stronghold h, int newOwner) {

    }

    @Override
    public void onStrongholdBalanceChanged(Stronghold h, int newBalance) {

    }
}
