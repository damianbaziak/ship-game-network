package client.game.enums.logic;

public enum ShipRemovalStatus {
    INSTANCE;

    private boolean wasRemoved = false;
    private RemovalSource whatWasDeleted = RemovalSource.NONE;
    private WhereWasDeleted whereWasDeleted = WhereWasDeleted.NONE;


    public boolean isWasRemoved() {
        return wasRemoved;
    }

    public void setWasRemoved(boolean wasRemoved) {
        this.wasRemoved = wasRemoved;
    }

    public RemovalSource getWhatWasDeleted() {
        return whatWasDeleted;
    }

    public void setWhatWasDeleted(RemovalSource whatWasDeleted) {
        this.whatWasDeleted = whatWasDeleted;
    }

    public WhereWasDeleted getWhereWasDeleted() {
        return whereWasDeleted;
    }

    public void setWhereWasDeleted(WhereWasDeleted whereWasDeleted) {
        this.whereWasDeleted = whereWasDeleted;
    }


    public enum RemovalSource {
        NONE, MAST, SHIP

    }

    public enum WhereWasDeleted {
        CURRENT_METHOD, ANOTHER_METHOD, NONE
    }
}
