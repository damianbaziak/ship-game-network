package client;

import client.game.messages.MessagePrinter;
import client.game.BoardCell;
import client.game.logic.LastShipCoordinatesRegister;
import client.game.logic.ShipRemovalStatus;
import client.game.messages.GameStateMessage;
import client.game.messages.ServerMessage;
import client.game.messages.ShotFeedbackMessage;
import client.model.coordinate.Coordinate;
import client.model.ship.Ship;
import client.model.ship.implementation.FourMastedShip;
import client.model.ship.implementation.SingleMastedShip;
import client.model.ship.implementation.ThreeMastedShip;
import client.model.ship.implementation.TwoMastedShip;
import client.game.service.ShipService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

public class ClientBattleship {
    private static final String SERVER_IP = "localhost";  // Server IP
    private static final int SERVER_PORT = 5050;          // Server port
    private static final int gameBoardLength = 10;
    private static final char water = BoardCell.WATER.getSymbol();
    private static final char ship = BoardCell.SHIP.getSymbol();
    private static final char HIT_AND_SUNK_CHAR = BoardCell.HIT_AND_SUNK.getSymbol();
    private static final char HIT_MAST_CHAR = BoardCell.HIT_MAST.getSymbol();
    private static final char miss = BoardCell.MISS.getSymbol();
    private static final int singleMastedShipNumber = 4;
    private static final int twoMastedShipNumber = 3;
    private static final int threeMastedShipNumber = 2;
    private static final int fourMastedShipNumber = 1;
    private static final int UNSET = -1;
    private static final ShipService shipService = new ShipService();

    public static void main(String[] args) throws InterruptedException {

        Scanner scanner = new Scanner(System.in);

        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {

            if (socket.isConnected()) {
                System.out.println();
                System.out.println(GameStateMessage.CONNECTED_TO_SERVER.getMessage());
                System.out.println();
                System.out.println(GameStateMessage.SCANNING_HORIZON.getMessage());
            }

            // Odbieramy wiadomość od serwera
            String messageFromServer = (String) input.readObject();
            if (ServerMessage.START.getMessage().equals(messageFromServer)) {
                MessagePrinter.printGreeting();
            }

            char[][] myBoard = createBoard();
            char[][] opponentBoard = createBoard();

            boolean isShipDeployingFromStart = true;
            placeSingleMastedShips(myBoard, water, ship, scanner, output, isShipDeployingFromStart);

            String serverMessageToWarBeginning = (String) input.readObject();

            if (ServerMessage.THE_WAR_BEGUN.getMessage().equals(serverMessageToWarBeginning)) {
                MessagePrinter.displayLetsStart();
                runningGame(myBoard, opponentBoard, ship, HIT_AND_SUNK_CHAR, miss, scanner, input, output);
            }

        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("ServerMessage not found");
        }
    }

    private static void runningGame(char[][] myBoard, char[][] opponentBoard, char ship, char hitAndSunk,
                                    char miss, Scanner scanner, ObjectInputStream input, ObjectOutputStream output)
            throws IOException, InterruptedException, ClassNotFoundException {

        List<Ship> myShips = shipService.getListOfMyCreatedShips();
        List<Ship> copyOfMyShipsForMessagesToOpponentAfterSunk = copyMyShips(myShips);
        List<Coordinate> myShipsHitCoordinates = new ArrayList<>();
        // Map<Integer, List<Ship>> hitOpponentShipsBySize = new HashMap<>();
        List<Ship> remainingOpponentShips = createListOfRemainingOpponentShips();

        printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);

        boolean gameRunning = true;

        while (gameRunning) {

            String whoseTurnIsIt = (String) input.readObject();

            if (ServerMessage.YOUR_TURN.getMessage().equals(whoseTurnIsIt)) {
                makeShot(myBoard, opponentBoard, scanner, input, output, ship, hitAndSunk, miss,
                        remainingOpponentShips);
            } else if (ServerMessage.PLEASE_WAIT.getMessage().equals(whoseTurnIsIt)) {
                opponentShot(myBoard, opponentBoard, myShips, remainingOpponentShips,
                        copyOfMyShipsForMessagesToOpponentAfterSunk, myShipsHitCoordinates, input, output, ship,
                        hitAndSunk);
            } else if (ServerMessage.GAME_OVER.getMessage().equals(whoseTurnIsIt)) {
                gameRunning = false;
            } else System.out.println("Something is wrong. No message has been received from the server.");
        }
    }

    private static List<Ship> createListOfRemainingOpponentShips() {
        return new ArrayList<>(
                Arrays.asList(
                        new SingleMastedShip(), new SingleMastedShip(), new SingleMastedShip(), new SingleMastedShip(),
                        new TwoMastedShip(), new TwoMastedShip(), new TwoMastedShip(),
                        new ThreeMastedShip(), new ThreeMastedShip(),
                        new FourMastedShip()));
    }

    private static List<Ship> copyMyShips(List<Ship> myShips) {
        List<Ship> copyOfList = new ArrayList<>();
        for (Ship s : myShips) {
            if (s.getSize() == 1) {
                copyOfList.add(new SingleMastedShip(s));
            } else if (s.getSize() == 2) {
                copyOfList.add(new TwoMastedShip(s));
            } else if (s.getSize() == 3) {
                copyOfList.add(new ThreeMastedShip(s));
            } else copyOfList.add(new FourMastedShip(s));

        }
        return copyOfList;
    }

    private static void opponentShot(
            char[][] myBoard, char[][] opponentBoard, List<Ship> myShips, List<Ship> remainingOpponentShips,
            List<Ship> copyOfMyShipsListForMessagesAfterSunk, List<Coordinate> alreadyHitCoordinates,
            ObjectInputStream input, ObjectOutputStream output, char ship,
            char hit) throws IOException, InterruptedException, ClassNotFoundException {

        boolean opponentHitYouWait = true;

        while (opponentHitYouWait) {

            System.out.println();
            System.out.println(GameStateMessage.OPPONENT_IS_FIRING.getMessage());
            String opponentShot = (String) input.readObject();

            String rowNumber = opponentShot.substring(1);
            int row = Integer.parseInt(rowNumber) - 1;
            int col = Integer.parseInt(String.valueOf(Character.toUpperCase(opponentShot.charAt(0)) - 'A'));

            Coordinate opponentShotCoordinate = new Coordinate(row, col);

            Optional<Ship> possibleHitShip = myShips.stream()
                    .filter(s -> s.getCoordinates().contains(opponentShotCoordinate))
                    .findFirst();

            if (alreadyHitCoordinates.contains(opponentShotCoordinate)) {

                output.writeObject(ShotFeedbackMessage.ALREADY_FIRED.getMessage());
                output.writeObject(null);
                output.writeObject(null);
                output.writeObject(null);
                output.writeObject(null);

                printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);
                System.out.println();
                Thread.sleep(500);
                System.out.println(ShotFeedbackMessage.FIRED_AT.getFormattedOpponentFeedback(opponentShot));
                Thread.sleep(1000);
                System.out.println();
                System.out.println(ShotFeedbackMessage.ALREADY_FIRED.getOpponentFeedback());
                Thread.sleep(1000);

                opponentHitYouWait = false;

            } else if ((!alreadyHitCoordinates.contains(opponentShotCoordinate)) && (possibleHitShip.isPresent())) {

                alreadyHitCoordinates.add(opponentShotCoordinate);

                Ship myShip = possibleHitShip.get();

                myShip.getCoordinates().remove(opponentShotCoordinate);

                myBoard[row][col] = hit;

                String firstHitMessageToDisplay;
                String secondHitMessageToDisplay = "";
                String thirdHitMessageToDisplay = "";
                String fourthHitMessageToDisplay;

                printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);
                System.out.println();
                Thread.sleep(500);
                System.out.println(ShotFeedbackMessage.FIRED_AT.getFormattedOpponentFeedback(opponentShot));
                Thread.sleep(1000);
                System.out.println();

                if (myShip.getSize() == 1) {

                    output.writeObject(ShotFeedbackMessage.HIT_SINGLE_MAST.getMessage());
                    firstHitMessageToDisplay = ShotFeedbackMessage.HIT_SINGLE_MAST.getOpponentFeedback();

                    if (myShip.getCoordinates().isEmpty()) {

                        output.writeObject(ShotFeedbackMessage.SUNK_SINGLE_MAST_SHIP.getMessage());

                        Ship sunkShip = getSunkShip(opponentShotCoordinate, copyOfMyShipsListForMessagesAfterSunk);
                        output.writeObject(sunkShip);

                        secondHitMessageToDisplay = ShotFeedbackMessage.SUNK_SINGLE_MAST_SHIP.getOpponentFeedback();
                        shipService.removeShip(myShip);

                    } else {
                        output.writeObject(null);
                        output.writeObject(null);
                    }

                    boolean allOneMastedShipsSunk = myShips.stream()
                            .filter(s -> s.getSize() == 1)
                            .allMatch(s -> s.getCoordinates().isEmpty());

                    if (allOneMastedShipsSunk) {

                        output.writeObject(ShotFeedbackMessage.ALL_SINGLE_MAST_SHIPS_SUNK.getMessage());
                        thirdHitMessageToDisplay =
                                ShotFeedbackMessage.ALL_SINGLE_MAST_SHIPS_SUNK.getOpponentFeedback();

                    } else output.writeObject(null);

                    fourthHitMessageToDisplay = areAllShipsSunk(myShips, output);

                    displayMessages(firstHitMessageToDisplay, secondHitMessageToDisplay, thirdHitMessageToDisplay,
                            fourthHitMessageToDisplay);

                    if (fourthHitMessageToDisplay != null && !fourthHitMessageToDisplay.isEmpty()) {

                        MessagePrinter.printYouLose();
                        opponentHitYouWait = false;

                    }

                } else if (myShip.getSize() == 2) {

                    output.writeObject(ShotFeedbackMessage.HIT_TWO_MAST.getMessage());
                    firstHitMessageToDisplay = ShotFeedbackMessage.HIT_TWO_MAST.getOpponentFeedback();

                    if (myShip.getCoordinates().isEmpty()) {

                        output.writeObject(ShotFeedbackMessage.SUNK_TWO_MAST_SHIP.getMessage());

                        Ship sunkTwoMastedShip = getSunkShip(
                                opponentShotCoordinate, copyOfMyShipsListForMessagesAfterSunk);
                        output.writeObject(sunkTwoMastedShip);

                        secondHitMessageToDisplay = ShotFeedbackMessage.SUNK_TWO_MAST_SHIP.getOpponentFeedback();
                        shipService.removeShip(myShip);

                    } else {
                        output.writeObject(null);
                        output.writeObject(null);
                    }

                    boolean allTwoMastedShipsSunk = myShips.stream()
                            .filter(s -> s.getSize() == 2)
                            .allMatch(s -> s.getCoordinates().isEmpty());

                    if (allTwoMastedShipsSunk) {
                        output.writeObject(ShotFeedbackMessage.ALL_TWO_MAST_SHIPS_SUNK.getMessage());
                        thirdHitMessageToDisplay = ShotFeedbackMessage.ALL_TWO_MAST_SHIPS_SUNK.getOpponentFeedback();

                    } else output.writeObject(null);

                    fourthHitMessageToDisplay = areAllShipsSunk(myShips, output);

                    displayMessages(firstHitMessageToDisplay, secondHitMessageToDisplay, thirdHitMessageToDisplay,
                            fourthHitMessageToDisplay);

                    if (fourthHitMessageToDisplay != null && !fourthHitMessageToDisplay.isEmpty()) {
                        MessagePrinter.printYouLose();
                        opponentHitYouWait = false;

                    }

                } else if (myShip.getSize() == 3) {

                    output.writeObject(ShotFeedbackMessage.HIT_THREE_MAST.getMessage());
                    firstHitMessageToDisplay = ShotFeedbackMessage.HIT_THREE_MAST.getOpponentFeedback();

                    if (myShip.getCoordinates().isEmpty()) {

                        output.writeObject(ShotFeedbackMessage.SUNK_THREE_MAST_SHIP.getMessage());

                        Ship sunkThreeMastedship =
                                getSunkShip(opponentShotCoordinate, copyOfMyShipsListForMessagesAfterSunk);
                        output.writeObject(sunkThreeMastedship);

                        secondHitMessageToDisplay = ShotFeedbackMessage.SUNK_THREE_MAST_SHIP.getOpponentFeedback();
                        shipService.removeShip(myShip);

                    } else {
                        output.writeObject(null);
                        output.writeObject(null);
                    }

                    boolean allThreeMastedShipsSunk = myShips.stream()
                            .filter(s -> s.getSize() == 3)
                            .allMatch(s -> s.getCoordinates().isEmpty());

                    if (allThreeMastedShipsSunk) {
                        output.writeObject(ShotFeedbackMessage.ALL_THREE_MAST_SHIPS_SUNK.getMessage());
                        thirdHitMessageToDisplay =
                                ShotFeedbackMessage.ALL_THREE_MAST_SHIPS_SUNK.getOpponentFeedback();

                    } else output.writeObject(null);

                    fourthHitMessageToDisplay = areAllShipsSunk(myShips, output);

                    displayMessages(firstHitMessageToDisplay, secondHitMessageToDisplay, thirdHitMessageToDisplay,
                            fourthHitMessageToDisplay);

                    if (fourthHitMessageToDisplay != null && !fourthHitMessageToDisplay.isEmpty()) {
                        MessagePrinter.printYouLose();
                        opponentHitYouWait = false;

                    }

                } else if (possibleHitShip.get().getSize() == 4) {

                    output.writeObject(ShotFeedbackMessage.HIT_FOUR_MAST.getMessage());
                    firstHitMessageToDisplay = ShotFeedbackMessage.HIT_FOUR_MAST.getOpponentFeedback();

                    if (myShip.getCoordinates().isEmpty()) {

                        output.writeObject(ShotFeedbackMessage.SUNK_FOUR_MAST_SHIP.getMessage());

                        Ship sunkFourMastedShip =
                                getSunkShip(opponentShotCoordinate, copyOfMyShipsListForMessagesAfterSunk);
                        output.writeObject(sunkFourMastedShip);

                        secondHitMessageToDisplay = ShotFeedbackMessage.SUNK_FOUR_MAST_SHIP.getOpponentFeedback();
                        shipService.removeShip(myShip);

                    } else {
                        output.writeObject(null);
                        output.writeObject(null);
                    }

                    boolean allFourMastedShipsSunk = myShips.stream()
                            .filter(s -> s.getSize() == 4)
                            .allMatch(s -> s.getCoordinates().isEmpty());

                    if (allFourMastedShipsSunk) {
                        output.writeObject(ShotFeedbackMessage.ALL_FOUR_MAST_SHIPS_SUNK.getMessage());
                        thirdHitMessageToDisplay =
                                ShotFeedbackMessage.ALL_FOUR_MAST_SHIPS_SUNK.getOpponentFeedback();

                    } else output.writeObject(null);

                    fourthHitMessageToDisplay = areAllShipsSunk(myShips, output);

                    displayMessages(firstHitMessageToDisplay, secondHitMessageToDisplay, thirdHitMessageToDisplay,
                            fourthHitMessageToDisplay);

                    if (fourthHitMessageToDisplay != null && !fourthHitMessageToDisplay.isEmpty()) {
                        MessagePrinter.printYouLose();
                        opponentHitYouWait = false;

                    }
                }

            } else {

                alreadyHitCoordinates.add(opponentShotCoordinate);

                output.writeObject(ShotFeedbackMessage.MISS.getMessage());
                output.writeObject(null);
                output.writeObject(null);
                output.writeObject(null);
                output.writeObject(null);

                printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);
                System.out.println();
                Thread.sleep(500);
                System.out.println(ShotFeedbackMessage.FIRED_AT.getFormattedOpponentFeedback(opponentShot));
                Thread.sleep(1000);
                System.out.println();
                System.out.println(ShotFeedbackMessage.MISS.getOpponentFeedback());
                Thread.sleep(1000);

                opponentHitYouWait = false;
            }
        }
    }

    private static void makeShot(
            char[][] myBoard, char[][] opponentBoard, Scanner scanner, ObjectInputStream input,
            ObjectOutputStream output, char ship, char hitAndSunk, char miss, List<Ship> remainingOpponentShips)
            throws IOException,
            InterruptedException, ClassNotFoundException {

        boolean youHitYouTurn = true;

        while (youHitYouTurn) {

            System.out.println();
            System.out.println(GameStateMessage.YOUR_TURN.getMessage());

            String myShot = scanner.nextLine();

            boolean isValidInput = validateInputFields(myShot, myBoard, opponentBoard, ship,
                    remainingOpponentShips);
            if (!isValidInput) continue;

            String rowNumber = myShot.substring(1);

            int col = Character.toUpperCase(myShot.charAt(0)) - 'A';
            int row = Integer.parseInt(rowNumber) - 1;

            output.writeObject(myShot);

            String opponentReport = (String) input.readObject();
            String secondOpponentReport = (String) input.readObject();
            Ship opponentSunkShip = (Ship) input.readObject();
            String fourthOpponentReport = (String) input.readObject();
            String fifthOpponentReport = (String) input.readObject();

            if (ShotFeedbackMessage.ALREADY_FIRED.getMessage().equals(opponentReport)) {

                Thread.sleep(500);
                printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);
                Thread.sleep(500);
                MessagePrinter.printAlreadyHit();
                Thread.sleep(1000);

                youHitYouTurn = false;

            } else if (opponentReport.contains("YOU HIT")) {

                Coordinate opponentShotCoordinate = new Coordinate(row, col);

                if (ShotFeedbackMessage.HIT_SINGLE_MAST.getMessage().equals(opponentReport)) {

                    remainingOpponentShips
                            .stream()
                            .filter(s -> s.getSize() == 1)
                            .findFirst()
                            .ifPresent(remainingOpponentShips::remove);

                    // Mark the ship as sunk immediately because one-masted ships are destroyed with a single hit.
                    opponentBoard[opponentShotCoordinate.getRow()][opponentShotCoordinate.getCol()] = hitAndSunk;

                    printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);
                    Thread.sleep(500);
                    MessagePrinter.printHit();
                    Thread.sleep(1000);

                    System.out.println(ShotFeedbackMessage.HIT_SINGLE_MAST.getMessage());
                    Thread.sleep(1000);

                    if (ShotFeedbackMessage.SUNK_SINGLE_MAST_SHIP.getMessage().equals(secondOpponentReport)
                            && opponentSunkShip != null) {

                        System.out.println();
                        System.out.println(ShotFeedbackMessage.SUNK_SINGLE_MAST_SHIP.getMessage());
                        Thread.sleep(1000);
                    }

                    if (ShotFeedbackMessage.ALL_SINGLE_MAST_SHIPS_SUNK.getMessage().equals(fourthOpponentReport)) {

                        System.out.println();
                        System.out.println(ShotFeedbackMessage.ALL_SINGLE_MAST_SHIPS_SUNK.getMessage());
                        Thread.sleep(1000);
                    }

                    if (didPLayerWin(fifthOpponentReport)) {
                        youHitYouTurn = false;
                    }

                } else if (ShotFeedbackMessage.HIT_TWO_MAST.getMessage().equals(opponentReport)) {

                    if (ShotFeedbackMessage.SUNK_TWO_MAST_SHIP.getMessage().equals(secondOpponentReport) &&
                            opponentSunkShip != null) {

                        remainingOpponentShips
                                .stream()
                                .filter(s -> s.getSize() == 2)
                                .findFirst()
                                .ifPresent(remainingOpponentShips::remove);

                        opponentSunkShip.getCoordinates().forEach(coordinate -> {
                            opponentBoard[coordinate.getRow()][coordinate.getCol()] = hitAndSunk;
                        });


                        printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);
                        Thread.sleep(500);
                        MessagePrinter.printHit();
                        Thread.sleep(1000);
                        System.out.println(ShotFeedbackMessage.HIT_TWO_MAST.getMessage());
                        Thread.sleep(1000);
                        System.out.println();
                        System.out.println(ShotFeedbackMessage.SUNK_TWO_MAST_SHIP.getMessage());
                        Thread.sleep(1000);

                    } else {

                        opponentBoard[opponentShotCoordinate.getRow()][opponentShotCoordinate.getCol()] = HIT_MAST_CHAR;

                        printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);
                        Thread.sleep(500);
                        MessagePrinter.printHit();
                        Thread.sleep(1000);
                        System.out.println(ShotFeedbackMessage.HIT_TWO_MAST.getMessage());
                        Thread.sleep(1000);
                    }

                    if (ShotFeedbackMessage.ALL_TWO_MAST_SHIPS_SUNK.getMessage().equals(fourthOpponentReport)) {

                        System.out.println();
                        System.out.println(ShotFeedbackMessage.ALL_TWO_MAST_SHIPS_SUNK.getMessage());
                        Thread.sleep(1000);
                    }

                    if (didPLayerWin(fifthOpponentReport)) {
                        youHitYouTurn = false;
                    }


                } else if (ShotFeedbackMessage.HIT_THREE_MAST.getMessage().equals(opponentReport)) {

                    if (ShotFeedbackMessage.SUNK_THREE_MAST_SHIP.getMessage().equals(secondOpponentReport)
                            && opponentSunkShip != null) {

                        remainingOpponentShips
                                .stream()
                                .filter(s -> s.getSize() == 3)
                                .findFirst()
                                .ifPresent(remainingOpponentShips::remove);

                        opponentSunkShip.getCoordinates().forEach(coordinate -> {
                            opponentBoard[coordinate.getRow()][coordinate.getCol()] = hitAndSunk;
                        });

                        printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);
                        Thread.sleep(500);
                        MessagePrinter.printHit();
                        Thread.sleep(1000);
                        System.out.println(ShotFeedbackMessage.HIT_THREE_MAST.getMessage());
                        Thread.sleep(1000);
                        System.out.println();
                        System.out.println(ShotFeedbackMessage.SUNK_THREE_MAST_SHIP.getMessage());
                        Thread.sleep(1000);

                    } else {

                        opponentBoard[opponentShotCoordinate.getRow()][opponentShotCoordinate.getCol()] = HIT_MAST_CHAR;

                        printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);
                        Thread.sleep(500);
                        MessagePrinter.printHit();
                        Thread.sleep(1000);
                        System.out.println(ShotFeedbackMessage.HIT_THREE_MAST.getMessage());
                        Thread.sleep(1000);

                    }

                    if (ShotFeedbackMessage.ALL_THREE_MAST_SHIPS_SUNK.getMessage().equals(fourthOpponentReport)) {

                        System.out.println();
                        System.out.println(ShotFeedbackMessage.ALL_THREE_MAST_SHIPS_SUNK.getMessage());
                        Thread.sleep(1000);
                    }

                    if (didPLayerWin(fifthOpponentReport)) {
                        youHitYouTurn = false;
                    }


                } else if (ShotFeedbackMessage.HIT_FOUR_MAST.getMessage().equals(opponentReport)) {

                    if (ShotFeedbackMessage.SUNK_FOUR_MAST_SHIP.getMessage().equals(secondOpponentReport)
                            && opponentSunkShip != null) {

                        remainingOpponentShips
                                .stream()
                                .filter(s -> s.getSize() == 4)
                                .findFirst()
                                .ifPresent(remainingOpponentShips::remove);

                        opponentSunkShip.getCoordinates().forEach(coordinate -> {
                            opponentBoard[coordinate.getRow()][coordinate.getCol()] = hitAndSunk;
                        });

                        printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);
                        Thread.sleep(500);
                        MessagePrinter.printHit();
                        Thread.sleep(1000);
                        System.out.println(ShotFeedbackMessage.HIT_FOUR_MAST.getMessage());
                        Thread.sleep(1000);
                        System.out.println();
                        System.out.println(ShotFeedbackMessage.SUNK_FOUR_MAST_SHIP.getMessage());
                        Thread.sleep(1000);

                    } else {

                        opponentBoard[opponentShotCoordinate.getRow()][opponentShotCoordinate.getCol()] = HIT_MAST_CHAR;

                        printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);
                        Thread.sleep(500);
                        MessagePrinter.printHit();
                        Thread.sleep(1000);
                        System.out.println(ShotFeedbackMessage.HIT_FOUR_MAST.getMessage());
                        Thread.sleep(1000);
                    }

                    if (ShotFeedbackMessage.ALL_FOUR_MAST_SHIPS_SUNK.getMessage().equals(fourthOpponentReport)) {

                        System.out.println();
                        System.out.println(ShotFeedbackMessage.ALL_FOUR_MAST_SHIPS_SUNK.getMessage());
                        Thread.sleep(1000);
                    }

                    if (didPLayerWin(fifthOpponentReport)) {
                        youHitYouTurn = false;
                    }


                }


            } else {
                opponentBoard[row][col] = miss;

                Thread.sleep(500);
                printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);
                Thread.sleep(500);
                MessagePrinter.displayMiss();
                Thread.sleep(1000);

                youHitYouTurn = false;


            }
        }


    }

    private static void placeSingleMastedShips(
            char[][] myBoard, char water, char ship, Scanner scanner, ObjectOutputStream output,
            boolean isShipDeploymentFromStart) throws InterruptedException, IOException {

        System.out.printf(GameStateMessage.PLACE_YOUR_SHIPS.getMessage(), singleMastedShipNumber, "Single");
        System.out.println();

        ShipRemovalStatus removalStatus = ShipRemovalStatus.REMOVAL_STATUS;

        int placedSingleMastedShips;

        if (isShipDeploymentFromStart) {
            placedSingleMastedShips = 0;
            printMyBoard(myBoard, ship);
        } else {
            placedSingleMastedShips = 3;
        }

        int row;
        int col;

        while (placedSingleMastedShips < singleMastedShipNumber) {

            System.out.printf(GameStateMessage.ENTER_COORDINATES_SINGLE_MAST_SHIPS.getMessage(), placedSingleMastedShips + 1);
            System.out.printf(GameStateMessage.ENTER_OPTIONS.getMessage());

            String input = scanner.nextLine();

            removalStatus.setWasRemoved(false);
            removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
            removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.NONE);

            if ("Options".equalsIgnoreCase(input)) {
                selectAndRemoveMastOrShip(myBoard, scanner, ship, removalStatus);
                if (removalStatus.isWasRemoved()) {
                    switch (removalStatus.getWhatRemoved()) {
                        case MAST, SHIP -> placedSingleMastedShips--;
                    }
                }
                continue;
            }

            boolean isValidInput = validateInputFields(input, myBoard, ship);
            if (!isValidInput) continue;

            char colChar = input.charAt(0);

            String rowNumber = input.substring(1);

            col = Character.toUpperCase(colChar) - 'A';
            row = Integer.parseInt(rowNumber) - 1;

            char possiblePlacement = myBoard[row][col];


            if (possiblePlacement != water) {
                printMyBoard(myBoard, ship);
                System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                System.out.println();
                continue;
            }

            // Sprawdzamy sąsiednie pola, czy są wolne
            boolean canPlaceShip = true;

            // Sprawdzanie dolnego pola
            if (row < myBoard.length - 1 && myBoard[row + 1][col] != water) {
                canPlaceShip = false;
            }
            // Sprawdzanie górnego pola
            if (row > 0 && myBoard[row - 1][col] != water) {
                canPlaceShip = false;
            }
            // Sprawdzanie lewego pola
            if (col > 0 && myBoard[row][col - 1] != water) {
                canPlaceShip = false;
            }
            // Sprawdzanie prawego pola
            if (col < myBoard[0].length - 1 && myBoard[row][col + 1] != water) {
                canPlaceShip = false;
            }
            // Lewo-góra
            if (row > 0 && col > 0 && myBoard[row - 1][col - 1] != water) {
                canPlaceShip = false;
            }
            // Lewo-dół
            if (row < myBoard.length - 1 && col > 0 && myBoard[row + 1][col - 1] != water) {
                canPlaceShip = false;
            }
            // Prawo-góra
            if (row > 0 && col < myBoard[0].length - 1 && myBoard[row - 1][col + 1] != water) {
                canPlaceShip = false;
            }
            // Prawo-dół
            if (row < myBoard.length - 1 && col < myBoard[0].length - 1 && myBoard[row + 1][col + 1] != water) {
                canPlaceShip = false;
            }
            // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
            if (!canPlaceShip) {
                System.out.println();
                printMyBoard(myBoard, ship);
                System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                System.out.println();
                continue;
            }

            Coordinate coordinate = new Coordinate(row, col);
            SingleMastedShip singleMastedShip = new SingleMastedShip(coordinate);
            shipService.addShip(singleMastedShip);

            myBoard[row][col] = ship;
            placedSingleMastedShips++;

            printMyBoard(myBoard, ship);

        }

        boolean isDeploymentFromStart = true;
        placeTwoMastedShips(myBoard, water, ship, scanner, output, isDeploymentFromStart);

    }


    private static void placeTwoMastedShips(
            char[][] myBoard, char water, char ship, Scanner scanner, ObjectOutputStream output,
            boolean isShipDeploymentFromStart) throws InterruptedException, IOException {

        if (isShipDeploymentFromStart) {
            Thread.sleep(500);
            System.out.println(GameStateMessage.SINGLE_MAST_SHIPS_PLACED.getMessage());
            Thread.sleep(1000);
            System.out.println();
        }
        System.out.println();
        System.out.printf(GameStateMessage.PLACE_YOUR_SHIPS.getMessage(), twoMastedShipNumber, "Two");
        Thread.sleep(1000);
        System.out.println();

        ShipRemovalStatus removalStatus = ShipRemovalStatus.REMOVAL_STATUS;

        // Get the coordinates of the last removed ship.
        LastShipCoordinatesRegister lastShipCoordinatesRegister = LastShipCoordinatesRegister.COORDINATES;
        List<Coordinate> lastRemovedShipCoordinates = lastShipCoordinatesRegister.getLastShipCoordinates();

        int placedTwoMastedShips;
        // Set the variable to '0', if ship placement start from the beginning. Set variable to '2' if it resumes after
        // removing the last three-masted ship.
        if (isShipDeploymentFromStart) {
            placedTwoMastedShips = 0;
        } else placedTwoMastedShips = 2;

        String mastToPlace = "";

        int firstMastRow = 0;
        int firstMastCol = 0;

        if (removalStatus.getWhereRemoved() == ShipRemovalStatus.WhereIsRemoved.ANOTHER_METHOD
                && removalStatus.getWhatRemoved() == ShipRemovalStatus.WhatIsRemoved.MAST) {
            firstMastRow = lastRemovedShipCoordinates.get(0).getRow();
            firstMastCol = lastRemovedShipCoordinates.get(0).getCol();
        }

        int secondMastRow;
        int secondMastCol;

        while (placedTwoMastedShips < twoMastedShipNumber) {

            if (myBoard[firstMastRow][firstMastCol] != '1') {

                // ******************* INPUT AND VALIDATION FOR THE FIRST MAST **********************

                mastToPlace = "FIRST";

                System.out.printf(GameStateMessage.ENTER_COORDINATES_FOR_MAST.getMessage(), mastToPlace,
                        placedTwoMastedShips + 1, twoMastedShipNumber, "Two");
                System.out.printf(GameStateMessage.ENTER_OPTIONS.getMessage());

                String firstInput = scanner.nextLine();

                removalStatus.setWasRemoved(false);
                removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.NONE);

                if ("Options".equalsIgnoreCase(firstInput)) {
                    selectAndRemoveMastOrShip(myBoard, scanner, ship, removalStatus);
                    if (placedTwoMastedShips == 0 && removalStatus.isWasRemoved()) {

                        switch (removalStatus.getWhatRemoved()) {
                            case MAST, SHIP:
                                boolean isDeploymentFromStart = false;
                                removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.ANOTHER_METHOD);
                                placeSingleMastedShips(myBoard, water, ship, scanner, output, isDeploymentFromStart);
                                return;
                        }

                    } else if (placedTwoMastedShips > 0 && removalStatus.isWasRemoved()) {

                        switch (removalStatus.getWhatRemoved()) {
                            case SHIP:
                                placedTwoMastedShips--;
                                removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.CURRENT_METHOD);
                                continue;

                            case MAST:
                                placedTwoMastedShips--;
                                removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.CURRENT_METHOD);

                                if (lastShipCoordinatesRegister.getLastShipCoordinates().isEmpty()) {
                                    throw new IllegalStateException(
                                            "LastShipCoordinates should never be empty at this point");
                                }
                                firstMastRow = lastShipCoordinatesRegister.getLastShipCoordinates().get(0).getRow();
                                firstMastCol = lastShipCoordinatesRegister.getLastShipCoordinates().get(0).getCol();
                                break;
                        }
                    } else continue;
                }


                if (myBoard[firstMastRow][firstMastCol] != '1') {

                    boolean isValidInput = validateInputFields(firstInput, myBoard, ship);
                    if (!isValidInput) continue;

                    char colChar = firstInput.charAt(0);

                    String rowNumber = firstInput.substring(1);


                    firstMastCol = Character.toUpperCase(colChar) - 'A';
                    firstMastRow = Integer.parseInt(rowNumber) - 1;


                    char possiblePlacement = myBoard[firstMastRow][firstMastCol];

                    if (possiblePlacement != water) {
                        printMyBoard(myBoard, ship);
                        System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                        System.out.println();
                        continue;
                    }

                    // SPRAWDZAMY CZY SASIEDNIE POLA SA WOLNE
                    boolean canPlaceFirstMast = true;

                    // Sprawdzanie dolnego pola
                    if (firstMastRow < myBoard.length - 1 && myBoard[firstMastRow + 1][firstMastCol] != water) {
                        canPlaceFirstMast = false;
                    }
                    // Sprawdzanie górnego pola
                    if (firstMastRow > 0 && myBoard[firstMastRow - 1][firstMastCol] != water) {
                        canPlaceFirstMast = false;
                    }
                    // Sprawdzanie lewego pola
                    if (firstMastCol > 0 && myBoard[firstMastRow][firstMastCol - 1] != water) {
                        canPlaceFirstMast = false;
                    }
                    // Sprawdzanie prawego pola
                    if (firstMastCol < myBoard[0].length - 1 && myBoard[firstMastRow][firstMastCol + 1] != water) {
                        canPlaceFirstMast = false;
                    }
                    // Lewo-góra
                    if (firstMastRow > 0 && firstMastCol > 0 && myBoard[firstMastRow - 1][firstMastCol - 1] != water) {
                        canPlaceFirstMast = false;
                    }
                    // Lewo-dół
                    if (firstMastRow < myBoard.length - 1 && firstMastCol > 0 && myBoard[firstMastRow + 1][firstMastCol - 1] != water) {
                        canPlaceFirstMast = false;
                    }
                    // Prawo-góra
                    if (firstMastRow > 0 && firstMastCol < myBoard[0].length - 1 && myBoard[firstMastRow - 1][firstMastCol + 1] != water) {
                        canPlaceFirstMast = false;
                    }
                    // Prawo-dół
                    if (firstMastRow < myBoard.length - 1 && firstMastCol < myBoard[0].length - 1 && myBoard[firstMastRow + 1][firstMastCol + 1] != water) {
                        canPlaceFirstMast = false;
                    }

                    // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
                    if (!canPlaceFirstMast) {
                        printMyBoard(myBoard, ship);
                        System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                        System.out.println();
                        continue;
                    }

                    myBoard[firstMastRow][firstMastCol] = '1';
                    printMyBoard(myBoard, ship);
                }
            }


            // ******************* INPUT AND VALIDATION FOR THE SECOND MAST **********************

            boolean secondMastIsNotPlaced = true;

            while (secondMastIsNotPlaced) {

                mastToPlace = "SECOND";

                System.out.printf(GameStateMessage.ENTER_COORDINATES_FOR_MAST.getMessage(), mastToPlace,
                        placedTwoMastedShips + 1, twoMastedShipNumber, "Two");
                System.out.printf(GameStateMessage.ENTER_OPTIONS.getMessage());

                String secondInput = scanner.nextLine();

                removalStatus.setWasRemoved(false);
                removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.NONE);

                if ("Options".equalsIgnoreCase(secondInput)) {

                    selectAndRemoveMastOrShip(myBoard, scanner, ship, removalStatus, firstMastRow, firstMastCol);

                    if (placedTwoMastedShips == 0 && removalStatus.isWasRemoved()) {
                        switch (removalStatus.getWhatRemoved()) {
                            case MAST:
                                break;
                            case SHIP:
                                boolean isDeploymentFromStart = false;
                                placeSingleMastedShips(myBoard, water, ship, scanner, output, isDeploymentFromStart);
                        }

                    } else if (placedTwoMastedShips > 0 && removalStatus.isWasRemoved()) {
                        switch (removalStatus.getWhatRemoved()) {
                            case MAST:
                                break;
                            case SHIP:
                                placedTwoMastedShips--;
                                break;
                        }
                    } else continue;
                }

                if (removalStatus.isWasRemoved()) break;


                boolean isValidSecondInput = validateInputFields(secondInput, myBoard, ship);
                if (!isValidSecondInput) continue;

                char secondColChar = secondInput.charAt(0);

                String secondRowNumber = secondInput.substring(1);


                secondMastCol = Character.toUpperCase(secondColChar) - 'A';
                secondMastRow = Integer.parseInt(secondRowNumber) - 1;

                // Sprawdzenie czy drugi maszt lezy lezy dokladnie obok pierwszego
                boolean isTheSecondAdjacent =
                        (secondMastRow == firstMastRow && Math.abs(secondMastCol - firstMastCol) == 1) ||
                                (secondMastCol == firstMastCol && Math.abs(secondMastRow - firstMastRow) == 1);

                if (!isTheSecondAdjacent) {
                    printMyBoard(myBoard, ship);
                    System.out.println(("Second mast must be placed directly next to the first one " +
                            "(vertically or horizontally)!").toUpperCase());
                    System.out.println();
                    continue;
                }


                // Pozostala czesc walidacji
                char possiblePlacementForSecondMast = myBoard[secondMastRow][secondMastCol];

                if (possiblePlacementForSecondMast != water) {
                    printMyBoard(myBoard, ship);
                    System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                    System.out.println();
                    continue;
                }

                // Sprawdzamy sąsiednie pola, czy są wolne
                boolean canPlaceSecondMast = true;

                // Sprawdzanie dolnego pola
                if (secondMastRow < myBoard.length - 1 && myBoard[secondMastRow + 1][secondMastCol] != water) {
                    canPlaceSecondMast = myBoard[secondMastRow + 1][secondMastCol] == '1';
                }
                // Sprawdzanie górnego pola
                if (secondMastRow > 0 && myBoard[secondMastRow - 1][secondMastCol] != water) {
                    canPlaceSecondMast = myBoard[secondMastRow - 1][secondMastCol] == '1';
                }
                // Sprawdzanie lewego pola
                if (secondMastCol > 0 && myBoard[secondMastRow][secondMastCol - 1] != water) {
                    canPlaceSecondMast = myBoard[secondMastRow][secondMastCol - 1] == '1';
                }
                // Sprawdzanie prawego pola
                if (secondMastCol < myBoard[0].length - 1 && myBoard[secondMastRow][secondMastCol + 1] != water) {
                    canPlaceSecondMast = myBoard[secondMastRow][secondMastCol + 1] == '1';
                }
                // Lewo-góra
                if (secondMastRow > 0 && secondMastCol > 0 && myBoard[secondMastRow - 1][secondMastCol - 1] != water) {
                    canPlaceSecondMast = false;
                }
                // Lewo-dół
                if (secondMastRow < myBoard.length - 1 && secondMastCol > 0 && myBoard[secondMastRow + 1][secondMastCol - 1] != water) {
                    canPlaceSecondMast = false;
                }
                // Prawo-góra
                if (secondMastRow > 0 && secondMastCol < myBoard[0].length - 1
                        && myBoard[secondMastRow - 1][secondMastCol + 1] != water) {
                    canPlaceSecondMast = false;
                }
                // Prawo-dół
                if (secondMastRow < myBoard.length - 1 && secondMastCol < myBoard[0].length - 1
                        && myBoard[secondMastRow + 1][secondMastCol + 1] != water) {
                    canPlaceSecondMast = false;
                }
                // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
                if (!canPlaceSecondMast) {
                    printMyBoard(myBoard, ship);
                    System.out.println("Cannot place mast here. There is another ship nearby!".toUpperCase());
                    System.out.println();
                    continue;
                }

                Coordinate firstCoordinate = new Coordinate(firstMastRow, firstMastCol);
                Coordinate secondCoordinate = new Coordinate(secondMastRow, secondMastCol);
                TwoMastedShip twoMastedShip = new TwoMastedShip(firstCoordinate, secondCoordinate);
                shipService.addShip(twoMastedShip);

                myBoard[firstMastRow][firstMastCol] = ship;
                myBoard[secondMastRow][secondMastCol] = ship;
                placedTwoMastedShips++;

                secondMastIsNotPlaced = false;

            }

            if (removalStatus.isWasRemoved()) continue;

            printMyBoard(myBoard, ship);
        }

        boolean isDeploymentFromStart = true;
        placeThreeMastedShips(myBoard, water, ship, scanner, output, isDeploymentFromStart);
    }

    private static void placeThreeMastedShips(
            char[][] myBoard, char water, char ship, Scanner scanner, ObjectOutputStream output,
            boolean shipDeploymentFromStart) throws InterruptedException, IOException {

        if (shipDeploymentFromStart) {
            Thread.sleep(500);
            System.out.println(GameStateMessage.TWO_MAST_SHIPS_PLACED.getMessage());
            Thread.sleep(1000);
            System.out.println();
        }
        System.out.println();
        System.out.printf(GameStateMessage.PLACE_YOUR_SHIPS.getMessage(), threeMastedShipNumber, "Three");
        Thread.sleep(1000);
        System.out.println();

        ShipRemovalStatus removalStatus = ShipRemovalStatus.REMOVAL_STATUS;

        // Get the coordinates of the last removed ship.
        LastShipCoordinatesRegister lastShipCoordinatesRegister = LastShipCoordinatesRegister.COORDINATES;
        List<Coordinate> lastRemovedShipCoordinates = lastShipCoordinatesRegister.getLastShipCoordinates();

        // Set the variable to '0', if ship placement start from the beginning. Set variable to '1' if it resumes after
        // removing the last three-masted ship.
        int placedThreeMastedShips;
        if (shipDeploymentFromStart) {
            placedThreeMastedShips = 0;
        } else placedThreeMastedShips = 1;

        String mastToPlace = "";

        int firstMastRow = 0;
        int firstMastCol = 0;

        int secondMastRow = 0;
        int secondMastCol = 0;

        if (removalStatus.getWhereRemoved() == ShipRemovalStatus.WhereIsRemoved.ANOTHER_METHOD
                && removalStatus.getWhatRemoved() == ShipRemovalStatus.WhatIsRemoved.MAST) {
            firstMastRow = lastRemovedShipCoordinates.get(0).getRow();
            firstMastCol = lastRemovedShipCoordinates.get(0).getCol();
            secondMastRow = lastRemovedShipCoordinates.get(1).getRow();
            secondMastCol = lastRemovedShipCoordinates.get(1).getCol();
        }

        int thirdMastRow;
        int thirdMastCol;


        while (placedThreeMastedShips < threeMastedShipNumber) {

            //if (!(removalStatus.getWhereRemoved() == ShipRemovalStatus.WhereIsRemoved.ANOTHER_METHOD
            //        && removalStatus.getWhatRemoved() == ShipRemovalStatus.WhatIsRemoved.MAST)) {

                if (!(myBoard[firstMastRow][firstMastCol] == '1')) {

                    // ******************* INPUT AND VALIDATION FOR THE FIRST MAST **********************

                    mastToPlace = "FIRST";

                    System.out.printf(GameStateMessage.ENTER_COORDINATES_FOR_MAST.getMessage(), mastToPlace,
                            placedThreeMastedShips + 1, threeMastedShipNumber, "Three");
                    System.out.printf(GameStateMessage.ENTER_OPTIONS.getMessage());

                    String firstInput = scanner.nextLine();

                    removalStatus.setWasRemoved(false);
                    removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                    removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.NONE);

                    if ("Options".equalsIgnoreCase(firstInput)) {

                        selectAndRemoveMastOrShip(myBoard, scanner, ship, removalStatus);

                        if (placedThreeMastedShips == 0 && removalStatus.isWasRemoved()) {

                            switch (removalStatus.getWhatRemoved()) {
                                case MAST, SHIP:
                                    boolean isDeploymentFromStart = false;
                                    removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.ANOTHER_METHOD);
                                    placeTwoMastedShips(myBoard, water, ship, scanner, output, isDeploymentFromStart);
                                    return;
                            }

                        } else if (placedThreeMastedShips > 0 && removalStatus.isWasRemoved()) {

                            switch (removalStatus.getWhatRemoved()) {
                                case SHIP:
                                    placedThreeMastedShips--;
                                    removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.CURRENT_METHOD);
                                    continue;
                                case MAST:
                                    placedThreeMastedShips--;
                                    removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.CURRENT_METHOD);

                                    if (lastShipCoordinatesRegister.getLastShipCoordinates().isEmpty()) {
                                        throw new IllegalStateException(
                                                "LastShipCoordinates should never be empty at this point");
                                    }
                                    firstMastRow = lastShipCoordinatesRegister.getLastShipCoordinates().get(0).getRow();
                                    firstMastCol = lastShipCoordinatesRegister.getLastShipCoordinates().get(0).getCol();
                                    secondMastRow = lastShipCoordinatesRegister.getLastShipCoordinates().get(1).getRow();
                                    secondMastCol = lastShipCoordinatesRegister.getLastShipCoordinates().get(1).getCol();
                                    break;
                            }
                        } else continue;
                    }

                    if (!(myBoard[firstMastRow][firstMastCol] == '1')) {

                        boolean isValidInput = validateInputFields(firstInput, myBoard, ship);
                        if (!isValidInput) continue;

                        char colChar = firstInput.charAt(0);

                        String rowNumber = firstInput.substring(1);

                        firstMastCol = Character.toUpperCase(colChar) - 'A';
                        firstMastRow = Integer.parseInt(rowNumber) - 1;

                        char possiblePlacement = myBoard[firstMastRow][firstMastCol];

                        if (possiblePlacement != water) {
                            printMyBoard(myBoard, ship);
                            System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                            System.out.println();
                            continue;
                        }

                        // Sprawdzamy sąsiednie pola, czy są wolne
                        boolean canPlaceFirstMast = true;

                        // Sprawdzanie dolnego pola
                        if (firstMastRow < myBoard.length - 1 && myBoard[firstMastRow + 1][firstMastCol] != water) {
                            canPlaceFirstMast = false;
                        }
                        // Sprawdzanie górnego pola
                        if (firstMastRow > 0 && myBoard[firstMastRow - 1][firstMastCol] != water) {
                            canPlaceFirstMast = false;
                        }
                        // Sprawdzanie lewego pola
                        if (firstMastCol > 0 && myBoard[firstMastRow][firstMastCol - 1] != water) {
                            canPlaceFirstMast = false;
                        }
                        // Sprawdzanie prawego pola
                        if (firstMastCol < myBoard[0].length - 1 && myBoard[firstMastRow][firstMastCol + 1] != water) {
                            canPlaceFirstMast = false;
                        }
                        // Lewo-góra
                        if (firstMastRow > 0 && firstMastCol > 0 && myBoard[firstMastRow - 1][firstMastCol - 1] != water) {
                            canPlaceFirstMast = false;
                        }
                        // Lewo-dół
                        if (firstMastRow < myBoard.length - 1 && firstMastCol > 0 && myBoard[firstMastRow + 1][firstMastCol - 1] != water) {
                            canPlaceFirstMast = false;
                        }
                        // Prawo-góra
                        if (firstMastRow > 0 && firstMastCol < myBoard[0].length - 1 && myBoard[firstMastRow - 1][firstMastCol + 1] != water) {
                            canPlaceFirstMast = false;
                        }
                        // Prawo-dół
                        if (firstMastRow < myBoard.length - 1 && firstMastCol < myBoard[0].length - 1
                                && myBoard[firstMastRow + 1][firstMastCol + 1] != water) {
                            canPlaceFirstMast = false;
                        }
                        // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
                        if (!canPlaceFirstMast) {
                            printMyBoard(myBoard, ship);
                            System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                            System.out.println();
                            continue;
                        }

                        myBoard[firstMastRow][firstMastCol] = '1';
                        printMyBoard(myBoard, ship);
                    }
                }


                if (!(myBoard[secondMastRow][secondMastCol] == '2')) {

                    // ******************* INPUT AND VALIDATION FOR THE SECOND MAST **********************

                    boolean secondMastNotIsPlaced = true;

                    while (secondMastNotIsPlaced) {

                        mastToPlace = "SECOND";

                        System.out.printf(GameStateMessage.ENTER_COORDINATES_FOR_MAST.getMessage(), mastToPlace,
                                placedThreeMastedShips + 1, threeMastedShipNumber, "Two");
                        System.out.printf(GameStateMessage.ENTER_OPTIONS.getMessage());

                        String secondInput = scanner.nextLine();

                        removalStatus.setWasRemoved(false);
                        removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                        removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.NONE);

                        if ("Options".equalsIgnoreCase(secondInput)) {

                            selectAndRemoveMastOrShip(myBoard, scanner, ship, removalStatus, firstMastRow, firstMastCol);

                            if (placedThreeMastedShips == 0 && removalStatus.isWasRemoved()) {
                                switch (removalStatus.getWhatRemoved()) {
                                    case MAST:
                                        removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.CURRENT_METHOD);
                                        break;
                                    case SHIP:
                                        boolean deploymentFromStart = false;
                                        removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.ANOTHER_METHOD);
                                        placeTwoMastedShips(myBoard, water, ship, scanner, output, deploymentFromStart);
                                        return;
                                }

                            } else if (placedThreeMastedShips > 0 && removalStatus.isWasRemoved()) {
                                switch (removalStatus.getWhatRemoved()) {
                                    case MAST:
                                        removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.CURRENT_METHOD);
                                        break;
                                    case SHIP:
                                        placedThreeMastedShips--;
                                        removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.CURRENT_METHOD);
                                        break;
                                }
                            } else continue;
                        }

                        if (removalStatus.isWasRemoved()) break;

                        boolean isValidSecondInput = validateInputFields(secondInput, myBoard, ship);
                        if (!isValidSecondInput) continue;

                        char secondColChar = secondInput.charAt(0);

                        String secondRowNumber = secondInput.substring(1);

                        secondMastCol = Character.toUpperCase(secondColChar) - 'A';
                        secondMastRow = Integer.parseInt(secondRowNumber) - 1;

                        // Sprawdzenie czy drugi maszt lezy dokladnie obok pierwszego
                        boolean isTheSecondAdjacent =
                                (secondMastRow == firstMastRow && Math.abs(secondMastCol - firstMastCol) == 1) ||
                                        (secondMastCol == firstMastCol && Math.abs(secondMastRow - firstMastRow) == 1);

                        if (!isTheSecondAdjacent) {
                            printMyBoard(myBoard, ship);
                            System.out.println(("Second mast must be placed directly next to the first one " +
                                    "(vertically or horizontally)!").toUpperCase());
                            System.out.println();
                            continue;
                        }

                        // Pozostala czesc walidacji
                        char possiblePlacementForSecondMast = myBoard[secondMastRow][secondMastCol];

                        if (possiblePlacementForSecondMast != water) {
                            printMyBoard(myBoard, ship);
                            System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                            System.out.println();
                            continue;
                        }

                        // Sprawdzamy sąsiednie pola, czy są wolne
                        boolean canPlaceSecondMast = true;

                        // Sprawdzanie dolnego pola
                        if (secondMastRow < myBoard.length - 1 && myBoard[secondMastRow + 1][secondMastCol] != water) {
                            canPlaceSecondMast = myBoard[secondMastRow + 1][secondMastCol] == '1';
                        }
                        // Sprawdzanie górnego pola
                        if (secondMastRow > 0 && myBoard[secondMastRow - 1][secondMastCol] != water) {
                            canPlaceSecondMast = myBoard[secondMastRow - 1][secondMastCol] == '1';
                        }
                        // Sprawdzanie lewego pola
                        if (secondMastCol > 0 && myBoard[secondMastRow][secondMastCol - 1] != water) {
                            canPlaceSecondMast = myBoard[secondMastRow][secondMastCol - 1] == '1';
                        }
                        // Sprawdzanie prawego pola
                        if (secondMastCol < myBoard[0].length - 1 && myBoard[secondMastRow][secondMastCol + 1] != water) {
                            canPlaceSecondMast = myBoard[secondMastRow][secondMastCol + 1] == '1';
                        }
                        // Lewo-góra
                        if (secondMastRow > 0 && secondMastCol > 0 && myBoard[secondMastRow - 1][secondMastCol - 1] != water) {
                            canPlaceSecondMast = false;
                        }
                        // Lewo-dół
                        if (secondMastRow < myBoard.length - 1 && secondMastCol > 0 && myBoard[secondMastRow + 1][secondMastCol - 1] != water) {
                            canPlaceSecondMast = false;
                        }
                        // Prawo-góra
                        if (secondMastRow > 0 && secondMastCol < myBoard[0].length - 1
                                && myBoard[secondMastRow - 1][secondMastCol + 1] != water) {
                            canPlaceSecondMast = false;
                        }
                        // Prawo-dół
                        if (secondMastRow < myBoard.length - 1 && secondMastCol < myBoard[0].length - 1
                                && myBoard[secondMastRow + 1][secondMastCol + 1] != water) {
                            canPlaceSecondMast = false;
                        }
                        // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
                        if (!canPlaceSecondMast) {
                            printMyBoard(myBoard, ship);
                            System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                            System.out.println();
                            continue;
                        }

                        myBoard[secondMastRow][secondMastCol] = '2';
                        secondMastNotIsPlaced = false;

                        printMyBoard(myBoard, ship);
                    }

                    if (removalStatus.isWasRemoved()) continue;
                }


            //}

            // ******************* INPUT AND VALIDATION FOR THE THIRD MAST **********************

            boolean thirdMastIsNotPlaced = true;

            while (thirdMastIsNotPlaced) {

                mastToPlace = "THIRD";

                System.out.printf(GameStateMessage.ENTER_COORDINATES_FOR_MAST.getMessage(), mastToPlace,
                        placedThreeMastedShips + 1, threeMastedShipNumber, "Three");
                System.out.printf(GameStateMessage.ENTER_OPTIONS.getMessage());

                String thirdInput = scanner.nextLine();

                removalStatus.setWasRemoved(false);
                removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.NONE);

                if ("Options".equalsIgnoreCase(thirdInput)) {

                    selectAndRemoveMastOrShip(myBoard, scanner, ship, removalStatus, firstMastRow, firstMastCol,
                            secondMastRow, secondMastCol);

                    if (placedThreeMastedShips == 0 && removalStatus.isWasRemoved()) {
                        switch (removalStatus.getWhatRemoved()) {
                            case MAST:
                                removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.CURRENT_METHOD);
                                break;
                            case SHIP:
                                boolean deploymentFromStart = false;
                                removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.ANOTHER_METHOD);
                                placeTwoMastedShips(myBoard, water, ship, scanner, output, deploymentFromStart);
                                return;
                        }
                    } else if (placedThreeMastedShips > 0 && removalStatus.isWasRemoved()) {
                        switch (removalStatus.getWhatRemoved()) {
                            case MAST:
                                removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.CURRENT_METHOD);
                                break;
                            case SHIP:
                                placedThreeMastedShips--;
                                removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.CURRENT_METHOD);
                                break;
                        }
                    } else continue;
                }

                if (removalStatus.isWasRemoved()) break;

                boolean isValidThirdInput = validateInputFields(thirdInput, myBoard, ship);
                if (!isValidThirdInput) continue;

                char thirdColChar = thirdInput.charAt(0);

                String thirdRowNumber = thirdInput.substring(1);

                thirdMastCol = Character.toUpperCase(thirdColChar) - 'A';
                thirdMastRow = Integer.parseInt(thirdRowNumber) - 1;

                // Sprawdzenie czy trzeci maszt jest w lini z pierwszym i drugim masztem i czy lezy dokladnie obok
                // drugiego lub obok pierwszego. Jezeli lezy kolo pierwszego to jest sprawdzana odpowiednia odleglosc
                // od drugiego.
                boolean isThirdMastAdjacent =
                        (thirdMastRow == firstMastRow && Math.abs(thirdMastCol - firstMastCol) == 2
                                && Math.abs(thirdMastCol - secondMastCol) == 1) ||
                                (thirdMastCol == firstMastCol && Math.abs(thirdMastRow - firstMastRow) == 2
                                        && Math.abs(thirdMastRow - secondMastRow) == 1) ||
                                (thirdMastRow == firstMastRow && Math.abs(thirdMastCol - firstMastCol) == 1
                                        && Math.abs(thirdMastCol - secondMastCol) == 2) ||
                                (thirdMastCol == firstMastCol && Math.abs(thirdMastRow - firstMastRow) == 1
                                        && Math.abs(thirdMastRow - secondMastRow) == 2);

                if (!isThirdMastAdjacent) {
                    printMyBoard(myBoard, ship);
                    System.out.println(
                            "Third mast must placed be directly next to the second or first one!".toUpperCase());
                    System.out.println();
                    continue;
                }

                // Pozostala czesc walidacji
                char possiblePlacementForThirdMast = myBoard[thirdMastRow][thirdMastCol];

                if (possiblePlacementForThirdMast != water) {
                    printMyBoard(myBoard, ship);
                    System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                    System.out.println();
                    continue;
                }

                // Sprawdzamy sąsiednie pola, czy są wolne
                boolean canPlaceThirdMast = true;

                // Sprawdzanie dolnego pola
                if (thirdMastRow < myBoard.length - 1 && myBoard[thirdMastRow + 1][thirdMastCol] != water) {
                    canPlaceThirdMast = myBoard[thirdMastRow + 1][thirdMastCol] == '2' ||
                            myBoard[thirdMastRow + 1][thirdMastCol] == '1';
                }
                // Sprawdzanie górnego pola
                if (thirdMastRow > 0 && myBoard[thirdMastRow - 1][thirdMastCol] != water) {
                    canPlaceThirdMast = myBoard[thirdMastRow - 1][thirdMastCol] == '2' ||
                            myBoard[thirdMastRow - 1][thirdMastCol] == '1';
                }
                // Sprawdzanie lewego pola
                if (thirdMastCol > 0 && myBoard[thirdMastRow][thirdMastCol - 1] != water) {
                    canPlaceThirdMast = myBoard[thirdMastRow][thirdMastCol - 1] == '2' ||
                            myBoard[thirdMastRow][thirdMastCol - 1] == '1';
                }
                // Sprawdzanie prawego pola
                if (thirdMastCol < myBoard[0].length - 1 && myBoard[thirdMastRow][thirdMastCol + 1] != water) {
                    canPlaceThirdMast = myBoard[thirdMastRow][thirdMastCol + 1] == '2' ||
                            myBoard[thirdMastRow][thirdMastCol + 1] == '1';
                }
                // Lewo-góra
                if (thirdMastRow > 0 && thirdMastCol > 0 && myBoard[thirdMastRow - 1][thirdMastCol - 1] != water) {
                    canPlaceThirdMast = false;
                }
                // Lewo-dół
                if (thirdMastRow < myBoard.length - 1 && thirdMastCol > 0 && myBoard[thirdMastRow + 1][thirdMastCol - 1] != water) {
                    canPlaceThirdMast = false;
                }
                // Prawo-góra
                if (thirdMastRow > 0 && thirdMastCol < myBoard[0].length - 1
                        && myBoard[thirdMastRow - 1][thirdMastCol + 1] != water) {
                    canPlaceThirdMast = false;
                }
                // Prawo-dół
                if (thirdMastRow < myBoard.length - 1 && thirdMastCol < myBoard[0].length - 1
                        && myBoard[thirdMastRow + 1][thirdMastCol + 1] != water) {
                    canPlaceThirdMast = false;
                }

                // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
                if (!canPlaceThirdMast) {
                    printMyBoard(myBoard, ship);
                    System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                    System.out.println();
                    continue;
                }

                Coordinate firstCoordinate = new Coordinate(firstMastRow, firstMastCol);
                Coordinate secondCoordinate = new Coordinate(secondMastRow, secondMastCol);
                Coordinate thirdCoordinate = new Coordinate(thirdMastRow, thirdMastCol);
                ThreeMastedShip threeMastedShip = new ThreeMastedShip(firstCoordinate, secondCoordinate, thirdCoordinate);
                shipService.addShip(threeMastedShip);

                myBoard[firstMastRow][firstMastCol] = ship;
                myBoard[secondMastRow][secondMastCol] = ship;
                myBoard[thirdMastRow][thirdMastCol] = ship;

                thirdMastIsNotPlaced = false;
                placedThreeMastedShips++;

                // System.out.println("PLacedThreeMastedShips na koncu lokowaniu pierwszego statku. " + placedThreeMastedShips);

                printMyBoard(myBoard, ship);
            }
        }
        placeFourMastedShips(myBoard, water, ship, scanner, output);

    }

    private static List<Coordinate> getCoordinatesOfTheLastShip(int shipSize) {
        Ship lastShip = shipService.getListOfMyCreatedShips().getLast();
        if (lastShip.getSize() == shipSize) {
            return lastShip.getCoordinates();
        } else throw new IllegalStateException("The last ship doesn't match the required size.");
    }

    private static List<Coordinate> getCoordinatesOfTheLastShip() {
        Ship lastShip = shipService.getListOfMyCreatedShips().getLast();
        return lastShip.getCoordinates();
    }


    private static void placeFourMastedShips(
            char[][] myBoard, char water, char ship, Scanner scanner, ObjectOutputStream output)
            throws InterruptedException, IOException {


        Thread.sleep(500);
        System.out.println(GameStateMessage.THREE_MAST_SHIPS_PLACED.getMessage());
        Thread.sleep(1000);
        System.out.println();

        ShipRemovalStatus removalStatus = ShipRemovalStatus.REMOVAL_STATUS;

        // Get the coordinates of the last removed ship.
        LastShipCoordinatesRegister lastShipCoordinatesRegister = LastShipCoordinatesRegister.COORDINATES;
        List<Coordinate> lastRemovedShipCoordinates = lastShipCoordinatesRegister.getLastShipCoordinates();

        System.out.println();
        System.out.printf(GameStateMessage.PLACE_YOUR_FOUR_MASTED.getMessage(), fourMastedShipNumber, "Four");
        Thread.sleep(1000);
        System.out.println();

        String mastToPlace = "";

        int placedFourMastedShips = 0;

        int firstMastRow = 0;
        int firstMastCol = 0;

        int secondMastRow = 0;
        int secondMastCol = 0;

        int thirdMastRow = 0;
        int thirdMastCol = 0;

        int fourthMastRow;
        int fourthMastCol;

        // ******************* INPUT AND VALIDATION FOR THE FIRST MAST **********************

        while (placedFourMastedShips < fourMastedShipNumber) {

            if (myBoard[firstMastRow][firstMastCol] != '1') {

                mastToPlace = "FIRST";

                System.out.printf(GameStateMessage.ENTER_COORDINATES_FOR_MAST.getMessage(), mastToPlace,
                        placedFourMastedShips + 1, fourMastedShipNumber, "Four");
                System.out.println(GameStateMessage.ENTER_OPTIONS.getMessage());

                String firstInput = scanner.nextLine();

                removalStatus.setWasRemoved(false);
                removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.NONE);


                if ("Options".equalsIgnoreCase(firstInput)) {

                    selectAndRemoveMastOrShip(myBoard, scanner, ship, removalStatus);

                    if (removalStatus.isWasRemoved()) {

                        switch (removalStatus.getWhatRemoved()) {
                            case MAST, SHIP:
                                boolean isDeploymentFromStart = false;
                                removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.ANOTHER_METHOD);
                                placeThreeMastedShips(myBoard, water, ship, scanner, output, isDeploymentFromStart);
                                return;
                        }

                    } else continue;
                }

                boolean isValidInput = validateInputFields(firstInput, myBoard, ship);
                if (!isValidInput) continue;

                char colChar = firstInput.charAt(0);

                String rowNumber = firstInput.substring(1);

                firstMastCol = Character.toUpperCase(colChar) - 'A';
                firstMastRow = Integer.parseInt(rowNumber) - 1;

                char possiblePlacement = myBoard[firstMastRow][firstMastCol];

                if (possiblePlacement != water) {
                    printMyBoard(myBoard, ship);
                    System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                    System.out.println();
                    continue;
                }

                // Sprawdzamy sąsiednie pola, czy są wolne
                boolean canPlaceFirstMast = true;

                // Sprawdzanie dolnego pola
                if (firstMastRow < myBoard.length - 1 && myBoard[firstMastRow + 1][firstMastCol] != water) {
                    canPlaceFirstMast = false;
                }
                // Sprawdzanie górnego pola
                if (firstMastRow > 0 && myBoard[firstMastRow - 1][firstMastCol] != water) {
                    canPlaceFirstMast = false;
                }
                // Sprawdzanie lewego pola
                if (firstMastCol > 0 && myBoard[firstMastRow][firstMastCol - 1] != water) {
                    canPlaceFirstMast = false;
                }
                // Sprawdzanie prawego pola
                if (firstMastCol < myBoard[0].length - 1 && myBoard[firstMastRow][firstMastCol + 1] != water) {
                    canPlaceFirstMast = false;
                }
                // Lewo-góra
                if (firstMastRow > 0 && firstMastCol > 0 && myBoard[firstMastRow - 1][firstMastCol - 1] != water) {
                    canPlaceFirstMast = false;
                }
                // Lewo-dół
                if (firstMastRow < myBoard.length - 1 && firstMastCol > 0 && myBoard[firstMastRow + 1][firstMastCol - 1] != water) {
                    canPlaceFirstMast = false;
                }
                // Prawo-góra
                if (firstMastRow > 0 && firstMastCol < myBoard[0].length - 1 && myBoard[firstMastRow - 1][firstMastCol + 1] != water) {
                    canPlaceFirstMast = false;
                }
                // Prawo-dół
                if (firstMastRow < myBoard.length - 1 && firstMastCol < myBoard[0].length - 1 && myBoard[firstMastRow + 1][firstMastCol + 1] != water) {
                    canPlaceFirstMast = false;
                }
                // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
                if (!canPlaceFirstMast) {
                    printMyBoard(myBoard, ship);
                    System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                    System.out.println();
                    continue;
                }

                myBoard[firstMastRow][firstMastCol] = '1';
                printMyBoard(myBoard, ship);
            }


            if (myBoard[secondMastRow][secondMastCol] != '2') {

                // ******************* INPUT AND VALIDATION FOR THE SECOND MAST **********************

                mastToPlace = "SECOND";

                boolean secondMastIsNotPlaced = true;

                while (secondMastIsNotPlaced) {

                    System.out.printf(GameStateMessage.ENTER_COORDINATES_FOR_MAST.getMessage(), mastToPlace,
                            placedFourMastedShips + 1, fourMastedShipNumber, "Four");
                    System.out.printf(GameStateMessage.ENTER_OPTIONS.getMessage());

                    String secondInput = scanner.nextLine();

                    removalStatus.setWasRemoved(false);
                    removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                    removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.NONE);

                    if ("Options".equalsIgnoreCase(secondInput)) {

                        selectAndRemoveMastOrShip(myBoard, scanner, ship, removalStatus, firstMastRow, firstMastCol);

                        if (removalStatus.isWasRemoved()) {
                            switch (removalStatus.getWhatRemoved()) {
                                case MAST:
                                    removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.CURRENT_METHOD);
                                    break;
                                case SHIP:
                                    boolean deploymentFromStart = false;
                                    removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.ANOTHER_METHOD);
                                    placeThreeMastedShips(myBoard, water, ship, scanner, output, deploymentFromStart);
                                    return;
                            }
                        } else continue;
                    }

                    if (removalStatus.isWasRemoved()) break;

                    boolean isValidSecondInput = validateInputFields(secondInput, myBoard, ship);
                    if (!isValidSecondInput) continue;

                    char secondColChar = secondInput.charAt(0);

                    String secondRowNumber = secondInput.substring(1);

                    secondMastCol = Character.toUpperCase(secondColChar) - 'A';
                    secondMastRow = Integer.parseInt(secondRowNumber) - 1;

                    // Sprawdzenie czy drugi maszt lezy lezy dokladnie obok pierwszego
                    boolean isSecondMastAdjacent =
                            (secondMastRow == firstMastRow && Math.abs(secondMastCol - firstMastCol) == 1) ||
                                    (secondMastCol == firstMastCol && Math.abs(secondMastRow - firstMastRow) == 1);

                    if (!isSecondMastAdjacent) {
                        printMyBoard(myBoard, ship);
                        System.out.println("Second mast must be placed directly next to the first one (vertically or horizontally)!"
                                .toUpperCase());
                        System.out.println();
                        continue;
                    }

                    // Pozostala czesc walidacji
                    char possiblePlacementForSecondMast = myBoard[secondMastRow][secondMastCol];

                    if (possiblePlacementForSecondMast != water) {
                        printMyBoard(myBoard, ship);
                        System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                        continue;
                    }

                    // Sprawdzamy sąsiednie pola, czy są wolne
                    boolean canPlaceSecondMast = true;

                    // Sprawdzanie dolnego pola
                    if (secondMastRow < myBoard.length - 1 && myBoard[secondMastRow + 1][secondMastCol] != water) {
                        canPlaceSecondMast = myBoard[secondMastRow + 1][secondMastCol] == '1';
                    }
                    // Sprawdzanie górnego pola
                    if (secondMastRow > 0 && myBoard[secondMastRow - 1][secondMastCol] != water) {
                        canPlaceSecondMast = myBoard[secondMastRow - 1][secondMastCol] == '1';
                    }
                    // Sprawdzanie lewego pola
                    if (secondMastCol > 0 && myBoard[secondMastRow][secondMastCol - 1] != water) {
                        canPlaceSecondMast = myBoard[secondMastRow][secondMastCol - 1] == '1';
                    }
                    // Sprawdzanie prawego pola
                    if (secondMastCol < myBoard[0].length - 1 && myBoard[secondMastRow][secondMastCol + 1] != water) {
                        canPlaceSecondMast = myBoard[secondMastRow][secondMastCol + 1] == '1';
                    }
                    // Lewo-góra
                    if (secondMastRow > 0 && secondMastCol > 0 && myBoard[secondMastRow - 1][secondMastCol - 1] != water) {
                        canPlaceSecondMast = false;
                    }
                    // Lewo-dół
                    if (secondMastRow < myBoard.length - 1 && secondMastCol > 0 && myBoard[secondMastRow + 1][secondMastCol - 1] != water) {
                        canPlaceSecondMast = false;
                    }
                    // Prawo-góra
                    if (secondMastRow > 0 && secondMastCol < myBoard[0].length - 1
                            && myBoard[secondMastRow - 1][secondMastCol + 1] != water) {
                        canPlaceSecondMast = false;
                    }
                    // Prawo-dół
                    if (secondMastRow < myBoard.length - 1 && secondMastCol < myBoard[0].length - 1
                            && myBoard[secondMastRow + 1][secondMastCol + 1] != water) {
                        canPlaceSecondMast = false;
                    }
                    // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
                    if (!canPlaceSecondMast) {
                        printMyBoard(myBoard, ship);
                        System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                        System.out.println();
                        continue;
                    }

                    myBoard[secondMastRow][secondMastCol] = '2';
                    secondMastIsNotPlaced = false;

                    printMyBoard(myBoard, ship);
                }

                if (removalStatus.isWasRemoved()) continue;
            }

            if (myBoard[thirdMastRow][thirdMastCol] != '3') {

                // ******************* INPUT AND VALIDATION FOR THE THIRD MAST **********************

                mastToPlace = "THIRD";

                boolean thirdMastIsNotPlaced = true;

                while (thirdMastIsNotPlaced) {

                    System.out.printf(GameStateMessage.ENTER_COORDINATES_FOR_MAST.getMessage(), mastToPlace,
                            placedFourMastedShips + 1, fourMastedShipNumber, "Four");
                    System.out.printf(GameStateMessage.ENTER_OPTIONS.getMessage());

                    String thirdInput = scanner.nextLine();

                    removalStatus.setWasRemoved(false);
                    removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                    removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.NONE);

                    if ("Options".equalsIgnoreCase(thirdInput)) {

                        selectAndRemoveMastOrShip(myBoard, scanner, ship, removalStatus, firstMastRow, firstMastCol,
                                secondMastRow, secondMastCol);

                        if (removalStatus.isWasRemoved()) {
                            switch (removalStatus.getWhatRemoved()) {
                                case MAST:
                                    removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.CURRENT_METHOD);
                                    break;
                                case SHIP:
                                    boolean deploymentFromStart = false;
                                    removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.ANOTHER_METHOD);
                                    placeThreeMastedShips(myBoard, water, ship, scanner, output, deploymentFromStart);
                                    return;
                            }
                        } else continue;
                    }

                    if (removalStatus.isWasRemoved()) break;

                    boolean isValidThirdInput = validateInputFields(thirdInput, myBoard, ship);
                    if (!isValidThirdInput) continue;

                    char thirdColChar = thirdInput.charAt(0);

                    String thirdRowNumber = thirdInput.substring(1);

                    thirdMastCol = Character.toUpperCase(thirdColChar) - 'A';
                    thirdMastRow = Integer.parseInt(thirdRowNumber) - 1;

                    // Ensure the third mast is in line with the first and second,
                    // and directly adjacent to either the second or first mast.
                    boolean isThirdMastAdjacent =
                            (thirdMastRow == firstMastRow && Math.abs(thirdMastCol - firstMastCol) == 2
                                    && Math.abs(thirdMastCol - secondMastCol) == 1) ||
                                    (thirdMastCol == firstMastCol && Math.abs(thirdMastRow - firstMastRow) == 2
                                            && Math.abs(thirdMastRow - secondMastRow) == 1) ||
                                    (thirdMastRow == firstMastRow && Math.abs(thirdMastCol - firstMastCol) == 1
                                            && Math.abs(thirdMastCol - secondMastCol) == 2) ||
                                    (thirdMastCol == firstMastCol && Math.abs(thirdMastRow - firstMastRow) == 1
                                            && Math.abs(thirdMastRow - secondMastRow) == 2);

                    if (!isThirdMastAdjacent) {
                        printMyBoard(myBoard, ship);
                        System.out.println(
                                "Third mast must be placed directly next to the second or first one!".toUpperCase());
                        System.out.println();
                        continue;
                    }

                    // Pozostala czesc walidacji
                    char possiblePlacementForThirdMast = myBoard[thirdMastRow][thirdMastCol];

                    if (possiblePlacementForThirdMast != water) {
                        printMyBoard(myBoard, ship);
                        System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                        System.out.println();
                        continue;
                    }

                    // Sprawdzamy sąsiednie pola, czy są wolne
                    boolean canPlaceThirdMast = true;

                    // Sprawdzanie dolnego pola
                    if (thirdMastRow < myBoard.length - 1 && myBoard[thirdMastRow + 1][thirdMastCol] != water) {
                        canPlaceThirdMast = myBoard[thirdMastRow + 1][thirdMastCol] == '2' ||
                                myBoard[thirdMastRow + 1][thirdMastCol] == '1';
                    }
                    // Sprawdzanie górnego pola
                    if (thirdMastRow > 0 && myBoard[thirdMastRow - 1][thirdMastCol] != water) {
                        canPlaceThirdMast = myBoard[thirdMastRow - 1][thirdMastCol] == '2' ||
                                myBoard[thirdMastRow - 1][thirdMastCol] == '1';
                    }
                    // Sprawdzanie lewego pola
                    if (thirdMastCol > 0 && myBoard[thirdMastRow][thirdMastCol - 1] != water) {
                        canPlaceThirdMast = myBoard[thirdMastRow][thirdMastCol - 1] == '2' ||
                                myBoard[thirdMastRow][thirdMastCol - 1] == '1';
                    }
                    // Sprawdzanie prawego pola
                    if (thirdMastCol < myBoard[0].length - 1 && myBoard[thirdMastRow][thirdMastCol + 1] != water) {
                        canPlaceThirdMast = myBoard[thirdMastRow][thirdMastCol + 1] == '2' ||
                                myBoard[thirdMastRow][thirdMastCol + 1] == '1';
                    }
                    // Lewo-góra
                    if (thirdMastRow > 0 && thirdMastCol > 0 && myBoard[thirdMastRow - 1][thirdMastCol - 1] != water) {
                        canPlaceThirdMast = false;
                    }
                    // Lewo-dół
                    if (thirdMastRow < myBoard.length - 1 && thirdMastCol > 0 && myBoard[thirdMastRow + 1][thirdMastCol - 1] != water) {
                        canPlaceThirdMast = false;
                    }
                    // Prawo-góra
                    if (thirdMastRow > 0 && thirdMastCol < myBoard[0].length - 1
                            && myBoard[thirdMastRow - 1][thirdMastCol + 1] != water) {
                        canPlaceThirdMast = false;
                    }
                    // Prawo-dół
                    if (thirdMastRow < myBoard.length - 1 && thirdMastCol < myBoard[0].length - 1
                            && myBoard[thirdMastRow + 1][thirdMastCol + 1] != water) {
                        canPlaceThirdMast = false;
                    }

                    // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
                    if (!canPlaceThirdMast) {
                        printMyBoard(myBoard, ship);
                        System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                        System.out.println();
                        continue;
                    }

                    myBoard[thirdMastRow][thirdMastCol] = '3';
                    thirdMastIsNotPlaced = false;

                    printMyBoard(myBoard, ship);
                }
                if (removalStatus.isWasRemoved()) continue;
            }


            // ******************* INPUT AND VALIDATION FOR THE FOURTH MAST **********************

            mastToPlace = "FOURTH";

            boolean fourthMastIsNotPlaced = true;

            while (fourthMastIsNotPlaced) {

                System.out.printf(GameStateMessage.ENTER_COORDINATES_FOR_MAST.getMessage(), mastToPlace,
                        placedFourMastedShips + 1, fourMastedShipNumber, "Four");
                System.out.println(GameStateMessage.ENTER_OPTIONS.getMessage());

                String fourthInput = scanner.nextLine();

                removalStatus.setWasRemoved(false);
                removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.NONE);

                if ("Options".equalsIgnoreCase(fourthInput)) {

                    selectAndRemoveMastOrShip(myBoard, scanner, ship, removalStatus, firstMastRow, firstMastCol,
                            secondMastRow, secondMastCol, thirdMastRow, thirdMastCol);

                    if (removalStatus.isWasRemoved()) {
                        switch (removalStatus.getWhatRemoved()) {
                            case MAST:
                                removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.CURRENT_METHOD);
                                break;
                            case SHIP:
                                boolean deploymentFromStart = false;
                                removalStatus.setWhereRemoved(ShipRemovalStatus.WhereIsRemoved.ANOTHER_METHOD);
                                placeThreeMastedShips(myBoard, water, ship, scanner, output, deploymentFromStart);
                                return;
                        }
                    } else continue;
                }

                if (removalStatus.isWasRemoved()) break;

                boolean isValidFourthInput = validateInputFields(fourthInput, myBoard, ship);
                if (!isValidFourthInput) continue;

                char fourthColChar = fourthInput.charAt(0);

                String fourthRowNumber = fourthInput.substring(1);

                int fourthCol = Character.toUpperCase(fourthColChar) - 'A';
                int fourthRow = Integer.parseInt(fourthRowNumber) - 1;

                // Sprawdzenie czy trzeci maszt lezy dokladnie obok czwartego
                boolean isFourthMastAdjacent =
                        fourthRow == firstMastRow && Math.abs(fourthCol - firstMastCol) == 3 && Math.abs(fourthCol - thirdMastCol) == 1 ||
                                fourthCol == firstMastCol && Math.abs(fourthRow - firstMastRow) == 3 && Math.abs(fourthRow - thirdMastRow) == 1 ||
                                fourthRow == firstMastRow && Math.abs(fourthCol - firstMastCol) == 1 && Math.abs(fourthCol - secondMastCol) == 2 ||
                                fourthCol == firstMastCol && Math.abs(fourthRow - firstMastRow) == 1 && Math.abs(fourthRow - secondMastRow) == 2 ||
                                fourthRow == firstMastRow && Math.abs(fourthCol - firstMastCol) == 2 && Math.abs(fourthCol - secondMastCol) == 1 ||
                                fourthCol == firstMastCol && Math.abs(fourthRow - firstMastRow) == 2 && Math.abs(fourthRow - secondMastRow) == 1 ||
                                fourthRow == firstMastRow && Math.abs(fourthCol - firstMastCol) == 2 && Math.abs(fourthCol - thirdMastCol) == 1 ||
                                fourthCol == firstMastCol && Math.abs(fourthRow - firstMastRow) == 2 && Math.abs(fourthRow - thirdMastRow) == 1;

                if (!isFourthMastAdjacent) {
                    printMyBoard(myBoard, ship);
                    System.out.println("The fourth mast must be placed next to the other masts".toUpperCase());
                    System.out.println();
                    continue;
                }

                // Pozostala czesc walidacji
                char possiblePlacementForFourthMast = myBoard[fourthRow][fourthCol];

                if (possiblePlacementForFourthMast != water) {
                    printMyBoard(myBoard, ship);
                    System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                    System.out.println();
                    continue;
                }

                // Sprawdzamy sąsiednie pola, czy są wolne
                boolean canPlaceFourthMast = true;

                // Sprawdzanie dolnego pola
                if (fourthRow < myBoard.length - 1 && myBoard[fourthRow + 1][fourthCol] != water) {
                    canPlaceFourthMast = myBoard[fourthRow + 1][fourthCol] == '2' ||
                            myBoard[fourthRow + 1][fourthCol] == '3' ||
                            myBoard[fourthRow + 1][fourthCol] == '1';
                }
                // Sprawdzanie górnego pola
                if (fourthRow > 0 && myBoard[fourthRow - 1][fourthCol] != water) {
                    canPlaceFourthMast = myBoard[fourthRow - 1][fourthCol] == '2' ||
                            myBoard[fourthRow - 1][fourthCol] == '3' ||
                            myBoard[fourthRow - 1][fourthCol] == '1';
                }
                // Sprawdzanie lewego pola
                if (fourthCol > 0 && myBoard[fourthRow][fourthCol - 1] != water) {
                    canPlaceFourthMast = myBoard[fourthRow][fourthCol - 1] == '2' ||
                            myBoard[fourthRow][fourthCol - 1] == '3' ||
                            myBoard[fourthRow][fourthCol - 1] == '1';
                }
                // Sprawdzanie prawego pola
                if (fourthCol < myBoard[0].length - 1 && myBoard[fourthRow][fourthCol + 1] != water) {
                    canPlaceFourthMast = myBoard[fourthRow][fourthCol + 1] == '2' ||
                            myBoard[fourthRow][fourthCol + 1] == '3' ||
                            myBoard[fourthRow][fourthCol + 1] == '1';
                }
                // Lewo-góra
                if (fourthRow > 0 && fourthCol > 0 && myBoard[fourthRow - 1][fourthCol - 1] != water) {
                    canPlaceFourthMast = false;
                }
                // Lewo-dół
                if (fourthRow < myBoard.length - 1 && fourthCol > 0 && myBoard[fourthRow + 1][fourthCol - 1] != water) {
                    canPlaceFourthMast = false;
                }
                // Prawo-góra
                if (fourthRow > 0 && fourthCol < myBoard[0].length - 1
                        && myBoard[fourthRow - 1][fourthCol + 1] != water) {
                    canPlaceFourthMast = false;
                }
                // Prawo-dół
                if (fourthRow < myBoard.length - 1 && fourthCol < myBoard[0].length - 1
                        && myBoard[fourthRow + 1][fourthCol + 1] != water) {
                    canPlaceFourthMast = false;
                }
                // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
                if (!canPlaceFourthMast) {
                    printMyBoard(myBoard, ship);
                    System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                    System.out.println();
                    continue;
                }


                Coordinate firstCoordinate = new Coordinate(firstMastRow, firstMastCol);
                Coordinate secondCoordinate = new Coordinate(secondMastRow, secondMastCol);
                Coordinate thirdCoordinate = new Coordinate(thirdMastRow, thirdMastCol);
                Coordinate fourthCoordinate = new Coordinate(fourthRow, fourthCol);
                FourMastedShip fourMastedShip =
                        new FourMastedShip(firstCoordinate, secondCoordinate, thirdCoordinate, fourthCoordinate);
                shipService.addShip(fourMastedShip);

                placedFourMastedShips++;

                myBoard[firstMastRow][firstMastCol] = ship;
                myBoard[secondMastRow][secondMastCol] = ship;
                myBoard[thirdMastRow][thirdMastCol] = ship;
                myBoard[fourthRow][fourthCol] = ship;

                fourthMastIsNotPlaced = false;

                placedFourMastedShips++;
            }
        }

        printMyBoard(myBoard, ship);

        Thread.sleep(500);
        System.out.println(GameStateMessage.ALL_SHIPS_PLACED.getMessage());
        Thread.sleep(1000);
        System.out.println();
        System.out.println(GameStateMessage.WAITING_FOR_OPPONENT.getMessage());

        output.writeObject(ServerMessage.SHIPS_PLACED.getMessage());

    }

    private static Ship getSunkShip(Coordinate opponentShotCoordinate, List<Ship> copyOfMyShipsListForMessagesAfterSunk) {
        return copyOfMyShipsListForMessagesAfterSunk
                .stream()
                .filter(s -> s.getCoordinates().contains(opponentShotCoordinate))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Sunken ship not found in the ship list copy."));
    }

    private static void displayMessages(
            String firstHitMessageToDisplay, String secondHitMessageToDisplay,
            String thirdHitMessageToDisplay, String fourthHitMessageToDisplay) throws InterruptedException {

        // System.out.println();
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
        if (fourthHitMessageToDisplay != null && !fourthHitMessageToDisplay.isEmpty()) {
            System.out.println();
            System.out.println(fourthHitMessageToDisplay);
            Thread.sleep(1000);
        }
    }

    private static String areAllShipsSunk(
            List<Ship> myShips, ObjectOutputStream output) throws IOException {

        if (myShips.isEmpty()) {
            output.writeObject(ShotFeedbackMessage.ALL_SHIPS_SUNK.getMessage());
            return ShotFeedbackMessage.ALL_SHIPS_SUNK.getOpponentFeedback();

        } else output.writeObject(null);
        return null;
    }

    private static boolean didPLayerWin(String fifthOpponentReport) throws InterruptedException {

        if (ShotFeedbackMessage.ALL_SHIPS_SUNK.getMessage().equals(fifthOpponentReport)) {
            System.out.println();
            System.out.println(ShotFeedbackMessage.ALL_SHIPS_SUNK.getMessage());
            Thread.sleep(1000);
            MessagePrinter.printYouWin();
            return true;
        }
        return false;
    }

    private static boolean validateInputFields(
            String input, char[][] myBoard, char[][] opponentBoard, char ship, List<Ship> remainingOpponentShips) {

        if (input.length() < 2 || input.length() > 3) {
            printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);
            System.out.println("Invalid format. Enter e.g. A5 or B10");
            System.out.println();
            return false;
        }

        char colChar = input.charAt(0);

        if (!Character.isLetter(colChar)) {
            printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);
            System.out.println("THE FIRST CHARACTER MUST BE A LETTER!");
            System.out.println();
            return false;
        }

        String rowNumber = input.substring(1);
        char firstCharOfRowNumber = rowNumber.charAt(0);

        if (rowNumber.length() == 2) {
            char secondCharOfRowNumber = rowNumber.charAt(1);
            if (!Character.isDigit(secondCharOfRowNumber)) {
                printMyBoard(myBoard, ship);
                System.out.println("THE THIRD CHARACTER MUST BE A DIGIT!");
                System.out.println();
                return false;
            }
        }

        int col = Character.toUpperCase(colChar) - 'A';

        if (!Character.isDigit(firstCharOfRowNumber)) {
            printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);
            System.out.println("THE SECOND CHARACTER MUST BE A DIGIT!");
            System.out.println();
            return false;
        }

        int row = Integer.parseInt(rowNumber) - 1;


        if ((input.length() == 3) && !(rowNumber.equals("10"))) {
            printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);
            System.out.println("THE SECOND AND THIRD CHARACTER MUST BE '10'!");
            System.out.println();
            return false;
        }


        // Checking if the column letter is within A-J range
        if ((col < 0) || (col > 9)) {
            printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);
            System.out.println("Column must be between A and J!".toUpperCase());
            System.out.println();
            return false;
        }

        // Checking if the row number is within 1-10 range
        if ((row < 0) || (row > 9)) {
            printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);
            System.out.println("Row must be between 1 and 10!".toUpperCase());
            System.out.println();
            return false;
        }
        return true;
    }

    private static boolean validateInputFields(String input, char[][] myBoard, char ship) {

        if (input.length() < 2 || input.length() > 3) {
            printMyBoard(myBoard, ship);
            System.out.println("Invalid format. Enter e.g. A5 or B10");
            System.out.println();
            return false;
        }

        char colChar = input.charAt(0);

        if (!Character.isLetter(colChar)) {
            printMyBoard(myBoard, ship);
            System.out.println("THE FIRST CHARACTER MUST BE A LETTER!");
            System.out.println();
            return false;
        }

        String rowNumber = input.substring(1);
        char firstCharOfRowNumber = rowNumber.charAt(0);

        if (rowNumber.length() == 2) {
            char secondCharOfRowNumber = rowNumber.charAt(1);
            if (!Character.isDigit(secondCharOfRowNumber)) {
                printMyBoard(myBoard, ship);
                System.out.println("THE THIRD CHARACTER MUST BE A DIGIT!");
                System.out.println();
                return false;
            }
        }

        int col = Character.toUpperCase(colChar) - 'A';

        if (!Character.isDigit(firstCharOfRowNumber)) {
            printMyBoard(myBoard, ship);
            System.out.println("THE SECOND CHARACTER MUST BE A DIGIT!");
            System.out.println();
            return false;
        }

        int row = Integer.parseInt(rowNumber) - 1;


        if ((input.length() == 3) && !(rowNumber.equals("10"))) {
            printMyBoard(myBoard, ship);
            System.out.println("THE SECOND AND THIRD CHARACTER MUST BE '10'!");
            System.out.println();
            return false;
        }


        // Checking if the column letter is within A-J range
        if ((col < 0) || (col > 9)) {
            printMyBoard(myBoard, ship);
            System.out.println("Column must be between A and J!".toUpperCase());
            System.out.println();
            return false;
        }

        // Checking if the row number is within 1-10 range
        if ((row < 0) || (row > 9)) {
            printMyBoard(myBoard, ship);
            System.out.println("Row must be between 1 and 10!".toUpperCase());
            System.out.println();
            return false;
        }
        return true;
    }

    private static char[][] createBoard() {
        char[][] gameBoard = new char[gameBoardLength][gameBoardLength];
        for (char[] row : gameBoard) {
            Arrays.fill(row, water);
        }
        return gameBoard;
    }

    private static void printMyBoard(char[][] myBoard, char ship) {
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
                    System.out.print("\u001B[37m" + ship + "  " + "\u001B[0m");
                } else if (position == '1' || position == '2' || position == '3' || position == '4') {
                    System.out.print("\u001B[37m" + position + "  " + "\u001B[0m");
                } else {
                    System.out.print("\u001B[34m" + position + "  " + "\u001B[0m");
                }
            }
            System.out.println();
        }

        // This code displays the tenth row of the board.
        System.out.print(10 + " ");
        for (int col = 0; col < gameBoardLength; col++) {
            char position = myBoard[9][col];
            if (position == ship) {
                System.out.print("\u001B[37m" + ship + "  " + "\u001B[0m");
            } else if (position == '1' || position == '2' || position == '3' || position == '4') {
                System.out.print("\u001B[37m" + position + "  " + "\u001B[0m");
            } else {
                System.out.print("\u001B[34m" + position + "  " + "\u001B[0m");
            }
        }
        System.out.println();
        System.out.println();
        System.out.println();
    }

    private static void printEntireGameBoard(char[][] myBoard, char[][] opponentBoard, char ship, List<Ship>
            remainingOpponentShips) {

        long countSingleMastedShips = remainingOpponentShips.stream().filter(s -> s.getSize() == 1).count();
        long countTwoMastedShips = remainingOpponentShips.stream().filter(s -> s.getSize() == 2).count();
        long countThreeMastedShips = remainingOpponentShips.stream().filter(s -> s.getSize() == 3).count();
        long countFourMastedShips = remainingOpponentShips.stream().filter(s -> s.getSize() == 4).count();

        for (int i = 0; i < 2; i++) {
            System.out.println();
        }
        System.out.println("          OPPONENT BOARD                                                          " +
                "MY BOARD            ");
        System.out.println();

        // Ten kod wyswietla nazwy kolumn
        int myBoardLength = myBoard.length;
        int opponentBoardLength = opponentBoard.length;
        char[] rowName = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
                ' ', ' ', ' ', ' ', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'};
        System.out.print("   ");
        for (int i = 0; i < myBoardLength + 13 + opponentBoardLength; i++) {
            System.out.print(rowName[i] + "  ");
        }
        System.out.println();

        // Ta petla wyswietla wszystkie wiersze Opponent-Board bez dziesiatego plus wiadomosci o pozostalych statkach
        // przeciwnika plus wszystkie wiersze bez dziesiatego z My board.
        for (int row = 0; row < 9; row++) {
            System.out.print(" ");
            System.out.print(row + 1 + " ");
            for (int col = 0; col < gameBoardLength; col++) {
                char position = opponentBoard[row][col];
                if (position == ship) {
                    System.out.print("\u001B[37m" + ship + "  " + "\u001B[0m");
                } else if (position == ClientBattleship.miss) {
                    System.out.print("\u001B[33m" + position + "  " + "\u001B[0m");
                } else if (position == ClientBattleship.HIT_AND_SUNK_CHAR) {
                    System.out.print("\u001B[31m" + position + "  " + "\u001B[0m");
                } else if (position == ClientBattleship.HIT_MAST_CHAR) {
                    System.out.print("\u001B[31m" + position + "  " + "\u001B[0m");
                } else {
                    System.out.print("\u001B[34m" + position + "  " + "\u001B[0m");
                }

                // Display messages about opponent's remaining ships
                if (row == 0 && col == 9) {
                    System.out.print("      Opponent's remaining ships:");
                }

                if (row == 2 && col == 9) {
                    if (countSingleMastedShips == 1) {
                        System.out.print("      " + "\u001B[37m" + countSingleMastedShips + "\u001B[0m"
                                + " Single-Masted Ship");
                    } else System.out.print("      " + "\u001B[37m" + countSingleMastedShips + "\u001B[0m"
                            + " Single-Masted Ships");
                }

                if (row == 4 && col == 9) {
                    if (countTwoMastedShips == 1) {
                        System.out.print("      " + "\u001B[37m" + countTwoMastedShips + "\u001B[0m"
                                + " Two-Masted Ship");
                    } else System.out.print("      " + "\u001B[37m" + countTwoMastedShips + "\u001B[0m"
                            + " Two-Masted Ships");
                }

                if (row == 6 && col == 9) {
                    if (countThreeMastedShips == 1) {
                        System.out.print("      " + "\u001B[37m" + countThreeMastedShips + "\u001B[0m"
                                + " Three-Masted Ship");
                    } else System.out.print("      " + "\u001B[37m" + countThreeMastedShips + "\u001B[0m"
                            + " Three-Masted Ships");
                }

                if (row == 8 && col == 9) {
                    if (countFourMastedShips == 1) {
                        System.out.print("      " + "\u001B[37m" + countFourMastedShips + "\u001B[0m"
                                + " Four-Masted Ship");
                    } else System.out.print("      " + "\u001B[37m" + countFourMastedShips + "\u001B[0m"
                            + " Four-Masted Ships");
                }
            }

            // This code adjusts the spacing between opponent's board and my board, as well as between messages and
            // my board.
            if (row == 1 || row == 3 || row == 5 || row == 7) {
                System.out.print("                                       ");
            } else {
                if (row == 0) {
                    System.out.print("      ");
                } else if (row == 2) {
                    if (countSingleMastedShips == 1) {
                        System.out.print("             ");
                    } else System.out.print("            ");
                } else if (row == 4) {
                    if (countTwoMastedShips == 1) {
                        System.out.print("                ");
                    } else System.out.print("               ");
                } else if (row == 6) {
                    if (countThreeMastedShips == 1) {
                        System.out.print("              ");
                    } else System.out.print("             ");
                } else {
                    if (countFourMastedShips == 1) {
                        System.out.print("               ");
                    } else System.out.print("              ");
                }
            }

            // This code displays all rows of My-Board without tenth.
            for (int col = 0; col < gameBoardLength; col++) {
                char position = myBoard[row][col];
                if (position == ship) {
                    System.out.print("\u001B[37m" + ship + "  " + "\u001B[0m");
                } else if (position == ClientBattleship.miss) {
                    System.out.print("\u001B[33m" + position + "  " + "\u001B[0m");
                } else if (position == ClientBattleship.HIT_AND_SUNK_CHAR) {
                    System.out.print("\u001B[31m" + position + "  " + "\u001B[0m");
                } else if (position == ClientBattleship.HIT_MAST_CHAR) {
                    System.out.print("\u001B[31m" + position + "  " + "\u001B[0m");
                } else {
                    System.out.print("\u001B[34m" + position + "  " + "\u001B[0m");
                }
            }

            System.out.println();

        }

        // This code displays the tenth row of the Opponent Board.
        System.out.print(10 + " ");
        for (int col = 0; col < gameBoardLength; col++) {
            char position = opponentBoard[9][col];
            if (position == ship) {
                System.out.print("\u001B[37m" + ship + "  " + "\u001B[0m");
            } else if (position == ClientBattleship.miss) {
                System.out.print("\u001B[33m" + position + "  " + "\u001B[0m");
            } else if (position == ClientBattleship.HIT_AND_SUNK_CHAR) {
                System.out.print("\u001B[31m" + position + "  " + "\u001B[0m");
            } else if (position == ClientBattleship.HIT_MAST_CHAR) {
                System.out.print("\u001B[31m" + position + "  " + "\u001B[0m");
            } else {
                System.out.print("\u001B[34m" + position + "  " + "\u001B[0m");
            }
        }

        System.out.print("                                       ");

        // This code displays the tenth row of the My Board.
        for (int col = 0; col < gameBoardLength; col++) {
            char position = myBoard[9][col];
            if (position == ship) {
                System.out.print("\u001B[37m" + ship + "  " + "\u001B[0m");
            } else if (position == ClientBattleship.miss) {
                System.out.print("\u001B[33m" + position + "  " + "\u001B[0m");
            } else if (position == ClientBattleship.HIT_AND_SUNK_CHAR) {
                System.out.print("\u001B[31m" + position + "  " + "\u001B[0m");
            } else if (position == ClientBattleship.HIT_MAST_CHAR) {
                System.out.print("\u001B[31m" + position + "  " + "\u001B[0m");
            } else {
                System.out.print("\u001B[34m" + position + "  " + "\u001B[0m");
            }

        }

        System.out.println();
        System.out.println();
    }

    private static Coordinate getFirstMastCoordinates() {

        Ship lastShip = shipService.getListOfMyCreatedShips().getLast();
        List<Coordinate> coordinatesOfLastShip = lastShip.getCoordinates();

        if (lastShip.getCoordinates().size() > 1) {
            return coordinatesOfLastShip.get(coordinatesOfLastShip.size() - 2);
        }
        return null;
    }

    private static void selectAndRemoveMastOrShip(
            char[][] myBoard, Scanner scanner, char ship, ShipRemovalStatus removalStatus) {

        List<Ship> listOfShips = shipService.getListOfMyCreatedShips();

        String selectedOption = "";

        while (!List.of("1", "2", "3").contains(selectedOption)) {

            System.out.print(GameStateMessage.AVAILABLE_OPTIONS.getMessage());

            selectedOption = scanner.nextLine();

            if (selectedOption.length() > 1 || selectedOption.isBlank()) {
                printMyBoard(myBoard, ship);
                System.out.println(GameStateMessage.WRONG_OPTION.getMessage());
                continue;
            }

            if (listOfShips.isEmpty()) {
                printMyBoard(myBoard, ship);
                System.out.println(GameStateMessage.NO_MAST_TO_REMOVE.getMessage());
                System.out.println();
                removalStatus.setWasRemoved(false);
                removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                break;
            }

            Ship lastShip = listOfShips.getLast();
            List<Coordinate> coordinatesOfLastShip = lastShip.getCoordinates();


            LastShipCoordinatesRegister lastShipCoordinatesRegister = LastShipCoordinatesRegister.COORDINATES;
            lastShipCoordinatesRegister.setLastShipCoordinates(coordinatesOfLastShip);


            // W razie czego sprawdzam wspolrzedne gdyby ostatni statek, ktory nie ma zadnych przypisanych wspolrzednych
            // byl jakims cudem jeszcze w liscie i usuwamy go. Normalnie pusty statek powinien byc usuniety.
            if (coordinatesOfLastShip.isEmpty()) {
                shipService.removeShip(lastShip);
                printMyBoard(myBoard, ship);
                System.out.println(GameStateMessage.NO_SHIP_TO_REMOVE.getMessage());
                System.out.println();
                removalStatus.setWasRemoved(false);
                removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                break;
            }

            switch (selectedOption) {
                // First case: Remove the last placed mast.
                case "1":

                    Coordinate lastCoordinate = coordinatesOfLastShip.getLast();

                    // Ta metoda usunie statek jednomasztowy z pamieci listy bo ma tylko jeden maszt i to jest tak
                    // jakbym usunal caly statek.
                    if (lastShip.getSize() == 1) {
                        myBoard[lastCoordinate.getRow()][lastCoordinate.getCol()] = water;
                        shipService.removeShip(lastShip);
                    }

                    // Ta metoda usunie statek dwumasztowy z pamieci listy i oznaczy pierwszy maszt jedynka ‘1'
                    if (lastShip.getSize() == 2) {
                        if (coordinatesOfLastShip.size() == 2) {
                            myBoard[lastCoordinate.getRow()][lastCoordinate.getCol()] = water;
                            shipService.removeShip(lastShip);
                            Coordinate firstMastCoordinates = lastShip.getCoordinates().get(0);
                            myBoard[firstMastCoordinates.getRow()][firstMastCoordinates.getCol()] = '1';
                        }
                    }

                    // Ta metoda usunie statek trzymasztowy z pamieci  listy i oznaczy pierwszy maszt jedynka ‘1'
                    // i drugi maszt '2'
                    if (lastShip.getSize() == 3) {
                        if (coordinatesOfLastShip.size() == 3) {
                            myBoard[lastCoordinate.getRow()][lastCoordinate.getCol()] = water;
                            shipService.removeShip(lastShip);
                            Coordinate firstMastCoordinates = lastShip.getCoordinates().get(0);
                            Coordinate secondMastCoordinates = lastShip.getCoordinates().get(1);
                            myBoard[firstMastCoordinates.getRow()][firstMastCoordinates.getCol()] = '1';
                            myBoard[secondMastCoordinates.getRow()][secondMastCoordinates.getCol()] = '2';
                        }
                    }


                    printMyBoard(myBoard, ship);
                    System.out.println(GameStateMessage.LAST_MAST_REMOVED.getMessage());
                    System.out.println();

                    removalStatus.setWasRemoved(true);
                    removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.MAST);
                    break;

                // Second case: Remove the last placed ship.
                case "2":

                    coordinatesOfLastShip.forEach(coordinate -> {
                        myBoard[coordinate.getRow()][coordinate.getCol()] = water;
                    });

                    shipService.removeShip(lastShip);

                    printMyBoard(myBoard, ship);
                    System.out.println(GameStateMessage.LAST_SHIP_REMOVED.getMessage());
                    System.out.println();

                    removalStatus.setWasRemoved(true);
                    removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.SHIP);
                    break;

                // Third case: Exit.
                case "3":

                    printMyBoard(myBoard, ship);
                    removalStatus.setWasRemoved(false);
                    removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                    break;

                default:
                    printMyBoard(myBoard, ship);
                    System.out.println(GameStateMessage.WRONG_OPTION.getMessage());
            }

        }
    }

    private static void selectAndRemoveMastOrShip(
            char[][] myBoard, Scanner scanner, char ship, ShipRemovalStatus removalStatus, int firstMastRow,
            int firstMastCol) {

        List<Ship> listOfShips = shipService.getListOfMyCreatedShips();

        String selectedOption = "";

        while (!List.of("1", "2", "3").contains(selectedOption)) {

            System.out.print(GameStateMessage.AVAILABLE_OPTIONS.getMessage());

            selectedOption = scanner.nextLine();

            if (selectedOption.length() > 1 || selectedOption.isBlank()) {
                printMyBoard(myBoard, ship);
                System.out.println(GameStateMessage.WRONG_OPTION.getMessage());
                continue;
            }

            if (listOfShips.isEmpty()) {
                printMyBoard(myBoard, ship);
                System.out.println(GameStateMessage.NO_MAST_TO_REMOVE.getMessage());
                System.out.println();
                removalStatus.setWasRemoved(false);
                removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                break;
            }

            Ship lastShip = listOfShips.getLast();
            List<Coordinate> coordinatesOfLastShip = lastShip.getCoordinates();

            // W razie czego sprawdzam wspolrzedne gdyby ostatni statek, ktory nie ma zadnych przypisanych wspolrzednych
            // byl jakims cudem jeszcze w liscie i usuwamy go. Normalnie pusty statek powinien byc usuniety.
            if (coordinatesOfLastShip.isEmpty()) {
                shipService.removeShip(lastShip);
                printMyBoard(myBoard, ship);
                System.out.println(GameStateMessage.NO_SHIP_TO_REMOVE.getMessage());
                System.out.println();
                removalStatus.setWasRemoved(false);
                removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                break;
            }

            switch (selectedOption) {
                // Second case: Remove the last placed mast.
                case "1":

                    myBoard[firstMastRow][firstMastCol] = water;


                    printMyBoard(myBoard, ship);
                    System.out.println(GameStateMessage.LAST_MAST_REMOVED.getMessage());
                    System.out.println();
                    removalStatus.setWasRemoved(true);
                    removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.MAST);
                    break;

                // Second case: Remove the last placed ship.
                case "2":

                    myBoard[firstMastRow][firstMastCol] = water;

                    coordinatesOfLastShip.forEach(coordinate -> {
                        myBoard[coordinate.getRow()][coordinate.getCol()] = water;
                    });

                    shipService.removeShip(lastShip);

                    printMyBoard(myBoard, ship);
                    System.out.println(GameStateMessage.LAST_SHIP_REMOVED.getMessage());
                    System.out.println();
                    removalStatus.setWasRemoved(true);
                    removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.SHIP);
                    break;

                // Third case: Exit.
                case "3":

                    printMyBoard(myBoard, ship);
                    removalStatus.setWasRemoved(false);
                    removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                    break;

                default:
                    printMyBoard(myBoard, ship);
                    System.out.println(GameStateMessage.WRONG_OPTION.getMessage());
            }

        }
    }

    private static void selectAndRemoveMastOrShip(
            char[][] myBoard, Scanner scanner, char ship, ShipRemovalStatus removalStatus, int firstMastRow,
            int firstMastCol, int secondMastRow, int secondMastCol) {

        List<Ship> listOfShips = shipService.getListOfMyCreatedShips();

        String selectedOption = "";

        while (!List.of("1", "2", "3").contains(selectedOption)) {

            System.out.print(GameStateMessage.AVAILABLE_OPTIONS.getMessage());

            selectedOption = scanner.nextLine();

            if (selectedOption.length() > 1 || selectedOption.isBlank()) {
                printMyBoard(myBoard, ship);
                System.out.println(GameStateMessage.WRONG_OPTION.getMessage());
                continue;
            }

            if (listOfShips.isEmpty()) {
                printMyBoard(myBoard, ship);
                System.out.println(GameStateMessage.NO_MAST_TO_REMOVE.getMessage());
                System.out.println();
                removalStatus.setWasRemoved(false);
                removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                break;
            }

            Ship lastShip = listOfShips.getLast();
            List<Coordinate> coordinatesOfLastShip = lastShip.getCoordinates();

            // W razie czego sprawdzam wspolrzedne gdyby ostatni statek, ktory nie ma zadnych przypisanych wspolrzednych
            // byl jakims cudem jeszcze w liscie i usuwamy go. Normalnie pusty statek powinien byc usuniety.
            if (coordinatesOfLastShip.isEmpty()) {
                shipService.removeShip(lastShip);
                printMyBoard(myBoard, ship);
                System.out.println(GameStateMessage.NO_SHIP_TO_REMOVE.getMessage());
                System.out.println();
                removalStatus.setWasRemoved(false);
                removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                break;
            }

            switch (selectedOption) {

                // Second case: Remove the last placed mast.
                case "1":

                    myBoard[secondMastRow][secondMastCol] = water;


                    printMyBoard(myBoard, ship);
                    System.out.println(GameStateMessage.LAST_MAST_REMOVED.getMessage());
                    System.out.println();
                    removalStatus.setWasRemoved(true);
                    removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.MAST);
                    break;

                // Second case: Remove the last placed ship.
                case "2":

                    myBoard[firstMastRow][firstMastCol] = water;
                    myBoard[secondMastRow][secondMastCol] = water;

                    coordinatesOfLastShip.forEach(coordinate -> {
                        myBoard[coordinate.getRow()][coordinate.getCol()] = water;
                    });

                    shipService.removeShip(lastShip);

                    printMyBoard(myBoard, ship);
                    System.out.println(GameStateMessage.LAST_SHIP_REMOVED.getMessage());
                    System.out.println();
                    removalStatus.setWasRemoved(true);
                    removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.SHIP);
                    break;

                // Third case: Exit.
                case "3":

                    printMyBoard(myBoard, ship);
                    removalStatus.setWasRemoved(false);
                    removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                    break;

                default:
                    printMyBoard(myBoard, ship);
                    System.out.println(GameStateMessage.WRONG_OPTION.getMessage());
            }

        }
    }

    private static void selectAndRemoveMastOrShip(
            char[][] myBoard, Scanner scanner, char ship, ShipRemovalStatus removalStatus, int firstMastRow,
            int firstMastCol, int secondMastRow, int secondMastCol, int thirdMastRow, int thirdMastCol) {

        List<Ship> listOfShips = shipService.getListOfMyCreatedShips();

        String selectedOption = "";

        while (!List.of("1", "2", "3").contains(selectedOption)) {

            System.out.print(GameStateMessage.AVAILABLE_OPTIONS.getMessage());

            selectedOption = scanner.nextLine();

            if (selectedOption.length() > 1 || selectedOption.isBlank()) {
                printMyBoard(myBoard, ship);
                System.out.println(GameStateMessage.WRONG_OPTION.getMessage());
                continue;
            }

            if (listOfShips.isEmpty()) {
                printMyBoard(myBoard, ship);
                System.out.println(GameStateMessage.NO_MAST_TO_REMOVE.getMessage());
                System.out.println();
                removalStatus.setWasRemoved(false);
                removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                break;
            }

            Ship lastShip = listOfShips.getLast();
            List<Coordinate> coordinatesOfLastShip = lastShip.getCoordinates();

            // W razie czego sprawdzam wspolrzedne gdyby ostatni statek, ktory nie ma zadnych przypisanych wspolrzednych
            // byl jakims cudem jeszcze w liscie i usuwamy go. Normalnie pusty statek powinien byc usuniety.
            if (coordinatesOfLastShip.isEmpty()) {
                shipService.removeShip(lastShip);
                printMyBoard(myBoard, ship);
                System.out.println(GameStateMessage.NO_SHIP_TO_REMOVE.getMessage());
                System.out.println();
                removalStatus.setWasRemoved(false);
                removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                break;
            }

            switch (selectedOption) {
                // Second case: Remove the last placed mast.
                case "1":

                    myBoard[thirdMastRow][thirdMastCol] = water;


                    printMyBoard(myBoard, ship);
                    System.out.println(GameStateMessage.LAST_MAST_REMOVED.getMessage());
                    System.out.println();
                    removalStatus.setWasRemoved(true);
                    removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.MAST);
                    break;

                // Second case: Remove the last placed ship.
                case "2":

                    myBoard[firstMastRow][firstMastCol] = water;
                    myBoard[secondMastRow][secondMastCol] = water;
                    myBoard[thirdMastRow][thirdMastCol] = water;


                    coordinatesOfLastShip.forEach(coordinate -> {
                        myBoard[coordinate.getRow()][coordinate.getCol()] = water;
                    });

                    shipService.removeShip(lastShip);

                    printMyBoard(myBoard, ship);
                    System.out.println(GameStateMessage.LAST_SHIP_REMOVED.getMessage());
                    System.out.println();
                    removalStatus.setWasRemoved(true);
                    removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.SHIP);
                    break;

                // Third case: Exit.
                case "3":

                    printMyBoard(myBoard, ship);
                    removalStatus.setWasRemoved(false);
                    removalStatus.setWhatRemoved(ShipRemovalStatus.WhatIsRemoved.NONE);
                    break;

                default:
                    printMyBoard(myBoard, ship);
                    System.out.println(GameStateMessage.WRONG_OPTION.getMessage());
            }

        }
    }

    /*
    public static LastShipCoordinatesRegister registerTheLastShipCoordinates() {
        List<Ship> listOfShips = shipService.getListOfMyCreatedShips();

        if (listOfShips.isEmpty()) {
            System.out.println("No ship has been placed yet.");
        }

        Ship lastShip = listOfShips.getLast();
        List<Coordinate> coordinatesOfLastShip = lastShip.getCoordinates();

        LastShipCoordinatesRegister lastShipCoordinatesRegister = LastShipCoordinatesRegister.COORDINATES;
        lastShipCoordinatesRegister.setLastShipCoordinates(coordinatesOfLastShip);

        return lastShipCoordinatesRegister;
    }

     */


}



