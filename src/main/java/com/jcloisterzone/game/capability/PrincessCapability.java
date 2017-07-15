package com.jcloisterzone.game.capability;

import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.jcloisterzone.XMLUtils;
import com.jcloisterzone.action.PlayerAction;
import com.jcloisterzone.action.PrincessAction;
import com.jcloisterzone.board.pointer.FeaturePointer;
import com.jcloisterzone.board.pointer.MeeplePointer;
import com.jcloisterzone.feature.City;
import com.jcloisterzone.feature.Feature;
import com.jcloisterzone.feature.visitor.IsOccupied;
import com.jcloisterzone.figure.Follower;
import com.jcloisterzone.figure.Meeple;
import com.jcloisterzone.game.Capability;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.SnapshotCorruptedException;

import static com.jcloisterzone.XMLUtils.attributeBoolValue;

public class PrincessCapability extends Capability {

    boolean princessUsed = false;

    @Override
    public Feature initFeature(String tileId, Feature feature, Element xml) {
        if (feature instanceof City && attributeBoolValue(xml, "princess")) {
            feature = ((City)feature).setPrincess(true);
        }
        return feature;
    }

    @Override
    public void prepareActions(List<PlayerAction<?>> actions, Set<FeaturePointer> followerOptions) {
        City c = getCurrentTile().getCityWithPrincess();
        if (c == null || ! c.walk(new IsOccupied().with(Follower.class))) return;
        Feature cityRepresentative = c.getMaster();

        PrincessAction princessAction = null;
        for (Meeple m : game.getDeployedMeeples()) {
            if (!(m.getFeature() instanceof City)) continue;
            if (m.getFeature().getMaster().equals(cityRepresentative) && m instanceof Follower) {
                if (princessAction == null) {
                    princessAction = new PrincessAction();
                    actions.add(princessAction);
                }
                princessAction.add(new MeeplePointer(m));
            }
        }
    }

    @Override
    public void turnPartCleanUp() {
        princessUsed = false;
    }

    public boolean isPrincessUsed() {
        return princessUsed;
    }

    public void setPrincessUsed(boolean princessUsed) {
        this.princessUsed = princessUsed;
    }

    @Override
    public void saveToSnapshot(Document doc, Element node) {
        if (princessUsed) {
            node.setAttribute("princessUsed", "true");
        }
    }

    @Override
    public void loadFromSnapshot(Document doc, Element node) throws SnapshotCorruptedException {
        if (XMLUtils.attributeBoolValue(node, "princessUsed")) {
            princessUsed = true;
        }
    }
}
