package client.game.logic;

import client.model.coordinate.Coordinate;

import java.util.Collections;
import java.util.List;

public enum LastShipCoordinatesRegister {
    COORDINATES;

    private List<Coordinate> lastShipCoordinates = Collections.emptyList();

    public List<Coordinate> getLastShipCoordinates() {
        return lastShipCoordinates;
    }

    public void setLastShipCoordinates(List<Coordinate> lastShipCoordinates) {
        this.lastShipCoordinates = lastShipCoordinates;
    }
}
