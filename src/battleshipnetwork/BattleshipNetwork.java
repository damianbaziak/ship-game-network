package battleshipnetwork;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class BattleshipNetwork {
    private static final int PORT = 5050;

    public static void main(String[] args) {

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running, Waiting for the players...");

            // Waiting for the players
            Socket player1 = serverSocket.accept();
            System.out.println("Player 1 connected " + player1.getInetAddress());

            /*
            Socket player2 = serverSocket.accept();
            System.out.println("Player 2 connected " + player2.getInetAddress());

             */


            // We create communication streams
            BufferedReader input1 = new BufferedReader(new InputStreamReader(player1.getInputStream()));
            PrintWriter output1 = new PrintWriter(player1.getOutputStream(), true);

            /*
            BufferedReader input2 = new BufferedReader(new InputStreamReader(player2.getInputStream()));
            PrintWriter output2 = new PrintWriter(player2.getOutputStream(), true);

             */
            Thread.sleep(3000);

            // Informujemy graczy, że mogą zaczynać
            output1.println("START");
            // output2.println("START");

            // Waiting for the players to finish placing ships

            String messageFromPlayerOne = input1.readLine();
            //String messageFromPlayerTwo = input2.readLine();

            Thread.sleep(2000);

            if ("ships placed".equalsIgnoreCase(messageFromPlayerOne)) {
                output1.println("The war has begun.");
                output1.println("Player 1 starts");
                //output2.println("Please wait");
            }



        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}