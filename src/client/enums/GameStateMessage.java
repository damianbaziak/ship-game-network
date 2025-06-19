package client.enums;

public enum GameStateMessage {
    YOUR_TURN("Your turn! Enter the target coordinates (e.g., B7): "),
    OPPONENT_IS_FIRING("Opponent is firing. Waiting for their shot..."),
    PLACE_YOUR_SHIPS("Place your %d %s-Masted Ships%n"),
    ENTER_COORDINATES_FOR_FIRST_MAST("Enter coordinates for the FIRST MAST of the %d of %d %s-Masted Ships (e.g., A5):%n"),
    ENTER_COORDINATES_FOR_SECOND_MAST("Enter coordinates for the SECOND MAST of the %d of %d %s-Masted Ships (e.g., A5):%n"),
    ENTER_COORDINATES_FOR_THIRD_MAST("Enter coordinates for the THIRD MAST of the %d of %d %s-Masted Ships (e.g., A5):%n"),
    ENTER_COORDINATES_FOR_FOURTH_MAST("Enter coordinates for the FOURTH MAST of the %d of %d %s-Masted Ships (e.g., A5):%n"),
    ENTER_COORDINATES_SINGLE_MAST_SHIPS("Enter coordinates for the %d of 4 Single-Masted Ships (e.g., A5):%n"),
    SINGLE_MAST_SHIPS_PLACED("ALL SINGLE-MASTED SHIPS HAVE BEEN PLACED!"),
    TWO_MAST_SHIPS_PLACED("ALL TWO-MASTED SHIPS HAVE BEEN PLACED!"),
    THREE_MAST_SHIPS_PLACED("ALL THREE-MASTED SHIPS HAVE BEEN PLACED!"),
    ALL_SHIPS_PLACED("ALL SHIPS HAVE BEEN PLACED!"),
    CONNECTED_TO_SERVER("Connected to server :)"),
    SCANNING_HORIZON("Scanning the horizon... the opponent hasn't shown up yet."),
    WAITING_FOR_OPPONENT("Waiting for the opponent...");

    private final String message;

    GameStateMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
