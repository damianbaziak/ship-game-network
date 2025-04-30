package client.ship.service;

import client.ship.Ship;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShipService {
    private final List<Ship> ships = new ArrayList<>();

    public void addShip(Ship ship) {
        ships.add(ship);
    }

    public void removeSingleMastedShip(Ship ship) {
        ships.remove(ship);
    }

    public List<Ship> getShips() {
        return Collections.unmodifiableList(ships);
    }
}
