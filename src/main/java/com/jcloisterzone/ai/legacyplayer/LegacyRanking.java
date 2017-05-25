package com.jcloisterzone.ai.legacyplayer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.jcloisterzone.Player;
import com.jcloisterzone.ai.AiPlayer;
import com.jcloisterzone.ai.GameRanking;
import com.jcloisterzone.board.EdgePattern;
import com.jcloisterzone.board.Location;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Rotation;
import com.jcloisterzone.board.Tile;
import com.jcloisterzone.feature.Castle;
import com.jcloisterzone.feature.City;
import com.jcloisterzone.feature.Cloister;
import com.jcloisterzone.feature.Completable;
import com.jcloisterzone.feature.Farm;
import com.jcloisterzone.feature.Feature;
import com.jcloisterzone.feature.Road;
import com.jcloisterzone.feature.score.ScoreAllCallback;
import com.jcloisterzone.feature.score.ScoreAllFeatureFinder;
import com.jcloisterzone.feature.visitor.score.CityScoreContext;
import com.jcloisterzone.feature.visitor.score.CompletableScoreContext;
import com.jcloisterzone.feature.visitor.score.FarmScoreContext;
import com.jcloisterzone.feature.visitor.score.MonasteryAbbotScoreContext;
import com.jcloisterzone.feature.visitor.score.RoadScoreContext;
import com.jcloisterzone.feature.visitor.score.ScoreContext;
import com.jcloisterzone.figure.Barn;
import com.jcloisterzone.figure.BigFollower;
import com.jcloisterzone.figure.Builder;
import com.jcloisterzone.figure.Follower;
import com.jcloisterzone.figure.Meeple;
import com.jcloisterzone.figure.SmallFollower;
import com.jcloisterzone.figure.predicate.MeeplePredicates;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.capability.BuilderCapability;
import com.jcloisterzone.game.capability.BuilderCapability.BuilderState;
import com.jcloisterzone.game.capability.FairyCapability;
import com.jcloisterzone.game.capability.TowerCapability;
import com.jcloisterzone.game.phase.ScorePhase;


public class LegacyRanking implements GameRanking {

    private static final double TRAPPED_MY_FIGURE_POINTS = -12.0;
    private static final double TRAPPED_ENEMY_FIGURE_POINTS = 3.0;
    private static final double SELF_MERGE_PENALTY = 6.0;

    private static final double MIN_CHANCE = 0.4;

    protected int packSize, myTurnsLeft;
    protected int enemyPlayers;

    private static final int OPEN_COUNT_ROAD = 0;
    private static final int OPEN_COUNT_CITY = 1;
    private static final int OPEN_COUNT_FARM = 2;
    private static final int OPEN_COUNT_CLOITSTER = 3;

    private int[] openCount = new int[4]; //number of my open objects

    //don't use to accest points/followers etc. - this player is not related to cloned game and reflect current state of real game only!!!
    //TODO store only index to prevent accidentally access
    private final AiPlayer aiPlayer;


    public LegacyRanking(AiPlayer aiPlayer) {
        this.aiPlayer = aiPlayer;
    }

    protected boolean isMe(Player p) {
        return aiPlayer.getPlayer().equals(p);
    }

    protected void initVars(Game game) {
        packSize = game.getTilePack().totalSize();
        enemyPlayers = game.getAllPlayers().length() - 1;
        myTurnsLeft = ((packSize-1) / (enemyPlayers+1)) + 1;
    }

    @Override
	public double getPartialAfterTilePlacement(Game game, Tile tile) {
        Position pos = tile.getPosition();
        //return 0.001 * game.getBoard().getAdjacentAndDiagonalTiles(pos).size();
        return 0.001 * game.getBoard().getAdjacentTilesMap(pos).size(); //adjacent only is better
    }

    @Override
    public double getFinal(Game game) {
        double ranking = 0;
        initVars(game);

        //trigger score
        game.getPhase().next(ScorePhase.class);
        //skip scoring when no tile is places (eg. ranking pass in AbbeyPhase)
        if (game.getCurrentTile() != null) {
            game.getPhase().enter();
        }

        Arrays.fill(openCount, 0);

        ranking += meepleRating(game);
        ranking += pointRating(game);
        ranking += openObjectRating(game);
        ranking += rankPossibleFeatureConnections(game);

        ranking += rankFairy(game);

//        // --- dbg print --
//        Tile tile = game.getCurrentTile();
//        Feature meeplePlacement = Iterables.find(tile.getFeatures(), new Predicate<Feature>() {
//            @Override
//            public boolean apply(Feature f) {
//                return !f.getMeeples().isEmpty();
//            }
//        }, null);
//        Arrays.fill(openCount, 0);
//        System.err.println(
//                String.format("%8s (%4s) %10s %4s/%5s %8.3f = Mepl %.3f Poit %.3f Open %.3f Conn %.3f Covx %.3f Fair %.3f",
//                tile.getId(), tile.getRotation(), tile.getPosition(),
//                meeplePlacement == null ? "" : meeplePlacement.getClass().getSimpleName(),
//                meeplePlacement == null ? "" : meeplePlacement.getLocation(),
//                ranking, meepleRating(), pointRating(), openObjectRating(),
//                rankPossibleFeatureConnections(), rankConvexity(), rankFairy()));
//        // --- end of debug print

        //objectRatings.clear();

        return ranking;
    }

    protected double reducePoints(double points, Player p) {
        if (isMe(p)) return points;
        return -points/enemyPlayers;
    }

    protected double chanceToPlaceTile(Game game, Position pos) {
        EdgePattern pattern = game.getBoard().getAvailMoveEdgePattern(pos);
        if (pattern != null && pattern.wildcardSize() < 2) {
            int remains = game.getTilePack().getSizeForEdgePattern(pattern);
            if (remains == 0) return 0.0;
            if (remains < game.getAllPlayers().length()) {
                if (remains == 0) return 0.0;
                return 1.0 - Math.pow(1.0 - 1.0 / (game.getAllPlayers().length()), remains);
            }
        }
        return 1.0;
    }

    protected double meepleRating(Game game) {
        double rating = 0;

        for (Player p : game.getAllPlayers()) {
            double meeplePoints = 0;
            int limit = 0;
            for (Follower f : Iterables.filter(p.getFollowers(), MeeplePredicates.inSupply())) {
                if (f instanceof SmallFollower) {
                    meeplePoints += 0.15;
                } else if (f instanceof BigFollower) {
                    meeplePoints += 0.25;
                }
                if (++limit == myTurnsLeft) break;
            }
            rating += reducePoints(meeplePoints, p);

            if (p.equals(aiPlayer.getPlayer())) {
            	for (Follower f : p.getFollowers()) {
                	if (f.getLocation() == Location.TOWER) {
                		rating -= 9.0;
                	}
                }
            }
        }


        return rating;
    }

    class LegacyAiScoreAllCallback implements ScoreAllCallback {

        private final Game game;
        private double rank = 0;

        private Set<Position> towerDanger = new HashSet<>();

        public LegacyAiScoreAllCallback(Game game) {
            this.game = game;
            TowerCapability towerCap = game.getCapability(TowerCapability.class);
            if (towerCap != null) {
            	//TODO ignore if opponents has no tower tokens
            	int pieces = 0;
            	for (Player p : game.getAllPlayers()) {
            		if (p.equals(aiPlayer.getPlayer())) continue;
            		pieces += towerCap.getTowerPieces(p);
            	}
            	if (pieces > 0) {
	                for (Position towerPos : towerCap.getTowers()) {
	                    int dangerDistance = 1 + game.getBoard().get(towerPos).getTower().getHeight();
	                    towerDanger.add(towerPos);
	                    for (int i = 1; i < dangerDistance; i++) {
	                        towerDanger.add(towerPos.add(new Position(i, 0)));
	                        towerDanger.add(towerPos.add(new Position(-i, 0)));
	                        towerDanger.add(towerPos.add(new Position(0, i)));
	                        towerDanger.add(towerPos.add(new Position(0, -i)));
	                    }
	                }
            	}
            }
        }

        private boolean isInTowerDanger(ScoreContext ctx) {
            //not exact but it is easy heuristic at now
            if (!towerDanger.isEmpty()) {
                for (Follower f : ctx.getFollowers()) {
                    if (towerDanger.contains(f.getPosition())) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void scoreCastle(Meeple meeple, Castle castle) {
            throw new UnsupportedOperationException();
        }

        @Override
        public LegacyAiScoreContext getCompletableScoreContext(Completable completable) {
            //TODO uncomment after invalidate implemeted
//			AiScoreContext ctx = getScoreCache().get(completable);
//			if (ctx != null && ctx.isValid()) {
//				return (CompletableScoreContext) ctx;
//			}
            return new LegacyAiScoreContext(game, LegacyRanking.this, completable.getScoreContext()/*, getScoreCache()*/);
        }

        @Override
        public LegacyAiFarmScoreContext getFarmScoreContext(Farm farm) {
            //TODO uncomment after invalidate implemeted
//			AiScoreContext ctx = getScoreCache().get(farm);
//			if (ctx != null && ctx.isValid()) {
//				return (FarmScoreContext) ctx;
//			}
            return new LegacyAiFarmScoreContext(game/*, getScoreCache()*/);
        }

        @Override
        public void scoreFarm(FarmScoreContext ctx, Player player) {
            if (isInTowerDanger(ctx)) return;
            double points = getFarmPoints((Farm) ctx.getMasterFeature(), player, ctx);
            rank += reducePoints(points, player);
        }

        @Override
        public void scoreBarn(FarmScoreContext ctx, Barn meeple) {
            //prefer barn placement - magic constant
            rank += reducePoints(1.2 * ctx.getBarnPoints(), meeple.getPlayer());
        }

        @Override
        public void scoreCompletableFeature(CompletableScoreContext ctx) {
            if (ctx instanceof MonasteryAbbotScoreContext) {
                int points = ctx.getPoints();
                for (Player p : ctx.getMajorOwners()) {
                    rank += reducePoints(points, p);
                }
                return;
            }
            rank += rankTrappedMeeples((LegacyAiScoreContext) ctx);
            if (!isInTowerDanger(ctx)) {
            	rank += rankUnfishedCompletable(ctx.getMasterFeature(), (LegacyAiScoreContext) ctx);
            	rank += rankSpecialFigures(game, (LegacyAiScoreContext) ctx);
            }
        }

        public double getRanking() {
            return rank;
        }

    }

    protected double pointRating(Game game) {
        double rating = 0;

        for (Player p : game.getAllPlayers()) {
            rating += reducePoints(p.getPoints(game.getState()), p);
        }

        ScoreAllFeatureFinder scoreAll = new ScoreAllFeatureFinder();
        LegacyAiScoreAllCallback callback = new LegacyAiScoreAllCallback(game);
        scoreAll.scoreAll(game, callback);
        rating += callback.getRanking();

        return rating;
    }


    public static final double[][]  OPEN_PENALTY = {
        { 0.0, 1.0, 2.5, 4.5, 7.5, 10.5, 14.5, 19.0, 29.0 }, //road
        { 0.0, 0.5, 1.5, 3.0, 5.0, 8.0, 12.0, 17.0, 27.0 }, //city
        { 0.0, 5.0, 10.0, 19.0, 28.0, 37.0, 47.0, 57.0, 67.0 }, //farm
        { 0.0, 0.0, 0.4, 0.8, 1.2, 2.0, 4.0, 7.0, 11.0 } //cloister
    };

    protected double openObjectRating(Game game) {
        double rating = 0;

        for (int i = 0; i < OPEN_PENALTY.length; i++ ){
            double penalty;
            //fast fix for strange bug causes ArrayIndexOutOfBoundsException: 9
            if (openCount[i] >= OPEN_PENALTY[i].length) {
                penalty = OPEN_PENALTY[i][OPEN_PENALTY[i].length - 1];
            } else {
                penalty = OPEN_PENALTY[i][openCount[i]];
            }
            if (i == 2) {
                //Farm
                double modifier = (packSize - ((1+enemyPlayers) * 3)) / 20.0;
                if (modifier < 1.0) modifier = 1.0;
                rating -= modifier * penalty;
            } else {
                rating -= penalty;
            }
        }
        return rating;
    }

    private ScoreContext futureConnectionCreateScoreContext(Game game, Feature feature) {
        LegacyAiScoreAllCallback ctxHelper = new LegacyAiScoreAllCallback(game);
        ScoreContext ctx;
        if (feature instanceof Completable) {
            ctx = ctxHelper.getCompletableScoreContext((Completable) feature);
        } else {
            ctx = ctxHelper.getFarmScoreContext((Farm) feature);
            ((LegacyAiFarmScoreContext) ctx).setCityCache(new HashMap<City, CityScoreContext>());
        }
        feature.walk(ctx);
        return ctx;
    }

    private double futureConnectionGetFeaturePoints(Feature feature, ScoreContext featureCtx) {
        if (featureCtx instanceof CompletableScoreContext) {
            return getUnfinishedCompletablePoints((Completable) feature, (LegacyAiScoreContext) featureCtx);
        } else {
            return ((FarmScoreContext) featureCtx).getPoints(aiPlayer.getPlayer());
        }
    }

    private double futureConnectionRateConnection(Game game, Location toEmpty, Location toFeature, Position f2Pos, double chance) {
        Tile tile1 = game.getCurrentTile();
        Tile tile2 = game.getBoard().get(f2Pos);

        double rating = 0;

        Completable f1 = (Completable) tile1.getFeaturePartOf(toEmpty);
        Completable f2 = (Completable) tile2.getFeaturePartOf(toFeature.rev());

        if (f1 != null && f2 != null) {
            if (f1.getClass().equals(f2.getClass())) {
                //            System.err.println("    " + tile1.getPosition() + " <-->" + f2Pos + " / " + f1 + " " + f2);
                rating +=  futureConnectionRateFeatures(game, toEmpty, toFeature, chance, f1, f2);
            } else {
                rating +=  futureConnectionRateCrossing(game, toEmpty, toFeature, chance, f1, f2);
            }
        }

        if (toEmpty != toFeature) {
            boolean left = toEmpty.rotateCCW(Rotation.R90) == toFeature;
            Farm farm1 = (Farm) tile2.getFeaturePartOf(left ? toEmpty.getLeftFarm() : toEmpty.getRightFarm());
            Farm farm2 = (Farm) tile2.getFeaturePartOf(left ? toFeature.rev().getRightFarm() : toFeature.rev().getLeftFarm());

            if (farm1 != null && farm2 != null) {
//                System.err.println("    " + tile1.getPosition() + " <-->" + f2Pos + " / " + farm1 + " " + farm2);
                rating +=  futureConnectionRateFeatures(game, toEmpty, toFeature, chance, farm1, farm2);
            }
        }
        return rating;
    }

    private double futureConnectionRateCrossing(Game game, Location toEmpty, Location toFeature, double chance, Feature f1, Feature f2) {
        ScoreContext f1Ctx = futureConnectionCreateScoreContext(game, f1);
        ScoreContext f2Ctx = futureConnectionCreateScoreContext(game, f2);
        Map<Player, Integer> f1Powers = f1Ctx.getPowers();
        Map<Player, Integer> f2Powers = f2Ctx.getPowers();

        int[] powers = funtureConnectionSumPower(f2Powers, null);
        int myPower = powers[0];
        int bestEnemy = powers[1];

        if (f1Powers.size() == 0) {
            if (bestEnemy > myPower) {
                return 0.2;
            } else if (myPower > 0) {
                return -2.5;
            }
        } else {
            if (bestEnemy > myPower) {
                return -0.1;
            } else if (myPower > 0) {
                return -0.5;
            } else {
                return -0.5;
            }
        }
        return 0;
    }


    private double futureConnectionRateFeatures(Game game, Location toEmpty, Location toFeature, double chance, Feature f1, Feature f2) {
        ScoreContext f1Ctx = futureConnectionCreateScoreContext(game, f1);
        ScoreContext f2Ctx = futureConnectionCreateScoreContext(game, f2);
        Map<Player, Integer> f1Powers = f1Ctx.getPowers();
        Map<Player, Integer> f2Powers = f2Ctx.getPowers();

        if (f1Powers.size() == 0) {
            return 0;
        }

        if (f1Powers.size() == 1 && f2Powers.size() == 1 && f1Powers.containsKey(aiPlayer.getPlayer()) && f2Powers.containsKey(aiPlayer.getPlayer())) {
//            System.err.println("   !self merge");
//            System.err.println(f1Powers + " // " + f2Powers);
            return -SELF_MERGE_PENALTY;
        }

        double myPoints = futureConnectionGetFeaturePoints(f1, f1Ctx);
        double enemyPoints = futureConnectionGetFeaturePoints(f2, f2Ctx);

        if (enemyPoints < (toEmpty == toFeature ? 7.0 : 5.0)) {
//            System.err.println("too small penalty: " + enemyPoints);
            return  -0.05; //small penalty
        }

        int[] powers = funtureConnectionSumPower(f1Powers, f2Powers);
        int myPower = powers[0];
        int bestEnemy = powers[1];

        double coef = toEmpty != toFeature ? 0.7 : 0.4; //corner / straight connection

//        System.err.println("@@@ @@@ " + myPower + "/" + myPoints + " vs " + bestEnemy + "/" + enemyPoints);

        if (myPower == bestEnemy) {
            return coef * (enemyPoints - myPoints) * chance;
        }
        if (myPower > bestEnemy) {
            return coef * enemyPoints * chance;

        }
        return -myPoints * chance; //no coef here

    }

    private int[] funtureConnectionSumPower(Map<Player, Integer> f1Powers, Map<Player, Integer> f2Powers) {
        Map<Player, Integer> sum = new HashMap<Player, Integer>(f1Powers);
        if (f2Powers != null) {
            for (Entry<Player, Integer> epower : f2Powers.entrySet()) {
                Integer val = sum.get(epower.getKey());
                sum.put(epower.getKey(), val == null ? epower.getValue() : val + epower.getValue());
            }
        }
        int myPower = 0;
        int bestEnemy = 0;
        for (Entry<Player, Integer> esum : sum.entrySet()) {
            int value = esum.getValue();
            if (esum.getKey().equals(aiPlayer.getPlayer())) {
                myPower = value;
            } else {
                if (value > bestEnemy) bestEnemy = value;
            }
        }
        return new int[] { myPower, bestEnemy };
    }

    private double rankPossibleFeatureConnections(Game game) {
        double rank = 0;

        Tile tile = game.getCurrentTile();
        Position placement = tile.getPosition();
        assert placement != null;

        for (Entry<Location, Position> eplace : Position.ADJACENT.entrySet()) {
            Position pos = placement.add(eplace.getValue());
            if (game.getBoard().get(pos) != null) continue;

            double chance = chanceToPlaceTile(game, pos);
            if (chance < MIN_CHANCE) continue;

            for (Entry<Location, Position> econn : Position.ADJACENT.entrySet()) {
                Position conn = pos.add(econn.getValue());
                if (conn.equals(placement)) continue;
                Tile connTile = game.getBoard().get(conn);
                if (connTile == null) continue;

                rank += futureConnectionRateConnection(game, eplace.getKey(), econn.getKey(), conn, chance);
            }
        }
        return rank;
    }

    protected double rankFairy(Game game) {
        if (!game.hasCapability(FairyCapability.class)) return 0;
        FairyCapability fc = game.getCapability(FairyCapability.class);
        if (fc.getFairy().isInSupply()) return 0;

        double rating = 0;

//		TODO more sophisticated rating
        for (Meeple meeple : game.getDeployedMeeples()) {
            if (!(meeple instanceof Follower)) continue;
            if (!fc.isNextTo((Follower) meeple)) continue;
            if (meeple.getFeature() instanceof Castle) continue;

            rating += reducePoints(1.0, meeple.getPlayer());
        }

        return rating;
    }

    protected double rankUnfishedCompletable(Completable completable, LegacyAiScoreContext ctx) {
        double rating = 0.0;
        double points = getUnfinishedCompletablePoints(completable, ctx);

        for (Player p : ctx.getMajorOwners()) {
            rating += reducePoints(points, p);
        }
        return rating;
    }

    protected double getUnfinishedCompletablePoints(Completable complatable, LegacyAiScoreContext ctx) {
        if (complatable instanceof City) {
            return getUnfinishedCityPoints((City) complatable, ctx);
        }
        if (complatable instanceof Road) {
            return getUnfinishedRoadPoints((Road) complatable, ctx);
        }
        if (complatable instanceof Cloister) {
            return getUnfinishedCloisterPoints((Cloister) complatable, ctx);
        }
        throw new IllegalArgumentException();
    }

    protected double getUnfinishedCityPoints(City city, LegacyAiScoreContext ctx) {
        double chanceToClose = ctx.getChanceToClose();

        if (chanceToClose > MIN_CHANCE && ctx.getMajorOwners().contains(aiPlayer.getPlayer())) {
            openCount[OPEN_COUNT_CITY]++;
        }

        //legacy heuristic
        CityScoreContext cityCtx = (CityScoreContext) ctx.getCompletableScoreContext();
        if (chanceToClose < MIN_CHANCE) {
            return cityCtx.getPoints(false) + 3.0*chanceToClose;
        } else {
            return cityCtx.getPoints(true) - 3.0*(1.0-chanceToClose);
        }
    }

    protected double getUnfinishedRoadPoints(Road road, LegacyAiScoreContext ctx) {
        double chanceToClose = ctx.getChanceToClose();;

        if (chanceToClose > MIN_CHANCE && ctx.getMajorOwners().contains(aiPlayer.getPlayer())) {
            openCount[OPEN_COUNT_ROAD]++;
        }

        //legacy heuristic
        RoadScoreContext roadCtx = (RoadScoreContext) ctx.getCompletableScoreContext();
        if (chanceToClose < MIN_CHANCE) {
            return roadCtx.getPoints(false) + 3.0*chanceToClose;
        } else {
            return roadCtx.getPoints(true) - 3.0*(1.0-chanceToClose);
        }

    }

    protected double getUnfinishedCloisterPoints(Cloister cloister, LegacyAiScoreContext ctx) {
        List<Meeple> followers = cloister.getMeeples();
        if (!followers.isEmpty() && isMe(followers.get(0).getPlayer())) {
            openCount[OPEN_COUNT_CLOITSTER]++;
        }
        double chanceToClose = ctx.getChanceToClose();
        int points = ctx.getPoints();
        return points + (9-points)*chanceToClose;
    }

    protected double getFarmPoints(Farm farm, Player p, FarmScoreContext ctx) {
        if (isMe(p)) {
            openCount[OPEN_COUNT_FARM]++;
        }
        return ctx.getPoints(p);
    }

    protected double rankSpecialFigures(Game game, LegacyAiScoreContext ctx) {
        double rating = 0.0;
        for (Meeple m : ctx.getSpecialMeeples()) {
            if (m instanceof Builder && isMe(m.getPlayer())) {
                rating += rankBuilder(game, (Builder) m, ctx);
            }
        }
        return rating;
    }

    protected double rankBuilder(Game game, Builder builder, LegacyAiScoreContext ctx) {
        if (!ctx.getMajorOwners().contains(aiPlayer.getPlayer())) {
            return -3.0; //builder in enemy object penalty
        }
        if (ctx.getChanceToClose() < 0.55) return 0.0;
        double rating = 0.0;
        //builder placed in object
        if (builder.getFeature() instanceof City) {
            rating += 1.5;
        } else {
            rating += 0.5;
        }

        BuilderCapability bc = game.getCapability(BuilderCapability.class);
        //builder used on object
        if (bc.getBuilderState() == BuilderState.ACTIVATED) {
            rating += 3.5;
        }
        return rating;
    }

    private double rankTrappedMeeples(LegacyAiScoreContext ctx) {
        //musi tu byt dolni mez - btw nestaci toto misto hodnoceni figurek, spis asi :)

        //TODO lepe
        if (myTurnsLeft < 8) return 0.0;

        if (ctx.getChanceToClose() > 0.4) return 0.0;

        double rating = 0.0;
        for (Meeple m : ctx.getMeeples()) {
            if (isMe(m.getPlayer())) {
                rating += TRAPPED_MY_FIGURE_POINTS;
            } else {
                rating += TRAPPED_ENEMY_FIGURE_POINTS;
            }
        }
        return (1.0 - ctx.getChanceToClose()) * rating; //no reduce
    }
}
