package me.iron.stronghold.mod.AlienStation;

import api.listener.Listener;
import api.listener.events.systems.ShieldHitEvent;
import api.listener.events.weapon.AnyWeaponDamageCalculateEvent;
import api.listener.fastevents.DamageBeamHitListener;
import api.listener.fastevents.FastListenerCommon;
import api.mod.StarLoader;
import com.bulletphysics.linearmath.Transform;
import me.iron.stronghold.mod.ModMain;
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
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;

public class AlienGuardian implements Serializable {
    public LinkedList<String> catalogNames = new LinkedList<>();
    public int guardianFaction = -1;
    public float countMedian = 2;
    public float countStd = 1;
    private transient Random random = new Random();

    private static AlienGuardian instance;

    protected static AlienGuardian getInstance() {
        if (instance == null)
            return new AlienGuardian();
        else
            return instance;
    }

    public AlienGuardian() {
        instance = this;
        initListener();
    }

    protected void initListener() {
        StarLoader.registerListener(ShieldHitEvent.class, new Listener<ShieldHitEvent>() {
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
        }, ModMain.instance);

        FastListenerCommon.damageBeamHitListeners.add(new DamageBeamHitListener() {
            @Override
            public void handle(BeamState beamState, int i, BeamHandlerContainer<?> beamHandlerContainer, SegmentPiece segmentPiece, Vector3f vector3f, Vector3f vector3f1, Timer timer, Collection<Segment> collection, DamageBeamHitHandlerSegmentController damageBeamHitHandlerSegmentController) {
                int attackerFaction = beamHandlerContainer.getFactionId();
                int victimId = segmentPiece.getSegmentController().getFactionId();
                if (victimId == guardianFaction)
                    beamState.setPower(0);
            }
        });

        StarLoader.registerListener(AnyWeaponDamageCalculateEvent.class, new Listener<AnyWeaponDamageCalculateEvent>() {
            @Override
            public void onEvent(AnyWeaponDamageCalculateEvent anyWeaponDamageCalculateEvent) {
                int attackerFaction = anyWeaponDamageCalculateEvent.customOutputUnit.getSegmentController().getFactionId();
                if (attackerFaction == guardianFaction)
                    anyWeaponDamageCalculateEvent.damage = Float.MAX_VALUE;

            }
        }, ModMain.instance);
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

}
