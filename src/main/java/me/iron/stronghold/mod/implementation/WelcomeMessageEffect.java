package me.iron.stronghold.mod.implementation;

import api.DebugFile;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.framework.AbstractAreaEffect;
import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import me.iron.stronghold.mod.utility.DebugUI;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.Ship;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.Timer;
import org.schema.schine.network.server.ServerMessage;

import java.io.Serializable;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 25.02.2022
 * TIME: 11:53
 */
public class WelcomeMessageEffect extends AbstractAreaEffect implements AreaShipMovementEvent {
    private String generatorEntry; //object with a .toString method that returns the message.
    private String generatorLeave;
    public WelcomeMessageEffect() { //only use for serailization instantiation
        super();
    }

    public WelcomeMessageEffect(StellarControllableArea parent) {
        super("Welcome_"+parent.getUID());
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
                notifyPilots(generatorEntry, object);
        }
    }

    @Override
    public void onAreaInnerMovement(StellarControllableArea area, Vector3i leftSector, Vector3i enteredSector, Ship object) {
        DebugFile.log(
                "Inner movement in area" + area.getName() + " from " + leftSector + " to "+ enteredSector + " by object " + object.getName() + " faction " + object.getFactionId(),
                ModMain.instance);
    }

    @Override
    public void onAreaLeft(StellarControllableArea area, Vector3i leftSector, Ship object) {
        if (getParent().equals(area) && generatorLeave != null) {
            if (GameServerState.instance != null)
                notifyPilots(generatorLeave, object);
        }
    }

    private void notifyPilots(String mssg, Ship ship) {
        ship.sendControllingPlayersServerMessage(Lng.astr(mssg), ServerMessage.MESSAGE_TYPE_SIMPLE);
    }

    public String getGeneratorEntry() {
        return generatorEntry;
    }

    public void setGeneratorEntry(String generatorEntry) {
        this.generatorEntry = generatorEntry;
    }

    public String getGeneratorLeave() {
        return generatorLeave;
    }

    public void setGeneratorLeave(String generatorLeave) {
        this.generatorLeave = generatorLeave;
    }

    //tiny generator class, returns a generated or hardcoded message (overwrite to add custom behaviour)
    abstract static class MessageGenerator implements Serializable {
        MessageGenerator() {

        }

        public String getMessage(AbstractControllableArea enteredArea, Ship ship) {
            return "You have entered "+ enteredArea.getName()+ ".";
        }
    }

    @Override
    public void updateFromObject(SendableUpdateable origin) {
        super.updateFromObject(origin);
        assert origin instanceof WelcomeMessageEffect;
        setGeneratorEntry (((WelcomeMessageEffect) origin).getGeneratorEntry());
        setGeneratorLeave(((WelcomeMessageEffect) origin).getGeneratorLeave());
    }

    @Override
    public String toString() {
        return super.toString() +
                ", generatorEntry='" + generatorEntry + '\'' +
                ", generatorLeave='" + generatorLeave + '\'';
    }
}
