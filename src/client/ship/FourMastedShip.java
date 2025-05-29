package client.ship;

import client.ship.service.Coordinate;

import java.util.ArrayList;

public class FourMastedShip extends Ship {

    public FourMastedShip(Coordinate firstCoordinate, Coordinate secondCoordinate, Coordinate thirdCoordinate,
                          Coordinate fourthCoordinate) {
        this.coordinates = new ArrayList<>();
        coordinates.add(firstCoordinate);
        coordinates.add(secondCoordinate);
        coordinates.add(thirdCoordinate);
        coordinates.add(fourthCoordinate);
    }

    @Override
    public boolean isSunk() {
        return coordinates.size() == 4;
    }

    @Override
    public int getSize() {
        return 4;
    }

    @Override
    public String toString() {
        return "FourMastedShip{" +
                "hitCoordinate=" + hitCoordinates +
                ", coordinates=" + coordinates +
                '}';
    }
}
