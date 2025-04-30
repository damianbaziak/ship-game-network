package client;

import battleshipnetwork.MessagePrinter;
import client.ship.SingleMastedShip;
import client.ship.TwoMastedShip;
import client.ship.service.Coordinate;
import client.ship.service.ShipService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

public class ClientShipGameNetwork {
    private static final String SERVER_IP = "localhost";  // Server IP
    private static final int SERVER_PORT = 5050;              // Server port
    private static final int gameBoardLength = 10;
    private static final char water = '-';
    private static final char ship = 'S';
    private static final int singleMastedShipNumber = 4;
    private static final int twoMastedShipNumber = 3;
    private static final int threeMastedShipNumber = 2;
    private static final int fourMastedShipNumber = 1;
    private static final ShipService shipService = new ShipService();

    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) {

            if (socket.isConnected()) {
                System.out.println();
                System.out.println("Connected to server :)");
            }

            // Odbieramy wiadomość od serwera
            String serverMessage = input.readLine();
            if ("START".equals(serverMessage)) {
                MessagePrinter.printGreeting();
            }

            char[][] gameBoard = createGameBoard();

            placeSingleMastedShips(gameBoard, water, ship, scanner, output);


        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
        }
    }


    private static char[][] placeSingleMastedShips(
            char[][] gameBoard, char water, char ship, Scanner scanner, PrintWriter output) {

        int placedShips = 0;
        System.out.println("Place your four Single-Masted Ships");

        while (placedShips < singleMastedShipNumber) {
            System.out.println();
            printThisClientGameBoard(gameBoard, ship);
            System.out.printf("Enter coordinates for the %d of 3 Single-Masted Ship (e.g. A5):%n", placedShips + 1);
            String input = scanner.nextLine();

            // Walidacja wejścia
            if (input.length() < 2 || input.length() > 3) {
                System.out.println("Invalid format. Enter e.g. A5 or B10");
                continue;
            }

            char colChar = input.charAt(0);

            String rowNumber = input.substring(1);
            char firstCharOfRowNumber = rowNumber.charAt(0);

            if (!Character.isLetter(colChar)) {
                System.out.println("THE FIRST CHARACTER MUST BE A LETTER!");
                continue;
            }

            if (!Character.isDigit(firstCharOfRowNumber)) {
                System.out.println("THE SECOND CHARACTER MUST BE A DIGIT!");
                continue;
            }

            if ((input.length() == 3) && !(rowNumber.equals("10"))) {
                System.out.println("THE SECOND AND THIRD CHARACTER MUST BE '10'!");
                continue;
            }


            int row = Integer.parseInt(rowNumber) - 1;
            int col = Character.toUpperCase(colChar) - 'A';

            char possiblePlacement = gameBoard[row][col];

            if (possiblePlacement != water) {
                System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                continue;
            }

            // Sprawdzamy sąsiednie pola, czy są wolne
            boolean canPlaceShip = true;

            // Sprawdzanie dolnego pola
            if (row < gameBoard.length - 1 && gameBoard[row + 1][col] != water) {
                canPlaceShip = false;
            }
            // Sprawdzanie górnego pola
            if (row > 0 && gameBoard[row - 1][col] != water) {
                canPlaceShip = false;
            }
            // Sprawdzanie lewego pola
            if (col > 0 && gameBoard[row][col - 1] != water) {
                canPlaceShip = false;
            }
            // Sprawdzanie prawego pola
            if (col < gameBoard[0].length - 1 && gameBoard[row][col + 1] != water) {
                canPlaceShip = false;
            }
            // Lewo-góra
            if (row > 0 && col > 0 && gameBoard[row - 1][col - 1] != water) {
                canPlaceShip = false;
            }
            // Lewo-dół
            if (row < gameBoard.length - 1 && col > 0 && gameBoard[row + 1][col - 1] != water) {
                canPlaceShip = false;
            }
            // Prawo-góra
            if (row > 0 && col < gameBoard[0].length - 1 && gameBoard[row - 1][col + 1] != water) {
                canPlaceShip = false;
            }
            // Prawo-dół
            if (row < gameBoard.length - 1 && col < gameBoard[0].length - 1 && gameBoard[row + 1][col + 1] != water) {
                canPlaceShip = false;
            }
            // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
            if (!canPlaceShip) {
                System.out.println("Cannot place ship here. There is another ship nearby.".toUpperCase());
                continue;
            }
            gameBoard[row][col] = ship;
            placedShips++;


            Coordinate coordinate = new Coordinate(row, col);
            SingleMastedShip singleMastedShip = new SingleMastedShip(coordinate);
            shipService.addShip(singleMastedShip);
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("All Single-Masted ships have been placed!".toUpperCase());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return placeTwoMastedShips(gameBoard, water, ship, scanner, output);

    }

    private static char[][] placeTwoMastedShips(
            char[][] gameBoard, char water, char ship, Scanner scanner, PrintWriter output) {

        int placedTwoMastedShips = 0;
        System.out.println();
        System.out.println("Place your three Two-Masted Ships");

        while (placedTwoMastedShips < twoMastedShipNumber) {
            System.out.println();
            printThisClientGameBoard(gameBoard, ship);
            System.out.printf("Enter first coordinate for the %d of 3 Two-Masted Ship (e.g. A5):%n",
                    placedTwoMastedShips + 1);
            String firstInput = scanner.nextLine();

            if (firstInput.equals("show")) {
                System.out.println(shipService.getShips());
            }


            // Walidacja wejścia
            if (firstInput.length() < 2 || firstInput.length() > 3) {
                System.out.println("Invalid format. Enter e.g. A5 or B10");
                continue;
            }

            char colChar = firstInput.charAt(0);

            String rowNumber = firstInput.substring(1);
            char firstCharOfRowNumber = rowNumber.charAt(0);

            if (!Character.isLetter(colChar)) {
                System.out.println("THE FIRST CHARACTER MUST BE A LETTER!");
                continue;
            }

            if (!Character.isDigit(firstCharOfRowNumber)) {
                System.out.println("THE SECOND CHARACTER MUST BE A DIGIT!");
                continue;
            }

            if ((firstInput.length() == 3) && !(rowNumber.equals("10"))) {
                System.out.println("THE SECOND AND THIRD CHARACTER MUST BE '10'!");
                continue;
            }

            int row = Integer.parseInt(rowNumber) - 1;
            int col = Character.toUpperCase(colChar) - 'A';

            char possiblePlacement = gameBoard[row][col];

            if (possiblePlacement != water) {
                System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                continue;
            }

            // Sprawdzamy sąsiednie pola, czy są wolne
            boolean canPlaceFirstMast = true;

            // Sprawdzanie dolnego pola
            if (row < gameBoard.length - 1 && gameBoard[row + 1][col] != water) {
                canPlaceFirstMast = false;
            }
            // Sprawdzanie górnego pola
            if (row > 0 && gameBoard[row - 1][col] != water) {
                canPlaceFirstMast = false;
            }
            // Sprawdzanie lewego pola
            if (col > 0 && gameBoard[row][col - 1] != water) {
                canPlaceFirstMast = false;
            }
            // Sprawdzanie prawego pola
            if (col < gameBoard[0].length - 1 && gameBoard[row][col + 1] != water) {
                canPlaceFirstMast = false;
            }
            // Lewo-góra
            if (row > 0 && col > 0 && gameBoard[row - 1][col - 1] != water) {
                canPlaceFirstMast = false;
            }
            // Lewo-dół
            if (row < gameBoard.length - 1 && col > 0 && gameBoard[row + 1][col - 1] != water) {
                canPlaceFirstMast = false;
            }
            // Prawo-góra
            if (row > 0 && col < gameBoard[0].length - 1 && gameBoard[row - 1][col + 1] != water) {
                canPlaceFirstMast = false;
            }
            // Prawo-dół
            if (row < gameBoard.length - 1 && col < gameBoard[0].length - 1 && gameBoard[row + 1][col + 1] != water) {
                canPlaceFirstMast = false;
            }
            // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
            if (!canPlaceFirstMast) {
                System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                continue;
            }


            // INPUT FOR THE SECOND MAST

            System.out.printf("Enter second coordinate for the %d of 3 Two-Masted Ship (e.g. A5):%n",
                    placedTwoMastedShips + 1);
            String secondInput = scanner.nextLine();

            // Walidacja wejścia
            if (secondInput.length() < 2 || secondInput.length() > 3) {
                System.out.println("Invalid format. Enter e.g. A5 or B10");
                continue;
            }

            char secondColChar = secondInput.charAt(0);

            String secondRowNumber = secondInput.substring(1);
            char firstCharOfSecondRowNumber = secondRowNumber.charAt(0);

            if (!Character.isLetter(secondColChar)) {
                System.out.println("THE FIRST CHARACTER MUST BE A LETTER!");
                continue;
            }

            if (!Character.isDigit(firstCharOfSecondRowNumber)) {
                System.out.println("THE SECOND CHARACTER MUST BE A DIGIT!");
                continue;
            }

            if ((secondInput.length() == 3) && !(secondRowNumber.equals("10"))) {
                System.out.println("THE SECOND AND THIRD CHARACTER MUST BE '10'!");
                continue;
            }

            int secondRow = Integer.parseInt(secondRowNumber) - 1;
            int secondCol = Character.toUpperCase(secondColChar) - 'A';

            // Sprawdzenie czy drugi maszt lezy lezy dokladnie obok pierwszego
            boolean isAdjacent =
                    (secondRow == row && Math.abs(secondCol - col) == 1) ||
                            (secondCol == col && Math.abs(secondRow - row) == 1);

            if (!isAdjacent) {
                System.out.println("Second mast must be directly next to the first one (vertically or horizontally)!"
                        .toUpperCase());
                continue;
            }

            // Pozostala czesc walidacji
            char possiblePlacementForSecondMast = gameBoard[secondRow][secondCol];

            if (possiblePlacementForSecondMast != water) {
                System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                continue;
            }

            // Sprawdzamy sąsiednie pola, czy są wolne
            boolean canPlaceSecondMast = true;

            // Sprawdzanie dolnego pola
            if (secondRow < gameBoard.length - 1 && gameBoard[secondRow + 1][secondCol] != water) {
                canPlaceSecondMast = false;
            }
            // Sprawdzanie górnego pola
            if (secondRow > 0 && gameBoard[secondRow - 1][secondCol] != water) {
                canPlaceSecondMast = false;
            }
            // Sprawdzanie lewego pola
            if (secondCol > 0 && gameBoard[secondRow][secondCol - 1] != water) {
                canPlaceSecondMast = false;
            }
            // Sprawdzanie prawego pola
            if (secondCol < gameBoard[0].length - 1 && gameBoard[secondRow][secondCol + 1] != water) {
                canPlaceSecondMast = false;
            }
            // Lewo-góra
            if (secondRow > 0 && secondCol > 0 && gameBoard[secondRow - 1][secondCol - 1] != water) {
                canPlaceSecondMast = false;
            }
            // Lewo-dół
            if (secondRow < gameBoard.length - 1 && secondCol > 0 && gameBoard[secondRow + 1][secondCol - 1] != water) {
                canPlaceSecondMast = false;
            }
            // Prawo-góra
            if (secondRow > 0 && secondCol < gameBoard[0].length - 1
                    && gameBoard[secondRow - 1][secondCol + 1] != water) {
                canPlaceSecondMast = false;
            }
            // Prawo-dół
            if (secondRow < gameBoard.length - 1 && secondCol < gameBoard[0].length - 1
                    && gameBoard[secondRow + 1][secondCol + 1] != water) {
                canPlaceSecondMast = false;
            }
            // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
            if (!canPlaceSecondMast) {
                System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                continue;
            }


            gameBoard[row][col] = ship;
            gameBoard[secondRow][secondCol] = ship;
            placedTwoMastedShips++;

            Coordinate firstCoordinate = new Coordinate(row, col);
            Coordinate secondCoordinate = new Coordinate(secondRow, secondCol);
            TwoMastedShip twoMastedShip = new TwoMastedShip(firstCoordinate, secondCoordinate);
            shipService.addShip(twoMastedShip);

        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("All Two-Masted ships have been placed!".toUpperCase());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return placeThreeMastedShips(gameBoard, water, ship, scanner, output);
    }

    private static char[][] placeThreeMastedShips(
            char[][] gameBoard, char water, char ship, Scanner scanner, PrintWriter output) {


        int placedThreeMastedShips = 0;
        System.out.println();
        System.out.println("Place your three Three-Masted Ships");

        while (placedThreeMastedShips < threeMastedShipNumber) {
            System.out.println();
            printThisClientGameBoard(gameBoard, ship);
            System.out.printf("Enter first coordinate for the %d of 2 Three-Masted Ship (e.g. A5):%n",
                    placedThreeMastedShips + 1);
            String firstInput = scanner.nextLine();

            if (firstInput.equals("show")) {
                System.out.println(shipService.getShips());
            }

            // Walidacja wejścia
            if (firstInput.length() < 2 || firstInput.length() > 3) {
                System.out.println("Invalid format. Enter e.g. A5 or B10");
                continue;
            }

            char colChar = firstInput.charAt(0);

            String rowNumber = firstInput.substring(1);
            char firstCharOfRowNumber = rowNumber.charAt(0);

            if (!Character.isLetter(colChar)) {
                System.out.println("THE FIRST CHARACTER MUST BE A LETTER!");
                continue;
            }

            if (!Character.isDigit(firstCharOfRowNumber)) {
                System.out.println("THE SECOND CHARACTER MUST BE A DIGIT!");
                continue;
            }

            if ((firstInput.length() == 3) && !(rowNumber.equals("10"))) {
                System.out.println("THE SECOND AND THIRD CHARACTER MUST BE '10'!");
                continue;
            }

            int row = Integer.parseInt(rowNumber) - 1;
            int col = Character.toUpperCase(colChar) - 'A';

            char possiblePlacement = gameBoard[row][col];

            if (possiblePlacement != water) {
                System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                continue;
            }

            // Sprawdzamy sąsiednie pola, czy są wolne
            boolean canPlaceFirstMast = true;

            // Sprawdzanie dolnego pola
            if (row < gameBoard.length - 1 && gameBoard[row + 1][col] != water) {
                canPlaceFirstMast = false;
            }
            // Sprawdzanie górnego pola
            if (row > 0 && gameBoard[row - 1][col] != water) {
                canPlaceFirstMast = false;
            }
            // Sprawdzanie lewego pola
            if (col > 0 && gameBoard[row][col - 1] != water) {
                canPlaceFirstMast = false;
            }
            // Sprawdzanie prawego pola
            if (col < gameBoard[0].length - 1 && gameBoard[row][col + 1] != water) {
                canPlaceFirstMast = false;
            }
            // Lewo-góra
            if (row > 0 && col > 0 && gameBoard[row - 1][col - 1] != water) {
                canPlaceFirstMast = false;
            }
            // Lewo-dół
            if (row < gameBoard.length - 1 && col > 0 && gameBoard[row + 1][col - 1] != water) {
                canPlaceFirstMast = false;
            }
            // Prawo-góra
            if (row > 0 && col < gameBoard[0].length - 1 && gameBoard[row - 1][col + 1] != water) {
                canPlaceFirstMast = false;
            }
            // Prawo-dół
            if (row < gameBoard.length - 1 && col < gameBoard[0].length - 1 && gameBoard[row + 1][col + 1] != water) {
                canPlaceFirstMast = false;
            }
            // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
            if (!canPlaceFirstMast) {
                System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                continue;
            }


            // ******************* INPUT AND VALIDATION FOR THE SECOND MAST **********************

            System.out.printf("Enter second coordinate for the %d of 2 Three-Masted Ship (e.g. A5):%n",
                    placedThreeMastedShips + 1);
            String secondInput = scanner.nextLine();

            // Walidacja wejścia
            if (secondInput.length() < 2 || secondInput.length() > 3) {
                System.out.println("Invalid format. Enter e.g. A5 or B10");
                continue;
            }

            char secondColChar = secondInput.charAt(0);

            String secondRowNumber = secondInput.substring(1);
            char firstCharOfSecondRowNumber = secondRowNumber.charAt(0);

            if (!Character.isLetter(secondColChar)) {
                System.out.println("THE FIRST CHARACTER MUST BE A LETTER!");
                continue;
            }

            if (!Character.isDigit(firstCharOfSecondRowNumber)) {
                System.out.println("THE SECOND CHARACTER MUST BE A DIGIT!");
                continue;
            }

            if ((secondInput.length() == 3) && !(secondRowNumber.equals("10"))) {
                System.out.println("THE SECOND AND THIRD CHARACTER MUST BE '10'!");
                continue;
            }

            int secondRow = Integer.parseInt(secondRowNumber) - 1;
            int secondCol = Character.toUpperCase(secondColChar) - 'A';

            // Sprawdzenie czy drugi maszt lezy lezy dokladnie obok pierwszego
            boolean isAdjacent =
                    (secondRow == row && Math.abs(secondCol - col) == 1) ||
                            (secondCol == col && Math.abs(secondRow - row) == 1);

            if (!isAdjacent) {
                System.out.println("Second mast must be directly next to the first one (vertically or horizontally)!"
                        .toUpperCase());
                continue;
            }

            // Pozostala czesc walidacji
            char possiblePlacementForSecondMast = gameBoard[secondRow][secondCol];

            if (possiblePlacementForSecondMast != water) {
                System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                continue;
            }

            // Sprawdzamy sąsiednie pola, czy są wolne
            boolean canPlaceSecondMast = true;

            // Sprawdzanie dolnego pola
            if (secondRow < gameBoard.length - 1 && gameBoard[secondRow + 1][secondCol] != water) {
                canPlaceSecondMast = false;
            }
            // Sprawdzanie górnego pola
            if (secondRow > 0 && gameBoard[secondRow - 1][secondCol] != water) {
                canPlaceSecondMast = false;
            }
            // Sprawdzanie lewego pola
            if (secondCol > 0 && gameBoard[secondRow][secondCol - 1] != water) {
                canPlaceSecondMast = false;
            }
            // Sprawdzanie prawego pola
            if (secondCol < gameBoard[0].length - 1 && gameBoard[secondRow][secondCol + 1] != water) {
                canPlaceSecondMast = false;
            }
            // Lewo-góra
            if (secondRow > 0 && secondCol > 0 && gameBoard[secondRow - 1][secondCol - 1] != water) {
                canPlaceSecondMast = false;
            }
            // Lewo-dół
            if (secondRow < gameBoard.length - 1 && secondCol > 0 && gameBoard[secondRow + 1][secondCol - 1] != water) {
                canPlaceSecondMast = false;
            }
            // Prawo-góra
            if (secondRow > 0 && secondCol < gameBoard[0].length - 1
                    && gameBoard[secondRow - 1][secondCol + 1] != water) {
                canPlaceSecondMast = false;
            }
            // Prawo-dół
            if (secondRow < gameBoard.length - 1 && secondCol < gameBoard[0].length - 1
                    && gameBoard[secondRow + 1][secondCol + 1] != water) {
                canPlaceSecondMast = false;
            }
            // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
            if (!canPlaceSecondMast) {
                System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                continue;
            }


            // ******************* INPUT AND VALIDATION FOR THE THIRD MAST **********************

            System.out.printf("Enter second coordinate for the %d of 2 Three-Masted Ship (e.g. A5):%n",
                    placedThreeMastedShips + 1);
            String thirdInput = scanner.nextLine();

            // Walidacja wejścia
            if (thirdInput.length() < 2 || thirdInput.length() > 3) {
                System.out.println("Invalid format. Enter e.g. A5 or B10");
                continue;
            }

            char thirdColChar = thirdInput.charAt(0);

            String thirdRowNumber = thirdInput.substring(1);
            char firstCharOfThirdRowNumber = thirdRowNumber.charAt(0);

            if (!Character.isLetter(thirdColChar)) {
                System.out.println("THE FIRST CHARACTER MUST BE A LETTER!");
                continue;
            }

            if (!Character.isDigit(firstCharOfThirdRowNumber)) {
                System.out.println("THE SECOND CHARACTER MUST BE A DIGIT!");
                continue;
            }

            if ((thirdInput.length() == 3) && !(thirdRowNumber.equals("10"))) {
                System.out.println("THE SECOND AND THIRD CHARACTER MUST BE '10'!");
                continue;
            }

            int thirdRow = Integer.parseInt(thirdRowNumber) - 1;
            int thirdCol = Character.toUpperCase(thirdColChar) - 'A';

            // Sprawdzenie czy drugi maszt lezy lezy dokladnie obok pierwszego
            boolean isThirdMastAdjacent =
                    (thirdRow == row && Math.abs(thirdCol - col) == 2) ||
                            (thirdCol == col && Math.abs(thirdRow - row) == 2);

            if (!isThirdMastAdjacent) {
                System.out.println(("Second mast must be directly next to the first and second one " +
                        "(vertically or horizontally)!").toUpperCase());
                continue;
            }

            // Pozostala czesc walidacji
            char possiblePlacementForThirdMast = gameBoard[thirdRow][thirdCol];

            if (possiblePlacementForThirdMast != water) {
                System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                continue;
            }

            // Sprawdzamy sąsiednie pola, czy są wolne
            boolean canPlaceThirdMast = true;

            // Sprawdzanie dolnego pola
            if (thirdRow < gameBoard.length - 1 && gameBoard[thirdRow + 1][thirdCol] != water) {
                canPlaceThirdMast = false;
            }
            // Sprawdzanie górnego pola
            if (thirdRow > 0 && gameBoard[thirdRow - 1][thirdCol] != water) {
                canPlaceThirdMast = false;
            }
            // Sprawdzanie lewego pola
            if (thirdCol > 0 && gameBoard[thirdRow][thirdCol - 1] != water) {
                canPlaceThirdMast = false;
            }
            // Sprawdzanie prawego pola
            if (thirdCol < gameBoard[0].length - 1 && gameBoard[thirdRow][thirdCol + 1] != water) {
                canPlaceThirdMast = false;
            }
            // Lewo-góra
            if (thirdRow > 0 && thirdCol > 0 && gameBoard[thirdRow - 1][thirdCol - 1] != water) {
                canPlaceThirdMast = false;
            }
            // Lewo-dół
            if (thirdRow < gameBoard.length - 1 && thirdCol > 0 && gameBoard[thirdRow + 1][thirdCol - 1] != water) {
                canPlaceThirdMast = false;
            }
            // Prawo-góra
            if (thirdRow > 0 && thirdCol < gameBoard[0].length - 1
                    && gameBoard[thirdRow - 1][thirdCol + 1] != water) {
                canPlaceThirdMast = false;
            }
            // Prawo-dół
            if (thirdRow < gameBoard.length - 1 && thirdCol < gameBoard[0].length - 1
                    && gameBoard[thirdRow + 1][thirdCol + 1] != water) {
                canPlaceThirdMast = false;
            }
            // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
            if (!canPlaceThirdMast) {
                System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                continue;
            }


            gameBoard[row][col] = ship;
            gameBoard[secondRow][secondCol] = ship;
            gameBoard[thirdRow][thirdCol] = ship;
            placedThreeMastedShips++;

        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("All Three-Masted ships have been placed!".toUpperCase());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return placeFourMastedShips(gameBoard, water, ship, scanner, output);
    }

    private static char[][] placeFourMastedShips(
            char[][] gameBoard, char water, char ship, Scanner scanner, PrintWriter output) {

        int placedFourMastedShips = 0;
        System.out.println();
        System.out.println("Place your three Three-Masted Ships");

        while (placedFourMastedShips < fourMastedShipNumber) {
            System.out.println();
            printThisClientGameBoard(gameBoard, ship);
            System.out.printf("Enter first coordinate for the %d of 1 Four-Masted Ship (e.g. A5):%n",
                    placedFourMastedShips + 1);
            String firstInput = scanner.nextLine();

            if (firstInput.equals("show")) {
                System.out.println(shipService.getShips());
            }
        }
        return gameBoard;
    }


    private static char[][] createGameBoard() {
        char[][] gameBoard = new char[gameBoardLength][gameBoardLength];
        for (char[] row : gameBoard) {
            Arrays.fill(row, water);
        }
        return gameBoard;
    }


    private static void displayGameBoard(char[][] gameBoard) {
        System.out.println();
        for (char[] row : gameBoard) {
            for (int j = 0; j < gameBoard.length; j++) {
                System.out.print(row[j] + " ");
            }
            System.out.println();
        }
        for (int i = 0; i < 3; i++) {
            System.out.println();
        }
    }


    private static void printThisClientGameBoard(char[][] gameBoard, char ship) {
        // Ten kod wyswietla nazwy kolumn
        int gameBoardLength = gameBoard.length;
        char[] rowName = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'J'};
        System.out.print("   ");
        for (int i = 0; i < gameBoardLength; i++) {
            System.out.print(rowName[i] + "  ");
        }
        System.out.println();

        // Ten kod wyswietla wszystkie wiersze bez dziesiatego
        for (int row = 0; row < 9; row++) {
            System.out.print(" ");
            System.out.print(row + 1 + " ");
            for (int col = 0; col < gameBoardLength; col++) {
                char position = gameBoard[row][col];
                if (position == ship) {
                    System.out.print(ship + "  ");
                } else {
                    System.out.print(position + "  ");
                }
            }
            System.out.println();
        }

        System.out.print(10 + " ");
        for (int col = 0; col < gameBoardLength; col++) {
            char position = gameBoard[9][col];
            if (position == ship) {
                System.out.print(ship + "  ");
            } else {
                System.out.print(position + "  ");
            }
        }
        // ***
        System.out.println();
        System.out.println();
    }

    /*
    private static char[][] updateThisClientGameBoard(char[][] gameBoard, int col, int row) {
        gameBoard[row][col];
        return gameBoard;
    }

     */

}



