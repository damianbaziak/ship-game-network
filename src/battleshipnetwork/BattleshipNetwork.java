package battleshipnetwork;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class BattleshipNetwork {
    private static final int PORT = 5050;
    private static final String YOU_TURN = "Your turn.";
    private static final String START = "START";
    private static final String PLEASE_WAIT = "Please wait.";
    private static final String ALREADY_FIRED = "already fired";
    private static final String MISSED = "Missed.";
    private static final String SHIPS_PLACED= "Ships placed.";
    private static final String THE_WAR_HAS_BEGUN= "The war has begun.";

    public static void main(String[] args) throws IOException {

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running, Waiting for the players...");

            // Waiting for the players
            Socket player1 = serverSocket.accept();
            System.out.println("Player 1 connected " + player1.getInetAddress());


            Socket player2 = serverSocket.accept();
            System.out.println("Player 2 connected " + player2.getInetAddress());


            // We create communication streams
            BufferedReader input1 = new BufferedReader(new InputStreamReader(player1.getInputStream()));
            PrintWriter output1 = new PrintWriter(player1.getOutputStream(), true);


            BufferedReader input2 = new BufferedReader(new InputStreamReader(player2.getInputStream()));
            PrintWriter output2 = new PrintWriter(player2.getOutputStream(), true);


            Thread.sleep(1000);

            // Informujemy graczy, że mogą zaczynać
            output1.println(START);
            output2.println(START);

            // Waiting for the players to finish placing ships
            String messageFromPlayerOne = input1.readLine();
            String messageFromPlayerTwo = input2.readLine();
            System.out.println(messageFromPlayerTwo);
            System.out.println(messageFromPlayerOne);

            // Thread.sleep(2000);

            if (isReady(messageFromPlayerOne) && isReady(messageFromPlayerTwo)) {
                output1.println(THE_WAR_HAS_BEGUN);
                output2.println(THE_WAR_HAS_BEGUN);
                Thread.sleep(1000);
            }
            output1.println(YOU_TURN);
            output2.println(PLEASE_WAIT);

            boolean gameRunning = true;
            while (gameRunning) {
                String playerOnesShot = input1.readLine();
                output2.println(playerOnesShot);

                String playerTwosReport = input2.readLine();
                String playerTwosSecondReport = input2.readLine();
                String playerTwosThirdReport = input2.readLine();
                output1.println(playerTwosReport);
                output1.println(playerTwosSecondReport);
                output1.println(playerTwosThirdReport);

                if (playerTwosReport.contains(ALREADY_FIRED) || playerTwosReport.contains(MISSED)) {
                    output2.println(YOU_TURN);
                    output1.println(PLEASE_WAIT);
                    playerTwoShooting(input2, input1, output2, output1);
                }
            }


        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private static void playerTwoShooting(
            BufferedReader input2, BufferedReader input1, PrintWriter output2, PrintWriter output1) throws
            IOException {
        boolean playerTwoShooting = true;
        while (playerTwoShooting) {
            String playerTwosShot = input2.readLine();
            output1.println(playerTwosShot);

            String playerOnesReport = input1.readLine();
            String playerOnesSecondReport = input1.readLine();
            String playerOnesThirdReport = input1.readLine();
            output2.println(playerOnesReport);
            output2.println(playerOnesSecondReport);
            output2.println(playerOnesThirdReport);

            if (playerOnesReport.contains(ALREADY_FIRED) || playerOnesReport.contains(MISSED)) {
                output1.println(YOU_TURN);
                output2.println(PLEASE_WAIT);
                playerTwoShooting = false;

            }
        }
    }

    private static boolean isReady(String message) {
        return SHIPS_PLACED.equalsIgnoreCase(message);
    }

}
