package client.ship;

import client.ship.service.Coordinate;

import java.util.ArrayList;

public class ThreeMastedShip extends Ship {

    public ThreeMastedShip(Coordinate firstCoordinate, Coordinate secondCoordinate, Coordinate thirdCoordinate) {
        this.coordinates =  new ArrayList<>();
        coordinates.add(firstCoordinate);
        coordinates.add(secondCoordinate);
        coordinates.add(thirdCoordinate);

    }

    @Override
    public int getSize() {
        return 3;
    }

    @Override
    public String toString() {
        return "ThreeMastedShip{" +
                "coordinates=" + coordinates +
                '}';
    }
}

