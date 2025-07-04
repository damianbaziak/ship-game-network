package client.game.messages;

public enum GameStateMessage {
    YOUR_TURN("Your turn! Enter the target coordinates (e.g., B7): "),
    OPPONENT_IS_FIRING("Opponent is firing. Waiting for their shot..."),
    PLACE_YOUR_SHIPS("Place your %d %s-Masted Ships%n"),
    PLACE_YOUR_FOUR_MASTED("Place your %d %s-Masted Ship%n"),
    ENTER_COORDINATES_FOR_MAST("Enter coordinate for the %s MAST of the %d of %d %s-Masted Ships (e.g., A5):"),
    ENTER_COORDINATES_SINGLE_MAST_SHIPS("Enter coordinate for the %d of 4 Single-Masted Ships (e.g., A5):"),
    SINGLE_MAST_SHIPS_PLACED("ALL SINGLE-MASTED SHIPS HAVE BEEN PLACED!"),
    TWO_MAST_SHIPS_PLACED("ALL TWO-MASTED SHIPS HAVE BEEN PLACED!"),
    THREE_MAST_SHIPS_PLACED("ALL THREE-MASTED SHIPS HAVE BEEN PLACED!"),
    ALL_SHIPS_PLACED("ALL SHIPS HAVE BEEN PLACED!"),
    ENTER_OPTIONS("                   Or enter 'Options' to remove ships/masts:%n"),
    NO_SHIP_TO_REMOVE("NO SHIP FOUND TO REMOVE!"),
    NO_MAST_TO_REMOVE("NO MAST FOUND TO REMOVE!"),
    LAST_SHIP_REMOVED("THE LAST PLACED SHIP REMOVED!"),
    LAST_MAST_REMOVED("THE LAST PLACED MAST REMOVED!"),
    WRONG_OPTION("WRONG OPTION SELECTED!"),
    AVAILABLE_OPTIONS("""
                    
                    Available OPTIONS:
                    1. Remove the last placed mast.
                    2. Remove the last placed ship.
                    3. Exit.
                    
                    Select an option. Enter 1, 2 or 3:"""),
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
