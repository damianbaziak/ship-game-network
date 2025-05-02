package client.ship;

import client.ship.service.Coordinate;

import java.util.ArrayList;
import java.util.List;

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
    public List<Coordinate> getCoordinates() {
        return super.getCoordinates();
    }

    @Override
    public int getSize() {
        return 0;
    }
}
