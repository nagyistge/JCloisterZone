package com.jcloisterzone.figure;

import com.jcloisterzone.Player;
import com.jcloisterzone.board.pointer.FeaturePointer;
import com.jcloisterzone.feature.City;
import com.jcloisterzone.feature.Completable;
import com.jcloisterzone.feature.Feature;
import com.jcloisterzone.feature.Road;
import com.jcloisterzone.game.GameState;

public class Builder extends Special {

    private static final long serialVersionUID = 1L;

    public Builder(String id, Player player) {
        super(id, player);
    }

    @Override
    public DeploymentCheckResult isDeploymentAllowed(GameState state, FeaturePointer fp, Feature feature) {
        if (!(feature instanceof City || feature instanceof Road) ) {
            return new DeploymentCheckResult("Builder must be placed in city or on road only.");
        }
        Completable cf = (Completable) feature;
        if (cf.isCompleted(state)) {
            return new DeploymentCheckResult("Feature is completed.");
        }
        if (!feature.isOccupiedBy(state, getPlayer())) {
            return new DeploymentCheckResult("Feature is not occupied by follower.");
        }
        return super.isDeploymentAllowed(state, fp, feature);
    }

}
