package com.jcloisterzone.figure;

import com.jcloisterzone.Player;
import com.jcloisterzone.board.pointer.FeaturePointer;
import com.jcloisterzone.feature.Farm;
import com.jcloisterzone.feature.Feature;
import com.jcloisterzone.game.GameState;

import io.vavr.control.Option;

public class Pig extends Special {

    private static final long serialVersionUID = 1L;

    public Pig(String id, Player player) {
        super(id, player);
    }

    @Override
    public DeploymentCheckResult isDeploymentAllowed(GameState state, FeaturePointer fp, Feature feature) {
        if (!(feature instanceof Farm)) {
            return new DeploymentCheckResult("Pig must be placed on a farm only.");
        }
        if (!feature.isOccupiedBy(state, getPlayer())) {
            return new DeploymentCheckResult("Farm is not occupied by follower.");
        }
        return super.isDeploymentAllowed(state, fp, feature);
    }
}
