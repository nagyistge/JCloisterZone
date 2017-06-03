package com.jcloisterzone.board.pointer;

import java.util.Objects;

import com.jcloisterzone.Immutable;
import com.jcloisterzone.board.Location;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Rotation;
import com.jcloisterzone.feature.Farm;
import com.jcloisterzone.feature.Feature;

import io.vavr.collection.List;
import io.vavr.collection.Stream;

@Immutable
public class FeaturePointer implements BoardPointer {

    private static final long serialVersionUID = -3616884893003114766L;

    private final Position position;
    private final Location location;

    public FeaturePointer(Position position, Location location) {
        this.position = position;
        this.location = location;
    }

    @Override
    public FeaturePointer asFeaturePointer() {
        return this;
    }

    public FeaturePointer translate(Position pos) {
        return new FeaturePointer(position.add(pos), location);
    }

    public FeaturePointer rotateCW(Rotation rot) {
        return new FeaturePointer(position, location.rotateCW(rot));
    }

    public FeaturePointer rotateCCW(Rotation rot) {
        return new FeaturePointer(position, location.rotateCCW(rot));
    }

    public Stream<FeaturePointer> getAdjacent(Class<? extends Feature> forType) {
        if (Farm.class.isAssignableFrom(forType)) {
            return Stream.of(Location.sides()).flatMap(loc ->
                List.of(
                    new FeaturePointer(position.add(loc), loc.getLeftFarm().rev()),
                    new FeaturePointer(position.add(loc), loc.getRightFarm().rev())
                )
            );
        } else {
            return Stream.of(Location.sides()).map(loc ->
                new FeaturePointer(position.add(loc), loc.rev())
            );
        }
    }

    public Position getPosition() {
        return position;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return String.format("[x=%s,y=%s,loc=%s]", position.x, position.y, location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, location);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        FeaturePointer other = (FeaturePointer) obj;
        if (!Objects.equals(location, other.location)) return false;
        if (!Objects.equals(position, other.position)) return false;
        return true;
    }
}
