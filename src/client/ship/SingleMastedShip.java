package client.ship;

import client.ship.service.Coordinate;

import java.util.ArrayList;

public class SingleMastedShip extends Ship {

    public SingleMastedShip() {
    }

    public SingleMastedShip(Coordinate coordinate) {
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
