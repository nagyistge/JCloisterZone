package com.jcloisterzone.event.play;

import com.jcloisterzone.Player;

public class PlayerTurnEvent extends PlayEvent {

    private static final long serialVersionUID = 1L;

    private Player player;

    public PlayerTurnEvent(Player player) {
        super(null);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public String toString() {
        return super.toString() + " player:" + player;
    }

}
