package client.ship;

import client.ship.service.Coordinate;

import java.util.ArrayList;
import java.util.List;

public class ThreeMastedShip extends Ship {

    public ThreeMastedShip(Coordinate firstCoordinate, Coordinate secondCoordinate, Coordinate thirdCoordinate) {
        this.coordinates =  new ArrayList<>();
        coordinates.add(firstCoordinate);
        coordinates.add(secondCoordinate);
        coordinates.add(thirdCoordinate);

    }

    @Override
    public List<Coordinate> getCoordinates() {
        return super.getCoordinates();
    }

    @Override
    public int getSize() {
        return 0;
    }
}

