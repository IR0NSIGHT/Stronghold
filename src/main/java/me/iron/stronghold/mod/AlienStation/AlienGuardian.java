package me.iron.stronghold.mod.AlienStation;

import api.listener.Listener;
import api.listener.events.systems.ShieldHitEvent;
import api.listener.events.weapon.AnyWeaponDamageCalculateEvent;
import api.listener.fastevents.CannonProjectileHitListener;
import api.listener.fastevents.DamageBeamHitListener;
import api.listener.fastevents.FastListenerCommon;
import api.mod.StarLoader;
import com.bulletphysics.linearmath.Transform;
import me.iron.stronghold.mod.ModMain;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.BeamHandlerContainer;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.controller.damage.beam.DamageBeamHitHandlerSegmentController;
import org.schema.game.common.controller.damage.projectile.ProjectileController;
import org.schema.game.common.controller.damage.projectile.ProjectileHandlerSegmentController;
import org.schema.game.common.controller.damage.projectile.ProjectileParticleContainer;
import org.schema.game.common.controller.elements.BeamState;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.physics.CubeRayCastResult;
import org.schema.game.common.data.world.Segment;
import org.schema.game.server.controller.BluePrintController;
import org.schema.game.server.controller.EntityAlreadyExistsException;
import org.schema.game.server.controller.EntityNotFountException;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.graphicsengine.core.Timer;

import javax.vecmath.Vector3f;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
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
        //will catch cannon shots but NOT missile or beam.
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

        FastListenerCommon.cannonProjectileHitListeners.add(new CannonProjectileHitListener() {

            @Override
            public ProjectileController.ProjectileHandleState handle(Damager damager, ProjectileController projectileController, Vector3f vector3f, Vector3f vector3f1, ProjectileParticleContainer projectileParticleContainer, int i, CubeRayCastResult cubeRayCastResult, ProjectileHandlerSegmentController projectileHandlerSegmentController) {
                return null;
            }

            @Override
            public ProjectileController.ProjectileHandleState handleBefore(Damager damager, ProjectileController projectileController, Vector3f vector3f, Vector3f vector3f1, ProjectileParticleContainer projectileParticleContainer, int i, CubeRayCastResult cubeRayCastResult, ProjectileHandlerSegmentController projectileHandlerSegmentController) {
            //    int damagerF = damager.getFactionId();
                int victimF = cubeRayCastResult.getSegment().getSegmentController().getFactionId();
                //TODO really whacky way to set the damage to zero. find a better way, ideally with a proper listener
                if (victimF == guardianFaction){
                    try {
                        // Create an instance of ProjectileHandlerSegmentController
                        Class<?> clazz = projectileHandlerSegmentController.getClass();
                        Field field = clazz.getDeclaredField("shotHandler");
                        field.setAccessible(true);

                        // Get the value of the field for the given instance
                        ProjectileHandlerSegmentController.ShotHandler shotHandler = (ProjectileHandlerSegmentController.ShotHandler) field.get(projectileHandlerSegmentController);

                        shotHandler.initialDamage = 0;

                        clazz = shotHandler.getClass();

                        // Get the Field object for the private field "dmg"
                        Field dmgField = clazz.getDeclaredField("dmg");

                        // Make the dmg field accessible
                        dmgField.setAccessible(true);

                        // Overwrite the value of the dmg field
                        dmgField.setFloat(shotHandler, 0); // Set the damage to 50.0f

                        System.out.println("hello");
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            public ProjectileController.ProjectileHandleState handleAfterIfNotStopped(Damager damager, ProjectileController projectileController, Vector3f vector3f, Vector3f vector3f1, ProjectileParticleContainer projectileParticleContainer, int i, CubeRayCastResult cubeRayCastResult, ProjectileHandlerSegmentController projectileHandlerSegmentController) {
                return null;
            }

            @Override
            public void handleAfterAlways(Damager damager, ProjectileController projectileController, Vector3f vector3f, Vector3f vector3f1, ProjectileParticleContainer projectileParticleContainer, int i, CubeRayCastResult cubeRayCastResult, ProjectileHandlerSegmentController projectileHandlerSegmentController) {

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
