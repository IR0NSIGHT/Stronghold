package me.iron.stronghold.mod.AlienStation;

import it.unimi.dsi.fastutil.objects.ObjectCollection;
import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import me.iron.stronghold.mod.implementation.StellarControllableArea;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.player.inventory.Inventory;
import org.schema.game.network.objects.remote.RemoteTextBlockPair;
import org.schema.game.network.objects.remote.TextBlockPair;
import org.schema.game.server.data.GameServerState;

import java.text.SimpleDateFormat;
import java.util.*;

public class AlienArea extends StellarControllableArea {
    public static float lootFrequencyMinutes = 0.1f;

    public static boolean debugVisibleArea = true;
    private final static String noStation = ":(";
    private String centerStationUID = noStation;
    private int detectionRadius;
    HashSet<Long> loreKeys = new HashSet<>();
    private long nextLootFillUnix = 0;
    private long lastLootFillUnix = System.currentTimeMillis();
    private float meanLootPerChest = 1f;
    private float stdLootPerChest = 1;
    public static AlienArea aroundSpaceStation(SpaceStation station, int radius) {
        AlienArea area = new AlienArea();
        Vector3i center = station.getSector(new Vector3i());
        area.detectionRadius = radius;
        area.setDimensions(
                new Vector3i(center.x - radius, center.y - radius, center.z - radius),
                new Vector3i(center.x + radius, center.y + radius, center.z + radius));
        area.centerStationUID = station.getUniqueIdentifier();
        return area;
    }

    private static long getNextLootFillUnix() {
        Random random = new Random();
        return System.currentTimeMillis() + (long) (getGaussNumber(random, 1, 1) * 1000 * 60 * lootFrequencyMinutes);
    }

    private SpaceStation getStation() {
        Object o = GameServerState.instance.getSegmentControllersByName().get(centerStationUID);
        if (!(o instanceof SpaceStation))
            return null;
        SpaceStation station = (SpaceStation) o;
        return station;
    }

    private void setLoot(int amount, SpaceStation station) {
        //check if station is loaded
        ObjectCollection<Inventory> cargoCollectionManagers = station.getManagerContainer().getInventories().values();
        // Create an instance of Random
        Random random = new Random();

        //each chest gets a random amount of ship cores in them. amount is gauss controlled
        for (Inventory cm : cargoCollectionManagers) {
            cm.clear();

            ElementInformation info = ElementKeyMap.getInfoArray()[ElementKeyMap.CORE_ID];
            if (info == null)
                continue;

            cm.incExistingOrNextFreeSlotWithoutException(info.id, Math.max(0,getGaussNumber(random, stdLootPerChest, meanLootPerChest)));
            cm.sendAll();
        }
    }

    public static String[] splitString(String input, int maxLength) {
        if (input == null || maxLength <= 0) {
            throw new IllegalArgumentException("Input string cannot be null and maxLength must be greater than 0");
        }

        List<String> result = new LinkedList<>();

        int currentIndex = 0;
        StringBuilder b = new StringBuilder();
        int lastBreak = 0;
        while (currentIndex < input.length()) {
            int nextWordEnd = input.indexOf(' ', currentIndex + 1);
            if (input.charAt(currentIndex) == '\n' || currentIndex - lastBreak == maxLength || ((input.charAt(currentIndex) == ' ' && nextWordEnd - lastBreak > maxLength))) {
                result.add(b.toString());
                lastBreak = currentIndex;
                b = new StringBuilder();
                if (result.size() == 10)
                    break;
                ;
            }
            if (!(input.charAt(currentIndex) == '\n'))
                b.append(input.charAt(currentIndex));
            currentIndex++;
        }

        return result.toArray(new String[0]);
    }

    static String join(String joiner, String[] toJoin) {
        StringBuilder b = new StringBuilder();
        for (String s : toJoin)
            b.append(s).append(joiner);
        return b.toString();
    }

    @Override
    public void onUpdate(AbstractControllableArea area) {
        super.onUpdate(area);
        if (this != area)
            return;

        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis > nextLootFillUnix) {
            SpaceStation station = getStation();
            if (station == null)
                return;
            //add newly set blocks marked with "[lore]"
            addLoreTextBlocks(station, loreKeys);

            setLore(station, loreKeys);

            setLoot(getLootForTimeframe(currentTimeMillis - lastLootFillUnix), station);
            nextLootFillUnix = getNextLootFillUnix();
            lastLootFillUnix = currentTimeMillis;

            //    ModPlayground.broadcastMessage("updated alien area station with lore and loot!");
        }
    }

    HashSet<Long> addLoreTextBlocks(SegmentController station, HashSet<Long> loreKeys) {
        for (Map.Entry<Long, String> entry : station.getTextMap().entrySet()) {
            if (entry.getValue().contains("[lore]")) {
                loreKeys.add(entry.getKey());
            }
        }
        return loreKeys;
    }

    private void setLore(SpaceStation station, HashSet<Long> loreKeys) {
        String[][] conversations = {
                {
                        "#A: The food quality has been terrible lately.",
                        "#B: I know, it's like they forgot we're human beings up here.",
                        "#A: And don't get me started on the air quality. Feels like we're breathing in recycled garbage."
                },
                {
                        "#A: Can you believe they served that slop again for dinner?",
                        "#B: It's a disgrace. I'm tired of eating tasteless mush every day.",
                        "#A: And the officers don't care at all. It's like they're living in a different universe."
                },
                {
                        "#A: I swear, the accommodations here get worse every rotation.",
                        "#B: Tell me about it. My quarters are starting to feel like a glorified closet.",
                        "#A: And to top it off, the air vents are barely working. It's stifling in here."
                },
                {
                        "#A: The food is getting worse. Did you taste that excuse for lunch?",
                        "#B: I couldn't even finish it. It's insulting how they expect us to work on that.",
                        "#A: And the officers seem oblivious to our complaints. They're living in their own little world."
                },
                {
                        "#A: Have you noticed the decline in air quality lately?",
                        "#B: Absolutely. It's like they forgot to change the filters again.",
                        "#A: And don't get me started on the food. I miss real meals, not this freeze-dried nonsense."
                },
                {
                        "#A: The officers need to start listening to our concerns.",
                        "#B: They're too busy patting themselves on the back to care about us.",
                        "#A: And the food? It's a joke. I can't believe they expect us to eat this every day."
                },
                {
                        "#A: I can't stand another meal of recycled protein bars.",
                        "#B: Tell me about it. I'd kill for a fresh salad or a real sandwich.",
                        "#A: And don't get me started on the officers. They're more concerned with protocol than our well-being."
                },
                {
                        "#A: Did you see what they're calling dinner tonight?",
                        "#B: I saw. I don't know how they expect us to keep our energy up on that.",
                        "#A: And the accommodations? It's like they forgot we're supposed to live here, not just work."
                },
                {
                        "#A: The food quality is abysmal. How do they mess up freeze-dried meals?",
                        "#B: I wish I knew. It's like they're trying to see how little effort they can put in.",
                        "#A: And the officers? They couldn't care less about our concerns."
                },
                {
                        "#A: I can't believe they expect us to work in these conditions.",
                        "#B: It's like they forgot we're human beings who need decent food and air.",
                        "#A: And the officers? They're completely out of touch with what's going on down here."
                },
                {
                        "#A: The food tastes worse every day. How is that even possible?",
                        "#B: I have no idea. It's like they're actively trying to make us miserable.",
                        "#A: And don't get me started on the accommodations. I've seen better setups in old sci-fi movies."
                },
                {
                        "#A: Have you noticed the air smells off lately?",
                        "#B: It's not your imagination. I think the ventilation system is on its last legs.",
                        "#A: And the food? It's barely edible. I miss home-cooked meals more than ever."
                },
                {
                        "#A: I can't stand another meal of rehydrated mystery meat.",
                        "#B: I hear you. It's like they found the worst cuts possible and then overcooked them.",
                        "#A: And the officers? They need to step up and fix this mess instead of ignoring it."
                },
                {
                        "#A: The food is starting to affect my morale. It's that bad.",
                        "#B: I feel you. We're not asking for gourmet, just something edible.",
                        "#A: And the accommodations? It's like they want us to forget what comfort feels like."
                },
                {
                        "#A: Did you see the meal plan for next week? It's a disaster.",
                        "#B: I saw. How are we supposed to stay healthy on that junk?",
                        "#A: And don't even get me started on the officers. They're clueless about our needs."
                },
                {
                        "#A: I swear, the food gets worse every cycle.",
                        "#B: It's like they're trying to see how low they can go with the quality.",
                        "#A: And the officers? They need a reality check on what life is like for us down here."
                },
                {
                        "#A: I don't know how much longer I can eat this tasteless slop.",
                        "#B: I'm right there with you. It's like they have no respect for our taste buds.",
                        "#A: And the accommodations? They're getting worse, not better."
                },
                {
                        "#A: Have you noticed the air feels stale lately?",
                        "#B: Yeah, it's like we're breathing in old socks.",
                        "#A: And the food? It's a crime against taste buds."
                },
                {
                        "#A: I'm tired of eating flavorless paste every day.",
                        "#B: I hear you. I dream of a real meal with actual seasoning.",
                        "#A: And the officers? They seem more interested in paperwork than our well-being."
                },
                {
                        "#A: The food options are getting worse, not better.",
                        "#B: I agree. It's like they don't even try anymore.",
                        "#A: And the officers? They're oblivious to the fact that morale is plummeting."
                }
        };

        Long[] keys = station.getTextMap().keySet().toArray(new Long[0]);

        for (long key : keys) {
            if (!loreKeys.contains(key))
                continue;
            TextBlockPair pair = new TextBlockPair();
            pair.block = key;

            pair.text = ("Transmission log #" + (int) (Math.random() * 99999) + "\n" + join("\n", conversations[(int) (Math.random() * conversations.length)]));
            String[] lines = splitString(pair.text, 30);


            pair.text = join("\n", lines).replaceAll("#A", "#P-" + (int) (Math.random() * 100)).replaceAll("#B", "#P-" + (int) (Math.random() * 100));
            pair.provider = station.getListeners().get(0);

            station.getTextMap().put(pair.block, pair.text);
            station.getNetworkObject().textBlockChangeBuffer.add(new RemoteTextBlockPair(pair, true));
            station.receivedTextBlocks.enqueue(pair);

        }
    }

    protected static int getGaussNumber(Random random, float standardDeviation, float mean) {
        return (int) (random.nextGaussian() * standardDeviation + mean);
    }

    protected int getLootForTimeframe(long millisSinceRefill) {
        //TODO write a unit test for this, im to tired to be smart
        return 1; // (int)Math.ceil(amountLootPerUpdate * millisSinceRefill  / (float)(lootFrequencyMinutes* 60 * 1000));
    }

    @Override
    public void synch(SendableUpdateable a) {
        super.synch(a);
        assert a instanceof AlienArea;
        this.loreKeys = ((AlienArea) a).loreKeys;
        this.centerStationUID = ((AlienArea) a).centerStationUID;
        this.detectionRadius = ((AlienArea) a).detectionRadius;
        this.nextLootFillUnix = ((AlienArea) a).nextLootFillUnix;
        this.lastLootFillUnix = ((AlienArea) a).lastLootFillUnix;
        this.stdLootPerChest =  ((AlienArea) a).stdLootPerChest;
        this.meanLootPerChest =  ((AlienArea) a).meanLootPerChest;
    }

    @Override
    public void onAreaEntered(StellarControllableArea area, Vector3i enteredSector, Ship object) {
        super.onAreaEntered(area, enteredSector, object);
    }

    @Override
    public void onAreaInnerMovement(StellarControllableArea area, Vector3i leftSector, Vector3i enteredSector, Ship object) {
        super.onAreaInnerMovement(area, leftSector, enteredSector, object);
    }

    @Override
    public void onAreaLeft(StellarControllableArea area, Vector3i leftSector, Ship object) {
        super.onAreaLeft(area, leftSector, object);
    }

    @Override
    public boolean canBeConquered() {
        return false;
    }

    @Override
    public boolean isVisibleOnMap() {
        return debugVisibleArea;    //DEBUG: FOR NOW
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");

        return "AlienArea{" + "\n" +
                super.toString() +
                "centerStationUID='" + centerStationUID + '\'' + "\n" +
                ", detectionRadius=" + detectionRadius + "\n" +
                ", nextLootFillUnix=" + sdf.format(new Date(nextLootFillUnix)) + "\n" +
                ", lastLootFillUnix=" + sdf.format(new Date(lastLootFillUnix)) + "\n" +
                ", meanLootPerChest=" + meanLootPerChest + "\n" +
                ", stdLootPerChest=" + stdLootPerChest + "\n" +
                ", lootFrequencyMinutes=" + lootFrequencyMinutes + "\n" +
                '}';
    }
}
