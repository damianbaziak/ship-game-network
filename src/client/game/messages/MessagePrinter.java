package client.game.messages;

public class MessagePrinter {

    private static final String[] YOU_LOSE_STRING = {
            "#     # ####### #     #    #       #######  #####  #######    ###",
            " #   #  #     # #     #    #       #     # #     # #          ###",
            "  # #   #     # #     #    #       #     # #       #          ###",
            "   #    #     # #     #    #       #     #  #####  #####       # ",
            "   #    #     # #     #    #       #     #       # #             ",
            "   #    #     # #     #    #       #     # #     # #          ###",
            "   #    #######  #####     ####### #######  #####  #######    ###",


    };
    private static final String[] YOU_WIN_STRING = {
            "#     # ####### #     #    #     # ### #     #    ###",
            " #   #  #     # #     #    #  #  #  #  ##    #    ###",
            "  # #   #     # #     #    #  #  #  #  # #   #    ###",
            "   #    #     # #     #    #  #  #  #  #  #  #     # ",
            "   #    #     # #     #    #  #  #  #  #   # #       ",
            "   #    #     # #     #    #  #  #  #  #    ##    ###",
            "   #    #######  #####      ## ##  ### #     #    ###"
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

    private static final String[] LETS_START = {
            "#       ####### ####### ###  #####      #####  #######    #    ######  #######   ###",
            "#       #          #    ### #     #    #     #    #      # #   #     #    #      ###",
            "#       #          #     #  #          #          #     #   #  #     #    #      ###",
            "#       #####      #    #    #####      #####     #    #     # ######     #       # ",
            "#       #          #              #          #    #    ####### #   #      #         ",
            "#       #          #        #     #    #     #    #    #     # #    #     #      ###",
            "####### #######    #         #####      #####     #    #     # #     #    #      ###",
    };


    // =========================
    // ===      METHODS      ===
    // =========================


    public static void printGreeting() throws InterruptedException {
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
    }

    public static void displayLetsStart() throws InterruptedException {
        for (int i = 0; i < 2; i++) {
            System.out.println();
        }
        printMessage(LETS_START);
        Thread.sleep(2000);
        clearScreen();
    }

    public static void displayMiss() throws InterruptedException {
        String[] miss = {
                "╔╦╗ ╦ ╔═╗ ╔═╗  ┬",
                "║║║ ║ ╚═╗ ╚═╗  │",
                "╩ ╩ ╩ ╚═╝ ╚═╝  o"
        };
        printMessage(miss);
    }

    public static void printHit() {
        String[] hit = {
                "╦ ╦ ╦ ╔╦╗  ┬",
                "╠═╣ ║  ║   │",
                "╩ ╩ ╩  ╩   o"
        };
        printMessage(hit);
    }

    public static void printAlreadyHit() throws InterruptedException {
        String[] alreadyHit = {
                "╔═╗ ╦   ╦═╗ ╔═╗ ╔═╗╔╦╗ ╦ ╦   ╦ ╦ ╦ ╔╦╗ ┬",
                "╠═╣ ║   ╠╦╝ ║╣  ╠═╣ ║║ ╚╦╝   ╠═╣ ║  ║  │",
                "╩ ╩ ╩═╝ ╩╚═ ╚═╝ ╩ ╩═╩╝  ╩    ╩ ╩ ╩  ╩  o"
        };
        printMessage(alreadyHit);
    }

    public static void printYouWin() {
        makeSpace();
        printMessage(YOU_WIN_STRING);
    }

    public static void printYouLose() {
        makeSpace();
        printMessage(YOU_LOSE_STRING);
    }


    // =========================
    // === AUXILIARY METHODS ===
    // =========================


    private static void makeSpace() {
        for (int i = 0; i < 3; i++) {
            System.out.println();
        }
    }

    private static void printMessage(String[] strings) {
        System.out.println();
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
        System.out.println();
    }

    private static void clearScreen() {
        for (int i = 0; i < 40; i++) {
            System.out.println();
        }
    }

}
