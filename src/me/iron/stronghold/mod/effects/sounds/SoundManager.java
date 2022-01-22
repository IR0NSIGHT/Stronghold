package me.iron.stronghold.mod.effects.sounds;

import api.listener.Listener;
import api.listener.events.player.PlayerChatEvent;
import api.mod.StarLoader;
import api.utils.StarRunnable;
import api.utils.sound.AudioUtils;
import me.iron.stronghold.mod.ModMain;
import org.apache.commons.io.IOUtils;
import org.schema.schine.graphicsengine.core.Controller;

import java.io.*;
import java.util.ArrayList;
import java.util.Vector;

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
        initSounds();
    //    initDebug();
        initLoop();
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

    public enum Sound {
        system_shielded(            "01-system_shielded"), //soundfile name without the .ogg end
        voidshield_active(          "02-voidshield_active"),
        voidshield_acivated(        "03-voidshield_activated"),
        voidshield_deactivated(     "04-voidshield_deactivated"),
        strongpoint_captured(       "05-strongpoint_captured"),
        strongpoint_lost(           "06-strongpoint_lost"),
        strongpoint_contested(      "07-strongpoint_contested"),
        strongpoint_sector(         "08-strongpoint_sector"),
        loosing_this_region("01-loosing_this_region"),
        lost_a_region("02-lost_a_region"),
        winning_this_region("03-winning_this_region"),
        conquered_a_region("04-conquered_a_region"),
        this_region_conquered("05-this_region_conquered"),
        other_sp_lost("06-other_sp_lost"),
        other_sp_conquered("07-other_sp_conquered"),
        lost_sp_here("08-lost_sp_here"),
        conquered_sp_here("09-conquered_sp_here"),
        other_sp_here_conquere("10-other_sp_here_conquered");

        Sound(String path) {
            this.soundName = path;
        }
        private String soundName;

        public String getSoundName() {
            return soundName;
        }
    }
}
