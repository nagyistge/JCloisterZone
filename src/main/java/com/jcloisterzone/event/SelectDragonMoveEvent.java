package com.jcloisterzone.event;

import java.util.Set;

import com.jcloisterzone.Player;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.event.play.PlayEvent;

@Idempotent
public class SelectDragonMoveEvent extends PlayEvent {

    private final Set<Position> positions;
    private final int movesLeft;

    public SelectDragonMoveEvent(Player targetPlayer, Set<Position> positions, int movesLeft) {
        super(null, targetPlayer);
        this.positions = positions;
        this.movesLeft = movesLeft;
    }

    public Set<Position> getPositions() {
        return positions;
    }

    public int getMovesLeft() {
        return movesLeft;
    }


}
