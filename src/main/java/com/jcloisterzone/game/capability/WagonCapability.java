package com.jcloisterzone.game.capability;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.jcloisterzone.Player;
import com.jcloisterzone.XMLUtils;
import com.jcloisterzone.action.MeepleAction;
import com.jcloisterzone.action.PlayerAction;
import com.jcloisterzone.board.Location;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Tile;
import com.jcloisterzone.board.TileDefinition;
import com.jcloisterzone.board.pointer.FeaturePointer;
import com.jcloisterzone.feature.City;
import com.jcloisterzone.feature.Cloister;
import com.jcloisterzone.feature.Feature;
import com.jcloisterzone.feature.Road;
import com.jcloisterzone.feature.TileFeature;
import com.jcloisterzone.figure.Follower;
import com.jcloisterzone.figure.Mayor;
import com.jcloisterzone.figure.MeepleIdProvider;
import com.jcloisterzone.figure.Wagon;
import com.jcloisterzone.game.Capability;
import com.jcloisterzone.game.Game;

import io.vavr.collection.List;

import static com.jcloisterzone.XMLUtils.contentAsLocations;

public class WagonCapability extends Capability {

    private final Map<Player, Feature> scoredWagons = new HashMap<>();

    @Override
    public List<Follower> createPlayerFollowers(Player player, MeepleIdProvider idProvider) {
        return List.of((Follower) new Wagon(idProvider.generateId(Wagon.class), player));
    }

    public void wagonScored(Wagon m, Feature feature) {
        scoredWagons.put(m.getPlayer(), feature);
    }

    public void removeScoredWagon(Player owner) {
        scoredWagons.remove(owner);
    }

    @Override
    public Object backup() {
        return new HashMap<>(scoredWagons);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void restore(Object data) {
        scoredWagons.clear();
        scoredWagons.putAll((Map<Player, Feature>) data);
    }

    @Override
    public void initPlayer(Player player) {
        player.addMeeple(new Wagon(game, null, player));
    }

    public Map<Player, Feature> getScoredWagons() {
        return scoredWagons;
    }

    @Override
    public TileDefinition initTile(TileDefinition tile, Element xml) {
        NodeList nl = xml.getElementsByTagName("wagon-move");
        assert nl.getLength() <= 1;
        if (nl.getLength() == 1) {
            nl = ((Element) nl.item(0)).getElementsByTagName("neighbouring");
            for (int i = 0; i < nl.getLength(); i++) {
                processNeighbouringElement(tile, (Element) nl.item(i));
            }
        }
    }

    private void processNeighbouringElement(Tile tile, Element e) {
        String[] sides = contentAsLocations(e);
        Feature[] te = new Feature[sides.length];
        for (int i = 0; i < te.length; i++) {
            te[i] = tile.getFeaturePartOf(Location.valueOf(sides[i]));
        }
        for (int i = 0; i < te.length; i++) {
            Feature[] neighbouring = new Feature[te.length - 1];
            int ni = 0;
            for (int j = 0; j < te.length; j++) {
                if (j == i) continue;
                neighbouring[ni++] = te[j];
            }
            ((TileFeature) te[i]).addNeighbouring(neighbouring);
        }
    }

    @Override
    public void turnPartCleanUp() {
        scoredWagons.clear();
    }

    public Player getWagonPlayer() {
        if (scoredWagons.isEmpty()) return null;
        int pi = game.getTurnPlayer().getIndex();
        while (!scoredWagons.containsKey(game.getAllPlayers().get(pi))) {
            pi++;
            if (pi == game.getAllPlayers().length()) pi = 0;
        }
        return game.getAllPlayers().get(pi);
    }

    private Set<FeaturePointer> filterWagonLocations(Set<FeaturePointer> followerOptions) {
        return Sets.filter(followerOptions, new Predicate<FeaturePointer>() {
            @Override
            public boolean apply(FeaturePointer bp) {
                Feature fe = getBoard().get(bp);
                return fe instanceof Road || fe instanceof City || fe instanceof Cloister;
            }
        });
    }

    @Override
    public void prepareActions(List<PlayerAction<?>> actions, Set<FeaturePointer> followerOptions) {
        if (game.getActivePlayer().hasFollower(Wagon.class) && !followerOptions.isEmpty()) {
            Set<FeaturePointer> wagonLocations = filterWagonLocations(followerOptions);
            if (!wagonLocations.isEmpty()) {
                actions.add(new MeepleAction(Wagon.class).addAll(wagonLocations));
            }
        }
    }


    @Override
    public void saveToSnapshot(Document doc, Element node) {
        for (Entry<Player, Feature> rv : scoredWagons.entrySet()) {
            Element el = doc.createElement("wagon");
            el.setAttribute("player", "" + rv.getKey().getIndex());
            el.setAttribute("loc", "" + rv.getValue().getLocation());
            XMLUtils.injectPosition(el, rv.getValue().getTile().getPosition());
            node.appendChild(el);
        }
    }

    @Override
    public void loadFromSnapshot(Document doc, Element node) {
        NodeList nl = node.getElementsByTagName("wagon");
        for (int i = 0; i < nl.getLength(); i++) {
            Element wg = (Element) nl.item(i);
            Location loc = Location.valueOf(wg.getAttribute("loc"));
            Position pos = XMLUtils.extractPosition(wg);
            int playerIndex = Integer.parseInt(wg.getAttribute("player"));
            Player player = game.getPlayer(playerIndex);
            scoredWagons.put(player, getBoard().get(pos).getFeature(loc));
        }
    }
}
