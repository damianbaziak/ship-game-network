package battleshipnetwork;

import client.ship.Ship;
import client.ship.SingleMastedShip;
import client.ship.service.Coordinate;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BattleshipNetwork {
    private static final int PORT = 5050;
    private static final String YOU_TURN = "Your turn.";
    private static final String START = "START";
    private static final String PLEASE_WAIT = "Please wait.";
    private static final String ALREADY_FIRED = "already fired";
    private static final String MISSED = "Missed.";
    private static final String SHIPS_PLACED = "Ships placed.";
    private static final String THE_WAR_HAS_BEGUN = "The war has begun.";
    private static final String YOU_WIN = "You win";
    private static final String PLAYER_TWO_WIN = "Player 2 win";
    private static final String GAME_OVER = "Game over.";

    public static void main(String[] args) throws IOException {

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running, Waiting for the players...");

            // Waiting for the players
            Socket player1 = serverSocket.accept();
            System.out.println("Player 1 connected " + player1.getInetAddress());


            Socket player2 = serverSocket.accept();
            System.out.println("Player 2 connected " + player2.getInetAddress());


            // We create communication streams
            ObjectInputStream input1 = new ObjectInputStream(player1.getInputStream());
            ObjectOutputStream output1 = new ObjectOutputStream(player1.getOutputStream());

            ObjectInputStream input2 = new ObjectInputStream(player2.getInputStream());
            ObjectOutputStream output2 = new ObjectOutputStream(player2.getOutputStream());


            Thread.sleep(1000);

            // Informujemy graczy, że mogą zaczynać
            output1.writeObject(START);
            output2.writeObject(START);

            // Waiting for the players to finish placing ships
            String messageFromPlayerOne = (String) input1.readObject();
            String messageFromPlayerTwo = (String) input2.readObject();
            System.out.println(messageFromPlayerOne);
            System.out.println(messageFromPlayerTwo);

            // Thread.sleep(2000);

            if (isReady(messageFromPlayerOne) && isReady(messageFromPlayerTwo)) {
                output1.writeObject(THE_WAR_HAS_BEGUN);
                output2.writeObject(THE_WAR_HAS_BEGUN);
                Thread.sleep(1000);
            }
            output1.writeObject(YOU_TURN);
            output2.writeObject(PLEASE_WAIT);

            boolean gameRunning = true;
            while (gameRunning) {
                String playerOnesShot = (String) input1.readObject();
                output2.writeObject(playerOnesShot);

                String playerTwosReport = (String) input2.readObject();
                String playerTwosSecondReport = (String) input2.readObject();
                Ship playerTwosThirdReport = (Ship) input2.readObject();
                String playerTwosFourthReport = (String) input2.readObject();
                String playerTwosFifthReport = (String) input2.readObject();

                output1.writeObject(playerTwosReport);
                output1.writeObject(playerTwosSecondReport);
                output1.writeObject(playerTwosThirdReport);
                output1.writeObject(playerTwosFourthReport);
                output1.writeObject(playerTwosFifthReport);

                String playerTwosTurn = "";

                if (playerTwosReport.contains(ALREADY_FIRED) || playerTwosReport.contains(MISSED)) {
                    output2.writeObject(YOU_TURN);
                    output1.writeObject(PLEASE_WAIT);
                    playerTwosTurn = playerTwoShooting(input2, input1, output2, output1);
                }

                if (playerTwosFifthReport.equals(YOU_WIN) || (playerTwosTurn != null &&
                        playerTwosTurn.equals(PLAYER_TWO_WIN))) {
                    gameRunning = false;
                }

            }

            output2.writeObject(GAME_OVER);
            output1.writeObject(GAME_OVER);

            Thread.sleep(2000);


        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static String playerTwoShooting(
            ObjectInputStream input2, ObjectInputStream input1, ObjectOutputStream output2, ObjectOutputStream output1)
            throws IOException, ClassNotFoundException {

        boolean playerTwoIsShooting = true;
        while (playerTwoIsShooting) {
            String playerTwosShot = (String) input2.readObject();
            output1.writeObject(playerTwosShot);

            String playerOnesReport = (String) input1.readObject();
            String playerOnesSecondReport = (String) input1.readObject();
            Ship playerOnesThirdReport = (Ship) input1.readObject();
            String playerOnesFourthReport = (String) input1.readObject();
            String playerOnesFifthReport = (String) input1.readObject();

            output2.writeObject(playerOnesReport);
            output2.writeObject(playerOnesSecondReport);
            output2.writeObject(playerOnesThirdReport);
            output2.writeObject(playerOnesFourthReport);
            output2.writeObject(playerOnesFifthReport);

            if (playerOnesReport.contains(ALREADY_FIRED) || playerOnesReport.contains(MISSED)) {
                output1.writeObject(YOU_TURN);
                output2.writeObject(PLEASE_WAIT);
                playerTwoIsShooting = false;
            }

            if (playerOnesFourthReport.contains(YOU_WIN)) {
                return PLAYER_TWO_WIN;
            }
        }
        return null;
    }

    private static boolean isReady(String message) {
        return SHIPS_PLACED.equalsIgnoreCase(message);
    }

}
