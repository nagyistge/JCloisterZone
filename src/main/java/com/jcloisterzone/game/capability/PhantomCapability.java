package com.jcloisterzone.game.capability;

import java.util.List;
import java.util.Set;

import com.jcloisterzone.Player;
import com.jcloisterzone.action.MeepleAction;
import com.jcloisterzone.action.PlayerAction;
import com.jcloisterzone.board.pointer.FeaturePointer;
import com.jcloisterzone.figure.Phantom;
import com.jcloisterzone.game.Capability;
import com.jcloisterzone.game.Game;

public class PhantomCapability extends Capability {

    @Override
    public void initPlayer(Player player) {
        player.addMeeple(new Phantom(game, null, player));
    }
}
