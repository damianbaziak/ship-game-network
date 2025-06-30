package client.game.logic;

public enum ShipRemovalStatus {
    REMOVAL_STATUS;

    private boolean wasRemoved = false;
    private WhatIsRemoved whatRemoved = WhatIsRemoved.NONE;
    private WhereIsRemoved whereRemoved = WhereIsRemoved.NONE;


    public boolean isWasRemoved() {
        return wasRemoved;
    }

    public void setWasRemoved(boolean wasRemoved) {
        this.wasRemoved = wasRemoved;
    }

    public WhatIsRemoved getWhatRemoved() {
        return whatRemoved;
    }

    public void setWhatRemoved(WhatIsRemoved whatRemoved) {
        this.whatRemoved = whatRemoved;
    }

    public WhereIsRemoved getWhereRemoved() {
        return whereRemoved;
    }

    public void setWhereRemoved(WhereIsRemoved whereRemoved) {
        this.whereRemoved = whereRemoved;
    }


    public enum WhatIsRemoved {
        NONE, MAST, SHIP

    }

    public enum WhereIsRemoved {
        CURRENT_METHOD, ANOTHER_METHOD, NONE
    }
}
