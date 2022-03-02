package org.schema.game.client.view.gamemap;

import api.ModPlayground;
import api.listener.fastevents.FastListenerCommon;
import api.listener.fastevents.GameMapDrawListener;
import com.bulletphysics.linearmath.AabbUtil2;
import com.bulletphysics.linearmath.Transform;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import libpackage.drawer.MapDrawer;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.schema.common.FastMath;
import org.schema.common.util.ByteUtil;
import org.schema.common.util.CompareTools;
import org.schema.common.util.StringTools;
import org.schema.common.util.linAlg.Vector3fTools;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.controller.manager.ingame.map.MapControllerManager;
import org.schema.game.client.controller.manager.ingame.navigation.NavigationFilter;
import org.schema.game.client.controller.manager.ingame.navigation.NavigationFilterEditDialog;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.data.gamemap.GameMap;
import org.schema.game.client.view.camera.GameMapCamera;
import org.schema.game.client.view.effects.ConstantIndication;
import org.schema.game.client.view.gui.GalaxyOrientationElement;
import org.schema.game.client.view.gui.PlayerPanel;
import org.schema.game.client.view.gui.shiphud.HudIndicatorOverlay;
import org.schema.game.common.controller.trade.TradeActive;
import org.schema.game.common.controller.trade.TradeNodeClient;
import org.schema.game.common.controller.trade.TradeNodeStub;
import org.schema.game.common.data.element.Element;
import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.FleetMember;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.common.data.player.faction.FactionRelation.RType;
import org.schema.game.common.data.world.FTLConnection;
import org.schema.game.common.data.world.SystemRange;
import org.schema.game.common.data.world.VoidSystem;
import org.schema.game.server.data.Galaxy;
import org.schema.game.server.data.GalaxyTmpVars;
import org.schema.game.server.data.simulation.npc.NPCFaction;
import org.schema.game.server.data.simulation.npc.geo.FactionResourceRequestContainer;
import org.schema.game.server.data.simulation.npc.geo.NPCSystemStub;
import org.schema.schine.common.InputHandler;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.Timer;
import org.schema.schine.graphicsengine.core.*;
import org.schema.schine.graphicsengine.core.settings.EngineSettings;
import org.schema.schine.graphicsengine.forms.BoundingBox;
import org.schema.schine.graphicsengine.forms.PositionableSubColorSprite;
import org.schema.schine.graphicsengine.forms.Sprite;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUIProgressBar;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.graphicsengine.util.WorldToScreenConverter;
import org.schema.schine.graphicsengine.util.timer.SinusTimerUtil;
import org.schema.schine.input.KeyEventInterface;
import org.schema.schine.input.KeyboardMappings;

import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.nio.IntBuffer;
import java.util.*;
import java.util.Map.Entry;

public class GameMapDrawer implements Drawable, InputHandler {

    public static final Vector3i positionVec = new Vector3i();
    public final static int FILTER_X = 1;
    public final static int FILTER_Y = 2;
    public final static int FILTER_Z = 4;
    public static final float size = 100;
    public static final float halfsize = size * 0.5f;
    public static final float sectorSize = size / VoidSystem.SYSTEM_SIZEf;
    public static final float sectorSizeHalf = sectorSize * 0.5f;
    public static final NavigationFilterEditDialog filterCallback = null;
    private static final float systemCubeAlpha = 0.45f;
    public static IntBuffer selectBuffer = BufferUtils.createIntBuffer(2048);
    public static int filterAxis = 0;//FILTER_X;
    public static boolean drawFactionTerritory = true;
    public static boolean drawFactionByRelation = false;
    public static boolean drawPlanetOrbits = true;
    public static boolean drawAsteroidBeltOrbits = true;
    public static boolean drawWormHoles = true;
    public static boolean drawWarpGates = true;
    public static boolean highlightOrbitSectors;
    protected static long filterMask = NavigationFilter.POW_DOCKED | NavigationFilter.POW_FLOATINGROCK | NavigationFilter.POW_PLANET | NavigationFilter.POW_PLANET | NavigationFilter.POW_PLANET_CORE
            | NavigationFilter.POW_PLAYER | NavigationFilter.POW_SHIP | NavigationFilter.POW_SHIP | NavigationFilter.POW_SHOP | NavigationFilter.POW_SPACESTATION | NavigationFilter.POW_TURRET;
    public static final NavigationFilter filter = new NavigationFilter() {
        /**
         * @return the filter
         */
        @Override
        public long getFilter() {
            return GameMapDrawer.filterMask;
        }

        /**
         * @param filter the filter to set
         */
        @Override
        public void setFilter(long filter) {
            GameMapDrawer.filterMask = (filter);
        }
    };

    private static boolean drawResources = true;
    private static boolean debug = false;
    private final GameMapPosition gameMapPosition;
    private final GameClientState state;
    private final Vector3i playerSystem = new Vector3i();
    Matrix3f mY = new Matrix3f();
    Matrix3f mYB = new Matrix3f();
    Matrix3f mYC = new Matrix3f();
    Matrix3f mX = new Matrix3f();
    Matrix3f mXB = new Matrix3f();
    Matrix3f mXC = new Matrix3f();
    Vector3i tmp = new Vector3i();
    Vector3f posBuffer = new Vector3f();
    Transform worldpos = new Transform();
    int[] orbits = new int[8];
    Transform orbitRot = new Transform();
    Transform orbitRotInv = new Transform();
    private GameMapCamera camera;
    private SinusTimerUtil sinus = new SinusTimerUtil();
    PositionableSubColorSprite[] ownPosition = new PositionableSubColorSprite[]{new PositionableSubColorSprite() {
        Vector4f c = new Vector4f();
        private Vector3f pos = new Vector3f();

        @Override
        public Vector4f getColor() {
            c.set(((1.0f - sinus.getTime()) / 2f + 0.5f), 1, ((1.0f - sinus.getTime()) / 2f + 0.5f), 1);
            return c;
        }

        @Override
        public Vector3f getPos() {
            return pos;
        }

        @Override
        public float getScale(long time) {
            return 0.1f + (0.07f * sinus.getTime());
        }

        @Override
        public int getSubSprite(Sprite sprite) {
            return 10;
        }

        @Override
        public boolean canDraw() {
            return true;
        }

    }};
    private WorldToScreenConverter worldToScreenConverter;
    private GalaxyOrientationElement galaxyOrientationElement;
    private int coordList;
    private GUITextOverlay[] systemWallText;
    private Galaxy lastGalaxy;
    private Vector3i sys = new Vector3i();
    private ConstantIndication unexploredIndication;
    private GUIProgressBar npcStatus;

    public GameMapDrawer(GameClientState state) {
        this.state = state;
        this.gameMapPosition = new GameMapPosition(state, this);

    }

    public void checkSystem(Vector3i secPos) {
        if (state.getController().getClientChannel() != null) {
            state.getController().getClientChannel().getClientMapRequestManager().check(secPos);
            state.getController().getClientChannel().getGalaxyManagerClient().checkSystemBySectorPos(secPos);
        }
    }

    @Override
    public void cleanUp() {
        // TODO Auto-generated method stub

    }
    public boolean doDraw(){
        if (!isMapActive()) {
            return false;
        }

        if (state.getController().getClientChannel() == null || state.getController().getClientChannel().getClientMapRequestManager() == null) {
            return false;
        }

        return true;
    }
    @Override
    public void draw() {

        if(!doDraw()){
            return;
        }
        if(!checkGLErrorOnce){
            checkGLErrorOnce = Keyboard.isKeyDown(GLFW.GLFW_KEY_F2);
        }
        GL11.glClearColor(0, 0, 0, 1);

        GlUtil.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
        GlUtil.glDisable(GL11.GL_LIGHTING);
        GlUtil.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        MapControllerManager.selected.clear();

        drawResources = Keyboard.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT) && Keyboard.isKeyDown(GLFW.GLFW_KEY_MINUS);

        if (debug || checkGLErrorOnce) {
            GlUtil.printGlErrorCritical("begin");
        }

        if (debug || checkGLErrorOnce) {
            GlUtil.printGlErrorCritical("clear");
        }
        //INSERTED CODE
        for (GameMapDrawListener listener : FastListenerCommon.gameMapListeners) {
            listener.galaxy_PreDraw(this);
        }
        ///
        drawSystemClose();
        if (debug || checkGLErrorOnce) {
            GlUtil.printGlErrorCritical("after sys close");
        }
        drawHighlights();
        if (debug || checkGLErrorOnce) {
            GlUtil.printGlErrorCritical("after highlights");
        }
        drawNavConnection();

        drawFTLConnections();

        //drawTradeRoutes();

        drawGalaxyBack();

        drawSystemCloseFront();

        drawCoordinateSystem();

        //INSERTED CODE
        for (GameMapDrawListener listener : FastListenerCommon.gameMapListeners) {
            listener.galaxy_PostDraw(this);
        }
        ///

        if (debug || checkGLErrorOnce) {
            GlUtil.printGlErrorCritical("after close systems");
        }

        drawGalaxy();
        if (debug || checkGLErrorOnce) {
            GlUtil.printGlErrorCritical("after galaxy");
        }

        drawOrientation();

        if (debug || checkGLErrorOnce) {
            GlUtil.printGlErrorCritical("after galaxy");
        }

        GlUtil.glDisable(GL11.GL_LINE_SMOOTH);
        checkGLErrorOnce = false;
    }

    @Override
    public boolean isInvisible() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onInit() {

        camera = new GameMapCamera(state, this.gameMapPosition);
//		this.gameMapPosition.set(state.getPlayer().getCurrentSector().x, state.getPlayer().getCurrentSector().y, state.getPlayer().getCurrentSector().z, true);
        camera.setCameraStartOffset(88.487305f);
        camera.setCameraOffset(88.487305f);
        camera.alwaysAllowWheelZoom = true;

        npcStatus = new GUIProgressBar(getState(), 200, 24, new Object(){
            @Override
            public String toString() {
                if(npcStatus.getPercent() < 0f){
                    return Lng.str("Status pending...");
                }else{
                    return Lng.str("Status: %s%%", npcStatus.getPercent()*100f);
                }
            }

        }, true, null);
        npcStatus.onInit();
        mY.setIdentity();
        mY.rotY(FastMath.HALF_PI);
        mYB.setIdentity();
        mYB.rotY(-FastMath.HALF_PI);
        mYC.setIdentity();
        mYC.rotY(FastMath.PI);

        mX.setIdentity();
        mX.rotX(FastMath.HALF_PI);
        mXB.setIdentity();
        mXB.rotX(-FastMath.HALF_PI);
        mXC.setIdentity();
        mXC.rotX(FastMath.PI);

        worldpos.setIdentity();
        worldToScreenConverter = new WorldToScreenConverter();
        state.getCurrentGalaxy().onInit();

        galaxyOrientationElement = new GalaxyOrientationElement(state);
        galaxyOrientationElement.onInit();

        systemWallText = new GUITextOverlay[12];

        for (int i = 0; i < systemWallText.length; i++) {
            systemWallText[i] = new GUITextOverlay(100, 20, FontLibrary.getBoldArial32(), state);
            systemWallText[i].setTextSimple("TEST");
        }

        createCoordinateSystem();
    }

    public Vector3i getPlayerSystem() {
        return VoidSystem.getContainingSystem(getPlayerSector(), sys);
    }
    public Vector3i getPlayerSector() {
        return (state.getPlayer().isInTutorial() || state.getPlayer().isInTestSector() || state.getPlayer().isInPersonalSector())
                ? new Vector3i(0, 0, 0) : state.getPlayer().getCurrentSector();
    }
    private final Random r = new Random();
    private float time;

    public static boolean debugSecret = true;//EngineSettings.SECRET.getCurrentState().toString().contains("GAMEMAP");


    public static Vector3i highlightIcon;

    private boolean drawAllFleets(){
        return false;//!getState().getGameState().isFow();
    }
    private boolean showAllTrades(){
        return false;//!getState().getGameState().isFow();
    }
    private boolean seeAllFov(){
        return !getState().getGameState().isFow();
    }
    private boolean drawAllFleetTarget(){return !getState().getGameState().isFow();}


    private float[] param = new float[1];
    private Vector3f normal = new Vector3f();

    private void drawInner() {
        Set<Entry<Vector3i, GameMap>> entrySet = state.getController().getClientChannel().getClientMapRequestManager().getSystemMap().entrySet();

        VoidSystem.getPosFromSector(getPlayerSector(), playerSystem);
        for (Entry<Vector3i, GameMap> e : entrySet) {
            boolean active = gameMapPosition.isActive(e.getKey());
            if (!active) {
                continue;
            }
            HudIndicatorOverlay.toDrawMapInterfaces.add(e.getValue());

            GlUtil.glPushMatrix();

            GlUtil.translateModelview(e.getKey().x * size, e.getKey().y * size, e.getKey().z * size);

            GlUtil.translateModelview(-halfsize, -halfsize, -halfsize);

            GlUtil.glDisable(GL11.GL_LIGHTING);
            GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
            GlUtil.glEnable(GL11.GL_BLEND);
            GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            //has to be done here for all the sprite culling
            camera.updateFrustum();
            //drawNodesSprites(e, active);

            GlUtil.glEnable(GL11.GL_LIGHTING);
            GlUtil.glDisable(GL11.GL_TEXTURE_2D);
            GlUtil.glEnable(GL11.GL_CULL_FACE);
            GlUtil.glEnable(GL11.GL_BLEND);
            GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
            GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);


            if (active) {
                Vector3i curPos = gameMapPosition.get(new Vector3i());
                if (!curPos.equals(getPlayerSector()) && isVisibleSector(curPos)) {
                    GlUtil.glPushMatrix();
                    GlUtil.translateModelview(-e.getKey().x * size, -e.getKey().y * size, -e.getKey().z * size);
                    drawSelectedSector();
                    GlUtil.glPopMatrix();
                }
            }

            GlUtil.glPopMatrix();
        }




        drawOwnPosition();
        GlUtil.glColor4f(1,1,1,1);
    }
    private void drawFleets(){
        if(drawAllFleets()){
            synchronized(state){
                drawFleet(getState().getFleetManager().fleetCache.keySet());
            }
        }else{
            synchronized(state){
                drawFleet(getState().getFleetManager().fleetCache.keySet());
            }

        }
    }
    private void drawFleet(LongSet fleets){
        String player = getState().getPlayer().getName().toLowerCase(Locale.ENGLISH);
        if(fleets != null){
            for(long fleetId : fleets){
                Fleet fleet = getState().getFleetManager().fleetCache.get(fleetId);

                if(fleet != null && !fleet.getMembers().isEmpty()){

                    boolean ownFleet = player.equals(fleet.getOwner().toLowerCase(Locale.ENGLISH));
//					if(ownFleet){
//						System.err.println("OWNER:: "+player+"; "+fleet.getOwner()+": own "+ownFleet);
//					}
                    boolean fleetInOwnVisibleSystem =
                            (isVisibleSystem(getState().getPlayer().getCurrentSystem()) &&
                                    SystemRange.isInSystem(	fleet.getFlagShip().getSector(), getState().getPlayer().getCurrentSystem())
                            );
                    FleetMember m = fleet.getMembers().get(0);
                    if(drawAllFleets() || ownFleet ){//|| fleetInOwnVisibleSystem){

                        GlUtil.glPushMatrix();

                        r.setSeed(m.entityDbId);

                        GlUtil.translateModelview(
                                -halfsize + sectorSizeHalf + (r.nextFloat() * (sectorSizeHalf*2f) - sectorSizeHalf),
                                -halfsize + sectorSizeHalf + (r.nextFloat() * (sectorSizeHalf*2f) - sectorSizeHalf),
                                -halfsize + sectorSizeHalf + (r.nextFloat() * (sectorSizeHalf*2f) - sectorSizeHalf));

                        GlUtil.glDisable(GL11.GL_LIGHTING);
                        GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
                        GlUtil.glEnable(GL11.GL_BLEND);
                        GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                        Sprite sprite = Controller.getResLoader().getSprite("map-sprites-8x2-c-gui-");
                        sprite.setBillboard(true);
                        sprite.setBlend(true);
                        sprite.setFlip(true);

                        HudIndicatorOverlay.toDrawFleet.add(m.mapEntry);

                        gameMapPosition.get(positionVec);

                        camera.updateFrustum();
                        Sprite.draw3D(sprite, new PositionableSubColorSprite[]{m.mapEntry}, camera);

                        sprite.setBillboard(false);
                        sprite.setFlip(false);

                        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
                        GlUtil.glEnable(GL11.GL_LIGHTING);
                        GlUtil.glEnable(GL11.GL_CULL_FACE);
                        GlUtil.glEnable(GL11.GL_BLEND);
                        GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
                        GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                        GlUtil.glPopMatrix();

                    }
                    if(fleet.getCurrentMoveTarget() != null && ( ownFleet )){
                        startDrawDottedLine();
                        drawDottedLine(m.getSector(), fleet.getCurrentMoveTarget(), new Vector4f(0,1,1,1), time);
                        endDrawDottedLine();
                    }

                }

            }
        }
    }

    Vector3f fromTmp = new Vector3f();
    Vector3f toTmp = new Vector3f();
    Vector3f aabbMin = new Vector3f();
    Vector3f aabbMax = new Vector3f();
    private boolean canSeeTarget(Vector3i from, Vector3i to) {
        if(isVisibleSystem(getState().getPlayer().getCurrentSystem())){
            int x = getState().getPlayer().getCurrentSystem().x * VoidSystem.SYSTEM_SIZE;
            int y = getState().getPlayer().getCurrentSystem().y * VoidSystem.SYSTEM_SIZE;
            int z = getState().getPlayer().getCurrentSystem().z * VoidSystem.SYSTEM_SIZE;

            int xEnd = x+VoidSystem.SYSTEM_SIZE;
            int yEnd = y+VoidSystem.SYSTEM_SIZE;
            int zEnd = z+VoidSystem.SYSTEM_SIZE;

            aabbMin.set(x,y,z);
            aabbMax.set(xEnd,yEnd,zEnd);

            fromTmp.set(from.x, from.y, from.z);
            toTmp.set(to.x, to.y, to.z);
            param[0] = 1;
            boolean rayAabb = AabbUtil2.rayAabb(fromTmp, toTmp, aabbMin, aabbMax, param, normal);
            return rayAabb;
        }
        return false;
    }

    private boolean isVisibleSector(Vector3i curPos) {
        return seeAllFov() || getState().getController().getClientChannel().getGalaxyManagerClient().isSectorVisiblyClientIncludingLastVisited(curPos);
    }
    private boolean isVisibleSystem(Vector3i system) {
        return seeAllFov() || getState().getController().getClientChannel().getGalaxyManagerClient().isSystemVisiblyClient(system);
    }

    private void drawLocalSystemBack(Map<Vector3i, VoidSystem> clientData) {
        GlUtil.glPushMatrix();

        GlUtil.translateModelview(
                gameMapPosition.getCurrentSysPos().x * size,
                gameMapPosition.getCurrentSysPos().y * size,
                gameMapPosition.getCurrentSysPos().z * size);
        GL11.glCullFace(GL11.GL_FRONT);
        VoidSystem voidSystem = clientData.get(gameMapPosition.getCurrentSysPos());
        if (voidSystem == null || voidSystem.getOwnerFaction() == 0 || !state.getFactionManager().existsFaction(voidSystem.getOwnerFaction())) {
            drawBox(halfsize, systemCubeAlpha, true);
        }
        GL11.glCullFace(GL11.GL_BACK);

        GlUtil.glPopMatrix();
    }

    private void drawLocalSystemFront(Map<Vector3i, VoidSystem> clientData) {
        GlUtil.glPushMatrix();

        GlUtil.translateModelview(
                gameMapPosition.getCurrentSysPos().x * size,
                gameMapPosition.getCurrentSysPos().y * size,
                gameMapPosition.getCurrentSysPos().z * size);

        VoidSystem voidSystem = clientData.get(gameMapPosition.getCurrentSysPos());
        if (voidSystem == null || voidSystem.getOwnerFaction() == 0 || !state.getFactionManager().existsFaction(voidSystem.getOwnerFaction())) {
            drawBox(halfsize, systemCubeAlpha, true);
        }

        for (int i = 0; i < 12; i++) {
            drawBoxTexts(halfsize, 1, gameMapPosition.getCurrentSysPos(), i, i > 5);
        }
        GlUtil.glPopMatrix();
    }

    private void drawGrids() {
        //INSERTED CODE
        if(GlUtil.loadedShader != null){
            System.err.println("SHADER::: " + GlUtil.loadedShader);
        }
        ///
        GlUtil.glPushMatrix();
        GlUtil.translateModelview(
                gameMapPosition.getCurrentSysPos().x * size - halfsize,
                gameMapPosition.getCurrentSysPos().y * size - halfsize,
                gameMapPosition.getCurrentSysPos().z * size - halfsize);

        //			worldToScreenConverter.storeCurrentModelviewProjection();
        GlUtil.glDisable(GL11.GL_LIGHTING);
        GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glEnable(GL11.GL_TEXTURE_2D);
        GlUtil.glActiveTexture(GL13.GL_TEXTURE0);
        GlUtil.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
        GlUtil.glColor4fForced(1,1,1,1);

        GlUtil.glEnable(GL11.GL_BLEND);
        GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        drawSystemGrid(-size, size, 1.0f, 1.0f);
        GlUtil.glEnable(GL11.GL_BLEND);
        GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GlUtil.glDisable(GL11.GL_LIGHTING);
        GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glEnable(GL11.GL_TEXTURE_2D);
        GlUtil.glActiveTexture(GL13.GL_TEXTURE0);
        GlUtil.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
        GlUtil.glColor4fForced(1,1,1,1);
        drawGrid(size, size / VoidSystem.SYSTEM_SIZEf, 0.8f, 0.9f);
        GlUtil.glPopMatrix();
    }

    private void drawSystemClose() {
        GlUtil.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GlUtil.glPushMatrix();

        GlUtil.glLoadIdentity();
        camera.lookAt(true);
        camera.updateFrustum();

        AbstractScene.mainLight.draw();

        GlUtil.glEnable(GL11.GL_CULL_FACE);
        GlUtil.glEnable(GL11.GL_BLEND);
        GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
        GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlUtil.glDisable(GL11.GL_LIGHTING);
        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
        GlUtil.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GlUtil.glPushMatrix();
        worldToScreenConverter.storeCurrentModelviewProjection();
        GlUtil.glPopMatrix();
        GlUtil.glColor4fForced(1, 1, 1, 1);
        Map<Vector3i, VoidSystem> clientData = state.getController().getClientChannel().getGalaxyManagerClient().getClientData();
        //draw active sector's grid
        drawLocalSystemBack(clientData);
        GlUtil.glColor4fForced(1, 1, 1, 1);
        GlUtil.glDisable(GL11.GL_LIGHTING);
        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
        GlUtil.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        drawGrids();

        Transform t = new Transform();
        t.setIdentity();
        t.origin.set(gameMapPosition.getCurrentSysPos().x * size , gameMapPosition.getCurrentSysPos().y * size , gameMapPosition.getCurrentSysPos().z * size );
        unexploredIndication = new ConstantIndication(t, Lng.str("UNEXPLORED (scan or visit %s sectors)", getState().getGameState().getSectorsToExploreForSystemScan()));
        GlUtil.glDisable(GL11.GL_LIGHTING);
        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
        GlUtil.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        //INSERTED CODE
        ArrayList<GameMapDrawListener> listeners = FastListenerCommon.gameMapListeners;
        Vector3i sysPos = gameMapPosition.getCurrentSysPos();
        ///
        if(isVisibleSystem(gameMapPosition.getCurrentSysPos())){
            //INSERTED CODE
            for (GameMapDrawListener listener : listeners) {
                listener.system_PreDraw(this, sysPos, true);
            }
            ///
            drawInner();
            drawOrbits();
            //INSERTED CODE
            for (GameMapDrawListener listener : listeners) {
                listener.system_PostDraw(this, sysPos, true);
            }
            ///
        }else{
            //INSERTED CODE
            for (GameMapDrawListener listener : listeners) {
                listener.system_PreDraw(this, sysPos, false);
            }
            ///
            GlUtil.glDisable(GL11.GL_LIGHTING);
            GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
            GlUtil.glEnable(GL11.GL_BLEND);
            GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlUtil.glDisable(GL11.GL_TEXTURE_2D);
            GlUtil.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            GlUtil.glEnable(GL11.GL_CULL_FACE);
            GlUtil.glEnable(GL11.GL_BLEND);
            GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
            GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            drawOwnPosition();
            GlUtil.glColor4f(1, 1, 1, 1);
            GlUtil.glDisable(GL11.GL_LIGHTING);
            GlUtil.glDisable(GL11.GL_TEXTURE_2D);
            GlUtil.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            unexploredIndication.setText(Lng.str("UNEXPLORED (scan or visit %s/%s sectors)",
                    getState().getPlayer().getFogOfWar().getScannedCountForSystem(gameMapPosition.getCurrentSysPos()), getState().getGameState().getSectorsToExploreForSystemScan()));

            HudIndicatorOverlay.toDrawMapTexts.add(unexploredIndication);
            //INSERTED CODE
            for (GameMapDrawListener listener : listeners) {
                listener.system_PostDraw(this, sysPos, false);
            }
            ///
        }




        //drawTradeNodes();

        drawFleets();

        GlUtil.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
        GlUtil.glEnable(GL11.GL_LIGHTING);
        GlUtil.glDisable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glEnable(GL11.GL_CULL_FACE);
        GlUtil.glDisable(GL11.GL_BLEND);
        GL11.glCullFace(GL11.GL_BACK);

        GlUtil.glDisable(GL11.GL_BLEND);
        GlUtil.glEnable(GL11.GL_DEPTH_TEST);
        GlUtil.glDisable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glEnable(GL11.GL_LIGHTING);

        GlUtil.glPopMatrix();
        GlUtil.glColor4f(1, 1, 1, 1);

    }

    private void drawSystemCloseFront() {
        GlUtil.glPushMatrix();

        GlUtil.glLoadIdentity();
        camera.lookAt(true);
        camera.updateFrustum();

        AbstractScene.mainLight.draw();

        GlUtil.glEnable(GL11.GL_CULL_FACE);
        GlUtil.glEnable(GL11.GL_BLEND);
        GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GlUtil.glPushMatrix();
        worldToScreenConverter.storeCurrentModelviewProjection();
        GlUtil.glPopMatrix();

        Map<Vector3i, VoidSystem> clientData = state.getController().getClientChannel().getGalaxyManagerClient().getClientData();

        drawLocalSystemFront(clientData);

        GlUtil.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
        GlUtil.glEnable(GL11.GL_LIGHTING);
        GlUtil.glDisable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glEnable(GL11.GL_CULL_FACE);
        GlUtil.glDisable(GL11.GL_BLEND);
        GL11.glCullFace(GL11.GL_BACK);

        GlUtil.glDisable(GL11.GL_BLEND);
        GlUtil.glEnable(GL11.GL_DEPTH_TEST);
        GlUtil.glDisable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glEnable(GL11.GL_LIGHTING);

        GlUtil.glPopMatrix();
        GlUtil.glColor4f(1, 1, 1, 1);
    }

    private void drawOrientation() {

        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        Galaxy galaxy = state.getCurrentGalaxy();

        GlUtil.glMatrixMode(GL11.GL_PROJECTION);
        GlUtil.glPushMatrix();

        float aspect = (float) GLFrame.getWidth() / (float) GLFrame.getHeight(); //1.333333333333333333333333f
        GlUtil.gluPerspective(Controller.projectionMatrix, (Float) EngineSettings.G_FOV.getCurrentState(), aspect, 10, 25000, true);
        GlUtil.glMatrixMode(GL11.GL_MODELVIEW);
        GlUtil.glPushMatrix();

        GlUtil.glLoadIdentity();
        camera.lookAt(true);

        if (debug) {
            GlUtil.printGlErrorCritical("after galaxy intern draw");
        }
        galaxyOrientationElement.draw();
        GlUtil.glPopMatrix();
        GlUtil.glColor4f(1, 1, 1, 1);

        GlUtil.glMatrixMode(GL11.GL_PROJECTION);
        GlUtil.glPopMatrix();
        GlUtil.glMatrixMode(GL11.GL_MODELVIEW);
    }
    private List<Quad> quadBuffer = new ObjectArrayList<Quad>();
    private void drawGalaxyFactions() {
        if (debug || checkGLErrorOnce) {
            GlUtil.printGlErrorCritical("before factions");
        }
        Map<Vector3i, VoidSystem> clientData = state.getController().getClientChannel().getGalaxyManagerClient().getClientData();
        highlightIcon = null;

        for (Entry<Vector3i, VoidSystem> e : clientData.entrySet()) {
            Vector3i pos = e.getKey();
            bufferBoxFaction(halfsize-0.01f, 0.25f, pos, clientData, quadBuffer);

            if(!getState().getGameState().isNpcDebug() && getState().getFactionManager().getFaction(e.getValue().getOwnerFaction()) != null && pos.equals(getGameMapPosition().getCurrentSysPos())){

                float status = getState().getGameState().getClientNPCSystemStatus(e.getValue().getOwnerFaction(), pos);

                ObjectArrayList<String> ss = new ObjectArrayList<String>();

                Faction f = getState().getFactionManager().getFaction(e.getValue().getOwnerFaction());

                if(f != null){
                    ss.add(Lng.str("System Information:"));
                    ss.add(Lng.str("Claimed by %s",f.getName()));

                    RType relation = getState().getFactionManager().getRelation(getState().getPlayerName(),
                            getState().getPlayer().getFactionId(), f.getIdFaction());

                    switch(relation){
                        case ENEMY:
                            ss.add(Lng.str("You are currently at war!"));
                            break;
                        case FRIEND:
                            ss.add(Lng.str("You are currently allied!"));
                            break;
                        case NEUTRAL:
                            ss.add(Lng.str("You are neutral towards this faction!"));
                            break;
                        default:
                            break;

                    }
                    if(FactionManager.isNPCFaction(e.getValue().getOwnerFaction())){
                        if(status < 0f){
                            ss.add(Lng.str("System Status pending...", status));
                        }else{
                            ss.add(Lng.str("System Status %s%%", StringTools.formatPointZero(status*100f)));
                        }
                    }
                }else{
                    ss.add(Lng.str("System Request pending...", status));
                }
                if(e.getValue().getOwnerPos() != null){
                    highlightIcon = e.getValue().getOwnerPos();
                }



                StringBuffer b = new StringBuffer();
                for(int i = 0; i < ss.size(); i++){
                    b.append(ss.get(i));
                    b.append("\n");
                }

                PlayerPanel.infoText = b.toString();


            }else if(getState().getGameState().isNpcDebug() && FactionManager.isNPCFaction(e.getValue().getOwnerFaction()) && pos.equals(getGameMapPosition().getCurrentSysPos())){



                NPCSystemStub npcSystem = getState().getGameState().getClientNPCSystemMap().get(pos);

                if(npcSystem != null && ((NPCFaction)getState().getFactionManager().getFaction(npcSystem.getFactionId())) != null){

                    highlightIcon = npcSystem.systemBase;


                    PlayerPanel.infoText = String.format(
                            "FACTION %s\n\n"
                                    + "Status: %s%%\n"
                                    + "ResAvail: %s%% (mined %s)\n"
                                    + "Diplomacy: %s\n"
                                    + "Level: %s\n"
                                    + "DistFact: %s\n"
                                    + "BaseUID: %s\n"
                                    + "BaseSector: %s\n"
                                    + "Weight: %s\n"
                                    + "Lost: %s\n"
                                    + "\nContingent:\n%s\n\n"
                            ,npcSystem.getFactionId()
                            ,StringTools.formatPointZero(npcSystem.status*100d)
                            ,StringTools.formatPointZero(npcSystem.resourcesAvailable*100f), npcSystem.minedResources
                            ,((NPCFaction)getState().getFactionManager().getFaction(npcSystem.getFactionId())).getDiplomacy().printFor(getState().getPlayer())
                            ,npcSystem.getLevel()
                            ,npcSystem.distanceFactor
                            ,npcSystem.systemBaseUID
                            ,npcSystem.systemBase
                            ,npcSystem.getWeight()
                            ,npcSystem.getContingent().spawnedEntities.getLostEntities().size()
                            ,npcSystem.getContingent()

                    );
                }
            }

//			GlUtil.glPopMatrix();
        }

        Collections.sort(quadBuffer);
        GlUtil.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
        GlUtil.glDisable(GL11.GL_LIGHTING);
        GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glEnable(GL11.GL_BLEND);
        GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glCullFace(GL11.GL_BACK);

        GlUtil.glBegin(GL11.GL_QUADS);


        for(Quad q : quadBuffer){

            if(q.userData == this.camInsideFactionSystem){
                q.verticesReverse();
            }else{
                q.vertices();
            }
            freeQuad(q);
        }
        GlUtil.glEnd();
        if (debug || checkGLErrorOnce) {
            GlUtil.printGlErrorCritical("after factions aft end");
        }

        quadBuffer.clear();
        this.camInsideFactionSystem = 0;

    }

    private void drawGalaxyResources() {
        GlUtil.glEnable(GL11.GL_CULL_FACE);
        GlUtil.glEnable(GL11.GL_BLEND);
        GlUtil.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
        GlUtil.glDisable(GL11.GL_LIGHTING);
        GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Map<Vector3i, VoidSystem> clientData = state.getController().getClientChannel().getGalaxyManagerClient().getClientData();
        Galaxy galaxy = state.getCurrentGalaxy();

        FactionResourceRequestContainer r = new FactionResourceRequestContainer();
        GalaxyTmpVars t = new GalaxyTmpVars();
        GlUtil.glColor4f(1, 1, 1, 1);
//		for (StarPosition pos : galaxy.getSpriteCollection()) {
//
//			galaxy.getSystemResourcesFromLocal(
//					pos.relPosInGalaxy, r, t);
//			if(r.res[0] > 0){
//				GlUtil.glPushMatrix();
//
//				GlUtil.translateModelview((pos.relPosInGalaxy.x-Galaxy.halfSize) * size, (pos.relPosInGalaxy.y-Galaxy.halfSize) * size, (pos.relPosInGalaxy.z-Galaxy.halfSize) * size);
//				drawBox(halfsize, r.res[0] / 100f, false);
//				GlUtil.glPopMatrix();
//			}
//		}
        int range = 16;
        Vector3i sys = new Vector3i();
        for (int z = -16;z < 16; z++) {
            for (int y = -16;y < 16; y++) {
                for (int x = -16;x < 16; x++) {
                    sys.set(x, y, z);
                    galaxy.getSystemResources(sys, r, t);
                    if(r.res[0] > 70){
                        GlUtil.glPushMatrix();

                        GlUtil.translateModelview((sys.x) * size, (sys.y) * size, (sys.z) * size);
                        drawBox(halfsize, r.res[0] / 127f, false);
                        GlUtil.glPopMatrix();
                    }
                }
            }
        }
    }

    private void drawGalaxyBack() {

        GlUtil.glMatrixMode(GL11.GL_PROJECTION);
        GlUtil.glPushMatrix();

        float aspect = (float) GLFrame.getWidth() / (float) GLFrame.getHeight(); //1.333333333333333333333333f
        GlUtil.gluPerspective(Controller.projectionMatrix, (Float) EngineSettings.G_FOV.getCurrentState(), aspect, 10, 25000, true);
        GlUtil.glMatrixMode(GL11.GL_MODELVIEW);
        GlUtil.glPushMatrix();

        GlUtil.glLoadIdentity();
        camera.lookAt(true);

        GL11.glCullFace(GL11.GL_FRONT);
//		if (drawFactionTerritory) {
//			drawGalaxyFactions();
//		}
        if (drawResources) {
            drawGalaxyResources();
        }
        GL11.glCullFace(GL11.GL_BACK);
        GlUtil.glPopMatrix();
        GlUtil.glColor4f(1, 1, 1, 1);

        GlUtil.glMatrixMode(GL11.GL_PROJECTION);
        GlUtil.glPopMatrix();
        GlUtil.glMatrixMode(GL11.GL_MODELVIEW);

        GlUtil.glDisable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glEnable(GL11.GL_CULL_FACE);
        GlUtil.glDisable(GL11.GL_BLEND);
        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
    }

    private void drawGalaxy() {
        if (!Galaxy.USE_GALAXY) {
            GlUtil.glColor4f(1, 1, 1, 1);
            GL11.glColor4f(1, 1, 1, 1);
            return;
        }
        Galaxy galaxy = state.getCurrentGalaxy();

        GlUtil.glMatrixMode(GL11.GL_PROJECTION);
        GlUtil.glPushMatrix();

        float aspect = (float) GLFrame.getWidth() / (float) GLFrame.getHeight(); //1.333333333333333333333333f
        GlUtil.gluPerspective(Controller.projectionMatrix, (Float) EngineSettings.G_FOV.getCurrentState(), aspect, 10, EngineSettings.G_DRAW_SURROUNDING_GALAXIES_IN_MAP.isOn() ? 75000 : 25000, true);
        GlUtil.glMatrixMode(GL11.GL_MODELVIEW);
        GlUtil.glPushMatrix();

        GlUtil.glLoadIdentity();
        camera.lookAt(true);

//		System.err.println("POS: "+camera.getPos());
        if (debug) {
            GlUtil.printGlErrorCritical("after proj");
        }

        AbstractScene.mainLight.draw();
        GlUtil.glEnable(GL11.GL_CULL_FACE);
        GlUtil.glEnable(GL11.GL_BLEND);
        GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        if (debug) {
            GlUtil.printGlErrorCritical("after blend");
        }
        GlUtil.glPushMatrix();
        //		GlUtil.translateModelview(-halfsize, -halfsize, -halfsize);
        worldToScreenConverter.storeCurrentModelviewProjection();
        GlUtil.glPopMatrix();
        if (debug) {
            GlUtil.printGlErrorCritical("after convert");
        }
        gameMapPosition.get(positionVec);

        if (drawFactionTerritory) {
            drawGalaxyFactions();
        }
        if (drawResources) {
            drawGalaxyResources();
        }
        if (debug) {
            GlUtil.printGlErrorCritical("after Box");
        }
        //INSERTED CODE
        for (GameMapDrawListener listener : FastListenerCommon.gameMapListeners) {
            listener.galaxy_DrawSprites(this);
        }
        ///

        if (Galaxy.USE_GALAXY) {

            if (EngineSettings.G_DRAW_SURROUNDING_GALAXIES_IN_MAP.isOn()) {
                for (int z = -1; z < 2; z++) {
                    for (int y = -1; y < 2; y++) {
                        for (int x = -1; x < 2; x++) {
                            getState().getCurrentGalaxyNeighbor(new Vector3i(x, y, z)).draw(camera, (int) size, 0.25f);
                        }
                    }
                }
            } else {
                //get distance to currently select sector ==> zoomfactor
                Vector3f campos = new Vector3f(getCamera().getPos());
                Vector3f selectedPos = MapDrawer.posFromSector(gameMapPosition.get(new Vector3i()),false);
                campos.sub(selectedPos);
                float dist = campos.length()/100;//Math.round(campos.length()/200)*200;
                float multi = (float) Math.max(0, Math.min(0.25f,(float) (1/300f* Math.pow(dist,2))-0.025));//Math.min(1,dist/1000f)*0.2f + 0.05f;
                galaxy.draw(camera, (int) size, multi); //multi descides stars drawn
            }

            GlUtil.glDisable(GL11.GL_COLOR_MATERIAL);
            GlUtil.glEnable(GL11.GL_CULL_FACE);
            GlUtil.glDisable(GL11.GL_BLEND);
            GlUtil.glDisable(GL11.GL_TEXTURE_2D);
        }

        if (debug) {
            GlUtil.printGlErrorCritical("after galaxy");
        }

        GlUtil.glDisable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glEnable(GL11.GL_CULL_FACE);
        GlUtil.glDisable(GL11.GL_BLEND);
        GlUtil.glDisable(GL11.GL_TEXTURE_2D);

        if (debug) {
            GlUtil.printGlErrorCritical("after galaxy intern draw");
        }
        GlUtil.glPopMatrix();
        GlUtil.glColor4f(1, 1, 1, 1);

        GlUtil.glMatrixMode(GL11.GL_PROJECTION);
        GlUtil.glPopMatrix();
        GlUtil.glMatrixMode(GL11.GL_MODELVIEW);
    }

    private void createCoordinateSystem() {
        coordList = GL11.glGenLists(1);
        GL11.glNewList(coordList, GL11.GL_COMPILE);

        GlUtil.glBegin(GL11.GL_LINES);

        GlUtil.glColor4f(0.5f, 0.0f, 0.0f, 0.5f);

        GL11.glVertex3f(0, 0, 0);
        GL11.glVertex3f(64 * size, 0, 0);

        GL11.glVertex3f(0, 0, 0);
        GL11.glVertex3f(-64 * size, 0, 0);

        GlUtil.glColor4f(0.0f, 0.5f, 0.0f, 0.5f);
        GL11.glVertex3f(0, 0, 0);
        GL11.glVertex3f(0, 64 * size, 0);

        GL11.glVertex3f(0, 0, 0);
        GL11.glVertex3f(0, -64 * size, 0);

        GlUtil.glColor4f(0.0f, 0.0f, 0.5f, 0.5f);
        GL11.glVertex3f(0, 0, 0);
        GL11.glVertex3f(0, 0, 64 * size);

        GL11.glVertex3f(0, 0, 0);
        GL11.glVertex3f(0, 0, -64 * size);

        GlUtil.glEnd();

        GL11.glEndList();
    }

    private void drawCoordinateSystem() {
        GlUtil.glDisable(GL11.GL_LIGHTING);
        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
        GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glEnable(GL11.GL_BLEND);
        GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GlUtil.glMatrixMode(GL11.GL_PROJECTION);
        GlUtil.glPushMatrix();

        float aspect = (float) GLFrame.getWidth() / (float) GLFrame.getHeight(); //1.333333333333333333333333f
        GlUtil.gluPerspective(Controller.projectionMatrix, (Float) EngineSettings.G_FOV.getCurrentState(), aspect, 10, 25000, true);
        GlUtil.glMatrixMode(GL11.GL_MODELVIEW);
        GlUtil.glPushMatrix();

        GlUtil.glLoadIdentity();

        camera.lookAt(true);
        GlUtil.translateModelview(-halfsize, -halfsize, -halfsize);
        GL11.glCallList(coordList);
        GlUtil.glPopMatrix();
        GlUtil.glColor4f(1, 1, 1, 1);

        GlUtil.glMatrixMode(GL11.GL_PROJECTION);
        GlUtil.glPopMatrix();
        GlUtil.glMatrixMode(GL11.GL_MODELVIEW);

    }

    public GameClientState getState() {
        return state;
    }

    private void drawNavConnection() {
        Vector3i waypoint = state.getController().getClientGameData().getWaypoint();
        if (waypoint != null) {
            GlUtil.glDisable(GL11.GL_LIGHTING);
            GlUtil.glDisable(GL11.GL_TEXTURE_2D);
            GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
            GlUtil.glEnable(GL11.GL_BLEND);
            GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            GlUtil.glMatrixMode(GL11.GL_PROJECTION);
            GlUtil.glPushMatrix();

            float aspect = (float) GLFrame.getWidth() / (float) GLFrame.getHeight(); //1.333333333333333333333333f
            GlUtil.gluPerspective(Controller.projectionMatrix, (Float) EngineSettings.G_FOV.getCurrentState(), aspect, 10, 25000, true);
            GlUtil.glMatrixMode(GL11.GL_MODELVIEW);
            GlUtil.glPushMatrix();

            GlUtil.glLoadIdentity();

            camera.lookAt(true);
            GlUtil.translateModelview(-halfsize, -halfsize, -halfsize);
            GlUtil.glBegin(GL11.GL_LINES);

            Vector3i from = getPlayerSector();

            GlUtil.glColor4f(0.1f + HudIndicatorOverlay.selectColorValue, 0.8f + HudIndicatorOverlay.selectColorValue, 0.6f + HudIndicatorOverlay.selectColorValue, 0.4f + HudIndicatorOverlay.selectColorValue);
            GL11.glVertex3f(from.x * sectorSize + sectorSizeHalf, from.y * sectorSize + sectorSizeHalf, from.z * sectorSize + sectorSizeHalf);
            GL11.glVertex3f(waypoint.x * sectorSize + sectorSizeHalf, waypoint.y * sectorSize + sectorSizeHalf, waypoint.z * sectorSize + sectorSizeHalf);

            GlUtil.glEnd();

            GlUtil.glPopMatrix();
            GlUtil.glColor4f(1, 1, 1, 1);

            GlUtil.glMatrixMode(GL11.GL_PROJECTION);
            GlUtil.glPopMatrix();
            GlUtil.glMatrixMode(GL11.GL_MODELVIEW);
        }
    }


    public void drawTradeRoutes(){
        List<TradeActive> tn = state.getGameState().getTradeManager().getTradeActiveMap().getTradeList();


        startDrawDottedLine();

        Vector4f color = new Vector4f(0.1f, 1.0f, 0.1775f, 0.7f);
        for(TradeActive t : tn){
            if(!t.getSectorWayPoints().isEmpty()){
                if(showAllTrades() ||
                        t.canView(state.getPlayer()) ||
                        canSeeTarget(t.getStartSector(), t.getSectorWayPoints().get(0))){

//					if(showAllTrades || (isVisibleSector(t.getStartSector()) || isVisibleSector(t.getSectorWayPoints().get(0)))){
                    drawDottedLine(t.getStartSector(), t.getSectorWayPoints().get(0), color, time);
//					}
                }
            }
        }
        endDrawDottedLine();
    }
    public void drawTradeNodes(){
        Long2ObjectOpenHashMap<TradeNodeStub> tradeNodeDataById = state.getController().getClientChannel().getGalaxyManagerClient().getTradeNodeDataById();


        for(TradeNodeStub ma : tradeNodeDataById.values()){
            TradeNodeClient m = (TradeNodeClient)ma;
            if(isVisibleSector(m.getSector())){
                GlUtil.glPushMatrix();

                r.setSeed(m.getEntityDBId());
                GlUtil.translateModelview(
                        -halfsize + sectorSizeHalf + (r.nextFloat() * (sectorSizeHalf*2f) - sectorSizeHalf),
                        -halfsize + sectorSizeHalf + (r.nextFloat() * (sectorSizeHalf*2f) - sectorSizeHalf),
                        -halfsize + sectorSizeHalf + (r.nextFloat() * (sectorSizeHalf*2f) - sectorSizeHalf));

                GlUtil.glDisable(GL11.GL_LIGHTING);
                GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
                GlUtil.glEnable(GL11.GL_BLEND);
                GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                Sprite sprite = Controller.getResLoader().getSprite("map-sprites-8x2-c-gui-");
                sprite.setBillboard(true);
                sprite.setBlend(true);
                sprite.setFlip(true);

                m.mapEntry.currentSystemInMap.set(gameMapPosition.getCurrentSysPos());
                HudIndicatorOverlay.toDrawTradeNodes.add(m.mapEntry);

                gameMapPosition.get(positionVec);

                camera.updateFrustum();
                Sprite.draw3D(sprite, new PositionableSubColorSprite[]{m.mapEntry}, camera);

                sprite.setBillboard(false);
                sprite.setFlip(false);

                GlUtil.glDisable(GL11.GL_TEXTURE_2D);
                GlUtil.glEnable(GL11.GL_LIGHTING);
                GlUtil.glEnable(GL11.GL_CULL_FACE);
                GlUtil.glEnable(GL11.GL_BLEND);
                GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
                GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GlUtil.glPopMatrix();
            }
        }

    }
    private void endDrawDottedLine() {
        GlUtil.glEnd();
        GlUtil.glPopMatrix();
        GlUtil.glColor4f(1, 1, 1, 1);

        GlUtil.glMatrixMode(GL11.GL_PROJECTION);
        GlUtil.glPopMatrix();
        GlUtil.glMatrixMode(GL11.GL_MODELVIEW);

    }
    private void startDrawDottedLine() {
        Object2ObjectOpenHashMap<Vector3i, FTLConnection> cons = state.getController().getClientChannel().getGalaxyManagerClient().getFtlData();
        GlUtil.glDisable(GL11.GL_LIGHTING);
        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
        GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glEnable(GL11.GL_BLEND);
        GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GlUtil.glMatrixMode(GL11.GL_PROJECTION);
        GlUtil.glPushMatrix();

        float aspect = (float) GLFrame.getWidth() / (float) GLFrame.getHeight(); //1.333333333333333333333333f
        GlUtil.gluPerspective(Controller.projectionMatrix, (Float) EngineSettings.G_FOV.getCurrentState(), aspect, 10, 25000, true);
        GlUtil.glMatrixMode(GL11.GL_MODELVIEW);
        GlUtil.glPushMatrix();

        GlUtil.glLoadIdentity();
        camera.lookAt(true);
//		int galX = state.getCurrentGalaxy().galaxyPos.x * Galaxy.size *VoidSystem.SYSTEM_SIZE;
//		int galY = state.getCurrentGalaxy().galaxyPos.y * Galaxy.size *VoidSystem.SYSTEM_SIZE;
//		int galZ = state.getCurrentGalaxy().galaxyPos.z * Galaxy.size *VoidSystem.SYSTEM_SIZE;
        int galX = 0;
        int galY = 0;
        int galZ = 0;
        GlUtil.translateModelview(-halfsize, -halfsize, -halfsize);
        GlUtil.glBegin(GL11.GL_LINES);
    }
    private void drawDottedLine(Vector3i from, Vector3i to, Vector4f color, float moving) {

        Vector3f fromPx = new Vector3f();
        Vector3f toPx = new Vector3f();

        fromPx.set((from.x) * sectorSize + sectorSizeHalf, (from.y) * sectorSize + sectorSizeHalf, (from.z) * sectorSize + sectorSizeHalf);
        toPx.set((to.x) * sectorSize + sectorSizeHalf, (to.y) * sectorSize + sectorSizeHalf, (to.z) * sectorSize + sectorSizeHalf);
        if(fromPx.equals(toPx)){
            return;
        }
        Vector3f dir = new Vector3f();
        Vector3f dirN = new Vector3f();

        dir.sub(toPx, fromPx);

        dirN.set(dir);
        dirN.normalize();

        float len = dir.length();
        Vector3f a = new Vector3f();
        Vector3f b = new Vector3f();

        float dotedSize = Math.min(Math.max(2, len * 0.1f),40);

        GlUtil.glColor4f(color);
        boolean first = true;
        float f = ((moving % 1.0f) * dotedSize*2);
        for(; f < len; f += (dotedSize*2)){
            a.set(dirN);
            a.scale(f);
            if(first){
                a.set(0,0,0);
                first = false;
            }
            b.set(dirN);


            if((f+dotedSize) >= len){
                b.scale(len);
                f = len;
            }else{
                b.scale(f+dotedSize);
            }

            GL11.glVertex3f(fromPx.x + a.x, fromPx.y + a.y,fromPx.z + a.z);
            GL11.glVertex3f(fromPx.x + b.x, fromPx.y + b.y, fromPx.z + b.z);
        }
    }
    private void drawFTLConnections() {
        Object2ObjectOpenHashMap<Vector3i, FTLConnection> cons = state.getController().getClientChannel().getGalaxyManagerClient().getFtlData();
        GlUtil.glDisable(GL11.GL_LIGHTING);
        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
        GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glEnable(GL11.GL_BLEND);
        GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GlUtil.glMatrixMode(GL11.GL_PROJECTION);
        GlUtil.glPushMatrix();

        float aspect = (float) GLFrame.getWidth() / (float) GLFrame.getHeight(); //1.333333333333333333333333f
        GlUtil.gluPerspective(Controller.projectionMatrix, (Float) EngineSettings.G_FOV.getCurrentState(), aspect, 10, 25000, true);
        GlUtil.glMatrixMode(GL11.GL_MODELVIEW);
        GlUtil.glPushMatrix();

        GlUtil.glLoadIdentity();

        camera.lookAt(true);
//		int galX = state.getCurrentGalaxy().galaxyPos.x * Galaxy.size *VoidSystem.SYSTEM_SIZE;
//		int galY = state.getCurrentGalaxy().galaxyPos.y * Galaxy.size *VoidSystem.SYSTEM_SIZE;
//		int galZ = state.getCurrentGalaxy().galaxyPos.z * Galaxy.size *VoidSystem.SYSTEM_SIZE;
        int galX = 0;
        int galY = 0;
        int galZ = 0;
        GlUtil.translateModelview(-halfsize, -halfsize, -halfsize);
        GlUtil.glBegin(GL11.GL_LINES);
        Vector3f fromPx = new Vector3f();
        Vector3f toPx = new Vector3f();
        Vector3f dir = new Vector3f();
        Vector3f dirN = new Vector3f();
        Vector3f up = new Vector3f();
        Vector3f right = new Vector3f();
        Vector3f forward = new Vector3f();
        Vector3f mpos = new Vector3f();
        Vector3f mposDraw = new Vector3f();
        Vector3f mposR = new Vector3f();
        Vector3f mposL = new Vector3f();
        Vector3i fromMod = new Vector3i();
        Vector3i toMod = new Vector3i();
        //INSERTED CODE
        for (GameMapDrawListener listener : FastListenerCommon.gameMapListeners) {
            listener.galaxy_DrawLines(this);
        }
        ///
        for (FTLConnection c : cons.values()) {

            Vector3i from = c.from;
//			System.err.println("FROM:::: "+from+" -> "+VoidSystem.getContainingSystem(from, new Vector3i()));
            for (int i = 0; i < c.to.size(); i++) {

                Vector3i to = c.to.get(i);

                if(!isVisibleSector(to) && !isVisibleSector(from)){
                    continue;
                }

                Vector3i param = c.param.get(i);

                if (param.x == FTLConnection.TYPE_WARP_GATE) {
                    if (!drawWarpGates) {
                        continue;
                    }
                } else if (param.x == FTLConnection.TYPE_WORM_HOLE) {
                    if (!drawWormHoles) {
                        continue;
                    }
                }else if (param.x == FTLConnection.TYPE_RACE_WAY) {
                    continue;
                }


                if (param.x == FTLConnection.TYPE_WARP_GATE) {
                    GlUtil.glColor4f(0.1f, 0.1f, 1, 1);
                } else if (param.x == FTLConnection.TYPE_WORM_HOLE) {
                    GlUtil.glColor4f(0.4f, 0.1f, 0.7f, 1);
                }

                fromPx.set((from.x) * sectorSize + sectorSizeHalf, (from.y) * sectorSize + sectorSizeHalf, (from.z) * sectorSize + sectorSizeHalf);
                toPx.set((to.x) * sectorSize + sectorSizeHalf, (to.y) * sectorSize + sectorSizeHalf, (to.z) * sectorSize + sectorSizeHalf);

                GL11.glVertex3f(fromPx.x, fromPx.y, fromPx.z);

                if (param.x == FTLConnection.TYPE_WARP_GATE) {
                    GlUtil.glColor4f(0.1f + 0.6f, 0.1f + 0.6f, 1, 1);
                } else if (param.x == FTLConnection.TYPE_WORM_HOLE) {
                    GlUtil.glColor4f(0.4f + 0.6f, 0.1f + 0.6f, 0.7f + 0.2f, 1);
                }
                GL11.glVertex3f(toPx.x, toPx.y, toPx.z);

                dir.sub(toPx, fromPx);
                dirN.set(dir);
                dirN.normalize();
                up.set(0, 1, 0);
                if (up.equals(dirN)) {
                    up.set(1, 0, 0);
                }
                forward.set(dirN);
                right.cross(forward, up);
                right.normalize();
                up.cross(right, forward);
                up.normalize();

                mpos.set(dirN);
                mpos.scale(Math.max(dir.length() / 2, dir.length() - 100));
                mpos.add(fromPx);

                forward.negate();
                forward.scale(8);
                right.scale(8);
                if (param.x == FTLConnection.TYPE_WARP_GATE) {
                    GlUtil.glColor4f(0.3f, 0.3f, 1 * sinus.getTime(), 1);
                } else if (param.x == FTLConnection.TYPE_WORM_HOLE) {
                    GlUtil.glColor4f(0.6f * sinus.getTime(), 0.3f, 0.7f * sinus.getTime(), 1);
                }
                mposDraw.set(mpos);
                GL11.glVertex3f(mposDraw.x, mposDraw.y, mposDraw.z);
                mposDraw.add(right);
                mposDraw.add(forward);
                GL11.glVertex3f(mposDraw.x, mposDraw.y, mposDraw.z);

                mposDraw.set(mpos);
                right.negate();
                GL11.glVertex3f(mposDraw.x, mposDraw.y, mposDraw.z);
                mposDraw.add(right);
                mposDraw.add(forward);
                GL11.glVertex3f(mposDraw.x, mposDraw.y, mposDraw.z);

            }
        }
        GlUtil.glEnd();

        GlUtil.glPopMatrix();
        GlUtil.glColor4f(1, 1, 1, 1);

        GlUtil.glMatrixMode(GL11.GL_PROJECTION);
        GlUtil.glPopMatrix();
        GlUtil.glMatrixMode(GL11.GL_MODELVIEW);
    }

    private void drawHighlights() {

        GlUtil.glDisable(GL11.GL_LIGHTING);
        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
        GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glEnable(GL11.GL_BLEND);
        GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GlUtil.glMatrixMode(GL11.GL_PROJECTION);
        GlUtil.glPushMatrix();

        float aspect = (float) GLFrame.getWidth() / (float) GLFrame.getHeight(); //1.333333333333333333333333f
        GlUtil.gluPerspective(Controller.projectionMatrix, (Float) EngineSettings.G_FOV.getCurrentState(), aspect, 0.1f, 5000f, true);
        GlUtil.glMatrixMode(GL11.GL_MODELVIEW);
        GlUtil.glPushMatrix();

        GlUtil.glLoadIdentity();

        camera.lookAt(true);

        drawHighlightSector(getPlayerSector(), new Vector3f(0.8f, 0.8f, 0), sinus.getTime());

        Vector3i waypoint = getState().getController().getClientGameData().getWaypoint();
        if (waypoint != null) {
            drawHighlightSector(waypoint, new Vector3f(0.1f + HudIndicatorOverlay.selectColorValue, 0.8f + HudIndicatorOverlay.selectColorValue, 0.6f + HudIndicatorOverlay.selectColorValue), 0.4f + HudIndicatorOverlay.selectColorValue);
        }
        //INSERTED CODE
        for (GameMapDrawListener listener : FastListenerCommon.gameMapListeners) {
            listener.galaxy_DrawQuads(this);
        }
        ///

        GlUtil.glPopMatrix();
        GlUtil.glColor4f(1, 1, 1, 1);

        GlUtil.glMatrixMode(GL11.GL_PROJECTION);
        GlUtil.glPopMatrix();
        GlUtil.glMatrixMode(GL11.GL_MODELVIEW);

    }

    private ObjectArrayList<Quad> quadPool = new ObjectArrayList<Quad>();
    private int camInsideFactionSystem;
    private boolean checkGLErrorOnce;
    {
        for(int i = 0; i < 256; i++){
            quadPool.add(new Quad());
        }
    }
    public void freeQuad(Quad k){
        quadPool.add(k);
    }
    public Quad getQuad(){
        if(quadPool.isEmpty()){
            return new Quad();
        }else{
            return quadPool.remove(quadPool.size()-1);
        }
    }
    private static float quadSize = GameMapDrawer.halfsize-0.01f;
    private class Quad implements Comparable<Quad>{
        float[] v = new float[22];
        public int userData;


        private void pos(float v0, float v1, float v2){
            v[19] = v0;
            v[20] = v1;
            v[21] = v2;
        }
        private float distance(){
            float distx = ((v[19]*GameMapDrawer.size+v[0]*quadSize) - camera.getPos().x);
            float disty = ((v[20]*GameMapDrawer.size+v[1]*quadSize) - camera.getPos().y);
            float distz = ((v[21]*GameMapDrawer.size+v[2]*quadSize) - camera.getPos().z);

            return Vector3fTools.lengthSquared(distx, disty, distz);
        }
        @Override
        public int compareTo(Quad o) {
            return CompareTools.compare(o.distance(), distance());
        }

        private void verticesReverse(){
            float x = v[19]*GameMapDrawer.size;
            float y = v[20]*GameMapDrawer.size;
            float z = v[21]*GameMapDrawer.size;

            GlUtil.glColor4f(v[15], v[16], v[17], v[18]);
            GL11.glNormal3f(-v[0], -v[1], -v[2]);
            GL11.glVertex3f(x+v[12], y+v[13], z+v[14]);
            GL11.glVertex3f(x+v[9], y+v[10], z+v[11]);
            GL11.glVertex3f(x+v[6], y+v[7], z+v[8]);
            GL11.glVertex3f(x+v[3], y+v[4], z+v[5]);
        }
        private void vertices(){
            float x = v[19]*GameMapDrawer.size;
            float y = v[20]*GameMapDrawer.size;
            float z = v[21]*GameMapDrawer.size;

            GlUtil.glColor4f(v[15], v[16], v[17], v[18]);
            GL11.glNormal3f(v[0], v[1], v[2]);
            GL11.glVertex3f(x+v[3], y+v[4], z+v[5]);
            GL11.glVertex3f(x+v[6], y+v[7], z+v[8]);
            GL11.glVertex3f(x+v[9], y+v[10], z+v[11]);
            GL11.glVertex3f(x+v[12], y+v[13], z+v[14]);
        }

        private void normal3f(float n0, float n1, float n2){
            v[0] = n0;
            v[1] = n1;
            v[2] = n2;
        }
        private void vertex3f0(float v0, float v1, float v2){
            v[3] = v0;
            v[4] = v1;
            v[5] = v2;
        }
        private void vertex3f1(float v0, float v1, float v2){
            v[6] = v0;
            v[7] = v1;
            v[8] = v2;
        }
        private void vertex3f2(float v0, float v1, float v2){
            v[9] = v0;
            v[10] = v1;
            v[11] = v2;
        }
        private void vertex3f3(float v0, float v1, float v2){
            v[12] = v0;
            v[13] = v1;
            v[14] = v2;
        }
        private void color4ff3(float v0, float v1, float v2, float v3){
            v[15] = v0;
            v[16] = v1;
            v[17] = v2;
            v[18] = v3;
        }

        public void front(){
            normal3f(0, 0, 1);
            vertex3f0(quadSize, quadSize, -quadSize);
            vertex3f1(quadSize, -quadSize, -quadSize);
            vertex3f2(-quadSize, -quadSize, -quadSize);
            vertex3f3(-quadSize, quadSize, -quadSize);
        }
        public void back(){
            normal3f(0, 0, -1);
            vertex3f0(quadSize, -quadSize, quadSize);
            vertex3f1(quadSize, quadSize, quadSize);
            vertex3f2(-quadSize, quadSize, quadSize);
            vertex3f3(-quadSize, -quadSize, quadSize);
        }
        public void right(){
            normal3f(1, 0, 0);
            vertex3f0(-quadSize, -quadSize, quadSize);
            vertex3f1(-quadSize, quadSize, quadSize);
            vertex3f2(-quadSize, quadSize, -quadSize);
            vertex3f3(-quadSize, -quadSize, -quadSize);
        }
        public void left(){
            normal3f(-1, 0, 0);
            vertex3f0(quadSize, -quadSize, -quadSize);
            vertex3f1(quadSize, quadSize, -quadSize);
            vertex3f2(quadSize, quadSize, quadSize);
            vertex3f3(quadSize, -quadSize, quadSize);
        }
        public void bottom(){
            normal3f(0, -1, 0);
            vertex3f0(quadSize, quadSize, quadSize);
            vertex3f1(quadSize, quadSize, -quadSize);
            vertex3f2(-quadSize, quadSize, -quadSize);
            vertex3f3(-quadSize, quadSize, quadSize);
        }
        public void top(){
            normal3f(0, 1, 0);
            vertex3f0(quadSize, -quadSize, -quadSize);
            vertex3f1(quadSize, -quadSize, quadSize);
            vertex3f2(-quadSize, -quadSize, quadSize);
            vertex3f3(-quadSize, -quadSize, -quadSize);
        }




    }
    private void bufferBoxFaction(float size, float alpha, Vector3i now, Map<Vector3i, VoidSystem> map, List<Quad> quads) {
        VoidSystem voidSystem = map.get(now);
        Faction f;

        if (voidSystem.getOwnerFaction() != 0 && voidSystem.getOwnerUID() != null && (f = state.getFactionManager().getFaction(voidSystem.getOwnerFaction())) != null) {

            if( camera.getPos().x >= now.x * GameMapDrawer.size - halfsize &&
                    camera.getPos().y >= now.y * GameMapDrawer.size - halfsize &&
                    camera.getPos().z >= now.z * GameMapDrawer.size - halfsize &&
                    camera.getPos().x < now.x * GameMapDrawer.size + halfsize &&
                    camera.getPos().y < now.y * GameMapDrawer.size + halfsize &&
                    camera.getPos().z < now.z * GameMapDrawer.size + halfsize
            ){
                this.camInsideFactionSystem = voidSystem.getOwnerFaction();
            }

            RType r = getState().getFactionManager().getRelation(getState().getPlayer().getFactionId(), f.getIdFaction());

            VoidSystem neighbor;
            tmp.set(now);
            tmp.z -= 1;


            if ((neighbor = map.get(tmp)) == null || neighbor.getOwnerFaction() != voidSystem.getOwnerFaction()) {
                Quad q = getQuad();
                q.pos(now.x, now.y, now.z);
                q.userData = voidSystem.getOwnerFaction();
                quads.add(q);
                q.front();
                if (drawFactionByRelation) {
                    q.color4ff3(r.defaultColor.x, r.defaultColor.y, r.defaultColor.z, alpha);
                } else {
                    q.color4ff3(f.getColor().x, f.getColor().y, f.getColor().z, alpha);
                }
            }

            tmp.set(now);
            tmp.z += 1;
            if ((neighbor = map.get(tmp)) == null || neighbor.getOwnerFaction() != voidSystem.getOwnerFaction()) {
                Quad q = getQuad();
                q.pos(now.x, now.y, now.z);
                q.userData = voidSystem.getOwnerFaction();
                quads.add(q);
                q.back();
                if (drawFactionByRelation) {
                    q.color4ff3(r.defaultColor.x, r.defaultColor.y, r.defaultColor.z, alpha);
                } else {
                    q.color4ff3(f.getColor().x, f.getColor().y, f.getColor().z, alpha);
                }
            }

            tmp.set(now);
            tmp.x += 1;
            if ((neighbor = map.get(tmp)) == null || neighbor.getOwnerFaction() != voidSystem.getOwnerFaction()) {
                Quad q = getQuad();
                q.pos(now.x, now.y, now.z);
                q.userData = voidSystem.getOwnerFaction();
                quads.add(q);
                q.left();
                if (drawFactionByRelation) {
                    q.color4ff3(r.defaultColor.x, r.defaultColor.y, r.defaultColor.z, alpha);
                } else {
                    q.color4ff3(f.getColor().x, f.getColor().y, f.getColor().z, alpha);
                }
            }

            tmp.set(now);
            tmp.x -= 1;
            if ((neighbor = map.get(tmp)) == null || neighbor.getOwnerFaction() != voidSystem.getOwnerFaction()) {

                Quad q = getQuad();
                q.pos(now.x, now.y, now.z);
                q.userData = voidSystem.getOwnerFaction();
                quads.add(q);
                q.right();
                if (drawFactionByRelation) {
                    q.color4ff3(r.defaultColor.x, r.defaultColor.y, r.defaultColor.z, alpha);
                } else {
                    q.color4ff3(f.getColor().x, f.getColor().y, f.getColor().z, alpha);
                }
            }

            tmp.set(now);
            tmp.y += 1;
            if ((neighbor = map.get(tmp)) == null || neighbor.getOwnerFaction() != voidSystem.getOwnerFaction()) {

                Quad q = getQuad();
                q.pos(now.x, now.y, now.z);
                q.userData = voidSystem.getOwnerFaction();
                quads.add(q);
                q.bottom();
                if (drawFactionByRelation) {
                    q.color4ff3(r.defaultColor.x, r.defaultColor.y, r.defaultColor.z, alpha);
                } else {
                    q.color4ff3(f.getColor().x, f.getColor().y, f.getColor().z, alpha);
                }
            }

            tmp.set(now);
            tmp.y -= 1;
            if ((neighbor = map.get(tmp)) == null || neighbor.getOwnerFaction() != voidSystem.getOwnerFaction()) {
                Quad q = getQuad();
                q.pos(now.x, now.y, now.z);
                q.userData = voidSystem.getOwnerFaction();
                quads.add(q);
                q.top();
                if (drawFactionByRelation) {
                    q.color4ff3(r.defaultColor.x, r.defaultColor.y, r.defaultColor.z, alpha);
                } else {
                    q.color4ff3(f.getColor().x, f.getColor().y, f.getColor().z, alpha);
                }
            }
        }
    }
    private void drawBoxTexts(float size, float alpha, Vector3i pos, int orientation, boolean otherSide) {
        posBuffer.x = 0;
        posBuffer.y = 0;
        posBuffer.z = 0;

        GUITextOverlay systemWallText = this.systemWallText[orientation];
        orientation = orientation % 6;
        worldpos.basis.setIdentity();
//		worldpos.basis.set(c.getWorldTransformOnClient().basis);

        Vector3i d = Element.DIRECTIONSi[(otherSide ? Element.OPPOSITE_SIDE[orientation] : orientation)];
//		if((orientation == Element.LEFT || orientation == Element.RIGHT) && otherSide){
//			d.negate();
//		}

        systemWallText.getText().set(0, (pos.x + d.x) + ", " + (pos.y + d.y) + ", " + (pos.z + d.z));
        int m = otherSide ? -50 : 50;
        switch (orientation) {
            case (Element.FRONT):
                //				worldpos.basis.rotY(FastMath.HALF_PI);
                worldpos.basis.mul(mYC);
                posBuffer.z += m;
//			posBuffer.y += 0.5f;
//			posBuffer.z += 0.5f;
                break;
            case (Element.BACK):
                //				worldpos.basis.rotY(-FastMath.HALF_PI);
                //				worldpos.basis.mul(mYB);
                posBuffer.z -= m;
//			posBuffer.y += 0.5f;
//			posBuffer.z -= 0.5f;
                break;

            case (Element.TOP):
                //				worldpos.basis.mul(mYB);
                worldpos.basis.mul(mX);

                posBuffer.y += m;
//			posBuffer.y += 0.5f;
//			posBuffer.z += 0.5;
                break;
            case (Element.BOTTOM):
                worldpos.basis.mul(mYC);
                worldpos.basis.mul(mXB);

                posBuffer.y -= m;
//			posBuffer.y -= 0.5f;
//			posBuffer.z += 0.5f;
                break;

            case (Element.RIGHT):
                worldpos.basis.mul(mY);
//			posBuffer.x -= 0.5f;
//			posBuffer.y += 0.5f;
                posBuffer.x += m;
                break;
            case (Element.LEFT):
                //
                worldpos.basis.mul(mYB);
//			posBuffer.x += 0.5f;
//			posBuffer.y += 0.5f;
                posBuffer.x -= m;
                break;
        }

        worldpos.origin.set(posBuffer);

        GlUtil.glPushMatrix();

        GlUtil.glMultMatrix(worldpos);

        GlUtil.translateModelview(6, 0, 0);

        GlUtil.scaleModelview(-0.1f, -0.1f, 0.1f);

        systemWallText.draw();

        GlUtil.glPopMatrix();
    }

    private void drawBoxColor(float size, float alpha, Vector3f color) {

        //Multi-colored side - FRONT
        GlUtil.glBegin(GL11.GL_QUADS);

        GlUtil.glColor4f(color.x, color.y, color.z, alpha);

        GL11.glNormal3f(0, 0, 1);
        GL11.glVertex3f(size, size, -size);      // P1 is red
        GL11.glVertex3f(size, -size, -size);      // P2 is green
        GL11.glVertex3f(-size, -size, -size);      // P3 is blue
        GL11.glVertex3f(-size, size, -size);      // P4 is purple

        GL11.glNormal3f(0, 0, -1);
        GL11.glVertex3f(size, -size, size);
        GL11.glVertex3f(size, size, size);
        GL11.glVertex3f(-size, size, size);
        GL11.glVertex3f(-size, -size, size);

        GL11.glNormal3f(-1, 0, 0);
        GL11.glVertex3f(size, -size, -size);
        GL11.glVertex3f(size, size, -size);
        GL11.glVertex3f(size, size, size);
        GL11.glVertex3f(size, -size, size);

        GL11.glNormal3f(1, 0, 0);
        GL11.glVertex3f(-size, -size, size);
        GL11.glVertex3f(-size, size, size);
        GL11.glVertex3f(-size, size, -size);
        GL11.glVertex3f(-size, -size, -size);

        GL11.glNormal3f(0, -1, 0);
        GL11.glVertex3f(size, size, size);
        GL11.glVertex3f(size, size, -size);
        GL11.glVertex3f(-size, size, -size);
        GL11.glVertex3f(-size, size, size);

        GL11.glNormal3f(0, 1, 0);
        GL11.glVertex3f(size, -size, -size);
        GL11.glVertex3f(size, -size, size);
        GL11.glVertex3f(-size, -size, size);
        GL11.glVertex3f(-size, -size, -size);
        GlUtil.glEnd();

        GlUtil.glColor4f(1, 1, 1, 1);
    }

    private void drawBox(float size, float alpha, boolean useDefaultColor) {

        //Multi-colored side - FRONT
        GlUtil.glBegin(GL11.GL_QUADS);

        GlUtil.glColor4f(0.3f, 0.0f, 0.0f, alpha);

        GL11.glNormal3f(0, 0, 1);
        GL11.glVertex3f(size, size, -size);      // P1 is red
        GL11.glVertex3f(size, -size, -size);      // P2 is green
        GL11.glVertex3f(-size, -size, -size);      // P3 is blue
        GL11.glVertex3f(-size, size, -size);      // P4 is purple

        GlUtil.glColor4f(0.3f, 0.0f, 0.0f, alpha);
        // White side - BACK
        //		GlUtil.glColor4f(   1.0f,  1.0f, 1.0f, alpha );
        GL11.glNormal3f(0, 0, -1);
        GL11.glVertex3f(size, -size, size);
        GL11.glVertex3f(size, size, size);
        GL11.glVertex3f(-size, size, size);
        GL11.glVertex3f(-size, -size, size);

        GlUtil.glColor4f(0.0f, 0.3f, 0.0f, alpha);
        // Purple side - RIGHT
        //		GlUtil.glColor4f(  1.0f,  0.0f,  1.0f, alpha );
        GL11.glNormal3f(-1, 0, 0);
        GL11.glVertex3f(size, -size, -size);
        GL11.glVertex3f(size, size, -size);
        GL11.glVertex3f(size, size, size);
        GL11.glVertex3f(size, -size, size);

        GlUtil.glColor4f(0.0f, 0.3f, 0.0f, alpha);
        // Green side - LEFT
        //		GlUtil.glColor4f(   0.0f,  1.0f,  0.0f, alpha );
        GL11.glNormal3f(1, 0, 0);
        GL11.glVertex3f(-size, -size, size);
        GL11.glVertex3f(-size, size, size);
        GL11.glVertex3f(-size, size, -size);
        GL11.glVertex3f(-size, -size, -size);

        GlUtil.glColor4f(0.0f, 0.0f, 0.3f, alpha);
        // Blue side - TOP
        //		GlUtil.glColor4f(   0.0f,  0.0f,  1.0f, alpha );
        GL11.glNormal3f(0, -1, 0);
        GL11.glVertex3f(size, size, size);
        GL11.glVertex3f(size, size, -size);
        GL11.glVertex3f(-size, size, -size);
        GL11.glVertex3f(-size, size, size);

        GlUtil.glColor4f(0.0f, 0.0f, 0.3f, alpha);
        // Red side - BOTTOM
        //		GlUtil.glColor4f(   1.0f,  0.0f,  0.0f, alpha );
        GL11.glNormal3f(0, 1, 0);
        GL11.glVertex3f(size, -size, -size);
        GL11.glVertex3f(size, -size, size);
        GL11.glVertex3f(-size, -size, size);
        GL11.glVertex3f(-size, -size, -size);
        GlUtil.glEnd();

        GlUtil.glColor4f(1, 1, 1, 1);
    }

    private void drawGrid(float size, float spacing, float alpha, float selectAlpha) {

        boolean selected = false;
        Vector3i selectedPos = gameMapPosition.get(new Vector3i());

        selectedPos.x = ByteUtil.modU16(selectedPos.x);
        selectedPos.y = ByteUtil.modU16(selectedPos.y);
        selectedPos.z = ByteUtil.modU16(selectedPos.z);

        GlUtil.glBegin(GL11.GL_LINES);
        GlUtil.glColor4f(1, 1, 1, alpha);
        //		for(float z = 0; z <= size; z += spacing){
        //			for(float y = 0; y <= size; y += spacing){
        //
        //
        //				GL11.glVertex3f(y, z, 0);
        //				GL11.glVertex3f(y, z, size);
        //
        //
        //
        //				GL11.glVertex3f(0, y, z);
        //				GL11.glVertex3f(size, y, z);
        //
        //				GL11.glVertex3f(y, 0, z);
        //				GL11.glVertex3f(y, size, z);
        //			}
        //		}

        GlUtil.glColor4f(1, 1, 1, selectAlpha);
        GL11.glVertex3f(selectedPos.x * spacing, selectedPos.y * spacing, 0);
        GL11.glVertex3f(selectedPos.x * spacing, selectedPos.y * spacing, size);

        GL11.glVertex3f(0, selectedPos.y * spacing, selectedPos.z * spacing);
        GL11.glVertex3f(size, selectedPos.y * spacing, selectedPos.z * spacing);

        GL11.glVertex3f(selectedPos.x * spacing, 0, selectedPos.z * spacing);
        GL11.glVertex3f(selectedPos.x * spacing, size, selectedPos.z * spacing);

        GL11.glVertex3f(selectedPos.x * spacing, (selectedPos.y + 1) * spacing, 0);
        GL11.glVertex3f(selectedPos.x * spacing, (selectedPos.y + 1) * spacing, size);

        GL11.glVertex3f(0, (selectedPos.y) * spacing, (selectedPos.z + 1) * spacing);
        GL11.glVertex3f(size, (selectedPos.y) * spacing, (selectedPos.z + 1) * spacing);

        GL11.glVertex3f((selectedPos.x) * spacing, 0, (selectedPos.z + 1) * spacing);
        GL11.glVertex3f((selectedPos.x) * spacing, size, (selectedPos.z + 1) * spacing);

        GL11.glVertex3f((selectedPos.x + 1) * spacing, (selectedPos.y) * spacing, 0);
        GL11.glVertex3f((selectedPos.x + 1) * spacing, (selectedPos.y) * spacing, size);

        GL11.glVertex3f(0, (selectedPos.y + 1) * spacing, (selectedPos.z) * spacing);
        GL11.glVertex3f(size, (selectedPos.y + 1) * spacing, (selectedPos.z) * spacing);

        GL11.glVertex3f((selectedPos.x + 1) * spacing, 0, (selectedPos.z) * spacing);
        GL11.glVertex3f((selectedPos.x + 1) * spacing, size, (selectedPos.z) * spacing);

        GL11.glVertex3f((selectedPos.x + 1) * spacing, (selectedPos.y + 1) * spacing, 0);
        GL11.glVertex3f((selectedPos.x + 1) * spacing, (selectedPos.y + 1) * spacing, size);

        GL11.glVertex3f(0, (selectedPos.y + 1) * spacing, (selectedPos.z + 1) * spacing);
        GL11.glVertex3f(size, (selectedPos.y + 1) * spacing, (selectedPos.z + 1) * spacing);

        GL11.glVertex3f((selectedPos.x + 1) * spacing, 0, (selectedPos.z + 1) * spacing);
        GL11.glVertex3f((selectedPos.x + 1) * spacing, size, (selectedPos.z + 1) * spacing);
        GlUtil.glEnd();

    }

    private void drawOrbits() {
        if (!Galaxy.USE_GALAXY) {
            return;
        }
        Vector3i relPosInGalaxy = Galaxy.getRelPosInGalaxyFromAbsSystem(getGameMapPosition().getCurrentSysPos(), new Vector3i());
        if (!getState().getCurrentGalaxy().isStellarSystem(relPosInGalaxy)) {
            return;
        }
//		System.err.println("RELPOS: "+relPosInGalaxy);

        getState().getCurrentGalaxy().getSystemOrbits(relPosInGalaxy, orbits);
        Vector3i sunPositionOffset = getState().getCurrentGalaxy().getSunPositionOffset(relPosInGalaxy, new Vector3i());
        orbitRot.setIdentity();
        getState().getCurrentGalaxy().getAxisMatrix(relPosInGalaxy, orbitRot.basis);

        GlUtil.glPushMatrix();

        GlUtil.translateModelview(
                getGameMapPosition().getCurrentSysPos().x * size + sunPositionOffset.x * sectorSize + sectorSizeHalf,
                getGameMapPosition().getCurrentSysPos().y * size + sunPositionOffset.y * sectorSize + sectorSizeHalf,
                getGameMapPosition().getCurrentSysPos().z * size + sunPositionOffset.z * sectorSize + sectorSizeHalf);

        GlUtil.glMultMatrix(orbitRot);

        GlUtil.glDisable(GL11.GL_LIGHTING);
        GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glEnable(GL11.GL_BLEND);
        GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Sprite sprite = Controller.getResLoader().getSprite("map-sprites-8x2-c-gui-");
        sprite.setBillboard(true);
        sprite.setBlend(true);
        sprite.setFlip(true);

        gameMapPosition.get(positionVec);

        camera.updateFrustum();

        for (int j = 0; j < orbits.length; j++) {

            int orbitType = orbits[j];
            if (orbitType > 0) {

                float radius = (j + 1) * sectorSize + sectorSizeHalf;

                if (Galaxy.isPlanetOrbit(orbitType)) {
                    if (!drawPlanetOrbits) {
                        continue;
                    }
                    GlUtil.glColor4f(0.8f, 0.2f, 1, 1);
                } else {
                    if (!drawAsteroidBeltOrbits) {
                        continue;
                    }
                    GlUtil.glColor4f(1, 1, 0, 1);
                }

                GlUtil.glBegin(GL11.GL_LINE_STRIP);

                for (float i = 0; i < FastMath.TWO_PI; i += FastMath.PI / 64f) {
                    GL11.glVertex3f(FastMath.cos(i) * radius, 0, FastMath.sin(i) * radius);
                }
                GL11.glVertex3f(FastMath.cos(0) * radius, 0, FastMath.sin(0) * radius);
                GlUtil.glEnd();
            }
        }

        sprite.setBillboard(false);
        sprite.setFlip(false);

        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
        GlUtil.glEnable(GL11.GL_LIGHTING);
        GlUtil.glEnable(GL11.GL_CULL_FACE);
        GlUtil.glEnable(GL11.GL_BLEND);
        GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlUtil.glPopMatrix();

        orbitRotInv.basis.set(orbitRot.basis);
        Vector3f center = new Vector3f(sunPositionOffset.x * sectorSize,
                sunPositionOffset.y * sectorSize,
                sunPositionOffset.z * sectorSize);
        orbitRotInv.origin.set(center);
        orbitRotInv.inverse();

        Vector3f normal = getState().getCurrentGalaxy().getSystemAxis(relPosInGalaxy, new Vector3f());

        Vector3i s = getGameMapPosition().getCurrentSysPos();
        if (highlightOrbitSectors) {
            for (int z = 0; z < VoidSystem.SYSTEM_SIZE; z++) {
                for (int y = 0; y < VoidSystem.SYSTEM_SIZE; y++) {
                    for (int x = 0; x < VoidSystem.SYSTEM_SIZE; x++) {
                        Vector3f min = new Vector3f(-halfsize + x * sectorSize, -halfsize + y * sectorSize, -halfsize + z * sectorSize);
                        Vector3f max = new Vector3f(-halfsize + x * sectorSize + sectorSize, -halfsize + y * sectorSize + sectorSize, -halfsize + z * sectorSize + sectorSize);

                        BoundingBox box = new BoundingBox(min, max);
                        if (BoundingBox.intersectsPlane(center, normal, box)) {
                            for (int j = 0; j < orbits.length; j++) {

                                int orbitType = orbits[j];
                                if (orbitType > 0) {

                                    float radius = (j + 1) * sectorSize + sectorSizeHalf;
                                    Vector3f dist = new Vector3f();
                                    dist.sub(box.getCenter(new Vector3f()), center);
                                    dist.normalize();
                                    dist.scale(radius);

                                    dist.add(center);

                                    if (box.isInside(dist)) {
                                        //									Color c = ColorTools.getColor(j+100);
                                        Vector3f col;
                                        if (Galaxy.isPlanetOrbit(orbitType)) {
                                            if (!drawPlanetOrbits) {
                                                continue;
                                            }
                                            col = new Vector3f(0.8f, 0.2f, 1);
                                        } else {
                                            if (!drawAsteroidBeltOrbits) {
                                                continue;
                                            }
                                            col = new Vector3f(1, 1, 0);
                                        }
                                        //is on plane and has the right distance
                                        Vector3i pos = new Vector3i(s.x * VoidSystem.SYSTEM_SIZE + x, s.y * VoidSystem.SYSTEM_SIZE + y, s.z * VoidSystem.SYSTEM_SIZE + z);
                                        drawHighlightSector(pos, col, 0.6f);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void drawOwnPosition() {

        GlUtil.glPushMatrix();

        GlUtil.translateModelview(
                -halfsize + getPlayerSector().x * sectorSize + sectorSizeHalf,
                -halfsize + getPlayerSector().y * sectorSize + sectorSizeHalf,
                -halfsize + getPlayerSector().z * sectorSize + sectorSizeHalf);

        GlUtil.glDisable(GL11.GL_LIGHTING);
        GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glEnable(GL11.GL_BLEND);
        GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Sprite sprite = Controller.getResLoader().getSprite("map-sprites-8x2-c-gui-");
        sprite.setBillboard(true);
        sprite.setBlend(true);
        sprite.setFlip(true);

        gameMapPosition.get(positionVec);

        camera.updateFrustum();
        Sprite.draw3D(sprite, ownPosition, camera);

        sprite.setBillboard(false);
        sprite.setFlip(false);

        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
        GlUtil.glEnable(GL11.GL_LIGHTING);
        GlUtil.glEnable(GL11.GL_CULL_FACE);
        GlUtil.glEnable(GL11.GL_BLEND);
        GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlUtil.glPopMatrix();
    }

    private void drawNodesSprites(Entry<Vector3i, GameMap> e, boolean active) {

        Sprite sprite = Controller.getResLoader().getSprite("map-sprites-8x2-c-gui-");
        sprite.setBillboard(true);
        sprite.setBlend(true);
        sprite.setFlip(true);

        gameMapPosition.get(positionVec);

        Sprite.draw3D(sprite, e.getValue().getEntries(), camera);

        sprite.setBillboard(false);
        sprite.setFlip(false);

    }

    private void drawSelectedSector() {
        GlUtil.glPushMatrix();
        GlUtil.translateModelview(halfsize, halfsize, halfsize);
        Vector3i curPos = gameMapPosition.get(new Vector3i());
        GlUtil.translateModelview(
                gameMapPosition.getWorldTransform().origin.x,
                gameMapPosition.getWorldTransform().origin.y,
                gameMapPosition.getWorldTransform().origin.z);
        drawBox(size / 32, 0.4f, true);
        GlUtil.glPopMatrix();

    }

    private void drawHighlightSector(Vector3i sec, Vector3f color, float alpha) {
        GlUtil.glPushMatrix();
        GlUtil.glEnable(GL11.GL_COLOR_MATERIAL);
        GlUtil.glDisable(GL11.GL_TEXTURE_2D);
        GlUtil.glDisable(GL11.GL_LIGHTING);
        GlUtil.glEnable(GL11.GL_CULL_FACE);
        GlUtil.glEnable(GL11.GL_BLEND);
        GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);


        GlUtil.translateModelview(-halfsize + sec.x * sectorSize + sectorSizeHalf, -halfsize + sec.y * sectorSize + sectorSizeHalf, -halfsize + sec.z * sectorSize + sectorSizeHalf);

        drawBoxColor(sectorSizeHalf, alpha, color);
        GlUtil.glPopMatrix();

    }

    private void drawSystemGrid(float start, float spacing, float alpha, float selectAlpha) {
        GlUtil.glMatrixMode(GL11.GL_PROJECTION);
        GlUtil.glPushMatrix();

        float aspect = (float) GLFrame.getWidth() / (float) GLFrame.getHeight(); //1.333333333333333333333333f
        GlUtil.gluPerspective(Controller.projectionMatrix, (Float) EngineSettings.G_FOV.getCurrentState(), aspect, 10, 25000, true);
        GlUtil.glMatrixMode(GL11.GL_MODELVIEW);
        boolean selected = false;
        Vector3i selectedPos = new Vector3i();

        selectedPos.x = ByteUtil.modU16(selectedPos.x);
        selectedPos.y = ByteUtil.modU16(selectedPos.y);
        selectedPos.z = ByteUtil.modU16(selectedPos.z);

        GlUtil.glBegin(GL11.GL_LINES);
        //		for(float z = 0; z <= size; z += spacing){
        //			for(float y = 0; y <= size; y += spacing){
        //
        //
        //				GL11.glVertex3f(y, z, 0);
        //				GL11.glVertex3f(y, z, size);
        //
        //
        //
        //				GL11.glVertex3f(0, y, z);
        //				GL11.glVertex3f(size, y, z);
        //
        //				GL11.glVertex3f(y, 0, z);
        //				GL11.glVertex3f(y, size, z);
        //			}
        //		}
        float size = spacing * 3;
        float end = start + (1f / 3f) * size;
        float lineAlpha = 0;
        float lineAlphaB = 0;
        for (float i = 0; i < 3; i++) {
            lineAlphaB = 1;
            lineAlpha = 1;

            if (i == 0) {
                lineAlpha = 0;
                lineAlphaB = 0.6f;
            } else if (i == 2) {
                lineAlpha = 0.6f;
                lineAlphaB = 0;
            }
            GlUtil.glColor4f(1, 1, 1, lineAlpha);
            GL11.glVertex3f(selectedPos.x * spacing, selectedPos.y * spacing, start);
            GlUtil.glColor4f(1, 1, 1, lineAlphaB);
            GL11.glVertex3f(selectedPos.x * spacing, selectedPos.y * spacing, end);

            GlUtil.glColor4f(1, 1, 1, lineAlpha);
            GL11.glVertex3f(start, selectedPos.y * spacing, selectedPos.z * spacing);
            GlUtil.glColor4f(1, 1, 1, lineAlphaB);
            GL11.glVertex3f(end, selectedPos.y * spacing, selectedPos.z * spacing);

            GlUtil.glColor4f(1, 1, 1, lineAlpha);
            GL11.glVertex3f(selectedPos.x * spacing, start, selectedPos.z * spacing);
            GlUtil.glColor4f(1, 1, 1, lineAlphaB);
            GL11.glVertex3f(selectedPos.x * spacing, end, selectedPos.z * spacing);

            GlUtil.glColor4f(1, 1, 1, lineAlpha);
            GL11.glVertex3f(selectedPos.x * spacing, (selectedPos.y + 1) * spacing, start);
            GlUtil.glColor4f(1, 1, 1, lineAlphaB);
            GL11.glVertex3f(selectedPos.x * spacing, (selectedPos.y + 1) * spacing, end);

            GlUtil.glColor4f(1, 1, 1, lineAlpha);
            GL11.glVertex3f(start, (selectedPos.y) * spacing, (selectedPos.z + 1) * spacing);
            GlUtil.glColor4f(1, 1, 1, lineAlphaB);
            GL11.glVertex3f(end, (selectedPos.y) * spacing, (selectedPos.z + 1) * spacing);

            GlUtil.glColor4f(1, 1, 1, lineAlpha);
            GL11.glVertex3f((selectedPos.x) * spacing, start, (selectedPos.z + 1) * spacing);
            GlUtil.glColor4f(1, 1, 1, lineAlphaB);
            GL11.glVertex3f((selectedPos.x) * spacing, end, (selectedPos.z + 1) * spacing);

            GlUtil.glColor4f(1, 1, 1, lineAlpha);
            GL11.glVertex3f((selectedPos.x + 1) * spacing, (selectedPos.y) * spacing, start);
            GlUtil.glColor4f(1, 1, 1, lineAlphaB);
            GL11.glVertex3f((selectedPos.x + 1) * spacing, (selectedPos.y) * spacing, end);

            GlUtil.glColor4f(1, 1, 1, lineAlpha);
            GL11.glVertex3f(start, (selectedPos.y + 1) * spacing, (selectedPos.z) * spacing);
            GlUtil.glColor4f(1, 1, 1, lineAlphaB);
            GL11.glVertex3f(end, (selectedPos.y + 1) * spacing, (selectedPos.z) * spacing);

            GlUtil.glColor4f(1, 1, 1, lineAlpha);
            GL11.glVertex3f((selectedPos.x + 1) * spacing, start, (selectedPos.z) * spacing);
            GlUtil.glColor4f(1, 1, 1, lineAlphaB);
            GL11.glVertex3f((selectedPos.x + 1) * spacing, end, (selectedPos.z) * spacing);

            GlUtil.glColor4f(1, 1, 1, lineAlpha);
            GL11.glVertex3f((selectedPos.x + 1) * spacing, (selectedPos.y + 1) * spacing, start);
            GlUtil.glColor4f(1, 1, 1, lineAlphaB);
            GL11.glVertex3f((selectedPos.x + 1) * spacing, (selectedPos.y + 1) * spacing, end);

            GlUtil.glColor4f(1, 1, 1, lineAlpha);
            GL11.glVertex3f(start, (selectedPos.y + 1) * spacing, (selectedPos.z + 1) * spacing);
            GlUtil.glColor4f(1, 1, 1, lineAlphaB);
            GL11.glVertex3f(end, (selectedPos.y + 1) * spacing, (selectedPos.z + 1) * spacing);

            GlUtil.glColor4f(1, 1, 1, lineAlpha);
            GL11.glVertex3f((selectedPos.x + 1) * spacing, start, (selectedPos.z + 1) * spacing);
            GlUtil.glColor4f(1, 1, 1, lineAlphaB);
            GL11.glVertex3f((selectedPos.x + 1) * spacing, end, (selectedPos.z + 1) * spacing);

            end += (1f / 3f) * size;
            start += (1f / 3f) * size;
        }
        GlUtil.glEnd();

        GlUtil.glMatrixMode(GL11.GL_PROJECTION);
        GlUtil.glPopMatrix();
        GlUtil.glMatrixMode(GL11.GL_MODELVIEW);
    }

    /**
     * @return the camera
     */
    public GameMapCamera getCamera() {
        return camera;
    }

    /**
     * @return the filter
     */
    public int getFilter() {
        return filterAxis;
    }

    /**
     * @param filter the filter to set
     */
    public void setFilter(int filter) {
        GameMapDrawer.filterAxis = filter;
    }

    /**
     * @return the gameMapPosition
     */
    public GameMapPosition getGameMapPosition() {
        return gameMapPosition;
    }

    /**
     * @return the worldToScreenConverter
     */
    public WorldToScreenConverter getWorldToScreenConverter() {
        return worldToScreenConverter;
    }

    @Override
    public void handleKeyEvent(KeyEventInterface e) {
        if (KeyboardMappings.getEventKeyState(e, state)) {
            if (KeyboardMappings.getEventKeySingle(e) == KeyboardMappings.FORWARD.getMapping()) {
                move(0, 0, 1);
            }
            if (KeyboardMappings.getEventKeySingle(e) == KeyboardMappings.BACKWARDS.getMapping()) {
                move(0, 0, -1);
            }
            if (KeyboardMappings.getEventKeySingle(e) == KeyboardMappings.STRAFE_RIGHT.getMapping()) {
                move(-1, 0, 0);
            }
            if (KeyboardMappings.getEventKeySingle(e) == KeyboardMappings.STRAFE_LEFT.getMapping()) {
                move(1, 0, 0);
            }
            if (KeyboardMappings.getEventKeySingle(e) == KeyboardMappings.UP.getMapping()) {
                move(0, 1, 0);
            }
            if (KeyboardMappings.getEventKeySingle(e) == KeyboardMappings.DOWN.getMapping()) {
                move(0, -1, 0);
            }

            if (KeyboardMappings.getEventKeySingle(e) == GLFW.GLFW_KEY_1) {
                switchFilter(FILTER_X);
            }
            if (KeyboardMappings.getEventKeySingle(e) == GLFW.GLFW_KEY_2) {
                switchFilter(FILTER_Y);
            }
            if (KeyboardMappings.getEventKeySingle(e) == GLFW.GLFW_KEY_3) {
                switchFilter(FILTER_Z);
            }
            if (KeyboardMappings.getEventKeySingle(e) == GLFW.GLFW_KEY_4) {
                filterAxis = 0;
            }
        }

    }

    @Override
    public void handleMouseEvent(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public boolean isMapActive() {
        return state.getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().getMapControlManager().isTreeActive() &&
                state.getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().getMapControlManager().isActive();
    }

    private void move(int x, int y, int z) {
        Vector3f dir = new Vector3f();
        int m = 0;
        if (z != 0) {
            m = z;
            z = 0;
            GlUtil.getForwardVector(dir, camera.getWorldTransform());
        }
        if (y != 0) {
            m = y;
            y = 0;
            GlUtil.getUpVector(dir, camera.getWorldTransform());
        }
        if (x != 0) {
            m = x;
            x = 0;
            GlUtil.getRightVector(dir, camera.getWorldTransform());
        }

        if (Math.abs(dir.x) >= Math.abs(dir.y) && Math.abs(dir.x) >= Math.abs(dir.z)) {
            //x is biggest
            if (dir.x >= 0) {
                x = m;
            } else {
                x = -m;
            }
        } else if (Math.abs(dir.y) >= Math.abs(dir.x) && Math.abs(dir.y) >= Math.abs(dir.z)) {
            //y is biggest
            if (dir.y >= 0) {
                y = m;
            } else {
                y = -m;
            }

        } else if (Math.abs(dir.z) >= Math.abs(dir.y) && Math.abs(dir.z) >= Math.abs(dir.x)) {
            //z is biggest

            if (dir.z >= 0) {
                z = m;
            } else {
                z = -m;
            }

        }
        checkGLErrorOnce = true;
        gameMapPosition.add(x, y, z, camera.getCameraOffset(), false);
    }

    public void resetToCurrentSector() {
        gameMapPosition.set(
                getPlayerSector().x,
                getPlayerSector().y,
                getPlayerSector().z, true);

    }

    public void setFilter(int filterFlag, boolean on) {
        if (on) {
            filterAxis &= ~filterFlag;
        } else {
            filterAxis |= filterFlag;
        }
    }

    public void switchFilter(int filterFlag) {
        setFilter(filterFlag, (filterAxis & filterFlag) == filterFlag);
    }

    public void update(Timer timer) {
        if (!isMapActive()) {
            return;
        }
        this.time += timer.getDelta();
        if (lastGalaxy != null && lastGalaxy != state.getCurrentGalaxy()) {
            resetToCurrentSector();
        }
        lastGalaxy = state.getCurrentGalaxy();
        gameMapPosition.update(timer);
        sinus.update(timer);

        camera.alwaysAllowWheelZoom = !PlayerPanel.mouseInInfoScroll;

    }

    public boolean updateCamera(Timer timer) {

        if (isMapActive()) {

            camera.update(timer, false);

            return true;
        } else {
            //			if(wasActive){
            //				camera.update(timer);
            //				wasActive = false;
            //			}
        }
        return false;
    }

    public void updateMouse() {
        if (isMapActive()) {
            camera.updateMouseState();
            //			}else if(wasActive){
            //				camera.updateMouseState(state, timer);
            //				wasActive = false;
            //			}

        }
    }
}
