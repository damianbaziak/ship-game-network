package battleship;

public class MessageDisplayer {
    public static void displayGreeting() {
        String[] WELCOME = {
                "#     # ####### #        #####   ####### #     # #######",
                "#  #  # #       #       #     #  #     # ##   ## #",
                "#  #  # #       #       #        #     # # # # # #",
                "#  #  # #####   #       #        #     # #  #  # #####",
                "#  #  # #       #       #        #     # #     # #",
                "#  #  # #       #       #     #  #     # #     # #",
                " ## ##  ####### #######  #####   ####### #     # #######"
        };

        String[] TO = {
                "                ####### #######                         ",
                "                   #    #     #                         ",
                "                   #    #     #                         ",
                "                   #    #     #                         ",
                "                   #    #     #                         ",
                "                   #    #     #                         ",
                "                   #    #######                         "
        };

        String[] THE = {
                "              ####### #     # #######                   ",
                "                 #    #     # #                         ",
                "                 #    #     # #                         ",
                "                 #    ####### #####                     ",
                "                 #    #     # #                         ",
                "                 #    #     # #                         ",
                "                 #    #     # #######                   "
        };

        String[] shipGAME = {
                " #####  #     # ### ######      #####     #    #     # #######",
                "#     # #     #  #  #     #    #     #   # #   ##   ## #       ",
                "#       #     #  #  #     #    #        #   #  # # # # #       ",
                " #####  #######  #  ######     #  #### #     # #  #  # #####   ",
                "      # #     #  #  #          #     # ####### #     # #       ",
                "#     # #     #  #  #          #     # #     # #     # #       ",
                " #####  #     # ### #           #####  #     # #     # ####### "
        };

        // Wypisanie każdej linii z opóźnieniem
        printGreeting(WELCOME);
        System.out.println();
        printGreeting(TO);
        System.out.println();
        printGreeting(THE);
        System.out.println();
        printGreeting(shipGAME);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        clearScreen();
    }

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

    private static void clearScreen() {
        for (int i = 0; i < 40; i++) {
            System.out.println();
        }
    }

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

    private static void printGreeting(String[] strings) {
        for (int s = 0; s < 7; s++) {
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


    }


}
