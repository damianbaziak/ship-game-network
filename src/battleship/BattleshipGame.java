package battleship;

import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class BattleshipGame {

    public static void main(String[] args) {
        int gameBoardLength = 10;
        char water = '-';
        char ship = 's';
        char hit = 'X';
        char miss = '0';
        int shipNumber = 4;
        int twoMastedShipNumber = 3;
        int threeMastedShipNumber = 2;

        Scanner scanner = new Scanner(System.in);

        char[][] gameBoard = createGameBoard(gameBoardLength, water, ship, shipNumber, twoMastedShipNumber,
                threeMastedShipNumber);


        MessageDisplayer.displayGreeting();
        displayGameboard(gameBoard);
        printGameBoard(gameBoard, water, ship);
        int undetectedShipNumber = 16; // (shipNumber + twoMastedShipNumber + threeMastedShipNumber);
        while (undetectedShipNumber > 0) {
            int[] guessCoordinates = getUserCoordinates(gameBoardLength, scanner);
            char locationViewUpdate = evaluateGuessAndGetTheTarget(guessCoordinates, gameBoard, ship, water, hit, miss);
            if (locationViewUpdate == hit) {
                undetectedShipNumber--;
            }
            gameBoard = updateGameBoard(gameBoard, guessCoordinates, locationViewUpdate);
            printGameBoard(gameBoard, water, ship);
        }
        System.out.println("You Won!");

        scanner.close();
    }

    private static char[][] updateGameBoard(char[][] gameBoard, int[] guessCoordinates, char locationViewUpdate) {
        int row = guessCoordinates[0];
        int col = guessCoordinates[1];
        gameBoard[row][col] = locationViewUpdate;
        return gameBoard;
    }

    private static char evaluateGuessAndGetTheTarget(int[] guessCoordinates, char[][] gameBoard, char ship,
                                                     char water,
                                                     char hit, char miss) {
        int row = guessCoordinates[0];
        int col = guessCoordinates[1];
        String message = null;
        char target = gameBoard[row][col];
        if (target == ship) {
            MessageDisplayer.displayHit();
            target = hit;
        } else if (target == water) {
            MessageDisplayer.displayMiss();
            target = miss;
        } else {
            MessageDisplayer.displayAlreadyHit();
        }
        System.out.println(message);
        return target;
    }

    private static int[] getUserCoordinates(int gameBoardLength, Scanner scanner) {
        int row;
        int col;
        do {
            System.out.println("Row: ");
            row = scanner.nextInt();
        } while (row < 1 || row > gameBoardLength + 1);
        do {
            System.out.println("Column: ");
            col = scanner.nextInt();
        } while (col < 1 || col > gameBoardLength + 1);

        return new int[]{row - 1, col - 1};
    }

    private static void printGameBoard(char[][] gameBoard, char water, char ship) {
        // Ten kod wyswietla numeracje kolumn
        int gameBoardLength = gameBoard.length;
        System.out.print("   ");
        for (int i = 0; i < gameBoardLength; i++) {
            System.out.print(i + 1 + "  ");
        }
        System.out.println();
        // Ten kod wyswietla wszystkie wiersze bez dziesiatego
        for (int row = 0; row < 9; row++) {
            System.out.print(" ");
            System.out.print(row + 1 + " ");
            for (int col = 0; col < gameBoardLength; col++) {
                char position = gameBoard[row][col];
                if (position == ship) {
                    System.out.print(water + "  ");
                } else {
                    System.out.print(position + "  ");
                }
            }
            System.out.println();
        }
        // Ten kod wyswitla dziesiaty wiersz
        System.out.print(10 + " ");
        for (int col = 0; col < gameBoardLength; col++) {
            char position = gameBoard[9][col];
            if (position == ship) {
                System.out.print(water + "  ");
            } else {
                System.out.print(position + "  ");
            }
        }
        // ***
        System.out.println();
    }

    private static char[][] createGameBoard(int gameBoardLength, char water, char ship, int shipNumber,
                                            int twoMastedShipNumber, int threeMastedShipNumber) {
        char[][] gameBoard = new char[gameBoardLength][gameBoardLength];
        for (char[] row : gameBoard) {
            Arrays.fill(row, water);
        }
        return placeShips(gameBoard, threeMastedShipNumber, twoMastedShipNumber, shipNumber, water, ship);
    }

    private static char[][] placeShips(char[][] gameBoard, int threeMastedShipNumber, int twoMastedShipNumber,
                                       int shipNumber, char water, char ship) {
        int placedShips = 0;
        int gameBoardLength = gameBoard.length;
        while (placedShips < shipNumber) {
            int[] location = generateShipCoordinates(gameBoardLength);
            int row = location[0];
            int col = location[1];
            char possiblePlacement = gameBoard[row][col];

            if (possiblePlacement == water) {
                if (row == 0 && col == 0) {
                    if (gameBoard[row + 1][col] == water && gameBoard[row][col + 1] == water) {
                        gameBoard[row][col] = ship;
                        placedShips++;
                    }

                } else if (row > 0 && row < 9 && col == 0) {
                    if (gameBoard[row - 1][col] == water && gameBoard[row + 1][col] == water
                            && gameBoard[row][col + 1] == water) {
                        gameBoard[row][col] = ship;
                        placedShips++;
                    }

                } else if (row == 9 && col == 0) {
                    if (gameBoard[row - 1][col] == 0 && gameBoard[row][col + 1] == water) {
                        gameBoard[row][col] = ship;
                        placedShips++;
                    }

                } else if (row == 9 && col < 9) {
                    if (gameBoard[row][col - 1] == water && gameBoard[row][col + 1] == water
                            && gameBoard[row - 1][col] == water) {
                        gameBoard[row][col] = ship;
                        placedShips++;
                    }

                } else if (row == 9 && col == 9) {
                    if (gameBoard[row][col - 1] == 0 && gameBoard[row - 1][col] == water) {
                        gameBoard[row][col] = ship;
                        placedShips++;
                    }

                } else if (col == 9 && row > 0 && row < 9) {
                    if (gameBoard[row - 1][col] == water && gameBoard[row + 1][col] == water
                            && gameBoard[row][col - 1] == water) {
                        gameBoard[row][col] = ship;
                        placedShips++;
                    }

                } else if (row == 0 && col == 9) {
                    if (gameBoard[row + 1][col] == water && gameBoard[row][col - 1] == water) {
                        gameBoard[row][col] = ship;
                        placedShips++;
                    }

                } else if (row == 0 && col < 9) {
                    if (gameBoard[row + 1][col] == water && gameBoard[row][col - 1] == water
                            && gameBoard[row][col + 1] == water) {
                        gameBoard[row][col] = ship;
                        placedShips++;
                    }

                } else if (row > 0 && row < 9 && col < 9) {
                    if (gameBoard[row - 1][col] == water && gameBoard[row + 1][col] == water
                            && gameBoard[row][col - 1] == water && gameBoard[row][col + 1] == water) {
                        gameBoard[row][col] = ship;
                        placedShips++;
                    }

                }
            }
        }

        return placeTwoMastedShips(gameBoard, threeMastedShipNumber, twoMastedShipNumber, water, ship);
    }

    private static char[][] placeTwoMastedShips(char[][] gameBoard, int threeMastedShipNumber,
                                                int twoMastedShipNumber,
                                                char water, char ship) {
        int placedShips = 0;
        int gameBoardLength = gameBoard.length;
        while (placedShips < twoMastedShipNumber) {
            int[] location = generateShipCoordinates(gameBoardLength);
            int row = location[0];
            int col = location[1];
            char possiblePlacement = gameBoard[row][col];

            // Pionowo w dol
            if (possiblePlacement == water && row + 1 < gameBoardLength && gameBoard[row + 1][col] != ship) {

                if (row + 2 < gameBoardLength && col + 1 < gameBoardLength && col - 1 >= 0 && row - 1 >= 0) {
                    if (gameBoard[row + 2][col] == water
                            && gameBoard[row][col + 1] == water
                            && gameBoard[row][col - 1] == water
                            && gameBoard[row + 1][col + 1] == water
                            && gameBoard[row + 1][col - 1] == water
                            && gameBoard[row - 1][col] == water
                            && gameBoard[row - 1][col - 1] == water
                            && gameBoard[row - 1][col + 1] == water
                            && gameBoard[row + 2][col + 1] == water
                            && gameBoard[row + 2][col - 1] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row + 1][col] = ship;
                        placedShips++;
                    }
                } else if (row + 1 == 9 && col + 1 < gameBoardLength && col - 1 >= 0) {
                    if (gameBoard[row][col + 1] == water
                            && gameBoard[row][col - 1] == water
                            && gameBoard[row + 1][col + 1] == water
                            && gameBoard[row + 1][col - 1] == water
                            && gameBoard[row - 1][col] == water
                            && gameBoard[row - 1][col - 1] == water
                            && gameBoard[row - 1][col + 1] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row + 1][col] = ship;
                        placedShips++;
                    }

                } else if (row == 0 && col + 1 < gameBoardLength && col - 1 >= 0) {
                    if (gameBoard[row][col + 1] == water
                            && gameBoard[row][col - 1] == water
                            && gameBoard[row + 1][col + 1] == water
                            && gameBoard[row + 1][col - 1] == water
                            && gameBoard[row + 2][col + 1] == water
                            && gameBoard[row + 2][col - 1] == water
                            && gameBoard[row + 2][col] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row + 1][col] = ship;
                        placedShips++;
                    }

                } else if (row + 1 == 9 && col == 0) {
                    if (gameBoard[row][col + 1] == water
                            && gameBoard[row + 1][col + 1] == water
                            && gameBoard[row - 1][col] == water
                            && gameBoard[row - 1][col + 1] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row + 1][col] = ship;
                        placedShips++;
                    }

                } else if (row + 1 == 9 && col == 9) {
                    if (gameBoard[row + 1][col - 1] == water
                            && gameBoard[row - 1][col] == water
                            && gameBoard[row][col - 1] == water
                            && gameBoard[row - 1][col - 1] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row + 1][col] = ship;
                        placedShips++;
                    }

                } else if (row == 0 && col == 9) {
                    if (gameBoard[row + 2][col] == water
                            && gameBoard[row + 1][col - 1] == water
                            && gameBoard[row][col - 1] == water
                            && gameBoard[row + 2][col - 1] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row + 1][col] = ship;
                        placedShips++;
                    }

                } else if (row == 0 && col == 0) {
                    if (gameBoard[row + 2][col] == water
                            && gameBoard[row + 1][col + 1] == water
                            && gameBoard[row][col + 1] == water
                            && gameBoard[row + 2][col + 1] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row + 1][col] = ship;
                        placedShips++;
                    }
                }

                // Poziomo w prawo
            } else if (possiblePlacement == water && col + 1 < gameBoardLength && gameBoard[row][col + 1] != ship) {

                if (col + 2 < gameBoardLength && row + 1 < gameBoardLength && row - 1 >= 0 && col - 1 >= 0) {
                    if (gameBoard[row][col + 2] == water
                            && gameBoard[row + 1][col] == water
                            && gameBoard[row - 1][col] == water
                            && gameBoard[row + 1][col + 1] == water
                            && gameBoard[row - 1][col + 1] == water
                            && gameBoard[row][col - 1] == water
                            && gameBoard[row - 1][col - 1] == water
                            && gameBoard[row + 1][col - 1] == water
                            && gameBoard[row - 1][col + 2] == water
                            && gameBoard[row + 1][col + 2] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row][col + 1] = ship;
                        placedShips++;

                    }
                } else if (col + 1 == 9 && row + 1 < gameBoardLength && row - 1 >= 0) {
                    if (gameBoard[row + 1][col] == water
                            && gameBoard[row - 1][col] == water
                            && gameBoard[row + 1][col + 1] == water
                            && gameBoard[row - 1][col + 1] == water
                            && gameBoard[row][col - 1] == water
                            && gameBoard[row - 1][col - 1] == water
                            && gameBoard[row + 1][col - 1] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row][col + 1] = ship;
                        placedShips++;
                    }

                } else if (col == 0 && row + 1 < gameBoardLength && row - 1 >= 0) {
                    if (gameBoard[row + 1][col] == water
                            && gameBoard[row - 1][col] == water
                            && gameBoard[row + 1][col + 1] == water
                            && gameBoard[row - 1][col + 1] == water
                            && gameBoard[row - 1][col + 2] == water
                            && gameBoard[row + 1][col + 2] == water
                            && gameBoard[row][col + 2] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row][col + 1] = ship;
                        placedShips++;
                    }

                } else if (col + 1 == 9 && row == 0) {
                    if (gameBoard[row + 1][col] == water
                            && gameBoard[row + 1][col + 1] == water
                            && gameBoard[row][col - 1] == water
                            && gameBoard[row + 1][col - 1] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row][col + 1] = ship;
                        placedShips++;
                    }

                } else if (col + 1 == 9 && row == 9) {
                    if (gameBoard[row - 1][col + 1] == water
                            && gameBoard[row - 1][col] == water
                            && gameBoard[row][col - 1] == water
                            && gameBoard[row - 1][col - 1] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row][col + 1] = ship;
                        placedShips++;
                    }

                } else if (col == 0 && row == 9) {
                    if (gameBoard[row][col + 2] == water
                            && gameBoard[row - 1][col + 1] == water
                            && gameBoard[row - 1][col] == water
                            && gameBoard[row - 1][col + 2] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row][col + 1] = ship;
                        placedShips++;
                    }

                } else if (row == 0 && col == 0) {
                    if (gameBoard[row][col + 2] == water
                            && gameBoard[row + 1][col + 1] == water
                            && gameBoard[row][col + 1] == water
                            && gameBoard[row + 1][col + 2] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row][col + 1] = ship;
                        placedShips++;
                    }
                }

                // Poziomo w lewo
            } else if (possiblePlacement == water && col - 1 >= 0 && gameBoard[row][col - 1] != ship) {

                if (col - 2 >= 0 && row + 1 < gameBoardLength && row - 1 >= 0 && col + 1 < gameBoardLength) {
                    if (gameBoard[row][col - 2] == water
                            && gameBoard[row + 1][col] == water
                            && gameBoard[row - 1][col] == water
                            && gameBoard[row + 1][col - 1] == water
                            && gameBoard[row - 1][col - 1] == water
                            && gameBoard[row][col + 1] == water
                            && gameBoard[row - 1][col + 1] == water
                            && gameBoard[row + 1][col + 1] == water
                            && gameBoard[row - 1][col - 2] == water
                            && gameBoard[row + 1][col - 2] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row][col - 1] = ship;
                        placedShips++;

                    }
                } else if (col - 1 == 0 && row + 1 < gameBoardLength && row - 1 >= 0) {
                    if (gameBoard[row + 1][col] == water
                            && gameBoard[row - 1][col] == water
                            && gameBoard[row + 1][col - 1] == water
                            && gameBoard[row - 1][col - 1] == water
                            && gameBoard[row][col + 1] == water
                            && gameBoard[row - 1][col + 1] == water
                            && gameBoard[row + 1][col + 1] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row][col - 1] = ship;
                        placedShips++;
                    }

                } else if (col == 9 && row + 1 < gameBoardLength && row - 1 >= 0) {
                    if (gameBoard[row + 1][col] == water
                            && gameBoard[row - 1][col] == water
                            && gameBoard[row + 1][col - 1] == water
                            && gameBoard[row - 1][col - 1] == water
                            && gameBoard[row - 1][col - 2] == water
                            && gameBoard[row + 1][col - 2] == water
                            && gameBoard[row][col - 2] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row][col - 1] = ship;
                        placedShips++;
                    }

                } else if (col - 1 == 0 && row == 0) {
                    if (gameBoard[row + 1][col] == water
                            && gameBoard[row + 1][col - 1] == water
                            && gameBoard[row][col + 1] == water
                            && gameBoard[row + 1][col + 1] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row][col - 1] = ship;
                        placedShips++;
                    }

                } else if (col - 1 == 0 && row == 9) {
                    if (gameBoard[row - 1][col - 1] == water
                            && gameBoard[row - 1][col] == water
                            && gameBoard[row][col + 1] == water
                            && gameBoard[row - 1][col + 1] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row][col - 1] = ship;
                        placedShips++;
                    }

                } else if (col == 9 && row == 9) {
                    if (gameBoard[row][col - 2] == water
                            && gameBoard[row - 1][col - 1] == water
                            && gameBoard[row - 1][col] == water
                            && gameBoard[row - 1][col - 2] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row][col - 1] = ship;
                        placedShips++;
                    }

                } else if (row == 0 && col == 9) {
                    if (gameBoard[row][col - 2] == water
                            && gameBoard[row + 1][col - 1] == water
                            && gameBoard[row][col - 1] == water
                            && gameBoard[row + 1][col - 2] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row][col - 1] = ship;
                        placedShips++;
                    }
                }

                // Pionowo w gore
            } else if (possiblePlacement == water && row - 1 >= 0 && gameBoard[row - 1][col] != ship) {

                if (row - 2 >= 0 && col + 1 < gameBoardLength && col - 1 >= 0 && row + 1 < gameBoardLength) {
                    if (gameBoard[row - 2][col] == water
                            && gameBoard[row][col - 1] == water
                            && gameBoard[row][col + 1] == water
                            && gameBoard[row + 1][col] == water
                            && gameBoard[row + 1][col + 1] == water
                            && gameBoard[row + 1][col - 1] == water
                            && gameBoard[row - 1][col - 1] == water
                            && gameBoard[row - 1][col + 1] == water
                            && gameBoard[row - 2][col + 1] == water
                            && gameBoard[row - 2][col - 1] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row - 1][col] = ship;
                        placedShips++;
                    }
                } else if (row - 1 == 0 && col + 1 < gameBoardLength && col - 1 >= 0) {
                    if (gameBoard[row][col + 1] == water
                            && gameBoard[row][col - 1] == water
                            && gameBoard[row + 1][col] == water
                            && gameBoard[row + 1][col + 1] == water
                            && gameBoard[row + 1][col - 1] == water
                            && gameBoard[row - 1][col - 1] == water
                            && gameBoard[row - 1][col + 1] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row - 1][col] = ship;
                        placedShips++;
                    }

                } else if (row == 9 && col + 1 < gameBoardLength && col - 1 >= 0) {
                    if (gameBoard[row][col + 1] == water
                            && gameBoard[row][col - 1] == water
                            && gameBoard[row - 1][col + 1] == water
                            && gameBoard[row - 1][col - 1] == water
                            && gameBoard[row - 2][col + 1] == water
                            && gameBoard[row - 2][col - 1] == water
                            && gameBoard[row - 2][col] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row - 1][col] = ship;
                        placedShips++;
                    }

                } else if (row - 1 == 0 && col == 0) {
                    if (gameBoard[row][col + 1] == water
                            && gameBoard[row + 1][col + 1] == water
                            && gameBoard[row + 1][col] == water
                            && gameBoard[row - 1][col + 1] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row - 1][col] = ship;
                        placedShips++;
                    }

                } else if (row - 1 == 0 && col == 9) {
                    if (gameBoard[row + 1][col - 1] == water
                            && gameBoard[row + 1][col] == water
                            && gameBoard[row][col - 1] == water
                            && gameBoard[row - 1][col - 1] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row - 1][col] = ship;
                        placedShips++;
                    }

                } else if (row == 9 && col == 9) {
                    if (gameBoard[row - 2][col] == water
                            && gameBoard[row - 1][col - 1] == water
                            && gameBoard[row][col - 1] == water
                            && gameBoard[row - 2][col - 1] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row - 1][col] = ship;
                        placedShips++;
                    }

                } else if (row == 9 && col == 0) {
                    if (gameBoard[row - 2][col] == water
                            && gameBoard[row - 1][col + 1] == water
                            && gameBoard[row][col + 1] == water
                            && gameBoard[row - 2][col + 1] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row - 1][col] = ship;
                        placedShips++;
                    }
                }
                /*
            } else if (possiblePlacement == water && col + 1 < gameBoardLength && row + 1 < gameBoardLength
                    && gameBoard[row + 1][col] == ship) {

                if (col + 2 < gameBoardLength && col - 1 >= 0 && row - 2 >= 0) {
                    if (gameBoard[row - 1][col] == water && gameBoard[row - 1][col + 1] == water
                            && gameBoard[row - 1][col + 2] == water
                            && gameBoard[row - 2][col] == water && gameBoard[row - 1][col - 1] == water
                            && gameBoard[row - 2][col + 1] == water
                            && gameBoard[row][col + 1] == water) {
                        gameBoard[row - 1][col] = ship;
                        gameBoard[row - 1][col + 1] = ship;
                        placedShips++;
                    }
                }

            } else if (possiblePlacement == water && col == 0 && row + 1 < gameBoardLength
                    && gameBoard[row + 1][col] == ship) {
                if (row - 2 >= 0) {
                    if (gameBoard[row - 1][col] == water && gameBoard[row - 1][col + 1] == water
                            && gameBoard[row - 1][col + 2] == water
                            && gameBoard[row - 2][col] == water
                            && gameBoard[row - 2][col + 1] == water && gameBoard[row][col + 1] == water) {
                        gameBoard[row - 1][col] = ship;
                        gameBoard[row - 1][col + 1] = ship;
                        placedShips++;
                    }

                } else if (row - 1 == 0) {
                    if (gameBoard[row - 1][col] == water && gameBoard[row - 1][col + 1] == water
                            && gameBoard[row - 1][col + 2] == water
                            && gameBoard[row][col + 1] == water) {
                        gameBoard[row - 1][col] = ship;
                        gameBoard[row - 1][col + 1] = ship;
                        placedShips++;
                    }
                }

                 */
            }

        }
        return placeThreeMastedShips(gameBoard, threeMastedShipNumber, water, ship);
    }

    private static char[][] placeThreeMastedShips(
            char[][] gameBoard, int threeMastedShipNumber, char water, char ship) {
        int placedShips = 0;
        int gameBoardLength = gameBoard.length;
        while (placedShips < threeMastedShipNumber) {
            int[] location = generateShipCoordinates(gameBoardLength);
            int row = location[0];
            int col = location[1];
            char possiblePlacement = gameBoard[row][col];

            if (possiblePlacement == water) {
                if (row + 2 < gameBoardLength && gameBoard[row + 1][col] == water && gameBoard[row + 2][col] == water) {
                    if (row + 3 < gameBoardLength && row - 1 >= 0 && col + 1 <= 9 && col - 1 >= 0) {
                        if (gameBoard[row - 1][col] == water && gameBoard[row][col - 1] == water
                                && gameBoard[row][col + 1] == water && gameBoard[row + 1][col - 1] == water
                                && gameBoard[row + 1][col + 1] == water && gameBoard[row + 2][col - 1] == water
                                && gameBoard[row + 2][col + 1] == water
                                && gameBoard[row + 3][col] == water) {
                            gameBoard[row][col] = ship;
                            gameBoard[row + 1][col] = ship;
                            gameBoard[row + 2][col] = ship;
                            placedShips++;
                        }

                    } else if (row == 0 && col + 1 < gameBoardLength && col - 1 >= 0) {
                        if (gameBoard[row][col - 1] == water && gameBoard[row][col + 1] == water
                                && gameBoard[row + 1][col - 1] == water
                                && gameBoard[row + 1][col + 1] == water && gameBoard[row + 2][col - 1] == water
                                && gameBoard[row + 2][col + 1] == water
                                && gameBoard[row + 3][col] == water) {
                            gameBoard[row][col] = ship;
                            gameBoard[row + 1][col] = ship;
                            gameBoard[row + 2][col] = ship;
                            placedShips++;
                        }

                    } else if (row == 0 && col == 0) {
                        if (gameBoard[row][col + 1] == water && gameBoard[row + 1][col + 1] == water
                                && gameBoard[row + 2][col + 1] == water
                                && gameBoard[row + 3][col] == water) {
                            gameBoard[row][col] = ship;
                            gameBoard[row + 1][col] = ship;
                            gameBoard[row + 2][col] = ship;
                            placedShips++;
                        }

                    } else if (row == 0 && col == 9) {
                        if (gameBoard[row][col - 1] == water && gameBoard[row + 1][col - 1] == water
                                && gameBoard[row + 2][col - 1] == water
                                && gameBoard[row + 3][col] == water) {
                            gameBoard[row][col] = ship;
                            gameBoard[row + 1][col] = ship;
                            gameBoard[row + 2][col] = ship;
                            placedShips++;
                        }
                    }

                } else if (row + 2 == 9 && col + 1 < gameBoardLength && col - 1 >= 0) {
                    if (gameBoard[row][col - 1] == water && gameBoard[row][col + 1] == water
                            && gameBoard[row + 1][col - 1] == water
                            && gameBoard[row + 1][col + 1] == water && gameBoard[row + 2][col - 1] == water
                            && gameBoard[row + 2][col + 1] == water
                            && gameBoard[row + 2][col] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row + 1][col] = ship;
                        gameBoard[row + 2][col] = ship;
                        placedShips++;
                    }
                } else if (row + 2 == 9 && col == 0) {
                    if (gameBoard[row][col + 1] == water && gameBoard[row + 1][col + 1] == water
                            && gameBoard[row + 2][col + 1] == water
                            && gameBoard[row + 2][col] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row + 1][col] = ship;
                        gameBoard[row + 2][col] = ship;
                        placedShips++;
                    }
                } else if (row + 2 == 9 && col == 9) {
                    if (gameBoard[row][col - 1] == water && gameBoard[row + 1][col - 1] == water
                            && gameBoard[row + 2][col - 1] == water
                            && gameBoard[row + 2][col] == water) {
                        gameBoard[row][col] = ship;
                        gameBoard[row + 1][col] = ship;
                        gameBoard[row + 2][col] = ship;
                        placedShips++;
                    }
                } else if (col + 2 < gameBoardLength && row + 1 < gameBoardLength && gameBoard[row + 1][col] == ship) {
                    if (col + 3 < gameBoardLength && col - 1 >= 0 && row - 2 >= 0) {
                        if (gameBoard[row - 1][col] == water && gameBoard[row - 1][col + 1] == water
                                && gameBoard[row - 1][col + 2] == water &&
                                gameBoard[row - 1][col + 3] == water && gameBoard[row - 2][col] == water
                                && gameBoard[row - 1][col - 1] == water
                                && gameBoard[row - 2][col + 1] == water && gameBoard[row - 2][col + 2] == water
                                && gameBoard[row][col + 1] == water
                                && gameBoard[row][col + 2] == water) {
                            gameBoard[row - 1][col] = ship;
                            gameBoard[row - 1][col + 1] = ship;
                            gameBoard[row - 1][col + 2] = ship;
                            placedShips++;
                        }
                    }
                } else if (col == 0 && row + 1 < gameBoardLength && gameBoard[row + 1][col] == ship) {
                    if (row - 2 >= 0) {
                        if (gameBoard[row - 1][col] == water && gameBoard[row - 1][col + 1] == water
                                && gameBoard[row - 1][col + 2] == water &&
                                gameBoard[row - 1][col + 3] == water && gameBoard[row - 2][col] == water
                                && gameBoard[row - 2][col + 1] == water
                                && gameBoard[row - 2][col + 2] == water && gameBoard[row][col + 1] == water
                                && gameBoard[row][col + 2] == water) {
                            gameBoard[row - 1][col] = ship;
                            gameBoard[row - 1][col + 1] = ship;
                            gameBoard[row - 1][col + 2] = ship;
                            placedShips++;
                        }
                    } else if (row - 1 == 0) {
                        if (gameBoard[row - 1][col] == water && gameBoard[row - 1][col + 1] == water
                                && gameBoard[row - 1][col + 2] == water &&
                                gameBoard[row - 1][col + 3] == water && gameBoard[row][col + 1] == water
                                && gameBoard[row][col + 2] == water) {
                            gameBoard[row - 1][col] = ship;
                            gameBoard[row - 1][col + 1] = ship;
                            gameBoard[row - 1][col + 2] = ship;
                            placedShips++;
                        }
                    }
                }
            }
        }

        return gameBoard;
    }

    private static int[] generateShipCoordinates(int gameBoardLength) {
        int[] coordinates = new int[2];

        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] = new Random().nextInt(gameBoardLength);
        }
        return coordinates;
    }

    private static void displayGameboard(char[][] gameBoard) {
        System.out.println();
        for (char[] row : gameBoard) {
            for (int j = 0; j < gameBoard.length; j++) {
                System.out.print(row[j] + " ");
            }
            System.out.println();
        }
    }
}

