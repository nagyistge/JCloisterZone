package com.jcloisterzone.event;

import com.jcloisterzone.Player;
import com.jcloisterzone.board.Location;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.pointer.FeaturePointer;
import com.jcloisterzone.event.play.PlayEvent;

public class FeatureEvent extends PlayEvent {

    final FeaturePointer fp;

    public FeatureEvent(int type, Player triggeringPlayer, FeaturePointer fp) {
        super(type, triggeringPlayer, null);
        this.fp = fp;
    }

    public FeatureEvent(Player triggeringPlayer, FeaturePointer fp) {
        super(triggeringPlayer, null);
        this.fp = fp;
    }

    public FeaturePointer getFeaturePointer() {
        return fp;
    }

    public Position getPosition() {
        return fp.getPosition();
    }

    public Location getLocation() {
        return fp.getLocation();
    }

}
