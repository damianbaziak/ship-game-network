package client.game.enums.messages;

public enum ServerMessage {
    START("Start."),
    THE_WAR_BEGUN("The war has begun."),
    GAME_OVER("Game over."),
    YOUR_TURN("Your turn."),
    SHIPS_PLACED("Ships placed."),
    PLEASE_WAIT("Please wait.");

    private final String message;

    ServerMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
