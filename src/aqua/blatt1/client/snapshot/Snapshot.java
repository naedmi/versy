package aqua.blatt1.client.snapshot;

public class Snapshot {
    private int numberOfFish;
    private boolean done;

    public Snapshot(int numberOfFish) {
        this.numberOfFish = numberOfFish;
        this.done = false;
    }

    public void updateNumberOfFish(int numberOfFish) {
        this.numberOfFish += numberOfFish;
    }

    public int getNumberOfFish() {
        return numberOfFish;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone() {
        this.done = true;
    }
}
