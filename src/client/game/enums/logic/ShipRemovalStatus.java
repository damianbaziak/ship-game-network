package client.game.enums.logic;

public enum ShipRemovalStatus {
    INSTANCE;

    private boolean wasRemoved = false;
    private WhatWasRemove whatWasRemoved = WhatWasRemove.NONE;
    private WhereWasDeleted whereWasRemove = WhereWasDeleted.NONE;


    public boolean isWasRemoved() {
        return wasRemoved;
    }

    public void setWasRemoved(boolean wasRemoved) {
        this.wasRemoved = wasRemoved;
    }

    public WhatWasRemove getWhatWasRemoved() {
        return whatWasRemoved;
    }

    public void setWhatWasRemoved(WhatWasRemove whatWasRemoved) {
        this.whatWasRemoved = whatWasRemoved;
    }

    public WhereWasDeleted getWhereWasRemove() {
        return whereWasRemove;
    }

    public void setWhereWasRemove(WhereWasDeleted whereWasRemove) {
        this.whereWasRemove = whereWasRemove;
    }


    public enum WhatWasRemove {
        NONE, MAST, SHIP

    }

    public enum WhereWasDeleted {
        CURRENT_METHOD, ANOTHER_METHOD, NONE
    }
}
