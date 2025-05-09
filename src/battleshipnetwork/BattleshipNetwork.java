package battleshipnetwork;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class BattleshipNetwork {
    private static final int PORT = 5050;

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


            Thread.sleep(3000);

            // Informujemy graczy, że mogą zaczynać
            output1.println("START");
            output2.println("START");

            // Waiting for the players to finish placing ships
            String messageFromPlayerOne = input1.readLine();
            String messageFromPlayerTwo = input2.readLine();

            Thread.sleep(2000);

            if ("ships placed".equalsIgnoreCase(messageFromPlayerOne)
                    && "ship placed".equalsIgnoreCase(messageFromPlayerTwo)) {
                //output1.println("The war has begun.a");
                output1.println("Player one's turn.");
                output2.println("Please wait");
            }

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

                if (playerTwosReport.contains("already fired") || playerTwosReport.contains("Missed.")) {
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

            if (playerOnesReport.contains("already fired") || playerOnesReport.contains("Missed.")) {
                playerTwoShooting = false;

            }
        }
    }

}
