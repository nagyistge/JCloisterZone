package com.jcloisterzone.action;

import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.pointer.MeeplePointer;
import com.jcloisterzone.ui.annotations.LinkedGridLayer;
import com.jcloisterzone.ui.grid.layer.FollowerAreaLayer;

import io.vavr.collection.Map;
import io.vavr.collection.Set;


@LinkedGridLayer(FollowerAreaLayer.class)
public abstract class SelectFollowerAction extends PlayerAction<MeeplePointer> {

    public SelectFollowerAction(Set<MeeplePointer> options) {
        super(options);
    }

    //temporary legacy, TODO direct meeple selection on client

    public Map<Position, Set<MeeplePointer>> groupByPosition() {
        return Map.narrow(
            getOptions().groupBy(mp -> mp.getPosition())
        );
    }

//    //TODO direct implementation ?
//    public Set<MeeplePointer> getMeeplePointers(Position pos) {
//         return groupByPosition().getOrElse(pos, HashSet.empty());
//    }

}
