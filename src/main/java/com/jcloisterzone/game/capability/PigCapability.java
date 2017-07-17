package com.jcloisterzone.game.capability;

import com.jcloisterzone.Player;
import com.jcloisterzone.figure.Pig;
import com.jcloisterzone.figure.Special;
import com.jcloisterzone.game.Capability;

import io.vavr.collection.List;

public class PigCapability extends Capability {

    @Override
    public List<Special> createPlayerSpecialMeeples(Player player) {
        return List.of((Special) new Pig(player));
    }
}
