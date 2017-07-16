package com.jcloisterzone.game.capability;

import static com.jcloisterzone.XMLUtils.attributeBoolValue;

import org.w3c.dom.Element;

import com.jcloisterzone.feature.Farm;
import com.jcloisterzone.feature.Feature;
import com.jcloisterzone.game.Capability;
import com.jcloisterzone.game.CustomRule;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.GameSettings;

public class PigHerdCapability extends Capability {

    @Override
    public Feature initFeature(GameSettings gs, String tileId, Feature feature, Element xml) {
        if (feature instanceof Farm) {
            if (attributeBoolValue(xml, "pig")) {
                if (gs.getBooleanValue(CustomRule.PIG_HERD_ON_GQ_FARM) || !"GQ.F".equals(tileId)) {
                    feature = ((Farm) feature).setPigHerds(1);
                }
            }
        }
        return feature;
    }
}
