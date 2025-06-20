package client.service;

import client.model.coordinate.Coordinate;
import client.model.ship.Ship;

import java.util.*;

public class ShipService {
    private final List<Ship> listOfMyCreatedShips = new ArrayList<>();

    public void addShip(Ship ship) {
        listOfMyCreatedShips.add(ship);
    }

    public void removeShip(Ship ship) {
        listOfMyCreatedShips.remove(ship);
    }

    public List<Ship> getListOfMyCreatedShips() {
        return Collections.unmodifiableList(listOfMyCreatedShips);
    }

    public Optional<Ship> getShip(Coordinate coordinate) {
        return listOfMyCreatedShips
                .stream()
                .filter(ship1 -> ship1.getCoordinates().contains(coordinate))
                .findFirst();
    }
}

