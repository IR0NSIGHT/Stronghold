package org.schema.game.common.controller.elements;

import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import me.iron.stronghold.mod.implementation.VoidShield;
import org.schema.common.util.StringTools;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.effects.RaisingIndication;
import org.schema.game.client.view.effects.ShieldDrawer;
import org.schema.game.client.view.gui.shiphud.HudIndicatorOverlay;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.SendableSegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.controller.damage.DamageDealerType;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.controller.damage.HitReceiverType;
import org.schema.game.common.controller.damage.HitType;
import org.schema.game.common.controller.damage.effects.InterEffectHandler;
import org.schema.game.common.controller.damage.effects.InterEffectSet;
import org.schema.game.common.controller.elements.power.PowerAddOn;
import org.schema.game.common.controller.rails.RailRelation;
import org.schema.game.common.data.ManagedSegmentController;
import org.schema.game.common.data.blockeffects.FastSegmentControllerStatus;
import org.schema.game.common.data.blockeffects.config.StatusEffectType;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.world.SectorNotFoundException;
import org.schema.game.common.data.world.SegmentData;
import org.schema.game.network.objects.remote.RemoteValueUpdate;
import org.schema.game.network.objects.valueUpdate.NTValueUpdateInterface;
import org.schema.game.network.objects.valueUpdate.ShieldExpectedValueUpdate;
import org.schema.game.network.objects.valueUpdate.ShieldRechargeValueUpdate;
import org.schema.game.network.objects.valueUpdate.ShieldValueUpdate;
import org.schema.game.network.objects.valueUpdate.ValueUpdate.ValTypes;
import org.schema.game.server.data.ServerConfig;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.GLFrame;
import org.schema.schine.graphicsengine.core.Timer;
import org.schema.schine.graphicsengine.core.settings.EngineSettings;
import org.schema.schine.network.objects.remote.RemoteVector4f;

import com.bulletphysics.linearmath.Transform;

public class ShieldAddOn implements HittableInterface, ManagerUpdatableInterface {


    private final SegmentController segmentController;
    public ShieldContainerInterface sc;
    Vector3i posTmp = new Vector3i();
    private double shields;
    private double getShieldCapacityHP;
    private double shieldRechargeRate;
    private float accPower;
    private double recovery;
    private double initialShields;
    private double shieldsBefore;
    private double recoveryOnZeroShields;
    private double nerf;
    private double lastPowerConsumption;
    private boolean transferredDamage;
    private boolean regenEnabled = true;
    private int clientExpectedSize = -1;
    private final ShieldLocalAddOn shieldLocalAddOn;
    public ShieldAddOn(ShieldContainerInterface sc, SegmentController controller) {
        this.sc = sc;
        this.segmentController = controller;
        sc.addUpdatable(this);

        shieldLocalAddOn = new ShieldLocalAddOn(sc, controller);
    }

    public static void main(String[] safd) {
        for (int i = 1; i < 1000000; i++) {
            int log10 = (int) Math.ceil(Math.max(1, Math.log10(i)));
            if (i % (int) Math.pow(10, log10 - 1) == 0) {
                System.err.println(i + "C: " + log10 + "; " + (Math.pow(i * 2, 0.39) * 50));
                System.err.println(i + "E: " + log10 + "; " + (Math.pow(i * 2, 0.8) * 400));
            }
        }
    }
    public static final Vector4f shieldHitColor = new Vector4f(0.3f,0.3f,1.0f,1.0f);


    public double handleShieldHit(Damager damager, InterEffectSet defenseSet, Vector3f hitPoint, int projectileSectorId, DamageDealerType damageType, HitType hitType, double damage, long weaponId) throws SectorNotFoundException {
        //are we hitting ourselves or one of our docks
        if (damager != null && damager instanceof SegmentController && segmentController.railController.isInAnyRailRelationWith((SegmentController) damager)) {
            damage = 0;
            return 0;
        }

        //inserted code
        for (VoidShield s: VoidShield.shields) {
            damage = s.handleShieldHit(this, damager, defenseSet, hitPoint, projectileSectorId, damageType, hitType, damage, weaponId);
        }
        // end

        InterEffectSet attack = null;

        if(damager != null) {
            attack = damager.getAttackEffectSet(weaponId, damageType);
        }
        double damageWithDefenseEffect = damage;
        if(attack != null && defenseSet != null) {
            short typeParam = 0;
            damageWithDefenseEffect = InterEffectHandler.handleEffects((float) damage, attack, defenseSet, hitType, damageType, HitReceiverType.SHIELD, typeParam);
        }else {
            System.err.println("[WARNING] Shield attacked without attack effect set "+getSegmentController());
        }



        if(isUsingLocalShields()){
            double damageAfterShields = getShieldLocalAddOn().handleShieldHit(damager, hitPoint, projectileSectorId, damageType, damageWithDefenseEffect, weaponId);
            if(damageAfterShields == damageWithDefenseEffect) {
                //shields 0, don't apply shield defense effects
                return damage;
            }else {
                return damageAfterShields;
            }
        }

        this.transferredDamage = false;

        if (getShields() <= 0) {
            damage = checkRailIntercept(damage, damageType, true, false);
            //no shields, full damage
            return damage;
        }

        double shieldsBefore = getShields();

        long pos = 0; //legacy
        short type = 0; //legacy
        onHit(pos, type, (int) damage, damageType);

        double resultDamage = 0;

        if (getShields() <= 0) {
            if (damage < shieldsBefore) {
                resultDamage = shieldsBefore;
            } else {
                resultDamage = damage;
            }
        } else {
            resultDamage = 0;
        }

        //		System.err.println(sc.getState()+" SHIELDS BEFORE: "+shieldsBefore+"; AFTER: "+getShields()+"; damage-> "+damage);

        if (!sc.getSegmentController().isOnServer() && !GLFrame.isFinished() && sc.getSegmentController().isInClientRange()) {
            // prepare to add some explosions
            GameClientState s = (GameClientState) sc.getState();
            Transform t = new Transform();
            t.setIdentity();
            t.origin.set(hitPoint);
            FastSegmentControllerStatus status = ((SendableSegmentController) sc.getSegmentController()).getBlockEffectManager().status;
            damage = damage * (1f - status.shieldHarden);
            if (transferredDamage) {
                HudIndicatorOverlay.toDrawTexts.add(new RaisingIndication(t, String.valueOf((int) damage) + " transfer", shieldHitColor.x, shieldHitColor.y, shieldHitColor.z, shieldHitColor.w));
            } else {
                HudIndicatorOverlay.toDrawTexts.add(new RaisingIndication(t, String.valueOf((int) damage), shieldHitColor.x, shieldHitColor.y, shieldHitColor.z, shieldHitColor.w));
            }

            s.getWorldDrawer().getExplosionDrawer().addExplosion(hitPoint, 4, weaponId);

            if (!transferredDamage) {
                ShieldDrawer shieldDrawer = s.getWorldDrawer().getShieldDrawerManager().get(getSegmentController());
                if (shieldDrawer != null) {
                    shieldDrawer.addHitOld(hitPoint, (float)damage);
                } else {
                    System.err.println("[CLIENT] ERROR: shield drawer for " + getSegmentController() + " not initialized");
                }
            }
        }
        return resultDamage;
    }

    private double checkRailIntercept(double damage, DamageDealerType damageType, boolean useRecovery, boolean ignoreEffects) {
        if (getSegmentController().railController.isDockedAndExecuted()) {
            double damageBefore = damage;
            if (shields > 0 && damage >= shields) {
                double damageDeduct = shields - 1;
                shields = 1;
                damage -= damageDeduct;
            }
            SegmentController s = getSegmentController().railController.previous.rail.getSegmentController();
            if (s instanceof ManagedSegmentController<?> && ((ManagedSegmentController<?>) s).getManagerContainer() instanceof ShieldContainerInterface) {
                ShieldContainerInterface sc = (ShieldContainerInterface) ((ManagedSegmentController<?>) s).getManagerContainer();
                return sc.getShieldAddOn().checkRailDockingDamageRecursive(damageBefore, damageType, getSegmentController().railController.previous, useRecovery, ignoreEffects);
            }
        }
        //own shields take damage
        return damage;

    }

    /**
     * @return the initialShields
     */
    public double getInitialShields() {
        return initialShields;
    }

    public void setInitialShields(double value) {
        this.initialShields = value;
        //		System.err.println(this.getSegmentController()+"; "+getSegmentController().getState()+" Loaded initial shields "+getInitialShields());
    }

    /**
     * @return the nerf
     */
    public double getNerf() {
        return nerf;
    }

    /**
     * @return the recovery
     */
    public double getRecovery() {
        return recovery;
    }

    public void sendShieldUpdate() {
        if(isUsingLocalShields()){
            getShieldLocalAddOn().sendShieldUpdate(null);
            return;
        }
        assert (getSegmentController().isOnServer());
        ShieldValueUpdate shieldValueUpdate = new ShieldValueUpdate();
        assert (shieldValueUpdate.getType() == ValTypes.SHIELD);
        shieldValueUpdate.setServer(((ManagedSegmentController<?>) getSegmentController()).getManagerContainer());
        ((NTValueUpdateInterface) getSegmentController().getNetworkObject()).getValueUpdateBuffer().add(new RemoteValueUpdate(shieldValueUpdate, getSegmentController().isOnServer()));
    }
    public void sendShieldExpectedUpdate() {
        assert (getSegmentController().isOnServer());
        ShieldExpectedValueUpdate shieldValueUpdate = new ShieldExpectedValueUpdate();
        assert (shieldValueUpdate.getType() == ValTypes.SHIELD_EXPECTED);
        shieldValueUpdate.setServer(((ManagedSegmentController<?>) getSegmentController()).getManagerContainer());
        ((NTValueUpdateInterface) getSegmentController().getNetworkObject()).getValueUpdateBuffer().add(new RemoteValueUpdate(shieldValueUpdate, getSegmentController().isOnServer()));
    }

    public void sendRegenEnabledUpdate() {
        if(isUsingLocalShields()){
            getShieldLocalAddOn().sendRegenEnabledUpdate();
            return;
        }
        assert (getSegmentController().isOnServer());
        ShieldRechargeValueUpdate update = new ShieldRechargeValueUpdate();
        assert (update.getType() == ValTypes.SHIELD_REGEN_ENABLED);
        update.setServer(((ManagedSegmentController<?>) getSegmentController()).getManagerContainer());
        ((NTValueUpdateInterface) getSegmentController().getNetworkObject()).getValueUpdateBuffer().add(new RemoteValueUpdate(update, getSegmentController().isOnServer()));
    }

    /**
     * @return the shieldCapabilityHP
     */
    public double getShieldCapacityWithoutInitial() {
        return getShieldCapacityHP;
    }

    public double getShieldCapacity() {

        double cap = getShieldCapacityHP +
                ((this.segmentController instanceof Ship || this.segmentController instanceof SpaceStation) ?
                        VoidElementManager.SHIELD_CAPACITY_INITIAL : 0);
        cap = getSegmentController().getConfigManager().apply(StatusEffectType.SHIELD_CAPACITY, cap);
        return cap;
    }

    /**
     * @return the shieldRechargeRate
     */
    public double getShieldRechargeRate() {
        if (!regenEnabled) {
            return 0;
        }
        double reg = (shieldRechargeRate + VoidElementManager.SHIELD_RECHARGE_INITIAL) * ((SendableSegmentController) getSegmentController()).getBlockEffectManager().status.shieldRegenPercent;
        reg = getSegmentController().getConfigManager().apply(StatusEffectType.SHIELD_RECHARGE_RATE, reg);
        return reg;
    }

    /**
     * @param shieldRechargeRate the shieldRechargeRate to set
     */
    public void setShieldRechargeRate(long shieldRechargeRate) {
        this.shieldRechargeRate = shieldRechargeRate;
    }

    /**
     * @return the shields
     */
    public double getShields() {
        return shields;
    }

    /**
     * @param shields the shields to set
     */
    public void setShields(double shields) {
        this.shields = shields;
    }
    public void onHit(double damage, DamageDealerType damageType, boolean useRecovery, boolean ignoreEffects) {
        double dmg = damage;

        dmg = getSegmentController().getConfigManager().apply(StatusEffectType.SHIELD_DAMAGE_RESISTANCE, damageType, dmg);
        if (damage > shields && getSegmentController().railController.isDockedAndExecuted()) {
            damage = checkRailIntercept(damage, damageType, useRecovery, ignoreEffects);
        }

        double bef = shields;

        FastSegmentControllerStatus status = ((SendableSegmentController) sc.getSegmentController()).getBlockEffectManager().status;

        shields = Math.max(0d, shields - damage * (ignoreEffects ? 1f : (1f - status.shieldHarden)));

        if(useRecovery){
            useNormalRecovery();
        }


        if (shields == 0) {
            onShieldsZero(bef);
        }
    }
    @Override
    public void onHit(long pos, short type, double damage, DamageDealerType damageType) {
        onHit(damage, damageType, true, false);
    }

    public void useNormalRecovery() {
        float rec = VoidElementManager.SHIELD_DIRECT_RECOVERY_TIME_IN_SEC;
        rec = getSegmentController().getConfigManager().apply(StatusEffectType.SHIELD_UNDER_FIRE_TIMEOUT, rec);
        recovery = rec;
    }

    public void onShieldsZero(double beforeShields) {
        if (shields == 0 && getShieldCapacity() > 0) {

            float rec = VoidElementManager.SHIELD_RECOVERY_TIME_IN_SEC;
            rec = getSegmentController().getConfigManager().apply(StatusEffectType.SHIELD_UNDER_FIRE_TIMEOUT, rec);
            recoveryOnZeroShields = rec;
//			System.err.println("[SHIELDADDON] "+this.getSegmentController() + "; " + getSegmentController().getState() + " SHIELD OUTAGE");
            if (getSegmentController().isClientOwnObject()) {
                ((GameClientState) getSegmentController().getState()).getController().popupAlertTextMessage(Lng.str("!!!WARNING!!!\n\nShields DOWN\n(%s sec recovery)",  VoidElementManager.SHIELD_RECOVERY_TIME_IN_SEC), 0);
            } else if (getSegmentController().isOnServer()) {
                if (beforeShields > 0) {
                    sendShieldUpdate();
                }
            }
        }
    }

    private double checkRailDockingDamageRecursive(double damage, DamageDealerType damageType, RailRelation rel, boolean useRecovery, boolean ignoreEffects) {
        assert(VoidElementManager.SHIELD_DOCK_TRANSFER_LIMIT > 0);
        if (getPercentOne() > VoidElementManager.SHIELD_DOCK_TRANSFER_LIMIT && getShields() > damage) {

            //we can take the damage for the docked entity
            onHit(damage, damageType, useRecovery, ignoreEffects);
            transferredDamage = true;
            if (!sc.getSegmentController().isOnServer() && !GLFrame.isFinished() && sc.getSegmentController().isInClientRange()) {
                // prepare to add some explosions
                GameClientState s = (GameClientState) sc.getState();

                ShieldDrawer shieldDrawer = s.getWorldDrawer().getShieldDrawerManager().get(getSegmentController());
                if (shieldDrawer != null) {
                    Vector3f absolutePos = rel.rail.getAbsolutePos(new Vector3f());
                    absolutePos.x -= SegmentData.SEG_HALF;
                    absolutePos.y -= SegmentData.SEG_HALF;
                    absolutePos.z -= SegmentData.SEG_HALF;
                    getSegmentController().getWorldTransformOnClient().transform(absolutePos);
                    shieldDrawer.addHitOld(absolutePos, (float)damage);
                } else {
                    System.err.println("[CLIENT] ERROR: shield drawer for " + getSegmentController() + " not initialized");
                }
            }
            return 0;

        } else if (getSegmentController().railController.isDockedAndExecuted()) {
            //we cannot put damage on this chain link
            //check if there is another one to put the damage on
            SegmentController s = getSegmentController().railController.previous.rail.getSegmentController();
            if (s instanceof ManagedSegmentController<?> && ((ManagedSegmentController<?>) s).getManagerContainer() instanceof ShieldContainerInterface) {
                ShieldContainerInterface sc = (ShieldContainerInterface) ((ManagedSegmentController<?>) s).getManagerContainer();
                return sc.getShieldAddOn().checkRailDockingDamageRecursive(damage, damageType, getSegmentController().railController.previous, useRecovery, ignoreEffects);
            }
        }
        return damage;

    }
    public int getExpectedShieldSize() {
        return sc.getShieldCapacityManager().expected;
    }
    @Override
    public void update(Timer timer) {
        boolean requestedInitalValues = ((ManagedSegmentController<?>)getSegmentController())
                .getManagerContainer().isRequestedInitalValuesIfNeeded();
        if(!requestedInitalValues){
            return;
        }
        if(isUsingLocalShields()){
            getShieldLocalAddOn().update(timer);
            return;
        }
        if (shields >= getShieldCapacity() && initialShields > 0) {
            //			if(getSegmentController().isOnServer() && getSegmentController().toString().contains("The_Bl")){
            //				System.err.println("CHECKING SHIELDS OVER "+initialShields);
            //			}
            int bbShields;

            if(getSegmentController().isOnServer()){
                if (getSegmentController() instanceof Ship && ((Ship) getSegmentController()).getSpawnedFrom() != null) {
                    bbShields = ((Ship) getSegmentController()).getSpawnedFrom().getElementMap().get(ElementKeyMap.SHIELD_CAP_ID);
                } else {
                    bbShields = sc.getShieldCapacityManager().expected;
                }
            }else{
                bbShields = clientExpectedSize;
            }
            if (bbShields >= 0 && sc.getShieldCapacityManager().getTotalSize() >= bbShields) {
                System.err.println("[SHIELDS] " + getSegmentController() + " CHECKING against bb: " + bbShields + " / " + sc.getShieldCapacityManager().getTotalSize() + "; limit reached. setting cap to prevent overcharge");
                //all shield capacity blocks loaded
                //cap to preven overcharge from blueprints saved with different config
                initialShields = 0;

                shields = getShieldCapacity();
                if(getSegmentController().isOnServer()){
                    sendShieldExpectedUpdate();
                    sendShieldUpdate();
                }
            }
            return;
        }
        if (sc.getShieldRegenManager().getElementCollections().isEmpty() && !VoidElementManager.SHIELD_INITIAL_CORE) {
            return;
        }

        /*
         * replenish shields with loaded value as long
         * as the loaded value is bigger then the
         * shield capacity available
         */
        if (initialShields > 0 && shields < getShieldCapacity()) {

            double needed = getShieldCapacity() - shields;

            double got = Math.min(initialShields, needed);

            shields += got;

            initialShields -= got;
        }
        nerf = 1;

        /*
         * this is the direct recovery
         * shields recharge is nerfed when ship
         * is in battle
         */
        if (recovery > 0) {
            recovery = Math.max(0, recovery - timer.getDelta());
            double percent = (shields / getShieldCapacity());
            nerf = percent;
            nerf *= VoidElementManager.SHIELD_RECOVERY_NERF_MULT_PER_PERCENT;
            nerf = getSegmentController().getConfigManager().apply(StatusEffectType.SHIELD_UNDER_FIRE_RECHARGE_NERF, nerf);
            nerf = 1f - nerf;

            nerf *= VoidElementManager.SHIELD_RECOVERY_NERF_MULT;
        }
        if (segmentController.getHpController().isRebooting()) {
            shields = 0;
            return;
        }

        if (recoveryOnZeroShields > 0) {
            /*
             * the recoveryOut is the
             * recovery when shield reach zero percent
             * No recharge is done during that time
             */
            recoveryOnZeroShields = Math.max(0, recoveryOnZeroShields - timer.getDelta());
        } else {
            if (accPower < 0) {
                accPower = 0;
            }
            /*
             * attempting normal recharge cycle every
             * second
             */
            accPower += timer.getDelta();

            if (accPower > 10) {
                accPower = 10;
            }
            double cycleTime = VoidElementManager.SHIELD_RECHARGE_CYCLE_TIME;

            //			if(getSegmentController().toString().contains("schema")){
            //				System.err.println(getSegmentController().getState()+"; "+getSegmentController()+" CACLE TIME: "+cycleTime+"; "+accPower);
            //			}

            if (accPower >= cycleTime) {

                rechargeCycle(cycleTime);
                accPower -= cycleTime;
            }
            if (getSegmentController().isOnServer() && (shieldsBefore != shields)) {

                double percent = (getShieldCapacity() / 100);

                if (Math.abs(shieldsBefore - shields) >= percent * ((Integer) ServerConfig.BROADCAST_SHIELD_PERCENTAGE.getCurrentState()).doubleValue()
                        || shields == 0 || shields == getShieldCapacity()) {
                    //update NT when shields changed, and change is more then 1% or when it's full or empty

                    sendShieldUpdate();
                    shieldsBefore = shields;
                }
            }
        }

    }

    public void incShields(double plus) {
        shields = Math.min(getShieldCapacity(), shields + plus);
    }

    private void rechargeCycle(double cycleTime) {
        double powerCost;
        if (shields >= getShieldCapacity()) {
            powerCost = VoidElementManager.SHIELD_FULL_POWER_CONSUMPTION;
        } else {
            powerCost = VoidElementManager.SHIELD_RECHARGE_POWER_CONSUMPTION;
        }


        powerCost *= getShieldRechargeRate() * nerf * cycleTime;
        PowerAddOn powerManager = ((ShieldContainerInterface) ((ManagedSegmentController<?>) getSegmentController()).getManagerContainer()).getPowerAddOn();
        if (powerManager.getPower() < powerCost && !getSegmentController().getDockingController().isDocked()) {
            double powerLeft = powerManager.getPower();
            if (powerLeft > 0) {
                lastPowerConsumption = powerLeft;
                powerManager.consumePowerInstantly(powerLeft);
                //only recharge with the percentage of one tick we have
                double plus = (powerLeft / powerCost) * getShieldRechargeRate() * nerf * cycleTime;
                shields = Math.min(getShieldCapacity(), shields + plus);
            } else {
            }
        } else {
            lastPowerConsumption = powerCost;
            if (powerManager.consumePowerInstantly(powerCost)) {
                double plus = getShieldRechargeRate() * nerf * cycleTime;
                shields = Math.min(getShieldCapacity(), shields + plus);

            } else {
            }
        }

    }

    public boolean isUsingPowerReactors() {
        return sc.isUsingPowerReactors();
    }

    public SegmentController getSegmentController() {
        return sc.getSegmentController();
    }

    /**
     * @param getShieldCapacity the getShieldCapacityHP() to set
     */
    public void setShieldCapacityHP(double getShieldCapacity) {
        this.getShieldCapacityHP = getShieldCapacity;
    }

    /**
     * @return the recoveryOut
     */
    public double getRecoveryOut() {
        return recoveryOnZeroShields;
    }

    /**
     * @param recoveryOut the recoveryOut to set
     */
    public void setRecoveryOut(double recoveryOut) {
        this.recoveryOnZeroShields = recoveryOut;
    }

    public String getShieldString() {

        if (EngineSettings.G_SHOW_PURE_NUMBERS_FOR_SHIELD_AND_POWER.isOn()) {
            return Lng.str("[%s / %s # In: %s Shields]",  StringTools.formatPointZero(getShields()),  StringTools.formatPointZero(getShieldCapacity()),  (getInitialShields()));
        }

        if (getShieldCapacity() > 0) {
            return Lng.str("[%s%%%s Shields]",  StringTools.formatPointZero((getShields() / getShieldCapacity()) * 100d),  (getInitialShields() > 0 ? "(+)" : ""));
        } else {
            return "";
        }
    }

    public void flagCompleteLoad() {
    }

    /**
     * @return the lastPowerConsumption
     */
    public double getLastPowerConsumption() {
        return lastPowerConsumption;
    }

    public float getPercentOne() {
        return (float) (getShields() / getShieldCapacity());
    }

    public boolean isRegenEnabled() {
        return this.regenEnabled;
    }

    public void setRegenEnabled(boolean enabled) {
        this.regenEnabled = enabled;
        if (getSegmentController().isOnServer()) {
            sendRegenEnabledUpdate();
        }
    }
    public boolean isUsingLocalShields(){
        return segmentController.isUsingPowerReactors();
    }
    public void sendShieldHit(Vector3f worldPos, float damage) {
        assert (getSegmentController().isOnServer());
        getSegmentController().getNetworkObject().shieldHits.add(new RemoteVector4f(worldPos, damage, getSegmentController().isOnServer()));
    }

    public double getShieldsRail() {
        return segmentController.railController.getShieldsRecursive();
    }

    public void fillShieldsMapRecursive(Vector3f hitWorld, int projectileSectorId, Int2DoubleOpenHashMap shieldMap,
                                        Int2DoubleOpenHashMap shieldMapBef, Int2DoubleOpenHashMap shieldMapPercent, Int2IntOpenHashMap railMap, Int2IntOpenHashMap railRootMap, Int2LongOpenHashMap shieldLocalMap) throws SectorNotFoundException {

        segmentController.railController.fillShieldsMapRecursive(hitWorld, projectileSectorId, shieldMap, shieldMapBef, shieldMapPercent, railMap, railRootMap, shieldLocalMap);
    }

    public void setExpectedShieldClient(int val) {
        clientExpectedSize = val;
    }

    @Override
    public int updatePrio() {
        return 1;
    }

    public ShieldLocalAddOn getShieldLocalAddOn() {
        return shieldLocalAddOn;
    }

    public boolean isUsingLocalShieldsAtLeastOneActive() {
        return isUsingLocalShields() && getShieldLocalAddOn().isAtLeastOneActive();
    }

    @Override
    public boolean canUpdate() {
        return true;
    }
    @Override
    public void onNoUpdate(Timer timer) {
    }
}
