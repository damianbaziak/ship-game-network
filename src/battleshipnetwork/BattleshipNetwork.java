package battleshipnetwork;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

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

            // Waiting for player 1
            Socket player1 = serverSocket.accept();
            System.out.println("Player 1 connected " + player1.getInetAddress());
            // Create communication streams for Player 1
            ObjectInputStream input1 = new ObjectInputStream(player1.getInputStream());
            ObjectOutputStream output1 = new ObjectOutputStream(player1.getOutputStream());

            /*
            output1.writeObject(START);

             */

            // Waiting for player 2
            Socket player2 = serverSocket.accept();
            System.out.println("Player 2 connected " + player2.getInetAddress());
            // Create communication streams for Player 2
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
                Object playerOnesShot = input1.readObject();
                output2.writeObject(playerOnesShot);

                Object playerTwosReport = input2.readObject();
                Object playerTwosSecondReport = input2.readObject();
                Object playerTwosThirdReport = input2.readObject();
                Object playerTwosFourthReport = input2.readObject();
                Object playerTwosFifthReport = input2.readObject();

                output1.writeObject(playerTwosReport);
                output1.writeObject(playerTwosSecondReport);
                output1.writeObject(playerTwosThirdReport);
                output1.writeObject(playerTwosFourthReport);
                output1.writeObject(playerTwosFifthReport);

                String playerTwosTurn = "";

                if (playerTwosReport != null) {
                    if (playerTwosReport.toString().contains(ALREADY_FIRED)
                            || playerTwosReport.toString().contains(MISSED)) {
                        output2.writeObject(YOU_TURN);
                        output1.writeObject(PLEASE_WAIT);
                        playerTwosTurn = playerTwoShooting(input2, input1, output2, output1);
                    }
                }

                if (playerTwosFifthReport != null && playerTwosFifthReport.equals(YOU_WIN) || (playerTwosTurn != null &&
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
            Object playerTwosShot = input2.readObject();
            output1.writeObject(playerTwosShot);

            Object playerOnesReport = input1.readObject();
            Object playerOnesSecondReport = input1.readObject();
            Object playerOnesThirdReport = input1.readObject();
            Object playerOnesFourthReport = input1.readObject();
            Object playerOnesFifthReport = input1.readObject();

            output2.writeObject(playerOnesReport);
            output2.writeObject(playerOnesSecondReport);
            output2.writeObject(playerOnesThirdReport);
            output2.writeObject(playerOnesFourthReport);
            output2.writeObject(playerOnesFifthReport);

            if (playerOnesReport != null) {
                if (playerOnesReport.toString().contains(ALREADY_FIRED) || playerOnesReport.toString().contains(MISSED)) {
                    output1.writeObject(YOU_TURN);
                    output2.writeObject(PLEASE_WAIT);
                    playerTwoIsShooting = false;
                }
            }

            if (playerOnesFifthReport != null && playerOnesFifthReport.toString().contains(YOU_WIN)) {
                return PLAYER_TWO_WIN;
            }
        }
        return null;
    }

    private static boolean isReady(String message) {
        return SHIPS_PLACED.equalsIgnoreCase(message);
    }

}
