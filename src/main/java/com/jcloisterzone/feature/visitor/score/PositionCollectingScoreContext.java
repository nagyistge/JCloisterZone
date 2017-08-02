package com.jcloisterzone.feature.visitor.score;

import java.util.HashSet;
import java.util.Set;

import com.jcloisterzone.board.Position;
import com.jcloisterzone.feature.Completable;
import com.jcloisterzone.feature.Feature;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.capability.MageAndWitchCapability;

public abstract class PositionCollectingScoreContext extends MultiTileScoreContext implements CompletableScoreContext {

    private final MageAndWitchCapability mwCap;

    private Set<Position> positions = new HashSet<>();
    private boolean isCompleted = true;
    //TODO when mage/witch converted to meeple instances gather similar to Special and remove dependency on MageAndWitch cap
    private boolean containsMage = false;
    private boolean containsWitch = false;

    public PositionCollectingScoreContext(Game game) {
        super(game);
        mwCap = game.get(MageAndWitchCapability.class);
    }

    public abstract int getPoints(boolean completed);

    @Override
	public Completable getMasterFeature() {
        return (Completable) super.getMasterFeature();
    }

    public int getSize() {
        return positions.size();
    }

    @Override
	public Set<Position> getPositions() {
        return positions;
    }

    @Override
	public boolean isCompleted() {
        return isCompleted;
    }

    @Override
    public VisitResult visit(Feature feature) {
        positions.translate(feature.getTile().getPosition());
        if (((Completable)feature).isOpen()) {
            isCompleted = false;
        }
        if (mwCap != null) {
            containsMage = containsMage || (mwCap.getMage().at(feature));
            containsWitch = containsWitch || (mwCap.getWitch().at(feature));
        }
        return super.visit(feature);
    }

    protected int getMageAndWitchPoints(int points) {
        if (containsMage()) {
            points += getPositions().size();
        }
        if (containsWitch()) {
            if (points % 2 == 1) points++;
            points /= 2;
        }
        return points;
    }

    public boolean containsMage() {
        return containsMage;
    }

    public boolean containsWitch() {
        return containsWitch;
    }
}
