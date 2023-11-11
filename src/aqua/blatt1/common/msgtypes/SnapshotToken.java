package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public class SnapshotToken implements Serializable {
    private int numberOfFish;

    public SnapshotToken() {
        this.numberOfFish = 0;
    }

    public int getNumberOfFish() {
        return numberOfFish;
    }

    public void updateNumberOfFish(int numberOfFish) {
        this.numberOfFish += numberOfFish;
    }
}
