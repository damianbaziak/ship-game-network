package client.ship.service;

import client.ship.Ship;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
}
