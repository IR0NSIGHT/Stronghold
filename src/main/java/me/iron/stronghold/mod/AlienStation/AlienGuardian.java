package me.iron.stronghold.mod.AlienStation;

import api.listener.Listener;
import api.listener.events.systems.ShieldHitEvent;
import api.listener.events.weapon.AnyWeaponDamageCalculateEvent;
import api.listener.fastevents.DamageBeamHitListener;
import api.listener.fastevents.FastListenerCommon;
import api.mod.StarLoader;
import com.bulletphysics.linearmath.Transform;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.framework.AbstractAreaEffect;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.BeamHandlerContainer;
import org.schema.game.common.controller.damage.beam.DamageBeamHitHandlerSegmentController;
import org.schema.game.common.controller.elements.BeamState;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.world.Segment;
import org.schema.game.server.controller.BluePrintController;
import org.schema.game.server.controller.EntityAlreadyExistsException;
import org.schema.game.server.controller.EntityNotFountException;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.graphicsengine.core.Timer;

import javax.vecmath.Vector3f;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;

public class AlienGuardian extends AbstractAreaEffect {
    public LinkedList<String> catalogNames = new LinkedList<>();
    public int guardianFaction = -1;
    public float countMedian = 2;
    public float countStd = 1;

    private transient Listener<ShieldHitEvent> shieldHitEventListener;
    private transient DamageBeamHitListener damageBeamHitListener;
    private transient Listener<AnyWeaponDamageCalculateEvent> anyWeaponDamageCalculateEvent;
    private transient Random random = new Random();
    protected static AlienGuardian instance;


    @Override
    protected void synch(SendableUpdateable origin) {
        super.synch(origin);
        if (origin instanceof AlienGuardian) {
            this.catalogNames = ((AlienGuardian) origin).catalogNames;
            this.guardianFaction = ((AlienGuardian) origin).guardianFaction;
            this.countMedian = ((AlienGuardian) origin).countMedian;
            this.countStd = ((AlienGuardian) origin).countStd;
        }
    }

    //only for instantiation
    public AlienGuardian() {

    }

    //for adding
    public AlienGuardian(String name) {
        super("AlienGuardian");
    }

    @Override
    protected void destroy() {
        super.destroy();
        if (shieldHitEventListener != null)
            StarLoader.unregisterListener(ShieldHitEvent.class, shieldHitEventListener);
        if (anyWeaponDamageCalculateEvent != null)
            StarLoader.unregisterListener(AnyWeaponDamageCalculateEvent.class, anyWeaponDamageCalculateEvent);
        if (damageBeamHitListener != null)
            FastListenerCommon.damageBeamHitListeners.remove(damageBeamHitListener);
        if (this == instance)
            instance = null;
    }

    @Override
    protected void onClientAfterInstantiate() {
        super.onClientAfterInstantiate();
        initListener();
    }

    @Override
    protected void onServerAfterInstantiate() {
        super.onClientAfterInstantiate();
        initListener();
    }

    protected void initListener() {
        instance = this;

        //will catch cannon shots but NOT missile or beam.
        shieldHitEventListener = new Listener<ShieldHitEvent>() {
            @Override
            public void onEvent(ShieldHitEvent shieldHitEvent) {
                //guardians are invulnerable
                int victimFaction = shieldHitEvent.getHitController().getFactionId();
                int attackerFaction = shieldHitEvent.getShieldHit().damager.getFactionId();
                if (victimFaction == guardianFaction)
                    shieldHitEvent.setCanceled(true);
                    //guardians one hit shields
                else if (attackerFaction == guardianFaction) {
                    shieldHitEvent.getShieldHit().setDamage(Double.MAX_VALUE);
                }
            }
        };
        StarLoader.registerListener(ShieldHitEvent.class, shieldHitEventListener, ModMain.instance);

        this.damageBeamHitListener = new DamageBeamHitListener() {
            @Override
            public void handle(BeamState beamState, int i, BeamHandlerContainer<?> beamHandlerContainer, SegmentPiece segmentPiece, Vector3f vector3f, Vector3f vector3f1, Timer timer, Collection<Segment> collection, DamageBeamHitHandlerSegmentController damageBeamHitHandlerSegmentController) {
                int victimId = segmentPiece.getSegmentController().getFactionId();
                if (victimId == guardianFaction)
                    beamState.setPower(0);
            }
        };
        FastListenerCommon.damageBeamHitListeners.add(damageBeamHitListener);

        this.anyWeaponDamageCalculateEvent = new Listener<AnyWeaponDamageCalculateEvent>() {
            @Override
            public void onEvent(AnyWeaponDamageCalculateEvent anyWeaponDamageCalculateEvent) {
                int attackerFaction = anyWeaponDamageCalculateEvent.customOutputUnit.getSegmentController().getFactionId();
                if (attackerFaction == guardianFaction)
                    anyWeaponDamageCalculateEvent.damage = Float.MAX_VALUE;

            }
        };
        StarLoader.registerListener(AnyWeaponDamageCalculateEvent.class, anyWeaponDamageCalculateEvent, ModMain.instance);
    }

    private int getSpawnCount() {
        return AlienArea.getGaussNumber(random, countStd, countMedian);
    }

    public void spawnGuardian(Vector3i sector) {
        Transform t = new Transform();
        t.setIdentity();
        try {
            GameServerState.instance.spawnMobs(getSpawnCount(), catalogNames.get(random.nextInt(catalogNames.size())), sector, t, guardianFaction, BluePrintController.active);
        } catch (EntityNotFountException | IOException |
                 EntityAlreadyExistsException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "AlienGuardian{" + super.toString() +
                "catalogNames=" + catalogNames +
                ", guardianFaction=" + guardianFaction +
                ", countMedian=" + countMedian +
                ", countStd=" + countStd +
                '}';
    }
}
