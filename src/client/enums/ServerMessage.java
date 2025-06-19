package client.enums;

public enum ServerMessage {
    START("START"),
    THE_WAR_BEGUN("The war has begun."),
    GAME_OVER("Game over."),
    YOU_TURN("You turn."),
    SHIPS_PLACED("Ships placed.");

    private final String message;

    ServerMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
