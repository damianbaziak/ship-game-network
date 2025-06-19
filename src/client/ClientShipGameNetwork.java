package client;

import battleshipnetwork.MessagePrinter;
import client.enums.BoardCell;
import client.enums.GameStateMessage;
import client.enums.ServerMessage;
import client.enums.ShotFeedbackMessage;
import client.model.coordinate.Coordinate;
import client.model.ship.Ship;
import client.model.ship.implementation.FourMastedShip;
import client.model.ship.implementation.SingleMastedShip;
import client.model.ship.implementation.ThreeMastedShip;
import client.model.ship.implementation.TwoMastedShip;
import client.service.ShipService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

public class ClientShipGameNetwork {
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

            /*

            List<Ship> myShipForTest = new ArrayList<>();
            myShipForTest.add(new SingleMastedShip(new Coordinate(0, 0)));
            myShipForTest.add(new TwoMastedShip(new Coordinate(0, 0), new Coordinate(1, 1)));
            myShipForTest.add(new ThreeMastedShip(new Coordinate(0, 0), new Coordinate(1, 1),
                    new Coordinate(2, 2)));
            myShipForTest.add(new FourMastedShip(new Coordinate(0, 0), new Coordinate(1, 1),
                    new Coordinate(2, 2), new Coordinate(3, 3)));

            printEntireGameBoard(myBoard, opponentBoard, ship, myShipForTest);

             */


            placeShips(myBoard, water, ship, scanner, output);

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

            if (ServerMessage.YOU_TURN.getMessage().equals(whoseTurnIsIt)) {
                makeShot(myBoard, opponentBoard, scanner, input, output, ship, hitAndSunk, miss,
                        remainingOpponentShips);
            } else if (ServerMessage.GAME_OVER.getMessage().equals(whoseTurnIsIt)) {
                gameRunning = false;
            } else {
                opponentShot(myBoard, opponentBoard, myShips, remainingOpponentShips,
                        copyOfMyShipsForMessagesToOpponentAfterSunk, myShipsHitCoordinates, input, output, ship, hitAndSunk);
            }

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
            List<Ship> copyOfMyShipsListForMessagesAfterSunk, List<Coordinate> myShipHitCoordinates,
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

            if (myShipHitCoordinates.contains(opponentShotCoordinate)) {

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

            } else if ((!myShipHitCoordinates.contains(opponentShotCoordinate)) && (possibleHitShip.isPresent())) {

                myShipHitCoordinates.add(opponentShotCoordinate);

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

                myShipHitCoordinates.add(opponentShotCoordinate);

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

    private static Ship getSunkShip(Coordinate opponentShotCoordinate, List<Ship> copyOfMyShipsListForMessagesAfterSunk) {
        return copyOfMyShipsListForMessagesAfterSunk
                .stream()
                .filter(s -> s.getCoordinates().contains(opponentShotCoordinate))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Sunk ship not found in the copy ship list"));
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
                        System.out.println(ShotFeedbackMessage.ALL_SINGLE_MAST_SHIPS_SUNK);
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

                printEntireGameBoard(myBoard, opponentBoard, ship, remainingOpponentShips);
                Thread.sleep(500);
                MessagePrinter.displayMiss();
                Thread.sleep(1000);

                youHitYouTurn = false;


            }
        }


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

    private static void placeShips(
            char[][] myBoard, char water, char ship, Scanner scanner, ObjectOutputStream output) throws InterruptedException, IOException {

        int placedShips = 0;

        printMyBoard(myBoard, ship);
        System.out.printf(GameStateMessage.PLACE_YOUR_SHIPS.getMessage(), singleMastedShipNumber, "Single");
        System.out.println();

        while (placedShips < singleMastedShipNumber) {

            System.out.printf(GameStateMessage.ENTER_COORDINATES_SINGLE_MAST_SHIPS.getMessage(), placedShips + 1);
            String input = scanner.nextLine();

            boolean isValidInput = validateInputFields(input, myBoard, ship);
            if (!isValidInput) continue;

            char colChar = input.charAt(0);

            String rowNumber = input.substring(1);

            int col = Character.toUpperCase(colChar) - 'A';
            int row = Integer.parseInt(rowNumber) - 1;


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

            myBoard[row][col] = ship;
            placedShips++;

            printMyBoard(myBoard, ship);

            Coordinate coordinate = new Coordinate(row, col);
            SingleMastedShip singleMastedShip = new SingleMastedShip(coordinate);
            shipService.addShip(singleMastedShip);

        }

        placeTwoMastedShips(myBoard, water, ship, scanner, output);

    }

    private static void placeTwoMastedShips(
            char[][] myBoard, char water, char ship, Scanner scanner, ObjectOutputStream output) throws InterruptedException, IOException {

        Thread.sleep(500);
        System.out.println(GameStateMessage.SINGLE_MAST_SHIPS_PLACED.getMessage());
        Thread.sleep(1000);
        System.out.println();
        System.out.println();
        System.out.printf(GameStateMessage.PLACE_YOUR_SHIPS.getMessage(), twoMastedShipNumber, "Two");
        Thread.sleep(1000);
        System.out.println();

        int placedTwoMastedShips = 0;


        while (placedTwoMastedShips < twoMastedShipNumber) {

            // ******************* INPUT AND VALIDATION FOR THE FIRST MAST **********************

            System.out.printf(GameStateMessage.ENTER_COORDINATES_FOR_FIRST_MAST.getMessage(), placedTwoMastedShips + 1,
                    twoMastedShipNumber, "Two");
            String firstInput = scanner.nextLine();

            boolean isValidInput = validateInputFields(firstInput, myBoard, ship);
            if (!isValidInput) continue;

            char colChar = firstInput.charAt(0);

            String rowNumber = firstInput.substring(1);


            int col = Character.toUpperCase(colChar) - 'A';
            int row = Integer.parseInt(rowNumber) - 1;


            char possiblePlacement = myBoard[row][col];

            if (possiblePlacement != water) {
                printMyBoard(myBoard, ship);
                System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                System.out.println();
                continue;
            }

            // SPRAWDZAMY CZY SASIEDNIE POLA SA WOLNE
            boolean canPlaceFirstMast = true;

            // Sprawdzanie dolnego pola
            if (row < myBoard.length - 1 && myBoard[row + 1][col] != water) {
                canPlaceFirstMast = false;
            }
            // Sprawdzanie górnego pola
            if (row > 0 && myBoard[row - 1][col] != water) {
                canPlaceFirstMast = false;
            }
            // Sprawdzanie lewego pola
            if (col > 0 && myBoard[row][col - 1] != water) {
                canPlaceFirstMast = false;
            }
            // Sprawdzanie prawego pola
            if (col < myBoard[0].length - 1 && myBoard[row][col + 1] != water) {
                canPlaceFirstMast = false;
            }
            // Lewo-góra
            if (row > 0 && col > 0 && myBoard[row - 1][col - 1] != water) {
                canPlaceFirstMast = false;
            }
            // Lewo-dół
            if (row < myBoard.length - 1 && col > 0 && myBoard[row + 1][col - 1] != water) {
                canPlaceFirstMast = false;
            }
            // Prawo-góra
            if (row > 0 && col < myBoard[0].length - 1 && myBoard[row - 1][col + 1] != water) {
                canPlaceFirstMast = false;
            }
            // Prawo-dół
            if (row < myBoard.length - 1 && col < myBoard[0].length - 1 && myBoard[row + 1][col + 1] != water) {
                canPlaceFirstMast = false;
            }

            // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
            if (!canPlaceFirstMast) {
                printMyBoard(myBoard, ship);
                System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                System.out.println();
                continue;
            }

            myBoard[row][col] = '1';
            printMyBoard(myBoard, ship);


            // ******************* INPUT AND VALIDATION FOR THE SECOND MAST **********************

            boolean secondMastIsNotPlced = true;

            while (secondMastIsNotPlced) {

                System.out.printf(GameStateMessage.ENTER_COORDINATES_FOR_SECOND_MAST.getMessage(),
                        placedTwoMastedShips + 1, twoMastedShipNumber, "Two");
                String secondInput = scanner.nextLine();

                boolean isValidSecondInput = validateInputFields(secondInput, myBoard, ship);
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
                    printMyBoard(myBoard, ship);
                    System.out.println(("Second mast must be placed directly next to the first one " +
                            "(vertically or horizontally)!").toUpperCase());
                    System.out.println();
                    continue;
                }


                // Pozostala czesc walidacji
                char possiblePlacementForSecondMast = myBoard[secondRow][secondCol];

                if (possiblePlacementForSecondMast != water) {
                    printMyBoard(myBoard, ship);
                    System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                    System.out.println();
                    continue;
                }

                // Sprawdzamy sąsiednie pola, czy są wolne
                boolean canPlaceSecondMast = true;

                // Sprawdzanie dolnego pola
                if (secondRow < myBoard.length - 1 && myBoard[secondRow + 1][secondCol] != water) {
                    canPlaceSecondMast = myBoard[secondRow + 1][secondCol] == '1';
                }
                // Sprawdzanie górnego pola
                if (secondRow > 0 && myBoard[secondRow - 1][secondCol] != water) {
                    canPlaceSecondMast = myBoard[secondRow - 1][secondCol] == '1';
                }
                // Sprawdzanie lewego pola
                if (secondCol > 0 && myBoard[secondRow][secondCol - 1] != water) {
                    canPlaceSecondMast = myBoard[secondRow][secondCol - 1] == '1';
                }
                // Sprawdzanie prawego pola
                if (secondCol < myBoard[0].length - 1 && myBoard[secondRow][secondCol + 1] != water) {
                    canPlaceSecondMast = myBoard[secondRow][secondCol + 1] == '1';
                }
                // Lewo-góra
                if (secondRow > 0 && secondCol > 0 && myBoard[secondRow - 1][secondCol - 1] != water) {
                    canPlaceSecondMast = false;
                }
                // Lewo-dół
                if (secondRow < myBoard.length - 1 && secondCol > 0 && myBoard[secondRow + 1][secondCol - 1] != water) {
                    canPlaceSecondMast = false;
                }
                // Prawo-góra
                if (secondRow > 0 && secondCol < myBoard[0].length - 1
                        && myBoard[secondRow - 1][secondCol + 1] != water) {
                    canPlaceSecondMast = false;
                }
                // Prawo-dół
                if (secondRow < myBoard.length - 1 && secondCol < myBoard[0].length - 1
                        && myBoard[secondRow + 1][secondCol + 1] != water) {
                    canPlaceSecondMast = false;
                }
                // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
                if (!canPlaceSecondMast) {
                    printMyBoard(myBoard, ship);
                    System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                    System.out.println();
                    continue;
                }


                myBoard[row][col] = ship;
                myBoard[secondRow][secondCol] = ship;
                secondMastIsNotPlced = false;

                Coordinate firstCoordinate = new Coordinate(row, col);
                Coordinate secondCoordinate = new Coordinate(secondRow, secondCol);
                TwoMastedShip twoMastedShip = new TwoMastedShip(firstCoordinate, secondCoordinate);
                shipService.addShip(twoMastedShip);
            }

            placedTwoMastedShips++;

            printMyBoard(myBoard, ship);
        }


        placeThreeMastedShips(myBoard, water, ship, scanner, output);
    }

    private static void placeThreeMastedShips(
            char[][] myBoard, char water, char ship, Scanner scanner, ObjectOutputStream output) throws InterruptedException, IOException {

        Thread.sleep(500);
        System.out.println(GameStateMessage.TWO_MAST_SHIPS_PLACED.getMessage());
        Thread.sleep(1000);
        System.out.println();
        System.out.println();
        System.out.printf(GameStateMessage.PLACE_YOUR_SHIPS.getMessage(), threeMastedShipNumber, "Three");
        Thread.sleep(1000);
        System.out.println();


        int placedThreeMastedShips = 0;

        while (placedThreeMastedShips < threeMastedShipNumber) {

            // ******************* INPUT AND VALIDATION FOR THE SECOND MAST **********************

            System.out.printf(GameStateMessage.ENTER_COORDINATES_FOR_FIRST_MAST.getMessage(),
                    placedThreeMastedShips + 1, threeMastedShipNumber, "Three");

            String firstInput = scanner.nextLine();


            boolean isValidInput = validateInputFields(firstInput, myBoard, ship);
            if (!isValidInput) continue;

            char colChar = firstInput.charAt(0);

            String rowNumber = firstInput.substring(1);

            int col = Character.toUpperCase(colChar) - 'A';
            int row = Integer.parseInt(rowNumber) - 1;

            char possiblePlacement = myBoard[row][col];

            if (possiblePlacement != water) {
                printMyBoard(myBoard, ship);
                System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                System.out.println();
                continue;
            }

            // Sprawdzamy sąsiednie pola, czy są wolne
            boolean canPlaceFirstMast = true;

            // Sprawdzanie dolnego pola
            if (row < myBoard.length - 1 && myBoard[row + 1][col] != water) {
                canPlaceFirstMast = false;
            }
            // Sprawdzanie górnego pola
            if (row > 0 && myBoard[row - 1][col] != water) {
                canPlaceFirstMast = false;
            }
            // Sprawdzanie lewego pola
            if (col > 0 && myBoard[row][col - 1] != water) {
                canPlaceFirstMast = false;
            }
            // Sprawdzanie prawego pola
            if (col < myBoard[0].length - 1 && myBoard[row][col + 1] != water) {
                canPlaceFirstMast = false;
            }
            // Lewo-góra
            if (row > 0 && col > 0 && myBoard[row - 1][col - 1] != water) {
                canPlaceFirstMast = false;
            }
            // Lewo-dół
            if (row < myBoard.length - 1 && col > 0 && myBoard[row + 1][col - 1] != water) {
                canPlaceFirstMast = false;
            }
            // Prawo-góra
            if (row > 0 && col < myBoard[0].length - 1 && myBoard[row - 1][col + 1] != water) {
                canPlaceFirstMast = false;
            }
            // Prawo-dół
            if (row < myBoard.length - 1 && col < myBoard[0].length - 1 && myBoard[row + 1][col + 1] != water) {
                canPlaceFirstMast = false;
            }
            // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
            if (!canPlaceFirstMast) {
                printMyBoard(myBoard, ship);
                System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                System.out.println();
                continue;
            }

            myBoard[row][col] = '1';
            printMyBoard(myBoard, ship);


            // ******************* INPUT AND VALIDATION FOR THE SECOND MAST **********************

            int secondRow = 0;
            int secondCol = 0;

            boolean secondMastNotIsPlaced = true;

            while (secondMastNotIsPlaced) {

                System.out.printf(GameStateMessage.ENTER_COORDINATES_FOR_SECOND_MAST.getMessage(),
                        placedThreeMastedShips + 1, threeMastedShipNumber, "Three");
                String secondInput = scanner.nextLine();

                boolean isValidSecondInput = validateInputFields(secondInput, myBoard, ship);
                if (!isValidSecondInput) continue;

                char secondColChar = secondInput.charAt(0);

                String secondRowNumber = secondInput.substring(1);

                secondCol = Character.toUpperCase(secondColChar) - 'A';
                secondRow = Integer.parseInt(secondRowNumber) - 1;

                // Sprawdzenie czy drugi maszt lezy dokladnie obok pierwszego
                boolean isTheSecondAdjacent =
                        (secondRow == row && Math.abs(secondCol - col) == 1) ||
                                (secondCol == col && Math.abs(secondRow - row) == 1);

                if (!isTheSecondAdjacent) {
                    printMyBoard(myBoard, ship);
                    System.out.println(("Second mast must be placed directly next to the first one " +
                            "(vertically or horizontally)!").toUpperCase());
                    System.out.println();
                    continue;
                }

                // Pozostala czesc walidacji
                char possiblePlacementForSecondMast = myBoard[secondRow][secondCol];

                if (possiblePlacementForSecondMast != water) {
                    printMyBoard(myBoard, ship);
                    System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                    System.out.println();
                    continue;
                }

                // Sprawdzamy sąsiednie pola, czy są wolne
                boolean canPlaceSecondMast = true;

                // Sprawdzanie dolnego pola
                if (secondRow < myBoard.length - 1 && myBoard[secondRow + 1][secondCol] != water) {
                    canPlaceSecondMast = myBoard[secondRow + 1][secondCol] == '1';
                }
                // Sprawdzanie górnego pola
                if (secondRow > 0 && myBoard[secondRow - 1][secondCol] != water) {
                    canPlaceSecondMast = myBoard[secondRow - 1][secondCol] == '1';
                }
                // Sprawdzanie lewego pola
                if (secondCol > 0 && myBoard[secondRow][secondCol - 1] != water) {
                    canPlaceSecondMast = myBoard[secondRow][secondCol - 1] == '1';
                }
                // Sprawdzanie prawego pola
                if (secondCol < myBoard[0].length - 1 && myBoard[secondRow][secondCol + 1] != water) {
                    canPlaceSecondMast = myBoard[secondRow][secondCol + 1] == '1';
                }
                // Lewo-góra
                if (secondRow > 0 && secondCol > 0 && myBoard[secondRow - 1][secondCol - 1] != water) {
                    canPlaceSecondMast = false;
                }
                // Lewo-dół
                if (secondRow < myBoard.length - 1 && secondCol > 0 && myBoard[secondRow + 1][secondCol - 1] != water) {
                    canPlaceSecondMast = false;
                }
                // Prawo-góra
                if (secondRow > 0 && secondCol < myBoard[0].length - 1
                        && myBoard[secondRow - 1][secondCol + 1] != water) {
                    canPlaceSecondMast = false;
                }
                // Prawo-dół
                if (secondRow < myBoard.length - 1 && secondCol < myBoard[0].length - 1
                        && myBoard[secondRow + 1][secondCol + 1] != water) {
                    canPlaceSecondMast = false;
                }
                // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
                if (!canPlaceSecondMast) {
                    printMyBoard(myBoard, ship);
                    System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                    System.out.println();
                    continue;
                }

                myBoard[secondRow][secondCol] = '2';
                secondMastNotIsPlaced = false;

                printMyBoard(myBoard, ship);
            }


            // ******************* INPUT AND VALIDATION FOR THE THIRD MAST **********************

            boolean thirdMastIsNotPlaced = true;

            while (thirdMastIsNotPlaced) {

                System.out.printf(GameStateMessage.ENTER_COORDINATES_FOR_THIRD_MAST.getMessage(),
                        placedThreeMastedShips + 1, threeMastedShipNumber, "Three");
                String thirdInput = scanner.nextLine();

                boolean isValidThirdInput = validateInputFields(thirdInput, myBoard, ship);
                if (!isValidThirdInput) continue;

                char thirdColChar = thirdInput.charAt(0);

                String thirdRowNumber = thirdInput.substring(1);

                int thirdCol = Character.toUpperCase(thirdColChar) - 'A';
                int thirdRow = Integer.parseInt(thirdRowNumber) - 1;

                // Sprawdzenie czy trzeci maszt jest w lini z pierwszym i drugim masztem i czy lezy dokladnie obok
                // drugiego lub obok pierwszego. Jezeli lezy kolo pierwszego to jest sprawdzana odpowiednia odleglosc
                // od drugiego.
                boolean isThirdMastAdjacent =
                        (thirdRow == row && Math.abs(thirdCol - col) == 2
                                && Math.abs(thirdCol - secondCol) == 1) ||
                                (thirdCol == col && Math.abs(thirdRow - row) == 2
                                        && Math.abs(thirdRow - secondRow) == 1) ||
                                (thirdRow == row && Math.abs(thirdCol - col) == 1
                                        && Math.abs(thirdCol - secondCol) == 2) ||
                                (thirdCol == col && Math.abs(thirdRow - row) == 1
                                        && Math.abs(thirdRow - secondRow) == 2);

                if (!isThirdMastAdjacent) {
                    printMyBoard(myBoard, ship);
                    System.out.println(
                            "Third mast must placed be directly next to the second or first one!".toUpperCase());
                    System.out.println();
                    continue;
                }

                // Pozostala czesc walidacji
                char possiblePlacementForThirdMast = myBoard[thirdRow][thirdCol];

                if (possiblePlacementForThirdMast != water) {
                    printMyBoard(myBoard, ship);
                    System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                    System.out.println();
                    continue;
                }

                // Sprawdzamy sąsiednie pola, czy są wolne
                boolean canPlaceThirdMast = true;

                // Sprawdzanie dolnego pola
                if (thirdRow < myBoard.length - 1 && myBoard[thirdRow + 1][thirdCol] != water) {
                    canPlaceThirdMast = myBoard[thirdRow + 1][thirdCol] == '2' ||
                            myBoard[thirdRow + 1][thirdCol] == '1';
                }
                // Sprawdzanie górnego pola
                if (thirdRow > 0 && myBoard[thirdRow - 1][thirdCol] != water) {
                    canPlaceThirdMast = myBoard[thirdRow - 1][thirdCol] == '2' ||
                            myBoard[thirdRow - 1][thirdCol] == '1';
                }
                // Sprawdzanie lewego pola
                if (thirdCol > 0 && myBoard[thirdRow][thirdCol - 1] != water) {
                    canPlaceThirdMast = myBoard[thirdRow][thirdCol - 1] == '2' ||
                            myBoard[thirdRow][thirdCol - 1] == '1';
                }
                // Sprawdzanie prawego pola
                if (thirdCol < myBoard[0].length - 1 && myBoard[thirdRow][thirdCol + 1] != water) {
                    canPlaceThirdMast = myBoard[thirdRow][thirdCol + 1] == '2' ||
                            myBoard[thirdRow][thirdCol + 1] == '1';
                }
                // Lewo-góra
                if (thirdRow > 0 && thirdCol > 0 && myBoard[thirdRow - 1][thirdCol - 1] != water) {
                    canPlaceThirdMast = false;
                }
                // Lewo-dół
                if (thirdRow < myBoard.length - 1 && thirdCol > 0 && myBoard[thirdRow + 1][thirdCol - 1] != water) {
                    canPlaceThirdMast = false;
                }
                // Prawo-góra
                if (thirdRow > 0 && thirdCol < myBoard[0].length - 1
                        && myBoard[thirdRow - 1][thirdCol + 1] != water) {
                    canPlaceThirdMast = false;
                }
                // Prawo-dół
                if (thirdRow < myBoard.length - 1 && thirdCol < myBoard[0].length - 1
                        && myBoard[thirdRow + 1][thirdCol + 1] != water) {
                    canPlaceThirdMast = false;
                }

                // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
                if (!canPlaceThirdMast) {
                    printMyBoard(myBoard, ship);
                    System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                    System.out.println();
                    continue;
                }


                myBoard[row][col] = ship;
                myBoard[secondRow][secondCol] = ship;
                myBoard[thirdRow][thirdCol] = ship;

                thirdMastIsNotPlaced = false;

                Coordinate firstCoordinate = new Coordinate(row, col);
                Coordinate secondCoordinate = new Coordinate(secondRow, secondCol);
                Coordinate thirdCoordinate = new Coordinate(thirdRow, thirdCol);
                ThreeMastedShip threeMastedShip = new ThreeMastedShip(firstCoordinate, secondCoordinate, thirdCoordinate);
                shipService.addShip(threeMastedShip);

            }

            placedThreeMastedShips++;

            printMyBoard(myBoard, ship);
        }

        placeFourMastedShips(myBoard, water, ship, scanner, output);
    }

    private static void placeFourMastedShips(
            char[][] myBoard, char water, char ship, Scanner scanner, ObjectOutputStream output) throws InterruptedException, IOException {

        Thread.sleep(500);
        System.out.println(GameStateMessage.THREE_MAST_SHIPS_PLACED.getMessage());
        Thread.sleep(1000);
        System.out.println();
        System.out.println();
        System.out.printf(GameStateMessage.PLACE_YOUR_SHIPS.getMessage(), fourMastedShipNumber, "Four");
        Thread.sleep(1000);
        System.out.println();

        int placedFourMastedShips = 0;

        // ******************* INPUT AND VALIDATION FOR THE FIRST MAST **********************

        while (placedFourMastedShips < fourMastedShipNumber) {

            System.out.printf(GameStateMessage.ENTER_COORDINATES_FOR_FIRST_MAST.getMessage(),
                    placedFourMastedShips + 1, fourMastedShipNumber, "Four");
            String firstInput = scanner.nextLine();

            boolean isValidInput = validateInputFields(firstInput, myBoard, ship);
            if (!isValidInput) continue;

            char colChar = firstInput.charAt(0);

            String rowNumber = firstInput.substring(1);

            int col = Character.toUpperCase(colChar) - 'A';
            int row = Integer.parseInt(rowNumber) - 1;

            char possiblePlacement = myBoard[row][col];

            if (possiblePlacement != water) {
                printMyBoard(myBoard, ship);
                System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                System.out.println();
                continue;
            }

            // Sprawdzamy sąsiednie pola, czy są wolne
            boolean canPlaceFirstMast = true;

            // Sprawdzanie dolnego pola
            if (row < myBoard.length - 1 && myBoard[row + 1][col] != water) {
                canPlaceFirstMast = false;
            }
            // Sprawdzanie górnego pola
            if (row > 0 && myBoard[row - 1][col] != water) {
                canPlaceFirstMast = false;
            }
            // Sprawdzanie lewego pola
            if (col > 0 && myBoard[row][col - 1] != water) {
                canPlaceFirstMast = false;
            }
            // Sprawdzanie prawego pola
            if (col < myBoard[0].length - 1 && myBoard[row][col + 1] != water) {
                canPlaceFirstMast = false;
            }
            // Lewo-góra
            if (row > 0 && col > 0 && myBoard[row - 1][col - 1] != water) {
                canPlaceFirstMast = false;
            }
            // Lewo-dół
            if (row < myBoard.length - 1 && col > 0 && myBoard[row + 1][col - 1] != water) {
                canPlaceFirstMast = false;
            }
            // Prawo-góra
            if (row > 0 && col < myBoard[0].length - 1 && myBoard[row - 1][col + 1] != water) {
                canPlaceFirstMast = false;
            }
            // Prawo-dół
            if (row < myBoard.length - 1 && col < myBoard[0].length - 1 && myBoard[row + 1][col + 1] != water) {
                canPlaceFirstMast = false;
            }
            // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
            if (!canPlaceFirstMast) {
                printMyBoard(myBoard, ship);
                System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                System.out.println();
                continue;
            }

            myBoard[row][col] = '1';
            printMyBoard(myBoard, ship);


            // ******************* INPUT AND VALIDATION FOR THE SECOND MAST **********************

            int secondCol = 0;
            int secondRow = 0;

            boolean secondMastIsNotPlaced = true;

            while (secondMastIsNotPlaced) {

                System.out.printf(GameStateMessage.ENTER_COORDINATES_FOR_SECOND_MAST.getMessage(),
                        placedFourMastedShips + 1, fourMastedShipNumber, "Four");
                String secondInput = scanner.nextLine();

                boolean isValidSecondInput = validateInputFields(secondInput, myBoard, ship);
                if (!isValidSecondInput) continue;

                char secondColChar = secondInput.charAt(0);

                String secondRowNumber = secondInput.substring(1);

                secondCol = Character.toUpperCase(secondColChar) - 'A';
                secondRow = Integer.parseInt(secondRowNumber) - 1;

                // Sprawdzenie czy drugi maszt lezy lezy dokladnie obok pierwszego
                boolean isSecondMastAdjacent =
                        (secondRow == row && Math.abs(secondCol - col) == 1) ||
                                (secondCol == col && Math.abs(secondRow - row) == 1);

                if (!isSecondMastAdjacent) {
                    printMyBoard(myBoard, ship);
                    System.out.println("Second mast must be placed directly next to the first one (vertically or horizontally)!"
                            .toUpperCase());
                    System.out.println();
                    continue;
                }

                // Pozostala czesc walidacji
                char possiblePlacementForSecondMast = myBoard[secondRow][secondCol];

                if (possiblePlacementForSecondMast != water) {
                    printMyBoard(myBoard, ship);
                    System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                    continue;
                }

                // Sprawdzamy sąsiednie pola, czy są wolne
                boolean canPlaceSecondMast = true;

                // Sprawdzanie dolnego pola
                if (secondRow < myBoard.length - 1 && myBoard[secondRow + 1][secondCol] != water) {
                    canPlaceSecondMast = myBoard[secondRow + 1][secondCol] == '1';
                }
                // Sprawdzanie górnego pola
                if (secondRow > 0 && myBoard[secondRow - 1][secondCol] != water) {
                    canPlaceSecondMast = myBoard[secondRow - 1][secondCol] == '1';
                }
                // Sprawdzanie lewego pola
                if (secondCol > 0 && myBoard[secondRow][secondCol - 1] != water) {
                    canPlaceSecondMast = myBoard[secondRow][secondCol - 1] == '1';
                }
                // Sprawdzanie prawego pola
                if (secondCol < myBoard[0].length - 1 && myBoard[secondRow][secondCol + 1] != water) {
                    canPlaceSecondMast = myBoard[secondRow][secondCol + 1] == '1';
                }
                // Lewo-góra
                if (secondRow > 0 && secondCol > 0 && myBoard[secondRow - 1][secondCol - 1] != water) {
                    canPlaceSecondMast = false;
                }
                // Lewo-dół
                if (secondRow < myBoard.length - 1 && secondCol > 0 && myBoard[secondRow + 1][secondCol - 1] != water) {
                    canPlaceSecondMast = false;
                }
                // Prawo-góra
                if (secondRow > 0 && secondCol < myBoard[0].length - 1
                        && myBoard[secondRow - 1][secondCol + 1] != water) {
                    canPlaceSecondMast = false;
                }
                // Prawo-dół
                if (secondRow < myBoard.length - 1 && secondCol < myBoard[0].length - 1
                        && myBoard[secondRow + 1][secondCol + 1] != water) {
                    canPlaceSecondMast = false;
                }
                // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
                if (!canPlaceSecondMast) {
                    printMyBoard(myBoard, ship);
                    System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                    System.out.println();
                    continue;
                }

                myBoard[secondRow][secondCol] = '2';
                secondMastIsNotPlaced = false;

                printMyBoard(myBoard, ship);
            }


            // ******************* INPUT AND VALIDATION FOR THE THIRD MAST **********************

            int thirdRow = 0;
            int thirdCol = 0;
            boolean thirdMastIsNotPlaced = true;

            while (thirdMastIsNotPlaced) {

                System.out.printf(GameStateMessage.ENTER_COORDINATES_FOR_THIRD_MAST.getMessage(),
                        placedFourMastedShips + 1, fourMastedShipNumber, "Four");
                String thirdInput = scanner.nextLine();

                boolean isValidThirdInput = validateInputFields(thirdInput, myBoard, ship);
                if (!isValidThirdInput) continue;

                char thirdColChar = thirdInput.charAt(0);

                String thirdRowNumber = thirdInput.substring(1);

                thirdCol = Character.toUpperCase(thirdColChar) - 'A';
                thirdRow = Integer.parseInt(thirdRowNumber) - 1;

                // Ensure the third mast is in line with the first and second,
                // and directly adjacent to either the second or first mast.
                boolean isThirdMastAdjacent =
                        (thirdRow == row && Math.abs(thirdCol - col) == 2
                                && Math.abs(thirdCol - secondCol) == 1) ||
                                (thirdCol == col && Math.abs(thirdRow - row) == 2
                                        && Math.abs(thirdRow - secondRow) == 1) ||
                                (thirdRow == row && Math.abs(thirdCol - col) == 1
                                        && Math.abs(thirdCol - secondCol) == 2) ||
                                (thirdCol == col && Math.abs(thirdRow - row) == 1
                                        && Math.abs(thirdRow - secondRow) == 2);

                if (!isThirdMastAdjacent) {
                    printMyBoard(myBoard, ship);
                    System.out.println(
                            "Third mast must be placed directly next to the second or first one!".toUpperCase());
                    System.out.println();
                    continue;
                }

                // Pozostala czesc walidacji
                char possiblePlacementForThirdMast = myBoard[thirdRow][thirdCol];

                if (possiblePlacementForThirdMast != water) {
                    printMyBoard(myBoard, ship);
                    System.out.println("CANNOT PLACE SHIP HERE, POSITION ALREADY TAKEN!");
                    System.out.println();
                    continue;
                }

                // Sprawdzamy sąsiednie pola, czy są wolne
                boolean canPlaceThirdMast = true;

                // Sprawdzanie dolnego pola
                if (thirdRow < myBoard.length - 1 && myBoard[thirdRow + 1][thirdCol] != water) {
                    canPlaceThirdMast = myBoard[thirdRow + 1][thirdCol] == '2' ||
                            myBoard[thirdRow + 1][thirdCol] == '1';
                }
                // Sprawdzanie górnego pola
                if (thirdRow > 0 && myBoard[thirdRow - 1][thirdCol] != water) {
                    canPlaceThirdMast = myBoard[thirdRow - 1][thirdCol] == '2' ||
                            myBoard[thirdRow - 1][thirdCol] == '1';
                }
                // Sprawdzanie lewego pola
                if (thirdCol > 0 && myBoard[thirdRow][thirdCol - 1] != water) {
                    canPlaceThirdMast = myBoard[thirdRow][thirdCol - 1] == '2' ||
                            myBoard[thirdRow][thirdCol - 1] == '1';
                }
                // Sprawdzanie prawego pola
                if (thirdCol < myBoard[0].length - 1 && myBoard[thirdRow][thirdCol + 1] != water) {
                    canPlaceThirdMast = myBoard[thirdRow][thirdCol + 1] == '2' ||
                            myBoard[thirdRow][thirdCol + 1] == '1';
                }
                // Lewo-góra
                if (thirdRow > 0 && thirdCol > 0 && myBoard[thirdRow - 1][thirdCol - 1] != water) {
                    canPlaceThirdMast = false;
                }
                // Lewo-dół
                if (thirdRow < myBoard.length - 1 && thirdCol > 0 && myBoard[thirdRow + 1][thirdCol - 1] != water) {
                    canPlaceThirdMast = false;
                }
                // Prawo-góra
                if (thirdRow > 0 && thirdCol < myBoard[0].length - 1
                        && myBoard[thirdRow - 1][thirdCol + 1] != water) {
                    canPlaceThirdMast = false;
                }
                // Prawo-dół
                if (thirdRow < myBoard.length - 1 && thirdCol < myBoard[0].length - 1
                        && myBoard[thirdRow + 1][thirdCol + 1] != water) {
                    canPlaceThirdMast = false;
                }

                // Jeśli statki są zbyt blisko siebie, nie pozwalamy na umieszczenie statku
                if (!canPlaceThirdMast) {
                    printMyBoard(myBoard, ship);
                    System.out.println("Cannot place ship here. There is another ship nearby!".toUpperCase());
                    System.out.println();
                    continue;
                }

                myBoard[thirdRow][thirdCol] = '3';
                thirdMastIsNotPlaced = false;

                printMyBoard(myBoard, ship);
            }


            // ******************* INPUT AND VALIDATION FOR THE FOURTH MAST **********************

            boolean fourthMastIsNotPlaced = true;

            while (fourthMastIsNotPlaced) {

                System.out.printf(GameStateMessage.ENTER_COORDINATES_FOR_FOURTH_MAST.getMessage(),
                        placedFourMastedShips + 1, fourMastedShipNumber, "Four");
                String fourthInput = scanner.nextLine();

                boolean isValidFourthInput = validateInputFields(fourthInput, myBoard, ship);
                if (!isValidFourthInput) continue;

                char fourthColChar = fourthInput.charAt(0);

                String fourthRowNumber = fourthInput.substring(1);

                int fourthCol = Character.toUpperCase(fourthColChar) - 'A';
                int fourthRow = Integer.parseInt(fourthRowNumber) - 1;

                // Sprawdzenie czy trzeci maszt lezy dokladnie obok czwartego
                boolean isFourthMastAdjacent =
                        fourthRow == row && Math.abs(fourthCol - col) == 3 && Math.abs(fourthCol - thirdCol) == 1 ||
                                fourthCol == col && Math.abs(fourthRow - row) == 3 && Math.abs(fourthRow - thirdRow) == 1 ||
                                fourthRow == row && Math.abs(fourthCol - col) == 1 && Math.abs(fourthCol - secondCol) == 2 ||
                                fourthCol == col && Math.abs(fourthRow - row) == 1 && Math.abs(fourthRow - secondRow) == 2 ||
                                fourthRow == row && Math.abs(fourthCol - col) == 2 && Math.abs(fourthCol - secondCol) == 1 ||
                                fourthCol == col && Math.abs(fourthRow - row) == 2 && Math.abs(fourthRow - secondRow) == 1 ||
                                fourthRow == row && Math.abs(fourthCol - col) == 2 && Math.abs(fourthCol - thirdCol) == 1 ||
                                fourthCol == col && Math.abs(fourthRow - row) == 2 && Math.abs(fourthRow - thirdRow) == 1;

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


                myBoard[row][col] = ship;
                myBoard[secondRow][secondCol] = ship;
                myBoard[thirdRow][thirdCol] = ship;
                myBoard[fourthRow][fourthCol] = ship;

                fourthMastIsNotPlaced = false;

                Coordinate firstCoordinate = new Coordinate(row, col);
                Coordinate secondCoordinate = new Coordinate(secondRow, secondCol);
                Coordinate thirdCoordinate = new Coordinate(thirdRow, thirdCol);
                Coordinate fourthCoordinate = new Coordinate(fourthRow, fourthCol);
                FourMastedShip fourMastedShip =
                        new FourMastedShip(firstCoordinate, secondCoordinate, thirdCoordinate, fourthCoordinate);
                shipService.addShip(fourMastedShip);

            }

            placedFourMastedShips++;
        }

        printMyBoard(myBoard, ship);

        Thread.sleep(500);
        System.out.println(GameStateMessage.ALL_SHIPS_PLACED.getMessage());
        Thread.sleep(1000);
        System.out.println();
        System.out.println(GameStateMessage.WAITING_FOR_OPPONENT.getMessage());

        output.writeObject(ServerMessage.SHIPS_PLACED);

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
                } else if (position == ClientShipGameNetwork.miss) {
                    System.out.print("\u001B[33m" + position + "  " + "\u001B[0m");
                } else if (position == ClientShipGameNetwork.HIT_AND_SUNK_CHAR) {
                    System.out.print("\u001B[31m" + position + "  " + "\u001B[0m");
                } else if (position == ClientShipGameNetwork.HIT_MAST_CHAR) {
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
                } else if (position == ClientShipGameNetwork.miss) {
                    System.out.print("\u001B[33m" + position + "  " + "\u001B[0m");
                } else if (position == ClientShipGameNetwork.HIT_AND_SUNK_CHAR) {
                    System.out.print("\u001B[31m" + position + "  " + "\u001B[0m");
                } else if (position == ClientShipGameNetwork.HIT_MAST_CHAR) {
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
            } else if (position == ClientShipGameNetwork.miss) {
                System.out.print("\u001B[33m" + position + "  " + "\u001B[0m");
            } else if (position == ClientShipGameNetwork.HIT_AND_SUNK_CHAR) {
                System.out.print("\u001B[31m" + position + "  " + "\u001B[0m");
            } else if (position == ClientShipGameNetwork.HIT_MAST_CHAR) {
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
            } else if (position == ClientShipGameNetwork.miss) {
                System.out.print("\u001B[33m" + position + "  " + "\u001B[0m");
            } else if (position == ClientShipGameNetwork.HIT_AND_SUNK_CHAR) {
                System.out.print("\u001B[31m" + position + "  " + "\u001B[0m");
            } else if (position == ClientShipGameNetwork.HIT_MAST_CHAR) {
                System.out.print("\u001B[31m" + position + "  " + "\u001B[0m");
            } else {
                System.out.print("\u001B[34m" + position + "  " + "\u001B[0m");
            }

        }

        System.out.println();
        System.out.println();
    }

}



