package client.ship;

import client.ship.service.Coordinate;

import java.util.ArrayList;

public class TwoMastedShip extends Ship {

    public TwoMastedShip(Coordinate firstCoordinate, Coordinate secondCoordinate) {
        this.coordinates = new ArrayList<>();
        coordinates.add(firstCoordinate);
        coordinates.add(secondCoordinate);
    }

    @Override
    public int getSize() {
        return 2;
    }

    @Override
    public String toString() {
        return "TwoMastedShip{" +
                "coordinates=" + coordinates +
                '}';
    }
}
