package battleshipnetwork;

public class MessagePrinter {

    private static final String[] YOU_LOSE_STRING = {
            "#     # ####### #     #    #       #######  #####  #######    ### ### ###",
            " #   #  #     # #     #    #       #     # #     # #          ### ### ###",
            "  # #   #     # #     #    #       #     # #       #          ### ### ###",
            "   #    #     # #     #    #       #     #  #####  #####       #   #   # ",
            "   #    #     # #     #    #       #     #       # #                     ",
            "   #    #     # #     #    #       #     # #     # #          ### ### ###",
            "   #    #######  #####     ####### #######  #####  #######    ### ### ###",


    };

    private static final String[] YOU_WIN_STRING = {
            "#     # ####### #     #    #     # ### #     #    ### ### ###",
            " #   #  #     # #     #    #  #  #  #  ##    #    ### ### ###",
            "  # #   #     # #     #    #  #  #  #  # #   #    ### ### ###",
            "   #    #     # #     #    #  #  #  #  #  #  #     #   #   # ",
            "   #    #     # #     #    #  #  #  #  #   # #               ",
            "   #    #     # #     #    #  #  #  #  #    ##    ### ### ###",
            "   #    #######  #####      ## ##  ### #     #    ### ### ###"
    };
    private static final String[] GREETING_STRING = {
            "         #     # ####### #        #####   ####### #     # #######          ",
            "         #  #  # #       #       #     #  #     # ##   ## #                ",
            "         #  #  # #       #       #        #     # # # # # #                ",
            "         #  #  # #####   #       #        #     # #  #  # #####            ",
            "         #  #  # #       #       #        #     # #     # #                ",
            "         #  #  # #       #       #     #  #     # #     # #                ",
            "          ## ##  ####### #######  #####   ####### #     # #######           ",
            "                                                                           ",
            "                                                                           ",
            "                          ####### #######                                  ",
            "                             #    #     #                                  ",
            "                             #    #     #                                  ",
            "                             #    #     #                                  ",
            "                             #    #     #                                  ",
            "                             #    #     #                                  ",
            "                             #    #######                                  ",
            "                                                                           ",
            "                                                                           ",
            "                        ####### #     # #######                            ",
            "                           #    #     # #                                  ",
            "                           #    #     # #                                  ",
            "                           #    ####### #####                              ",
            "                           #    #     # #                                  ",
            "                           #    #     # #                                  ",
            "                           #    #     # #######                            ",
            "                                                                           ",
            "                                                                           ",
            "######     #    ####### ####### #       #######  #####  #     # ### ###### ",
            "#     #   # #      #       #    #       #       #     # #     #  #  #     #",
            "#     #  #   #     #       #    #       #       #       #     #  #  #     #",
            "######  #     #    #       #    #       #####    #####  #######  #  ###### ",
            "#     # #######    #       #    #       #             # #     #  #  #      ",
            "#     # #     #    #       #    #       #       #     # #     #  #  #      ",
            "######  #     #    #       #    ####### #######  #####  #     # ### #      "
    };


    // =========================
    // ===      METHODS      ===
    // =========================


    public static void displayGreeting() throws InterruptedException {
        clearScreen();
        for (String s : GREETING_STRING) {
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

    public static void displayMiss() {
        String[] miss = {
                "╔╦╗ ╦ ╔═╗ ╔═╗  ┬",
                "║║║ ║ ╚═╗ ╚═╗  │",
                "╩ ╩ ╩ ╚═╝ ╚═╝  o"
        };
        printMessage(miss);
    }

    public static void displayHit() {
        String[] hit = {
                "╦ ╦ ╦ ╔╦╗  ┬",
                "╠═╣ ║  ║   │",
                "╩ ╩ ╩  ╩   o"
        };
        printMessage(hit);
    }

    public static void displayAlreadyHit() {
        String[] alreadyHit = {
                "╔═╗ ╦   ╦═╗ ╔═╗ ╔═╗╔╦╗ ╦ ╦   ╦ ╦ ╦ ╔╦╗ ┬",
                "╠═╣ ║   ╠╦╝ ║╣  ╠═╣ ║║ ╚╦╝   ╠═╣ ║  ║  │",
                "╩ ╩ ╩═╝ ╩╚═ ╚═╝ ╩ ╩═╩╝  ╩    ╩ ╩ ╩  ╩  o"
        };
        printMessage(alreadyHit);
    }

    public static void displayYouWin() {
        printMessage(YOU_WIN_STRING);
    }

    public static void displayYouLose() {
        printMessage(YOU_LOSE_STRING);
    }

    // =========================
    // === AUXILIARY METHODS ===

    // =========================

    /*
    private static void makeSpace() {
        for (int i = 0; i < 3; i++) {
            System.out.println();
        }
    }

     */

    private static void printMessage(String[] strings) {
        for (String s : strings) {
            for (char c : s.toCharArray()) {
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
        //makeSpace();

    }

    private static void clearScreen() {
        for (int i = 0; i < 40; i++) {
            System.out.println();
        }
    }

}
