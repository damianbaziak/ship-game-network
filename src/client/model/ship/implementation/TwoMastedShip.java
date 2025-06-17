package client.model.ship.implementation;

import client.model.ship.Ship;
import client.model.coordinate.Coordinate;

import java.util.ArrayList;

public class TwoMastedShip extends Ship {

    public TwoMastedShip() {
        super();
    }

    public TwoMastedShip(Ship otherShip) {
        super(otherShip);
    }

    public TwoMastedShip(Coordinate firstCoordinate, Coordinate secondCoordinate) {
        super();
        this.coordinates = new ArrayList<>();
        coordinates.add(firstCoordinate);
        coordinates.add(secondCoordinate);
    }

    @Override
    public boolean isSunk() {
        return hitCoordinates.size() == 2;
    }

    @Override
    public int getSize() {
        return 2;
    }

    @Override
    public String toString() {
        return "TwoMastedShip{" +
                "hitCoordinate=" + hitCoordinates +
                ", coordinates=" + coordinates +
                '}';
    }
}
