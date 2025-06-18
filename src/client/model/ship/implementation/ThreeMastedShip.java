package client.model.ship.implementation;

import client.model.ship.Ship;
import client.model.coordinate.Coordinate;

import java.util.ArrayList;

public class ThreeMastedShip extends Ship {

    public ThreeMastedShip() {
        super();
    }

    public ThreeMastedShip(Ship otherShip) {
        super(otherShip);
    }

    public ThreeMastedShip(Coordinate firstCoordinate, Coordinate secondCoordinate, Coordinate thirdCoordinate) {
        this.coordinates =  new ArrayList<>();
        coordinates.add(firstCoordinate);
        coordinates.add(secondCoordinate);
        coordinates.add(thirdCoordinate);

    }

    @Override
    public boolean isSunk() {
        return hitCoordinates.size() == 3;
    }

    @Override
    public int getSize() {
        return 3;
    }

    @Override
    public String toString() {
        return "ThreeMastedShip{" +
                "hitCoordinate=" + hitCoordinates +
                ", coordinates=" + coordinates +
                '}';
    }
}

