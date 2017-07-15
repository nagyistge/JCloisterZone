package com.jcloisterzone.game.capability;

import org.w3c.dom.Element;

import com.jcloisterzone.board.RemoveTileException;
import com.jcloisterzone.board.TileDefinition;
import com.jcloisterzone.feature.Cloister;
import com.jcloisterzone.feature.Feature;
import com.jcloisterzone.game.Capability;
import com.jcloisterzone.game.CustomRule;
import com.jcloisterzone.game.Game;

import static com.jcloisterzone.XMLUtils.attributeBoolValue;

public class GermanMonasteriesCapability extends Capability {

    @Override
    public Feature initFeature(String tileId, Feature feature, Element xml) {
        if (feature instanceof Cloister) {
            ((Cloister)feature).setMonastery(attributeBoolValue(xml, "monastery"));
        }
    }

    @Override
    public TileDefinition initTile(TileDefinition tile, Element xml) {
        if (!game.getBooleanValue(CustomRule.KEEP_CLOISTERS)) {
            if (tile.getId().equals("BA.L") || tile.getId().equals("BA.LR")) {
                throw new RemoveTileException();
            }
        }
    }
}
