package client.game.logic;

public enum ShipRemovalStatus {
    INSTANCE;
    private boolean wasRemoved = false;
    private RemovalSource whatWasDeleted= RemovalSource.NONE;

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

    public enum RemovalSource {
        NONE, MAST, SHIP;

    }
}
