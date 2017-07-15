package com.jcloisterzone.ui.grid.layer;

import java.awt.Graphics2D;

import com.google.common.eventbus.Subscribe;
import com.jcloisterzone.Player;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.TileDefinition;
import com.jcloisterzone.board.pointer.FeaturePointer;
import com.jcloisterzone.event.FlierRollEvent;
import com.jcloisterzone.event.ScoreEvent;
import com.jcloisterzone.event.TileEvent;
import com.jcloisterzone.figure.Meeple;
import com.jcloisterzone.ui.GameController;
import com.jcloisterzone.ui.ImmutablePoint;
import com.jcloisterzone.ui.animation.Animation;
import com.jcloisterzone.ui.animation.AnimationService;
import com.jcloisterzone.ui.animation.FlierDiceRollAnimation;
import com.jcloisterzone.ui.animation.RecentPlacement;
import com.jcloisterzone.ui.animation.ScoreAnimation;
import com.jcloisterzone.ui.grid.GridPanel;

public class AnimationLayer extends AbstractGridLayer {

    private final AnimationService service;

    public AnimationLayer(GridPanel gridPanel, GameController gc) {
        super(gridPanel, gc);
        this.service = new AnimationService();
        this.service.setGridPanel(gridPanel);
        this.service.start();

        gc.register(this);
    }

    @Override
    public void paint(Graphics2D g2) {
        //HACK to correct animation order - TODO change animation design
        for (Animation a : service.getAnimations()) {
            if (!(a instanceof ScoreAnimation)) a.paint(this, g2);
        }
        for (Animation a : service.getAnimations()) {
            if (a instanceof ScoreAnimation) a.paint(this, g2);
        }
    }

    @Subscribe
    public void onFlierRollEvent(FlierRollEvent ev) {
        service.registerAnimation(new FlierDiceRollAnimation(ev.getPosition(), ev.getDistance()));
    }

    @Subscribe
    public void onScoreEvent(ScoreEvent ev) {
        if (ev.getFeaturePointer() == null) {
            scored(ev.getPosition(), ev.getTargetPlayer(), ev.getLabel(), ev.isFinal());
        } else {
            scored(ev.getFeaturePointer(), ev.getTargetPlayer(), ev.getLabel(), ev.getMeepleType(), ev.isFinal());
        }
    }

    @Subscribe
    public void onTileEvent(TileEvent ev) {
        if (ev.getType() == TileEvent.PLACEMENT) {
            TileDefinition tile = ev.getTileDefinition();
            Position pos = ev.getPosition();

            boolean initialPlacement = ev.getTriggeringPlayer() == null;//if triggering player is null we are placing initial tiles
            if ((!initialPlacement && !ev.getTriggeringPlayer().isLocalHuman()) ||
                (initialPlacement && tile.equals(getGame().getCurrentTile().getTileDefinition()))) {
                service.registerAnimation(new RecentPlacement(pos));
            }
        }
    }

    private Integer getScoreAnimationDuration() {
        Integer duration = getClient().getConfig().getScore_display_duration();
        return duration == null ? 10 : Math.max(duration, 1);
    }

    private void scored(FeaturePointer fp, Player player, String points, Class<? extends Meeple> meepleType, boolean finalScoring) {
        Position pos = fp.getPosition();
        //IMMUTABLE TODO (low priority probably) coupled with game by gc.getGame().getBoard().get(pos)
        ImmutablePoint offset = rm.getMeeplePlacement(gc.getGame().getBoard().get(pos), meepleType, fp.getLocation());
        service.registerAnimation(new ScoreAnimation(
            pos,
            points,
            offset,
            player.getColors().getMeepleColor(),
            finalScoring ? null : getScoreAnimationDuration()
        ));
    }

    private void scored(Position pos, Player player, String points, boolean finalScoring) {
    service.registerAnimation(new ScoreAnimation(
            pos,
            points,
            new ImmutablePoint(50, 50),
            player.getColors().getMeepleColor(),
            finalScoring ? null : getScoreAnimationDuration()
        ));
    }
}
