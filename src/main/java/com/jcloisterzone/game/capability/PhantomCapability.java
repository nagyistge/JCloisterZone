package com.jcloisterzone.game.capability;

import com.jcloisterzone.Player;
import com.jcloisterzone.figure.Follower;
import com.jcloisterzone.figure.MeepleIdProvider;
import com.jcloisterzone.figure.Phantom;
import com.jcloisterzone.game.Capability;

import io.vavr.collection.List;

public class PhantomCapability extends Capability {

    @Override
    public List<Follower> createPlayerFollowers(Player player, MeepleIdProvider idProvider) {
        return List.of((Follower) new Phantom(idProvider.generateId(Phantom.class), player));
    }
}
