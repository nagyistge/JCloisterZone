package com.jcloisterzone.game.phase;

import com.jcloisterzone.LittleBuilding;
import com.jcloisterzone.Player;
import com.jcloisterzone.action.ActionsState;
import com.jcloisterzone.action.MeepleAction;
import com.jcloisterzone.action.PlayerAction;
import com.jcloisterzone.action.PrincessAction;
import com.jcloisterzone.board.Location;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Tile;
import com.jcloisterzone.board.TileTrigger;
import com.jcloisterzone.board.pointer.BoardPointer;
import com.jcloisterzone.board.pointer.FeaturePointer;
import com.jcloisterzone.board.pointer.MeeplePointer;
import com.jcloisterzone.event.FlierRollEvent;
import com.jcloisterzone.feature.City;
import com.jcloisterzone.feature.Scoreable;
import com.jcloisterzone.figure.BigFollower;
import com.jcloisterzone.figure.Builder;
import com.jcloisterzone.figure.DeploymentCheckResult;
import com.jcloisterzone.figure.Follower;
import com.jcloisterzone.figure.Meeple;
import com.jcloisterzone.figure.Phantom;
import com.jcloisterzone.figure.Pig;
import com.jcloisterzone.figure.SmallFollower;
import com.jcloisterzone.figure.neutral.Fairy;
import com.jcloisterzone.figure.neutral.NeutralFigure;
import com.jcloisterzone.game.Capability;
import com.jcloisterzone.game.CustomRule;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.GameState;
import com.jcloisterzone.game.GameState.Flag;
import com.jcloisterzone.game.capability.BridgeCapability;
import com.jcloisterzone.game.capability.FlierCapability;
import com.jcloisterzone.game.capability.LittleBuildingsCapability;
import com.jcloisterzone.game.capability.PrincessCapability;
import com.jcloisterzone.game.capability.TowerCapability;
import com.jcloisterzone.game.capability.TunnelCapability;
import com.jcloisterzone.reducers.DeployMeeple;
import com.jcloisterzone.reducers.MoveNeutralFigure;
import com.jcloisterzone.reducers.UndeployMeeple;
import com.jcloisterzone.wsio.WsSubscribe;
import com.jcloisterzone.wsio.message.DeployFlierMessage;
import com.jcloisterzone.wsio.message.DeployMeepleMessage;
import com.jcloisterzone.wsio.message.MoveNeutralFigureMessage;
import com.jcloisterzone.wsio.message.PassMessage;
import com.jcloisterzone.wsio.message.ReturnMeepleMessage;

import io.vavr.Predicates;
import io.vavr.Tuple2;
import io.vavr.collection.Set;
import io.vavr.collection.Stream;
import io.vavr.collection.Vector;


public class ActionPhase extends Phase {

    private final TowerCapability towerCap;
    private final FlierCapability flierCap;
    private final PrincessCapability princessCapability;

    public ActionPhase(Game game) {
        super(game);
        towerCap = game.getCapability(TowerCapability.class);
        flierCap = game.getCapability(FlierCapability.class);
        princessCapability = game.getCapability(PrincessCapability.class);
    }

    private Stream<Tuple2<Location, Scoreable>> excludePrincess(Tile currentTile, Stream<Tuple2<Location, Scoreable>> s) {
        return s.filter(t -> {
            if (t._2 instanceof City) {
                City part = (City) currentTile.getInitialFeaturePartOf(t._1);
                return !part.isPrincess();
            } else {
                return true;
            }
        });
    }

    @Override
    public void enter(GameState state) {
        Player player = state.getTurnPlayer();

        Vector<Meeple> availMeeples = Vector
            .of(SmallFollower.class, BigFollower.class, Phantom.class, Builder.class, Pig.class)
            .map(cls -> player.getMeepleFromSupply(state, cls))
            .filter(Predicates.isNotNull());

        Tile currentTile = state.getBoard().getLastPlaced();
        Position currentTilePos = currentTile.getPosition();
        Stream<Tile> tiles;

        if (currentTile.hasTrigger(TileTrigger.PORTAL) && !state.getFlags().contains(Flag.PORTAL_USED)) {
            tiles = state.getBoard().getPlacedTiles();
        } else {
            tiles = Stream.of(currentTile);
        }

        Stream<Tuple2<FeaturePointer, Scoreable>> placesFp = tiles.flatMap(tile -> {
            Position pos = tile.getPosition();
            boolean isCurrentTile = pos.equals(currentTilePos);

            boolean placementAllowed = true;
            for (Capability cap : state.getCapabilities().values()) {
                if (!cap.isDeployAllowed(state, pos)) {
                    placementAllowed = false;
                    break;
                }
            }

            Stream<Tuple2<Location, Scoreable>> places;

            if (placementAllowed) {
                places = tile.getScoreables(!isCurrentTile);
                if (isCurrentTile && game.hasCapability(PrincessCapability.class) && state.getBooleanValue(CustomRule.PRINCESS_MUST_REMOVE_KNIGHT)) {
                    places = excludePrincess(tile, places);
                }
            } else {
                places = Stream.empty();
            }

            return places.map(t -> t.map1(loc -> new FeaturePointer(pos, loc)));
        });

        Vector<PlayerAction<?>> actions = availMeeples.map(meeple -> {
            Set<FeaturePointer> locations = placesFp
                .filter(t -> meeple.isDeploymentAllowed(state, t._1, t._2) == DeploymentCheckResult.OK)
                .map(t -> t._1)
                .toSet();

            PlayerAction<?> action = new MeepleAction(meeple.getClass(), locations);
            return action;
        });

        actions = actions.filter(action -> !action.isEmpty());

        GameState nextState = state.setPlayerActions(
            new ActionsState(player, actions, true)
        );

        for (Capability cap : nextState.getCapabilities().values()) {
            nextState = cap.onActionPhaseEntered(nextState);
        }

        promote(nextState);
    }

    @Override
    public void notifyRansomPaid() {
        enter(); //recompute available actions
    }

    @WsSubscribe
    public void handlePass(PassMessage msg) {
        GameState state = game.getState();
        state = clearActions(state);
        if (getDefaultNext() instanceof PhantomPhase) {
            //skip PhantomPhase if user pass turn
            getDefaultNext().next(state);
        } else {
            next(state);
        }
    }

    @WsSubscribe
    public void handleDeployMeeple(DeployMeepleMessage msg) {
        FeaturePointer fp = msg.getPointer();
        game.markUndo();
        GameState state = game.getState();
        Meeple m = state.getActivePlayer().getMeepleFromSupply(state, msg.getMeepleId());
        //TODO nice to have validation in separate class (can be turned off eg for loadFromSnapshots or in AI (to speed it)
        if (m instanceof Follower) {
            if (state.getBoard().get(fp).isOccupied(state)) {
                throw new IllegalArgumentException("Feature is occupied.");
            }
        }
        Tile tile = state.getBoard().getLastPlaced();

        state = (new DeployMeeple(m, fp)).apply(state);

        if (fp.getLocation() != Location.TOWER && tile.hasTrigger(TileTrigger.PORTAL) && !fp.getPosition().equals(tile.getPosition())) {
            state = state.addFlag(Flag.PORTAL_USED);
        }
        state = clearActions(state);
        next(state);
    }

    @WsSubscribe
    public void handleMoveNeutralFigure(MoveNeutralFigureMessage msg) {
        GameState state = game.getState();
        BoardPointer ptr = msg.getTo();
        NeutralFigure<?> fig = state.getNeutralFigures().getById(msg.getFigureId());
        if (fig instanceof Fairy) {
            // TODO IMMUTABLE validation against ActionState

            assert (state.getBooleanValue(CustomRule.FAIRY_ON_TILE) ? Position.class : BoardPointer.class).isInstance(ptr);

            Fairy fairy = (Fairy) fig;
            state = (new MoveNeutralFigure<BoardPointer>(fairy, ptr, state.getActivePlayer())).apply(state);
            state = clearActions(state);
            next(state);
            return;
        }
        throw new IllegalArgumentException("Illegal neutral figure move");
    }

    @WsSubscribe
    public void handleReturnMeeple(ReturnMeepleMessage msg) {
        //TOOD use different messages for undeploy actions?
        GameState state = game.getState();
        MeeplePointer ptr = msg.getPointer();

        Meeple meeple = state.getDeployedMeeples().find(m -> ptr.match(m._1)).map(t -> t._1)
            .getOrElseThrow(() -> new IllegalArgumentException("Pointer doesn't match any meeple"));

        switch (msg.getSource()) {
        case PRINCESS:
            PrincessAction princessAction = (PrincessAction) state.getPlayerActions()
                .getActions().find(Predicates.instanceOf(PrincessAction.class))
                .getOrElseThrow(() -> new IllegalArgumentException("Return meeple is not allowed"));
            if (princessAction.getOptions().contains(ptr)) {
                state = state.addFlag(Flag.PRINCESS_USED);
            } else {
                throw new IllegalArgumentException("Pointer doesn't match princess action");
            }
            break;
        default:
            throw new IllegalArgumentException("Return meeple is not allowed");
        }

        state = (new UndeployMeeple(meeple)).apply(state);
        state = clearActions(state);
        next(state);
    }


    @Override
    public void placeTowerPiece(Position p) {
        GameState state = game.getState();
        //TODO
        towerCap.placeTowerPiece(getActivePlayer(), p);

        state = clearActions(state);
        next(state, TowerCapturePhase.class);
    }

    @Override
    public void placeLittleBuilding(LittleBuilding lbType) {
        GameState state = game.getState();
        //TODO
        LittleBuildingsCapability lbCap = game.getCapability(LittleBuildingsCapability.class);
        lbCap.placeLittleBuilding(getActivePlayer(), lbType);

        state = clearActions(state);
        next(state);
    }


    @Override
    public void placeTunnelPiece(FeaturePointer fp, boolean isB) {
        game.getCapability(TunnelCapability.class).placeTunnelPiece(fp, isB);
        next(ActionPhase.class);
    }


    @Override
    public void deployBridge(Position pos, Location loc) {
        BridgeCapability bridgeCap = game.getCapability(BridgeCapability.class);
        bridgeCap.decreaseBridges(getActivePlayer());
        bridgeCap.deployBridge(pos, loc, false);
        next(ActionPhase.class);
    }

    @WsSubscribe
    public void handleDeployFlier(DeployFlierMessage msg) {
        game.updateRandomSeed(msg.getCurrentTime());
        int distance = game.getRandom().nextInt(3) + 1;
        flierCap.setFlierUsed(true);
        flierCap.setFlierDistance(msg.getMeepleTypeClass(), distance);
        game.post(new FlierRollEvent(getActivePlayer(), getTile().getPosition(), distance));
        next(FlierActionPhase.class);
    }
}
