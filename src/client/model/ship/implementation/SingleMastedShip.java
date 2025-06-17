package client.model.ship.implementation;

import client.model.ship.Ship;
import client.model.coordinate.Coordinate;

import java.util.ArrayList;

public class SingleMastedShip extends Ship {

    public SingleMastedShip() {
        super();
    }

    public SingleMastedShip(Ship otherShip) {
        super(otherShip);
    }

    public SingleMastedShip(Coordinate coordinate) {
        super();
        this.coordinates = new ArrayList<>();
        coordinates.add(coordinate);
    }

    @Override
    public String toString() {
        return "SingleMastedShip{" +
                "hitCoordinate=" + hitCoordinates +
                ", coordinates=" + coordinates +
                '}';
    }

    @Override
    public boolean isSunk() {
        return hitCoordinates.size() == 1;
    }

    @Override
    public int getSize() {
        return 1;
    }
}
