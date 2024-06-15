package me.iron.stronghold.mod.AlienStation;

import api.listener.Listener;
import api.listener.events.systems.ShieldHitEvent;
import api.listener.events.weapon.AnyWeaponDamageCalculateEvent;
import api.mod.StarLoader;
import com.bulletphysics.linearmath.Transform;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.framework.AbstractAreaEffect;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.server.controller.BluePrintController;
import org.schema.game.server.controller.EntityAlreadyExistsException;
import org.schema.game.server.controller.EntityNotFountException;
import org.schema.game.server.data.GameServerState;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;

public class AlienGuardian extends AbstractAreaEffect {
    public LinkedList<String> catalogNames = new LinkedList<>();
    public int guardianFaction = -1;
    public float countMedian = 2;
    public float countStd = 1;

    private transient Listener<ShieldHitEvent> shieldHitEventListener;

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

    //TODO run initListeners on client after instantiation.

    public AlienGuardian() {

    }

    public AlienGuardian(String name) {
        super("AlienGuardian");
    }

    @Override
    protected void destroy() {
        super.destroy();
        if (shieldHitEventListener != null)
            StarLoader.unregisterListener(ShieldHitEvent.class, shieldHitEventListener);
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

   /*     FastListenerCommon.damageBeamHitListeners.add(new DamageBeamHitListener() {
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

        */

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
