package me.iron.stronghold.mod.implementation;

import api.listener.Listener;
import api.listener.events.Event;
import api.listener.events.player.PlayerChangeSectorEvent;
import api.mod.StarLoader;
import api.utils.game.PlayerUtils;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.framework.AbstractAreaEffect;
import me.iron.stronghold.mod.framework.AbstractControllableArea;
import org.apache.poi.ss.formula.functions.Even;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.Timer;
import org.schema.schine.network.server.ServerMessage;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 25.02.2022
 * TIME: 11:53
 */
public class WelcomeMessageEffect extends AbstractAreaEffect implements AreaShipMovementEvent {
    private transient MessageGenerator generatorEntry; //object with a .toString method that returns the message.
    private transient MessageGenerator generatorLeave;
    public WelcomeMessageEffect() { //only use for serailization instantiation
        super();
    }

    public WelcomeMessageEffect(StellarControllableArea parent) {
        super("Welcome_"+parent.getUID());
        init();
    }

    private void init() {
        generatorEntry = new MessageGenerator(){
            @Override
            public String getMessage(AbstractControllableArea enteredArea, Ship ship) {
                return "You have entered "+enteredArea.getName();
            }
        };
        generatorLeave = new MessageGenerator() {
            @Override
            public String getMessage(AbstractControllableArea enteredArea, Ship ship) {
                return "You have left "+enteredArea.getName();
            }
        };
    }

    /**
     * player enters
     * message is displayed
     * - params: message/messageGenerator, friend/neutral/foe
     * @param timer
     */
    @Override
    public void update(Timer timer) { //gets called when area is loaded?
        super.update(timer);

    }

    @Override
    public void onAreaEntered(StellarControllableArea area, Vector3i enteredSector, Ship object) {
        if (getParent().equals(area) && generatorEntry != null) {
            if (GameServerState.instance != null)
                notifyPilots(generatorEntry.getMessage(area, object), object);
        }
    }

    @Override
    public void onAreaInnerMovement(StellarControllableArea area, Vector3i leftSector, Vector3i enteredSector, Ship object) {

    }

    @Override
    public void onAreaLeft(StellarControllableArea area, Vector3i leftSector, Ship object) {
        if (getParent().equals(area) && generatorLeave != null) {
            if (GameServerState.instance != null)
                notifyPilots(generatorLeave.getMessage(area, object), object);
        }
    }

    private void notifyPilots(String mssg, Ship ship) {
        ship.sendControllingPlayersServerMessage(Lng.astr(mssg), ServerMessage.MESSAGE_TYPE_SIMPLE);
    }

    public MessageGenerator getGeneratorEntry() {
        return generatorEntry;
    }

    public void setGeneratorEntry(MessageGenerator generatorEntry) {
        this.generatorEntry = generatorEntry;
    }

    public MessageGenerator getGeneratorLeave() {
        return generatorLeave;
    }

    public void setGeneratorLeave(MessageGenerator generatorLeave) {
        this.generatorLeave = generatorLeave;
    }

    //tiny generator class, returns a generated or hardcoded message (overwrite to add custom behaviour)
    abstract static class MessageGenerator {
        MessageGenerator() {

        }

        public String getMessage(AbstractControllableArea enteredArea, Ship ship) {
            return "You have entered "+ enteredArea.getName()+ ".";
        }
    }
}
