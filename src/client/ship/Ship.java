package client.ship;

import client.ship.service.Coordinate;

import java.util.List;

public abstract class Ship {
    protected List<Coordinate> coordinates;

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    public abstract int getSize(); // Metoda, która zwróci rozmiar statku (1 dla jednomasztowego, 2 dla dwumasztowego)

}
