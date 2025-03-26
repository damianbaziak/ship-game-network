package battleshipnetwork;

import java.io.PrintWriter;

public class MessagePrinter {
    private static final String[] GreetingStatic = {
            "#     # ####### #        #####   ####### #     # #######",
            "#  #  # #       #       #     #  #     # ##   ## #",
            "#  #  # #       #       #        #     # # # # # #",
            "#  #  # #####   #       #        #     # #  #  # #####",
            "#  #  # #       #       #        #     # #     # #",
            "#  #  # #       #       #     #  #     # #     # #",
            " ## ##  ####### #######  #####   ####### #     # #######",
            "                                                        ",
            "                                                        ",
            "                ####### #######                         ",
            "                   #    #     #                         ",
            "                   #    #     #                         ",
            "                   #    #     #                         ",
            "                   #    #     #                         ",
            "                   #    #     #                         ",
            "                   #    #######                         ",
            "                                                        ",
            "                                                        ",
            "              ####### #     # #######                   ",
            "                 #    #     # #                         ",
            "                 #    #     # #                         ",
            "                 #    ####### #####                     ",
            "                 #    #     # #                         ",
            "                 #    #     # #                         ",
            "                 #    #     # #######                   ",
            "                                                        ",
            "                                                        ",
            " #####  #     # ### ######      #####     #    #     # #######",
            "#     # #     #  #  #     #    #     #   # #   ##   ## #       ",
            "#       #     #  #  #     #    #        #   #  # # # # #       ",
            " #####  #######  #  ######     #  #### #     # #  #  # #####   ",
            "      # #     #  #  #          #     # ####### #     # #       ",
            "#     # #     #  #  #          #     # #     # #     # #       ",
            " #####  #     # ### #           #####  #     # #     # ####### "
    };

    public static void displayMiss() {
        String[] miss = {
                "╔╦╗ ╦ ╔═╗ ╔═╗  ┬",
                "║║║ ║ ╚═╗ ╚═╗  │",
                "╩ ╩ ╩ ╚═╝ ╚═╝  o"
        };
        printShotMessages(miss);
    }

    public static void displayHit() {
        String[] hit = {
                "╦ ╦ ╦ ╔╦╗  ┬",
                "╠═╣ ║  ║   │",
                "╩ ╩ ╩  ╩   o"
        };
        printShotMessages(hit);
    }

    public static void displayAlreadyHit() {
        String[] alreadyHit = {
                "╔═╗ ╦   ╦═╗ ╔═╗ ╔═╗╔╦╗ ╦ ╦   ╦ ╦ ╦ ╔╦╗ ┬",
                "╠═╣ ║   ╠╦╝ ║╣  ╠═╣ ║║ ╚╦╝   ╠═╣ ║  ║  │",
                "╩ ╩ ╩═╝ ╩╚═ ╚═╝ ╩ ╩═╩╝  ╩    ╩ ╩ ╩  ╩  o"
        };
        printShotMessages(alreadyHit);
    }


    // =========================
    // === AUXILIARY METHODS ===
    // =========================

    private static void makeSpace() {
        for (int i = 0; i < 3; i++) {
            System.out.println();
        }
    }

    private static void printShotMessages(String[] strings) {
        for (int s = 0; s < 3; s++) {
            for (char c : strings[s].toCharArray()) {
                System.out.print(c);
                System.out.flush();
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println();
        }
        makeSpace();

    }

    public static void printGreeting() throws InterruptedException {
        clearScreen();
        for (String s : GreetingStatic) {
            for (char c : s.toCharArray()) {
                System.out.print(c);
                System.out.flush();
                Thread.sleep(1);
            }
            System.out.println();
        }
        Thread.sleep(3000);
        clearScreen();


        System.out.print("LET'S START.");
        Thread.sleep(1000);
        System.out.print(".");
        Thread.sleep(1000);
        System.out.print(".");
        Thread.sleep(1000);
        clearScreen();
    }


    private static void clearScreen() {
        for (int i = 0; i < 40; i++) {
            System.out.println();
        }
    }

}
