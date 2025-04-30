package client.ship;

import client.ship.service.Coordinate;

import java.util.ArrayList;

public class SingleMastedShip extends Ship {

    public SingleMastedShip(Coordinate coordinate) {
        this.coordinates = new ArrayList<>();
        coordinates.add(coordinate);
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public String toString() {
        return "SingleMastedShip{" +
                "coordinates=" + coordinates +
                '}';
    }
}
