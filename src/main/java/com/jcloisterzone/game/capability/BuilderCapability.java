package com.jcloisterzone.game.capability;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.Iterables;
import com.jcloisterzone.Player;
import com.jcloisterzone.Player;
import com.jcloisterzone.action.MeepleAction;
import com.jcloisterzone.action.PlayerAction;
import com.jcloisterzone.board.Location;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Tile;
import com.jcloisterzone.board.pointer.FeaturePointer;
import com.jcloisterzone.feature.City;
import com.jcloisterzone.feature.Road;
import com.jcloisterzone.figure.Builder;
import com.jcloisterzone.figure.Special;
import com.jcloisterzone.game.Capability;
import com.jcloisterzone.game.Game;

import io.vavr.collection.List;

public class BuilderCapability extends Capability {

    public enum BuilderState { INACTIVE, ACTIVATED, BUILDER_TURN; }

    protected BuilderState builderState = BuilderState.INACTIVE;

    @Override
    public List<Special> createPlayerSpecialMeeples(Player p) {
        return List.of((Special) new Builder(p));
    }

    public BuilderState getBuilderState() {
        return builderState;
    }

    public void useBuilder() {
        if (builderState == BuilderState.INACTIVE) {
            builderState = BuilderState.ACTIVATED;
        }
    }

    public boolean hasPlayerAnotherTurn() {
        return builderState == BuilderState.ACTIVATED;
    }

    @Override
    public void prepareActions(java.util.List<PlayerAction<?>> actions, java.util.Set<FeaturePointer> followerOptions) {
        Player player = game.getActivePlayer();
        if (!player.hasSpecialMeeple(Builder.class)) return;

        Tile tile = getCurrentTile();
        if (!game.isDeployAllowed(tile, Builder.class)) return;

        Set<Location> roads = tile.getPlayerUncompletedFeatures(player, Road.class);
        Set<Location> cities = tile.getPlayerUncompletedFeatures(player, City.class);
        if (roads.isEmpty() && cities.isEmpty()) return;

        Position pos = tile.getPosition();
        MeepleAction builderAction = new MeepleAction(Builder.class);

        for (Location loc : Iterables.concat(roads, cities)) {
            builderAction.add(new FeaturePointer(pos, loc));
        }
        actions.add(builderAction);

    }

    @Override
    public void turnPartCleanUp() {
        switch (builderState) {
        case ACTIVATED:
            builderState = BuilderState.BUILDER_TURN;
            break;
        case BUILDER_TURN:
            builderState = BuilderState.INACTIVE;
            break;
        }
    }

    @Override
    public void saveToSnapshot(Document doc, Element node) {
        node.setAttribute("builderState", builderState.name());
    }

    @Override
    public void loadFromSnapshot(Document doc, Element node) {
        builderState = BuilderState.valueOf(node.getAttribute("builderState"));
    }

}
