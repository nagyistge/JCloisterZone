package com.jcloisterzone.game.capability;

import static com.jcloisterzone.XMLUtils.attributeBoolValue;

import org.w3c.dom.Element;

import com.jcloisterzone.feature.City;
import com.jcloisterzone.feature.Feature;
import com.jcloisterzone.game.Capability;
import com.jcloisterzone.game.state.GameState;

public class CathedralCapability extends Capability<Void> {

    private static final long serialVersionUID = 1L;

    @Override
    public Feature initFeature(GameState state, String tileId, Feature feature, Element xml) {
        if (feature instanceof City) {
            feature = ((City) feature).setCathedral(attributeBoolValue(xml, "cathedral"));
        }
        return feature;
    }
}
