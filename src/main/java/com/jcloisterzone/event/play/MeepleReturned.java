package com.jcloisterzone.event.play;

import com.jcloisterzone.Player;
import com.jcloisterzone.board.pointer.BoardPointer;
import com.jcloisterzone.event.play.PlayEvent.PlayEventMeta;
import com.jcloisterzone.figure.Meeple;

public class MeepleReturned extends PlayEvent {

    private static final long serialVersionUID = 1L;

    private Meeple meeple;
    private BoardPointer from;

    public MeepleReturned(PlayEventMeta metadata, Meeple meeple, BoardPointer from) {
        super(metadata);
        this.meeple = meeple;
        this.from = from;
    }

    public Meeple getMeeple() {
        return meeple;
    }

    public BoardPointer getFrom() {
        return from;
    }

}
