package client;

import battleshipnetwork.MessagePrinter;

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
        System.out.println("Place your four One-Masted Ships");

        while (placedShips < singleMastedShipNumber) {
            System.out.println();
            printThisClientGameBoard(gameBoard, ship);
            System.out.printf("Enter coordinates for Ship %d of 4 (One-Masted) (e.g. A5):%n", placedShips + 1);
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
                System.out.println("The first character should be a letter.");
                continue;
            }

            if (!Character.isDigit(firstCharOfRowNumber)) {
                System.out.println("The second character should be a digit");
                continue;
            }

            int col = Character.toUpperCase(colChar) - 'A';
            int row = Integer.parseInt(rowNumber) - 1;

            char possiblePlacement = gameBoard[row][col];

            if (possiblePlacement != water) {
                System.out.println("Cannot place ship here, position already taken!");
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
                System.out.println("Cannot place ship here. There is another ship nearby.");
                continue;
            }
            gameBoard[row][col] = ship;
            placedShips++;
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("All single-masted ships have been placed".toUpperCase());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return placeTwoMastedShips(gameBoard, water, ship, scanner, output);

    }

    private static char[][] placeTwoMastedShips(
            char[][] gameBoard, char water, char ship, Scanner scanner, PrintWriter output) {

        int placedShips = 0;
        System.out.println();
        System.out.println("Place your three Two-Masted Ships");

        while (placedShips < twoMastedShipNumber) {
            System.out.println();
            printThisClientGameBoard(gameBoard, ship);
            System.out.printf("Enter coordinates for Ship %d of 3 (Two-Masted) (e.g. A5):%n", placedShips + 1);
            String input = scanner.nextLine();


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



