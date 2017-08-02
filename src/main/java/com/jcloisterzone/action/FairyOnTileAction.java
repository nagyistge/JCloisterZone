package com.jcloisterzone.action;

import com.jcloisterzone.board.Position;
import com.jcloisterzone.ui.GameController;
import com.jcloisterzone.ui.annotations.LinkedGridLayer;
import com.jcloisterzone.ui.annotations.LinkedImage;
import com.jcloisterzone.ui.grid.layer.TileActionLayer;
import com.jcloisterzone.wsio.message.MoveNeutralFigureMessage;

import io.vavr.collection.Set;

//TODO generic NeutralMeepleAction
@LinkedImage("actions/fairy")
@LinkedGridLayer(TileActionLayer.class)
public class FairyOnTileAction extends SelectTileAction {

    private final String figureId;

    public FairyOnTileAction(String figureId, Set<Position> options) {
        super(options);
        this.figureId = figureId;
    }

    @Override
    public void perform(GameController gc, Position target) {
        gc.getConnection().send(
            new MoveNeutralFigureMessage(gc.getGameId(), figureId, target));
    }

    @Override
    public String toString() {
        return "move fairy";
    }
}
