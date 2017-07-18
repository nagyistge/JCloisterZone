package com.jcloisterzone.reducers;

import com.jcloisterzone.Player;
import com.jcloisterzone.event.play.PlayEvent.PlayEventMeta;
import com.jcloisterzone.event.play.PlayerTurnEvent;
import com.jcloisterzone.game.GameState;

public class SetNextPlayer implements Reducer {

    @Override
    public GameState apply(GameState state) {
        Player p = state.getTurnPlayer().getNextPlayer(state);
        state = state.setTurnPlayerIndex(p.getIndex());
        state = state.appendEvent(
            new PlayerTurnEvent(PlayEventMeta.createWithoutPlayer(), p)
        );
        return state;
    }

}
