package me.iron.stronghold.mod.effects.sounds;

import api.listener.Listener;
import api.listener.events.player.PlayerChatEvent;
import api.mod.StarLoader;
import api.utils.StarRunnable;
import api.utils.sound.AudioUtils;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.framework.Strongpoint;
import me.iron.stronghold.mod.events.GenericEvent;
import me.iron.stronghold.mod.events.StrongpointOwnerChangedEvent;
import org.apache.commons.io.IOUtils;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.world.VoidSystem;
import org.schema.schine.graphicsengine.core.Controller;

import java.io.*;
import java.util.ArrayList;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 18.12.2021
 * TIME: 19:28
 */
public class SoundManager {
    public static SoundManager instance;
    private ArrayList<Sound> soundQueue = new ArrayList<>(); //intended for voice sounds, times out for 2 seconds after playing each sound to not overlap.
    public static void main(String[] args) {
        new SoundManager();
    }
    public SoundManager() {
        instance = this;
        File test = new File(".","StarMade.jar");
        if (!test.exists()) {
            new NullPointerException().printStackTrace();
        } else {
            System.out.println("HOORAY!");
        }
        initSounds();
        initDebug();
        initLoop();
        initEH();
    }

    /**
     * add sound to internal library for runtime use.
     * @param name name of sound
     * @param file
     */
    private void addSound(String name, File file) {
        Controller.getAudioManager().addSound(name,file);
    }

    private void initDebug() {
        StarLoader.registerListener(PlayerChatEvent.class, new Listener<PlayerChatEvent>() {
            @Override
            public void onEvent(PlayerChatEvent event) {
                if (event.isServer())
                    return;
                if (event.getText().contains("ping")) {
                    for (int i = 0; i < Sound.values().length; i++) {
                        final int ii = i;
                        new StarRunnable(){
                            @Override
                            public void run() {
                                queueSound(Sound.values()[ii]);
                            }
                        }.runLater(ModMain.instance,i*10);

                    }

                }
            }
        },ModMain.instance);
    }

    /**
     * will add sounds and install soundfiles to the client if they dont already exist.
     */
    private void initSounds() {
        String folderPath = ModMain.instance.getSkeleton().getResourcesFolder().getPath().replace("\\","/")+"/resources/sounds/"; //in moddata
        File dir = new File(folderPath);
        if (!dir.exists())
            dir.mkdirs();

        File file;
        String name;
        String path;
        for (int i = 0; i< Sound.values().length; i++) {
            name = Sound.values()[i].getSoundName();
            path = folderPath + name +".ogg";
            System.out.println("trying to load sound '"+name+"' at :"+path);
            file = new File(".",path);
            if (!file.exists()) {
                try {
                    //install sound files to client
                    String jarPath = "me/iron/stronghold/mod/effects/sounds/res/" +Sound.values()[i].getSoundName()+".ogg";

                    InputStream source = ModMain.instance.getSkeleton().getClassLoader().getResourceAsStream(jarPath);

                    File targetFile = new File(".",path);
                    System.out.println(path);
                    targetFile.createNewFile();


                    FileOutputStream outStream = new FileOutputStream(targetFile);

                    IOUtils.copy(source,outStream);

                    source.close();
                    outStream.close();

                    file = new File(path);
                    if (!file.exists()) {
                        new FileNotFoundException().printStackTrace();
                    } else {
                        System.out.println("installed file at " + file.getCanonicalPath());
                    }
                } catch (IOException | NullPointerException e) {
                    e.printStackTrace();
                }
            }
            assert file.exists():"installation of warpspace soundfiles failed";
            addSound(name,file);

        }
    }

    /**
     * make a sound play once all other queued sounds were played.
     * @param s
     * @return
     */
    public boolean queueSound(Sound s) {
        if (soundQueue.size() > 5)
            return false;
        soundQueue.add(s);
        return true; //"0022_ambience loop - space wind omnious light warbling tones (loop)"
    }

    private void initLoop() {
        new StarRunnable(){
            long last;
            @Override
            public void run() {
                if (last + 2000 < System.currentTimeMillis() && soundQueue.size() >0) {
                    String name = soundQueue.remove(0).soundName;

                    AudioUtils.clientPlaySound(name,1f,1);
                    //    ModPlayground.broadcastMessage("playing:"+name);
                    last = System.currentTimeMillis();
                }
            }
        }.runTimer(ModMain.instance,10);
    }

    private void initEH() {
        GenericEvent.addListener(StrongpointOwnerChangedEvent.class, new me.iron.stronghold.mod.events.Listener(){
            @Override
            public void run(GenericEvent e) {
                super.run(e);
                ModMain.log("listener was run with: "+e.toString());
                if (e instanceof StrongpointOwnerChangedEvent) {
                    Strongpoint p = ((StrongpointOwnerChangedEvent)e).getStrongpoint();
                    int newOwner = ((StrongpointOwnerChangedEvent)e).getNewOwner();
                    Vector3i system = new Vector3i(p.getSector());
                    system.scale(1/ VoidSystem.SYSTEM_SIZE);
                    int playerFaction = GameClientState.instance.getPlayer().getFactionId();
                    if (GameClientState.instance.getPlayer().getCurrentSystem().equals(system)) {
                        if (playerFaction == newOwner) {
                            queueSound(Sound.strongpoint_captured);
                        } else if (playerFaction == p.getOwner()) {
                            queueSound(Sound.strongpoint_lost);
                        } else {
                            queueSound(Sound.strongpoint_contested);
                        }
                    }

                }
            }
        });
    }
    public enum Sound {
        system_shielded(            "01-system_shielded"), //soundfile name without the .ogg end
        voidshield_active(          "02-voidshield_active"),
        voidshield_acivated(        "03-voidshield_activated"),
        voidshield_deactivated(     "04-voidshield_deactivated"),
        strongpoint_captured(       "05-strongpoint_captured"),
        strongpoint_lost(           "06-strongpoint_lost"),
        strongpoint_contested(      "07-strongpoint_contested"),
        strongpoint_sector(         "08-strongpoint_sector");

        Sound(String path) {
            this.soundName = path;
        }
        private String soundName;

        public String getSoundName() {
            return soundName;
        }
    }
}
