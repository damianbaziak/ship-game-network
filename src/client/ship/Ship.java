package client.ship;

import client.ship.service.Coordinate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Ship implements Serializable {
    protected Set<Coordinate> hitCoordinates = new HashSet<>();
    protected List<Coordinate> coordinates;

    public Ship() {
    }

    public Ship(Ship otherShip) {
        this.coordinates = new ArrayList<>(otherShip.coordinates);
        this.hitCoordinates = new HashSet<>(otherShip.hitCoordinates);
    }

    public void addHit(Coordinate c) {
        hitCoordinates.add(c);
    }

    public abstract boolean isSunk();

    public Set<Coordinate> getHitCoordinates() {
        return hitCoordinates;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    public abstract int getSize(); // Metoda, która zwróci rozmiar statku (1 dla jednomasztowego, 2 dla dwumasztowego)

}
