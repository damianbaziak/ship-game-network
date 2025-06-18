package client.enums;

public enum ShotFeedbackMessage {
    HIT_SINGLE_MAST("YOU HIT A SINGLE-MASTED SHIP!",
            "OPPONENT HIT YOUR SINGLE-MASTED SHIP!", "HIT"),
    HIT_TWO_MAST("YOU HIT A TWO-MASTED SHIP!",
            "OPPONENT HIT YOUR TWO-MASTED SHIP!", "HIT"),
    HIT_THREE_MAST("YOU HIT A THREE-MASTED SHIP!",
            "OPPONENT HIT YOUR THREE-MASTED SHIP!", "HIT"),
    HIT_FOUR_MAST("YOU HIT A FOUR-MASTED SHIP!",
            "OPPONENT HIT YOUR FOUR-MASTED SHIP!", "HIT"),

    SUNK_SINGLE_MAST_SHIP("YOU'VE SUNK A SINGLE-MASTED SHIP!",
            "OPPONENT SUNK ONE OF YOUR SINGLE-MASTED SHIPS", "SUNK"),
    SUNK_TWO_MAST_SHIP("YOU'VE SUNK A TWO-MASTED SHIP!",
            "OPPONENT SUNK ONE OF YOUR TWO-MASTED SHIPS!", "SUNK"),
    SUNK_THREE_MAST_SHIP("YOU'VE SUNK A THREE-MASTED SHIP!",
            "OPPONENT SUNK ONE OF YOUR THREE-MASTED SHIPS!", "SUNK"),
    SUNK_FOUR_MAST_SHIP("YOU'VE SUNK A FOUR-MASTED SHIP!",
            "OPPONENT SUNK YOUR FOUR-MASTED SHIPS!", "SUNK"),

    ALL_SINGLE_MAST_SHIPS_SUNK("YOU'VE SUNK ALL SINGLE-MASTED SHIPS!",
            "OPPONENT SUNK ALL YOUR SINGLE-MASTED SHIPS!", "SUNK"),
    ALL_TWO_MAST_SHIPS_SUNK("YOU'VE SUNK ALL TWO-MASTED SHIPS!",
            "OPPONENT SUNK ALL YOUR TWO-MASTED SHIPS!", "SUNK"),
    ALL_THREE_MAST_SHIPS_SUNK("YOU'VE SUNK ALL THREE-MASTED SHIPS!",
            "OPPONENT SUNK ALL YOUR THREE-MASTED SHIPS!", "SUNK"),
    ALL_FOUR_MAST_SHIPS_SUNK("YOU'VE SUNK ALL FOUR-MASTED SHIPS!",
            "OPPONENT SUNK ALL YOUR FOUR-MASTED SHIPS!", "SUNK"),

    MISS("MISSED!", "OPPONENT MISSED!", "MISS"),
    ALREADY_FIRED("THIS SHOT HAS BEEN ALREADY FIRED!",
            "THE OPPONENT SHOT AT THE LOCATION THAT WAS ALREADY FIRED UPON!",
            "ALREADY FIRED"),
    FIRED_AT("", "Opponent has fired at ", "");

    private final String message;
    private final String opponentFeedback;
    private final String eventType;

    ShotFeedbackMessage(String message, String opponentFeedback, String eventType) {
        this.message = message;
        this.opponentFeedback = opponentFeedback;
        this.eventType = eventType;
    }

    public String getMessage() {
        return message;
    }

    public String getOpponentFeedback() {
        return opponentFeedback;
    }

    public String getFormattedOpponentFeedback(String position) {
        return opponentFeedback + position.toUpperCase();
    }

    public String getEventType() {
        return eventType;
    }
}
