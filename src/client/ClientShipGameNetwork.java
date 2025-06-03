package client;

import battleshipnetwork.MessagePrinter;
import client.ship.*;
import client.ship.service.Coordinate;
import client.ship.service.ShipService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class ClientShipGameNetwork {
    private static final String SERVER_IP = "localhost";  // Server IP
    private static final int SERVER_PORT = 5050;              // Server port
    private static final int gameBoardLength = 10;
    private static final char water = '-';
    private static final char ship = '#';
    private static final char HIT_AND_SUNK_CHAR = 'X';
    private static final char HIT_MAST_CHAR = '?';
    private static final char miss = '0';
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
                System.out.println();
                System.out.println("Scanning the horizon... the opponent hasn't shown up yet.");
            }

            // Odbieramy wiadomość od serwera
            String serverMessage = input.readLine();
            if ("START".equals(serverMessage)) {
                MessagePrinter.printGreeting();
            }

            char[][] myBoard = createBoard();
            char[][] opponentBoard = createBoard();

            placeShips(myBoard, water, ship, scanner, output);

            String serverMessageToWarBeginning = input.readLine();

            if ("The war has begun.".equalsIgnoreCase(serverMessageToWarBeginning)) {
                MessagePrinter.displayLetsStart();
                runningGame(myBoard, opponentBoard, water, ship, HIT_AND_SUNK_CHAR, miss, scanner, input, output);
            }


        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
        }
    }

    private static void runningGame(char[][] myBoard, char[][] opponentBoard, char water, char ship, char hitAndSunk,
                                    char miss, Scanner scanner, BufferedReader input, PrintWriter output)
            throws IOException, InterruptedException {

        List<Ship> myShips = shipService.getListOfMyCreatedShips();
        List<Coordinate> myShipsHitCoordinates = new ArrayList<>();
        Map<Integer, List<Ship>> hitOpponentShipsBySize = new HashMap<>();


        boolean gameRunning = true;

        while (gameRunning) {

            String whoseTurnIsIt = input.readLine();

            if ("Your turn.".equalsIgnoreCase(whoseTurnIsIt)) {
                makeShot(myBoard, opponentBoard, scanner, input, output, ship, hitAndSunk, miss, hitOpponentShipsBySize);
            } else if ("Game over.".equalsIgnoreCase(whoseTurnIsIt)) {
                gameRunning = false;
            } else {
                opponentShot(myBoard, opponentBoard, myShips, myShipsHitCoordinates, scanner, input, output, ship, hitAndSunk);
            }

        }
    }

    private static void opponentShot(
            char[][] myBoard, char[][] opponentBoard, List<Ship> myShips, List<Coordinate> myShipsHitCoordinates,
            Scanner scanner, BufferedReader input, PrintWriter output, char ship, char hit)
            throws IOException, InterruptedException {

        boolean opponentHitYouWait = true;

        while (opponentHitYouWait) {
            displayEntireGameBoard(myBoard, opponentBoard, ship);

            System.out.println("Opponent is firing. Waiting for their shot...");
            String opponentShot = input.readLine();
            System.out.println();
            System.out.println("Opponent has fired at " + opponentShot.toUpperCase());
            Thread.sleep(1000);

            String rowNumber = opponentShot.substring(1);
            int row = Integer.parseInt(rowNumber) - 1;
            int col = Integer.parseInt(String.valueOf(Character.toUpperCase(opponentShot.charAt(0)) - 'A'));

            Coordinate opponentShotCoordinate = new Coordinate(row, col);

            Optional<Ship> possibleHitShip = myShips.stream()
                    .filter(s -> s.getCoordinates().contains(opponentShotCoordinate))
                    .findFirst();

            if (myShipsHitCoordinates.contains(opponentShotCoordinate)) {
                output.println("This shot has been already fired!");
                output.println("");
                output.println("");
                output.println("");
                System.out.println();
                System.out.println("The opponent shot at a location that was already fired upon!".toUpperCase());
                Thread.sleep(1000);
                opponentHitYouWait = false;

            } else if ((!myShipsHitCoordinates.contains(opponentShotCoordinate)) && (possibleHitShip.isPresent())) {

                myShipsHitCoordinates.add(opponentShotCoordinate);

                Ship myShip = possibleHitShip.get();

                myShip.getCoordinates().remove(opponentShotCoordinate);

                myBoard[row][col] = hit;

                String firstHitMessageToDisplay;
                String secondHitMessageToDisplay = "";
                String thirdHitMessageToDisplay = "";
                String fourthHitMessageToDisplay;

                if (myShip.getSize() == 1) {
                    output.println("You hit a single-masted ship!");

                    firstHitMessageToDisplay = "Opponent hit your single-masted ship!".toUpperCase();

                    if (myShip.getCoordinates().isEmpty()) {
                        output.println("You've sunk a single-masted ship!");
                        secondHitMessageToDisplay = "Opponent sunk one of your Single-Masted Ships!".toUpperCase();
                        shipService.removeShip(myShip);

                    } else output.println("");

                    boolean allOneMastedShipsSunk = myShips.stream()
                            .filter(s -> s.getSize() == 1)
                            .allMatch(s -> s.getCoordinates().isEmpty());

                    if (allOneMastedShipsSunk) {
                        output.println("All Single-Masted Ships have been sunk!");
                        thirdHitMessageToDisplay = "Opponent has sunk all of your Single-Masted Ships!".toUpperCase();

                    } else output.println("");

                    fourthHitMessageToDisplay = areAllShipsSunk(myShips, output);

                    displayMessages(firstHitMessageToDisplay, secondHitMessageToDisplay, thirdHitMessageToDisplay,
                            fourthHitMessageToDisplay);

                    if (fourthHitMessageToDisplay != null && !fourthHitMessageToDisplay.isEmpty()) {
                        MessagePrinter.printYouLose();
                        opponentHitYouWait = false;

                    }

                } else if (myShip.getSize() == 2) {

                    output.println("You hit a two-masted ship!");
                    firstHitMessageToDisplay = "Opponent hit your Two-Masted Ship!".toUpperCase();

                    if (myShip.getCoordinates().isEmpty()) {
                        output.println("You've sunk a Two-Masted Ship!");
                        secondHitMessageToDisplay = "Opponent sunk one of your Two-Masted Ships!".toUpperCase();
                        shipService.removeShip(myShip);

                    } else output.println("");

                    boolean allTwoMastedShipsSunk = myShips.stream()
                            .filter(s -> s.getSize() == 2)
                            .allMatch(s -> s.getCoordinates().isEmpty());

                    if (allTwoMastedShipsSunk) {
                        output.println("All Two-Masted Ships have been sunk!");
                        thirdHitMessageToDisplay = "Opponent has sunk all of your Two-Masted Ships!".toUpperCase();

                    } else output.println("");

                    fourthHitMessageToDisplay = areAllShipsSunk(myShips, output);

                    displayMessages(firstHitMessageToDisplay, secondHitMessageToDisplay, thirdHitMessageToDisplay,
                            fourthHitMessageToDisplay);

                    if (fourthHitMessageToDisplay != null && !fourthHitMessageToDisplay.isEmpty()) {
                        MessagePrinter.printYouLose();
                        opponentHitYouWait = false;

                    }

                } else if (myShip.getSize() == 3) {

                    output.println("You hit a three-masted ship!");
                    firstHitMessageToDisplay = "Opponent hit your three-masted ship!".toUpperCase();

                    if (myShip.getCoordinates().isEmpty()) {
                        output.println("You've sunk a Three-Masted Ship!");
                        secondHitMessageToDisplay = "Opponent sunk one of your Three-Masted Ships!".toUpperCase();
                        shipService.removeShip(myShip);

                    } else output.println("");

                    boolean allThreeMastedShipsSunk = myShips.stream()
                            .filter(s -> s.getSize() == 3)
                            .allMatch(s -> s.getCoordinates().isEmpty());

                    if (allThreeMastedShipsSunk) {
                        output.println("All Three-Masted Ships have been sunk!");
                        thirdHitMessageToDisplay = "Opponent has sunk all of your Three-Masted Ships!".toUpperCase();

                    } else output.println("");

                    fourthHitMessageToDisplay = areAllShipsSunk(myShips, output);

                    displayMessages(firstHitMessageToDisplay, secondHitMessageToDisplay, thirdHitMessageToDisplay,
                            fourthHitMessageToDisplay);

                    if (fourthHitMessageToDisplay != null && !fourthHitMessageToDisplay.isEmpty()) {
                        MessagePrinter.printYouLose();
                        opponentHitYouWait = false;

                    }

                } else if (possibleHitShip.get().getSize() == 4) {

                    output.println("You hit a four-masted ship!");
                    firstHitMessageToDisplay = "Opponent hit your four-masted ship!".toUpperCase();

                    if (myShip.getCoordinates().isEmpty()) {
                        output.println("You've sunk a Four-Masted Ship!");
                        secondHitMessageToDisplay = "Opponent sunk your Four-Masted Ship!".toUpperCase();
                        shipService.removeShip(myShip);

                    } else output.println("");

                    boolean allFourMastedShipsSunk = myShips.stream()
                            .filter(s -> s.getSize() == 4)
                            .allMatch(s -> s.getCoordinates().isEmpty());

                    if (allFourMastedShipsSunk) {
                        output.println("All Four-Masted Ships have been sunk.");
                        thirdHitMessageToDisplay = "Opponent has sunk all of your Four-Masted Ships!".toUpperCase();

                    } else output.println("");

                    fourthHitMessageToDisplay = areAllShipsSunk(myShips, output);

                    displayMessages(firstHitMessageToDisplay, secondHitMessageToDisplay, thirdHitMessageToDisplay,
                            fourthHitMessageToDisplay);

                    if (fourthHitMessageToDisplay != null && !fourthHitMessageToDisplay.isEmpty()) {
                        MessagePrinter.printYouLose();
                        opponentHitYouWait = false;

                    }
                }

            } else {
                output.println("Missed.");
                output.println("");
                output.println("");
                output.println("");
                System.out.println();
                System.out.println("Opponent missed!".toUpperCase());
                Thread.sleep(1000);
                opponentHitYouWait = false;
            }
        }
    }

    private static void displayMessages(
            String firstHitMessageToDisplay, String secondHitMessageToDisplay,
            String thirdHitMessageToDisplay, String fourthHitMessageToDisplay) throws InterruptedException {

        System.out.println();
        System.out.println(firstHitMessageToDisplay);
        Thread.sleep(1000);

        if (!secondHitMessageToDisplay.isEmpty()) {
            System.out.println();
            System.out.println(secondHitMessageToDisplay);
            Thread.sleep(1000);
        }
        if (!thirdHitMessageToDisplay.isEmpty()) {
            System.out.println();
            System.out.println(thirdHitMessageToDisplay);
            Thread.sleep(1000);
        }
        if (!fourthHitMessageToDisplay.isEmpty()) {
            System.out.println();
            System.out.println(fourthHitMessageToDisplay);
            Thread.sleep(1000);
        }
    }

    private static String areAllShipsSunk(
            List<Ship> myShips, PrintWriter output) {

        if (myShips.isEmpty()) {
            output.println("All ships have been sunk. You win!");
            return "Opponent sunk all your ships. You lost the battle!".toUpperCase();

        } else output.println();
        return null;
    }

    private static void makeShot(
            char[][] myBoard, char[][] opponentBoard, Scanner scanner, BufferedReader input, PrintWriter output,
            char ship, char hitAndSunk, char miss,
            Map<Integer, List<Ship>> hitOpponentShipsBySize)
            throws IOException, InterruptedException {

        displayEntireGameBoard(myBoard, opponentBoard, ship);

        boolean youHitYouTurn = true;

        while (youHitYouTurn) {

            // displayEntireGameBoard(myBoard, opponentBoard, ship);

            System.out.println("Your turn! Enter the target coordinates: ");

            String myShot = scanner.nextLine();

            boolean isValidInput = validateInputFields(myShot);
            if (!isValidInput) continue;

            String rowNumber = myShot.substring(1);

            int col = Character.toUpperCase(myShot.charAt(0)) - 'A';
            int row = Integer.parseInt(rowNumber) - 1;

            output.println(myShot);
            String opponentReport = input.readLine();
            String secondOpponentReport = input.readLine();
            String thirdOpponentReport = input.readLine();
            String fourthOpponentReport = input.readLine();


            if ("This shot has been already fired!".equalsIgnoreCase(opponentReport)) {

                MessagePrinter.printAlreadyHit();
                Thread.sleep(1000);

                youHitYouTurn = false;

            } else if (opponentReport.startsWith("You hit")) {

                Coordinate hitCoordinate = new Coordinate(row, col);

                if ("You hit a single-masted ship!".equalsIgnoreCase(opponentReport)) {

                    List<Ship> hitOpponentSingleMastedShips =
                            hitOpponentShipsBySize.computeIfAbsent(1, k -> new ArrayList<>());

                    SingleMastedShip newShip = new SingleMastedShip();
                    newShip.addHit(hitCoordinate);
                    hitOpponentSingleMastedShips.add(newShip);

                    // Mark the ship as sunk immediately because one-masted ships are destroyed with a single hit.
                    opponentBoard[hitCoordinate.getRow()][hitCoordinate.getCol()] = hitAndSunk;

                    displayEntireGameBoard(myBoard, opponentBoard, ship);
                    Thread.sleep(500);
                    MessagePrinter.printHit();
                    Thread.sleep(1000);

                    System.out.println();
                    System.out.println("You hit a single-masted ship!".toUpperCase());
                    Thread.sleep(1000);

                    if (!secondOpponentReport.isBlank()
                            && "You've sunk a Single-Masted Ship!".equalsIgnoreCase(secondOpponentReport)) {

                        System.out.println();
                        System.out.println("You've sunk a Single-Masted Ship!".toUpperCase());
                        Thread.sleep(1000);
                    }

                    if (!thirdOpponentReport.isBlank()
                            && "All Single-Masted Ships have been sunk!".equalsIgnoreCase(thirdOpponentReport)) {

                        System.out.println();
                        System.out.println("All Single-Masted Ships have been sunk!".toUpperCase());
                        Thread.sleep(1000);
                    }

                    if (didPLayerWin(fourthOpponentReport)) {
                        youHitYouTurn = false;
                    }

                } else if ("You hit a two-masted ship!".equalsIgnoreCase(opponentReport)) {

                    List<Ship> hitOpponentTwoMastedShips =
                            hitOpponentShipsBySize.computeIfAbsent(2, k -> new ArrayList<>());

                    Optional<Ship> optionalShip = hitOpponentTwoMastedShips
                            .stream()
                            .filter(s -> !s.getHitCoordinates().contains(hitCoordinate))
                            .filter(s -> s.getHitCoordinates().size() == 1)
                            .filter(s -> s.getHitCoordinates().stream().anyMatch(
                                    coordinate -> areHitCoordinatesAdjacent(coordinate, hitCoordinate)))
                            .findFirst();

                    optionalShip.ifPresentOrElse(
                            s -> s.addHit(hitCoordinate),
                            () -> {
                                TwoMastedShip newShip = new TwoMastedShip();
                                newShip.addHit(hitCoordinate);
                                hitOpponentTwoMastedShips.add(newShip);
                            }
                    );


                    if (!secondOpponentReport.isBlank()
                            && "You've sunk a Two-Masted Ship!".equalsIgnoreCase(secondOpponentReport)) {

                        optionalShip.ifPresent(s -> {
                            s.getHitCoordinates().forEach(coordinate -> {
                                opponentBoard[coordinate.getRow()][coordinate.getCol()] = hitAndSunk;
                            });
                        });


                        displayEntireGameBoard(myBoard, opponentBoard, ship);
                        Thread.sleep(500);
                        MessagePrinter.printHit();
                        Thread.sleep(1000);
                        System.out.println("You hit a two-masted ship!".toUpperCase());
                        Thread.sleep(1000);
                        System.out.println();
                        System.out.println("You've sunk a Two-Masted Ship!".toUpperCase());
                        Thread.sleep(1000);

                    } else {

                        opponentBoard[hitCoordinate.getRow()][hitCoordinate.getCol()] = HIT_MAST_CHAR;

                        displayEntireGameBoard(myBoard, opponentBoard, ship);
                        Thread.sleep(500);
                        MessagePrinter.printHit();
                        Thread.sleep(1000);
                        System.out.println("You hit a two-masted ship!".toUpperCase());
                        Thread.sleep(1000);
                    }

                    if (!thirdOpponentReport.isBlank()
                            && "All Two-Masted Ships have been sunk!".equalsIgnoreCase(thirdOpponentReport)) {

                        System.out.println();
                        System.out.println("All Two-Masted Ships have been sunk!".toUpperCase());
                        Thread.sleep(1000);
                    }

                    if (didPLayerWin(fourthOpponentReport)) {
                        youHitYouTurn = false;
                    }


                } else if ("You hit a three-masted ship!".equalsIgnoreCase(opponentReport)) {

                    List<Ship> hitThreeMastedShips = hitOpponentShipsBySize.computeIfAbsent(
                            3, k -> new ArrayList<>());

                    Optional<Ship> optionalShip = hitThreeMastedShips
                            .stream()
                            .filter(s -> !s.getHitCoordinates().contains(hitCoordinate))
                            .filter(s -> s.getHitCoordinates().size() < 3)
                            .filter(s -> s.getHitCoordinates().stream().anyMatch(
                                    coordinate -> areHitCoordinatesAdjacent(coordinate, hitCoordinate)))
                            .findFirst();

                    optionalShip.ifPresentOrElse(s -> s.addHit(hitCoordinate),
                            () -> {
                                Ship newShip = new ThreeMastedShip();
                                newShip.addHit(hitCoordinate);
                                hitThreeMastedShips.add(newShip);
                            }
                    );

                    if (!secondOpponentReport.isBlank()
                            && "You've sunk a Three-Masted Ship!".equalsIgnoreCase(secondOpponentReport)) {

                        optionalShip.ifPresent(s -> s.getHitCoordinates().forEach(
                                        coordinate -> {
                                            opponentBoard[coordinate.getRow()][coordinate.getCol()] = hitAndSunk;
                                        }
                                )
                        );

                        displayEntireGameBoard(myBoard, opponentBoard, ship);
                        Thread.sleep(500);
                        MessagePrinter.printHit();
                        Thread.sleep(1000);
                        System.out.println("You hit a three-masted ship!".toUpperCase());
                        Thread.sleep(1000);
                        System.out.println();
                        System.out.println("You've sunk a Three-Masted Ship!".toUpperCase());
                        Thread.sleep(1000);

                    } else {

                        opponentBoard[hitCoordinate.getRow()][hitCoordinate.getCol()] = HIT_MAST_CHAR;

                        displayEntireGameBoard(myBoard, opponentBoard, ship);
                        Thread.sleep(500);
                        MessagePrinter.printHit();
                        Thread.sleep(1000);
                        System.out.println("You hit a three-masted ship!".toUpperCase());
                        Thread.sleep(1000);

                    }

                    if (!thirdOpponentReport.isBlank()
                            && "All Three-Masted Ships have been sunk!".equalsIgnoreCase(thirdOpponentReport)) {

                        System.out.println();
                        System.out.println("All Three-Masted Ships have been sunk!".toUpperCase());
                        Thread.sleep(1000);
                    }

                    if (didPLayerWin(fourthOpponentReport)) {
                        youHitYouTurn = false;
                    }


                } else if ("You hit a four-masted ship!".equalsIgnoreCase(opponentReport)) {

                    List<Ship> hitFourMastedShips = hitOpponentShipsBySize
                            .computeIfAbsent(4, k -> new ArrayList<>());

                    Optional<Ship> optionalShip = hitFourMastedShips
                            .stream()
                            .filter(s -> !s.getHitCoordinates().contains(hitCoordinate))
                            .filter(s -> s.getHitCoordinates().size() < 4)
                            .filter(s -> s.getHitCoordinates().stream().anyMatch(
                                    coordinate -> areHitCoordinatesAdjacent(coordinate, hitCoordinate)))
                            .findFirst();

                    optionalShip.ifPresentOrElse(
                            s -> s.addHit(hitCoordinate),
                            () -> {
                                Ship newShip = new FourMastedShip();
                                newShip.addHit(hitCoordinate);
                                hitFourMastedShips.add(newShip);
                            }
                    );

                    if (!secondOpponentReport.isBlank()
                            && "You've sunk a Four-Masted Ship!".equalsIgnoreCase(secondOpponentReport)) {

                        optionalShip.ifPresent(s -> s.getHitCoordinates().forEach(
                                        coordinate -> {
                                            opponentBoard[coordinate.getRow()][coordinate.getCol()] = hitAndSunk;
                                        }
                                )
                        );

                        displayEntireGameBoard(myBoard, opponentBoard, ship);
                        Thread.sleep(500);
                        MessagePrinter.printHit();
                        Thread.sleep(1000);
                        System.out.println("You hit a four-masted ship!".toUpperCase());
                        Thread.sleep(1000);
                        System.out.println();
                        System.out.println("You've sunk a Four-Masted Ship!".toUpperCase());
                        Thread.sleep(1000);

                    } else {
                        opponentBoard[hitCoordinate.getRow()][hitCoordinate.getCol()] = HIT_MAST_CHAR;


                        displayEntireGameBoard(myBoard, opponentBoard, ship);
                        Thread.sleep(500);
                        MessagePrinter.printHit();
                        Thread.sleep(1000);
                        System.out.println("You hit a four-masted ship!".toUpperCase());
                        Thread.sleep(1000);
                    }

                    if (!thirdOpponentReport.isBlank()
                            && "All Four-Masted Ships have been sunk!".equalsIgnoreCase(thirdOpponentReport)) {

                        System.out.println();
                        System.out.println("All Four-Masted Ships have been sunk!".toUpperCase());
                        Thread.sleep(1000);
                    }

                    if (didPLayerWin(fourthOpponentReport)) {
                        youHitYouTurn = false;
                    }


                }


            } else {
                opponentBoard[row][col] = miss;
                displayEntireGameBoard(myBoard, opponentBoard, ship);
                MessagePrinter.displayMiss();
                Thread.sleep(1000);
                youHitYouTurn = false;


            }
        }


    }

    private static boolean areHitCoordinatesAdjacent(Coordinate coordinate, Coordinate hitCoordinate) {
        int differenceCol = hitCoordinate.getCol() - coordinate.getCol();
        int differenceRow = hitCoordinate.getRow() - coordinate.getRow();
        return differenceRow + differenceCol == 1;
    }

    private static boolean didPLayerWin(String fourthOpponentReport) throws InterruptedException {
        if (!fourthOpponentReport.isBlank()
                && "All ships have been sunk. You win.".equalsIgnoreCase(fourthOpponentReport)) {
            System.out.println();
            System.out.println("All ships have been sunk. You win!".toUpperCase());
            Thread.sleep(1000);
            MessagePrinter.printYouWin();
            return true;
        }
        return false;
    }

    private static boolean validateInputFields(String input) {

        if (input.length() < 2 || input.length() > 3) {
            System.out.println();
            System.out.println("Invalid format. Enter e.g. A5 or B10");
            return false;
        }

        char colChar = input.charAt(0);

        if (!Character.isLetter(colChar)) {
            System.out.println();
            System.out.println("THE FIRST CHARACTER MUST BE A LETTER!");
            return false;
        }

        String rowNumber = input.substring(1);
        char firstCharOfRowNumber = rowNumber.charAt(0);

        int col = Character.toUpperCase(colChar) - 'A';
        int row = Integer.parseInt(rowNumber) - 1;


        if ((input.length() == 3) && !(rowNumber.equals("10"))) {
            System.out.println();
            System.out.println("THE SECOND AND THIRD CHARACTER MUST BE '10'!");
            return false;
        }

        if (!Character.isDigit(firstCharOfRowNumber)) {
            System.out.println();
            System.out.println("THE SECOND CHARACTER MUST BE A DIGIT!");
            return false;
        }

        // Checking if the column letter is within A-J range
        if ((col < 0) || (col > 9)) {
            System.out.println();
            System.out.println("Column must be between A and J!".toUpperCase());
            return false;
        }

        // Checking if the row number is within 1-10 range
        if ((row < 0) || (row > 9)) {
            System.out.println();
            System.out.println("Row must be between 1 and 10!".toUpperCase());
            return false;
        }
        return true;
    }

    private static char[][] placeShips(
            char[][] gameBoard, char water, char ship, Scanner scanner, PrintWriter output) {

        int placedShips = 0;
        System.out.println("Place your four Single-Masted Ships");

        while (placedShips < singleMastedShipNumber) {
            displayMyBoard(gameBoard, ship);
            System.out.printf("Enter coordinates for the %d of 4 Single-Masted Ship (e.g. A5):%n", placedShips + 1);
            String input = scanner.nextLine();

            boolean isValidInput = validateInputFields(input);
            if (!isValidInput) continue;

            char colChar = input.charAt(0);

            String rowNumber = input.substring(1);
            // char firstCharOfRowNumber = rowNumber.charAt(0);


            int col = Character.toUpperCase(colChar) - 'A';
            int row = Integer.parseInt(rowNumber) - 1;

            /*
            // Walidacja wejścia
            if (input.length() < 2 || input.length() > 3) {
                System.out.println("Invalid format. Enter e.g. A5 or B10");
                continue;
            }


            if (!Character.isLetter(colChar)) {
                System.out.println("THE FIRST CHARACTER MUST BE A LETTER!");
                continue;
            }

            if ((input.length() == 3) && !(rowNumber.equals("10"))) {
                System.out.println("THE SECOND AND THIRD CHARACTER MUST BE '10'!");
                continue;
            }

            if (!Character.isDigit(firstCharOfRowNumber)) {
                System.out.println("THE SECOND CHARACTER MUST BE A DIGIT!");
                continue;
            }




            // Checking if the the column letter is within A-J range
            if (col < 0 || col > 9) {
                System.out.println("Column must be between A and J!".toUpperCase());
                continue;
            }

             */


            char possiblePlacement = gameBoard[row][col];

            if (possiblePlacement != water) {
                System.out.println();
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
                System.out.println();
                System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
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
        System.out.println();
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
            displayMyBoard(gameBoard, ship);

            // ******************* INPUT AND VALIDATION FOR THE FIRST MAST **********************

            System.out.printf("Enter first coordinate for the %d of 3 Two-Masted Ship (e.g. A5):%n",
                    placedTwoMastedShips + 1);
            String firstInput = scanner.nextLine();

            boolean isValidInput = validateInputFields(firstInput);
            if (!isValidInput) continue;

            char colChar = firstInput.charAt(0);

            String rowNumber = firstInput.substring(1);


            int col = Character.toUpperCase(colChar) - 'A';
            int row = Integer.parseInt(rowNumber) - 1;


            char possiblePlacement = gameBoard[row][col];

            if (possiblePlacement != water) {
                System.out.println();
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
                System.out.println();
                System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                continue;
            }


            // ******************* INPUT AND VALIDATION FOR THE SECOND MAST **********************

            System.out.printf("Enter second coordinate for the %d of 3 Two-Masted Ship:%n",
                    placedTwoMastedShips + 1);
            String secondInput = scanner.nextLine();

            boolean isValidSecondInput = validateInputFields(secondInput);
            if (!isValidSecondInput) continue;

            char secondColChar = secondInput.charAt(0);

            String secondRowNumber = secondInput.substring(1);
            // char firstCharOfRowNumber = rowNumber.charAt(0);


            int secondCol = Character.toUpperCase(secondColChar) - 'A';
            int secondRow = Integer.parseInt(secondRowNumber) - 1;

            // Sprawdzenie czy drugi maszt lezy lezy dokladnie obok pierwszego
            boolean isTheSecondAdjacent =
                    (secondRow == row && Math.abs(secondCol - col) == 1) ||
                            (secondCol == col && Math.abs(secondRow - row) == 1);

            if (!isTheSecondAdjacent) {
                System.out.println();
                System.out.println("Second mast must be directly next to the first one (vertically or horizontally)!"
                        .toUpperCase());
                continue;
            }


            // Pozostala czesc walidacji
            char possiblePlacementForSecondMast = gameBoard[secondRow][secondCol];

            if (possiblePlacementForSecondMast != water) {
                System.out.println();
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
                System.out.println();
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
        System.out.println();
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
            displayMyBoard(gameBoard, ship);
            System.out.printf("Enter first coordinate for the %d of 2 Three-Masted Ship (e.g. A5):%n",
                    placedThreeMastedShips + 1);
            String firstInput = scanner.nextLine();


            boolean isValidInput = validateInputFields(firstInput);
            if (!isValidInput) continue;

            char colChar = firstInput.charAt(0);

            String rowNumber = firstInput.substring(1);

            int col = Character.toUpperCase(colChar) - 'A';
            int row = Integer.parseInt(rowNumber) - 1;

            char possiblePlacement = gameBoard[row][col];

            if (possiblePlacement != water) {
                System.out.println();
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
                System.out.println();
                System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                continue;
            }


            // ******************* INPUT AND VALIDATION FOR THE SECOND MAST **********************

            System.out.printf("Enter second coordinate for the %d of 2 Three-Masted Ship:%n",
                    placedThreeMastedShips + 1);
            String secondInput = scanner.nextLine();

            boolean isValidSecondInput = validateInputFields(secondInput);
            if (!isValidSecondInput) continue;

            char secondColChar = secondInput.charAt(0);

            String secondRowNumber = secondInput.substring(1);

            int secondCol = Character.toUpperCase(secondColChar) - 'A';
            int secondRow = Integer.parseInt(secondRowNumber) - 1;

            // Sprawdzenie czy drugi maszt lezy dokladnie obok pierwszego
            boolean isTheSecondAdjacent =
                    (secondRow == row && Math.abs(secondCol - col) == 1) ||
                            (secondCol == col && Math.abs(secondRow - row) == 1);

            if (!isTheSecondAdjacent) {
                System.out.println();
                System.out.println("Second mast must be directly next to the first one (vertically or horizontally)!"
                        .toUpperCase());
                continue;
            }

            // Pozostala czesc walidacji
            char possiblePlacementForSecondMast = gameBoard[secondRow][secondCol];

            if (possiblePlacementForSecondMast != water) {
                System.out.println();
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
                System.out.println();
                System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                continue;
            }


            // ******************* INPUT AND VALIDATION FOR THE THIRD MAST **********************

            System.out.printf("Enter third coordinate for the %d of 2 Three-Masted Ship:%n",
                    placedThreeMastedShips + 1);
            String thirdInput = scanner.nextLine();

            boolean isValidThirdInput = validateInputFields(thirdInput);
            if (!isValidThirdInput) continue;

            char thirdColChar = thirdInput.charAt(0);

            String thirdRowNumber = thirdInput.substring(1);

            int thirdCol = Character.toUpperCase(thirdColChar) - 'A';
            int thirdRow = Integer.parseInt(thirdRowNumber) - 1;

            // Sprawdzenie czy trzeci maszt lezy lezy dokladnie obok drugiego
            boolean isThirdMastAdjacent =
                    (thirdRow == row && Math.abs(thirdCol - col) == 2 && Math.abs(thirdCol - secondCol) == 1) ||
                            (thirdCol == col && Math.abs(thirdRow - row) == 2 && Math.abs(thirdRow - secondRow) == 1);

            if (!isThirdMastAdjacent) {
                System.out.println();
                System.out.println(("Third mast must be directly next to the second one " +
                        "(vertically or horizontally)!").toUpperCase());
                continue;
            }

            // Pozostala czesc walidacji
            char possiblePlacementForThirdMast = gameBoard[thirdRow][thirdCol];

            if (possiblePlacementForThirdMast != water) {
                System.out.println();
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
                System.out.println();
                System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                continue;
            }


            gameBoard[row][col] = ship;
            gameBoard[secondRow][secondCol] = ship;
            gameBoard[thirdRow][thirdCol] = ship;
            placedThreeMastedShips++;

            Coordinate firstCoordinate = new Coordinate(row, col);
            Coordinate secondCoordinate = new Coordinate(secondRow, secondCol);
            Coordinate thirdCoordinate = new Coordinate(thirdRow, thirdCol);
            ThreeMastedShip threeMastedShip = new ThreeMastedShip(firstCoordinate, secondCoordinate, thirdCoordinate);
            shipService.addShip(threeMastedShip);

        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println();
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
        System.out.println("Place your one Four-Masted Ship");

        while (placedFourMastedShips < fourMastedShipNumber) {
            displayMyBoard(gameBoard, ship);
            System.out.printf("Enter first coordinate for the %d of 1 Four-Masted Ship (e.g. A5):%n",
                    placedFourMastedShips + 1);
            String firstInput = scanner.nextLine();

            boolean isValidInput = validateInputFields(firstInput);
            if (!isValidInput) continue;

            char colChar = firstInput.charAt(0);

            String rowNumber = firstInput.substring(1);

            int col = Character.toUpperCase(colChar) - 'A';
            int row = Integer.parseInt(rowNumber) - 1;

            char possiblePlacement = gameBoard[row][col];

            if (possiblePlacement != water) {
                System.out.println();
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
                System.out.println();
                System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                continue;
            }


            // ******************* INPUT AND VALIDATION FOR THE SECOND MAST **********************

            System.out.printf("Enter second coordinate for the %d of 1 Four-Masted Ship:%n",
                    placedFourMastedShips + 1);
            String secondInput = scanner.nextLine();

            boolean isValidSecondInput = validateInputFields(secondInput);
            if (!isValidSecondInput) continue;

            char secondColChar = secondInput.charAt(0);

            String secondRowNumber = secondInput.substring(1);

            int secondCol = Character.toUpperCase(secondColChar) - 'A';
            int secondRow = Integer.parseInt(secondRowNumber) - 1;

            // Sprawdzenie czy drugi maszt lezy lezy dokladnie obok pierwszego
            boolean isSecondMastAdjacent =
                    (secondRow == row && Math.abs(secondCol - col) == 1) ||
                            (secondCol == col && Math.abs(secondRow - row) == 1);

            if (!isSecondMastAdjacent) {
                System.out.println();
                System.out.println("Second mast must be directly next to the first one (vertically or horizontally)!"
                        .toUpperCase());
                continue;
            }

            // Pozostala czesc walidacji
            char possiblePlacementForSecondMast = gameBoard[secondRow][secondCol];

            if (possiblePlacementForSecondMast != water) {
                System.out.println();
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
                System.out.println();
                System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                continue;
            }


            // ******************* INPUT AND VALIDATION FOR THE THIRD MAST **********************

            System.out.printf("Enter third coordinate for the %d of 1 Four-Masted Ship:%n",
                    placedFourMastedShips + 1);
            String thirdInput = scanner.nextLine();

            boolean isValidThirdInput = validateInputFields(thirdInput);
            if (!isValidThirdInput) continue;

            char thirdColChar = thirdInput.charAt(0);

            String thirdRowNumber = thirdInput.substring(1);

            int thirdCol = Character.toUpperCase(thirdColChar) - 'A';
            int thirdRow = Integer.parseInt(thirdRowNumber) - 1;

            // Sprawdzenie czy trzeci maszt lezy dokladnie obok pierwszego
            boolean isThirdMastAdjacent =
                    (thirdRow == row && Math.abs(thirdCol - col) == 2 && Math.abs(thirdCol - secondCol) == 1) ||
                            (thirdCol == col && Math.abs(thirdRow - row) == 2 && Math.abs(thirdRow - secondRow) == 1);

            if (!isThirdMastAdjacent) {
                System.out.println();
                System.out.println(("Third mast must be directly next to the second one " +
                        "(vertically or horizontally)!").toUpperCase());
                continue;
            }

            // Pozostala czesc walidacji
            char possiblePlacementForThirdMast = gameBoard[thirdRow][thirdCol];

            if (possiblePlacementForThirdMast != water) {
                System.out.println();
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
                System.out.println();
                System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                continue;
            }


            // ******************* INPUT AND VALIDATION FOR THE FOURTH MAST **********************

            System.out.printf("Enter fourth coordinate for the %d of 1 Four-Masted Ship:%n",
                    placedFourMastedShips + 1);
            String fourthInput = scanner.nextLine();

            boolean isValidFourthInput = validateInputFields(fourthInput);
            if (!isValidFourthInput) continue;

            char fourthColChar = fourthInput.charAt(0);

            String fourthRowNumber = fourthInput.substring(1);

            int fourthCol = Character.toUpperCase(fourthColChar) - 'A';
            int fourthRow = Integer.parseInt(fourthRowNumber) - 1;

            // Sprawdzenie czy trzeci maszt lezy dokladnie obok czwartego
            boolean isFourthMastAdjacent =
                    (fourthRow == row && Math.abs(fourthCol - col) == 3 && Math.abs(fourthCol - thirdCol) == 1) ||
                            (fourthCol == col && Math.abs(fourthRow - row) == 3 && Math.abs(fourthRow - thirdRow) == 1);

            if (!isFourthMastAdjacent) {
                System.out.println();
                System.out.println(("Fourth mast must be directly next to the third one " +
                        "(vertically or horizontally)!").toUpperCase());
                continue;
            }

            // Pozostala czesc walidacji
            char possiblePlacementForFourthMast = gameBoard[fourthRow][fourthCol];

            if (possiblePlacementForFourthMast != water) {
                System.out.println();
                System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                continue;
            }

            // Sprawdzamy sąsiednie pola, czy są wolne
            boolean canPlaceFourthMast = true;

            // Sprawdzanie dolnego pola
            if (fourthRow < gameBoard.length - 1 && gameBoard[fourthRow + 1][fourthCol] != water) {
                canPlaceFourthMast = false;
            }
            // Sprawdzanie górnego pola
            if (fourthRow > 0 && gameBoard[fourthRow - 1][fourthCol] != water) {
                canPlaceFourthMast = false;
            }
            // Sprawdzanie lewego pola
            if (fourthCol > 0 && gameBoard[fourthRow][fourthCol - 1] != water) {
                canPlaceFourthMast = false;
            }
            // Sprawdzanie prawego pola
            if (fourthCol < gameBoard[0].length - 1 && gameBoard[fourthRow][fourthCol + 1] != water) {
                canPlaceFourthMast = false;
            }
            // Lewo-góra
            if (fourthRow > 0 && fourthCol > 0 && gameBoard[fourthRow - 1][fourthCol - 1] != water) {
                canPlaceFourthMast = false;
            }
            // Lewo-dół
            if (fourthRow < gameBoard.length - 1 && fourthCol > 0 && gameBoard[fourthRow + 1][fourthCol - 1] != water) {
                canPlaceFourthMast = false;
            }
            // Prawo-góra
            if (fourthRow > 0 && fourthCol < gameBoard[0].length - 1
                    && gameBoard[fourthRow - 1][fourthCol + 1] != water) {
                canPlaceFourthMast = false;
            }
            // Prawo-dół
            if (fourthRow < gameBoard.length - 1 && fourthCol < gameBoard[0].length - 1
                    && gameBoard[fourthRow + 1][fourthCol + 1] != water) {
                canPlaceFourthMast = false;
            }
            // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
            if (!canPlaceFourthMast) {
                System.out.println();
                System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                continue;
            }


            gameBoard[row][col] = ship;
            gameBoard[secondRow][secondCol] = ship;
            gameBoard[thirdRow][thirdCol] = ship;
            gameBoard[fourthRow][fourthCol] = ship;
            placedFourMastedShips++;

            Coordinate firstCoordinate = new Coordinate(row, col);
            Coordinate secondCoordinate = new Coordinate(secondRow, secondCol);
            Coordinate thirdCoordinate = new Coordinate(thirdRow, thirdCol);
            Coordinate fourthCoordinate = new Coordinate(fourthRow, fourthCol);
            FourMastedShip fourMastedShip =
                    new FourMastedShip(firstCoordinate, secondCoordinate, thirdCoordinate, fourthCoordinate);
            shipService.addShip(fourMastedShip);

        }
        //displayMyBoard(gameBoard, ship);


        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println();
        System.out.println("ALL SHIPS HAVE BEEN PLACED!".toUpperCase());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        displayMyBoard(gameBoard, ship);
        System.out.println("Waiting for the opponent...");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        output.println("Ships placed.");

        return gameBoard;

    }

    private static char[][] createBoard() {
        char[][] gameBoard = new char[gameBoardLength][gameBoardLength];
        for (char[] row : gameBoard) {
            Arrays.fill(row, water);
        }
        return gameBoard;
    }

    /*

    private static void displayOpponentBoard(char[][] opponentBoard) {
        System.out.println();
        System.out.println("          OPPONENT BOARD");
        System.out.println();

        // Ten kod wyswietla nazwy kolumn
        int gameBoardLength = opponentBoard.length;
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
                char position = opponentBoard[row][col];
                if (position == ship) {
                    System.out.print(ship + "  ");
                } else {
                    System.out.print(position + "  ");
                }
            }
            System.out.println();
        }

        // This code displays the tenth row of the board.
        System.out.print(10 + " ");
        for (int col = 0; col < gameBoardLength; col++) {
            char position = opponentBoard[9][col];
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


     */

    private static void displayMyBoard(char[][] myBoard, char ship) {
        for (int i = 0; i < 2; i++) {
            System.out.println();
        }
        System.out.println("            MY BOARD");
        System.out.println();

        // Ten kod wyswietla nazwy kolumn
        int gameBoardLength = myBoard.length;
        char[] rowName = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'};
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
                char position = myBoard[row][col];
                if (position == ship) {
                    System.out.print(ship + "  ");
                } else {
                    System.out.print(position + "  ");
                }
            }
            System.out.println();
        }

        // This code displays the tenth row of the board.
        System.out.print(10 + " ");
        for (int col = 0; col < gameBoardLength; col++) {
            char position = myBoard[9][col];
            if (position == ship) {
                System.out.print(ship + "  ");
            } else {
                System.out.print(position + "  ");
            }
        }
        System.out.println();
        System.out.println();
        System.out.println();
    }

    private static void displayEntireGameBoard(char[][] myBoard, char[][] opponentBoard, char ship) {

        for (int i = 0; i < 2; i++) {
            System.out.println();
        }
        System.out.println("          OPPONENT BOARD                                  MY BOARD            ");
        System.out.println();

        // Ten kod wyswietla nazwy kolumn
        int myBoardLength = myBoard.length;
        int opponentBoardLength = opponentBoard.length;
        char[] rowName = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', ' ', ' ', ' ', ' ', ' ',
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'};
        System.out.print("   ");
        for (int i = 0; i < myBoardLength + 5 + opponentBoardLength; i++) {
            System.out.print(rowName[i] + "  ");
        }
        System.out.println();

        // Ten kod wyswietla wszystkie wiersze bez dziesiatego
        for (int row = 0; row < 9; row++) {
            System.out.print(" ");
            System.out.print(row + 1 + " ");
            for (int col = 0; col < gameBoardLength; col++) {
                char position2 = opponentBoard[row][col];
                if (position2 == ship) {
                    System.out.print(ship + "  ");
                } else {
                    System.out.print(position2 + "  ");
                }
            }

            System.out.print("               ");

            for (int col = 0; col < gameBoardLength; col++) {
                char position = myBoard[row][col];
                if (position == ship) {
                    System.out.print(ship + "  ");
                } else {
                    System.out.print(position + "  ");
                }
            }

            System.out.println();
        }

        // This code displays the tenth row of the Opponent Board.
        System.out.print(10 + " ");
        for (int col = 0; col < gameBoardLength; col++) {
            char position2 = opponentBoard[9][col];
            if (position2 == ship) {
                System.out.print(ship + "  ");
            } else {
                System.out.print(position2 + "  ");
            }
        }

        System.out.print("               ");

        // This code displays the tenth row of the My Board.
        for (int col = 0; col < gameBoardLength; col++) {
            char position = myBoard[9][col];
            if (position == ship) {
                System.out.print(ship + "  ");
            } else {
                System.out.print(position + "  ");
            }
        }

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



