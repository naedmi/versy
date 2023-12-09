package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.client.snapshot.Snapshot;
import aqua.blatt1.client.snapshot.SnapshotMode;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.NeighborUpdate;
import aqua.blatt1.common.msgtypes.SnapshotToken;

import javax.swing.*;

public class TankModel extends Observable implements Iterable<FishModel> {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected final Set<FishModel> fishies;
	protected int fishCounter = 0;
	protected final ClientCommunicator.ClientForwarder forwarder;
	private InetSocketAddress leftNeighbor;
	private InetSocketAddress rightNeighbor;
	protected boolean token = false;
	protected Timer timer = new Timer();
	protected Snapshot snapshot;
	protected SnapshotMode snapshotMode;
	protected SnapshotToken snapshotToken;
	protected Thread snapshotTokenHandOff;
	protected boolean initiator = false;
	private final Map<String, InetSocketAddress> homeAgent = new HashMap<>();

	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
	}

	synchronized void onRegistration(String id, long leaseTime) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));

		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				forwarder.register(); // renew registration
			}
		}, leaseTime / 2);
	}

	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
			y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
			homeAgent.put(fish.getId(), null);
		}
	}

	synchronized void receiveFish(FishModel fish) {
		if (snapshotMode != SnapshotMode.IDLE) {
			// snapshot.updateNumberOfFish(1);
		}
		fish.setToStart();
		fishies.add(fish);

		if (fish.getTankId().equals(id)) {
			homeAgent.put(fish.getId(), null);
		} else {
			forwarder.sendNameResolutionRequest(fish.getTankId(), fish.getId());
		}

	}

	synchronized void handleNeighborUpdate(NeighborUpdate update) {
		leftNeighbor = update.getLeftNeighbor();
		rightNeighbor = update.getRightNeighbor();
	}

	synchronized void receiveToken() {
		token = true;
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				token = false;
				forwarder.handOffToken(leftNeighbor);
			}
		}, Duration.ofSeconds(5).toMillis());
	}

	synchronized void receiveSnapshotMarker(InetSocketAddress sender) {
		switch (snapshotMode) {
			case IDLE:
				this.snapshot = new Snapshot(fishies.size());
				snapshotMode = (sender == leftNeighbor)
						? SnapshotMode.RIGHT
						: SnapshotMode.LEFT;

				forwarder.sendSnapshotMarker(leftNeighbor);
				forwarder.sendSnapshotMarker(rightNeighbor);
				break;
			case BOTH:
				snapshotMode = (sender == leftNeighbor)
						? SnapshotMode.RIGHT
						: SnapshotMode.LEFT;
				break;
			case LEFT:
				if (sender.equals(this.leftNeighbor))
					snapshotMode = SnapshotMode.IDLE;
				break;
			case RIGHT:
				if (sender.equals(this.rightNeighbor))
					snapshotMode = SnapshotMode.IDLE;
				break;
		}

		if (snapshotMode == SnapshotMode.IDLE) {
			snapshot.setDone();
		}

		if (this.snapshot.isDone() && this.snapshotToken != null) {
			System.out.println("Initiator send snapshot token");
			this.snapshotToken.updateNumberOfFish(this.snapshot.getNumberOfFish());
			forwarder.sendSnapshotToken(leftNeighbor, this.snapshotToken);

			this.snapshot = null;
			this.snapshotToken = null;
		}
	}

	public void receiveSnapshotToken(InetSocketAddress sender, SnapshotToken token) {
		if (this.initiator) {
			new Thread(() ->
					JOptionPane.showMessageDialog(null,
							"Global Snapshot: " + token.getNumberOfFish())).start();
			this.initiator = false;
		} else {

			if (this.snapshotTokenHandOff != null) {
				return;
			}

			this.snapshotTokenHandOff = new Thread(() -> {
				while(!this.snapshot.isDone()) {
					Thread.onSpinWait();
				}

				System.out.println("Client updated snapshot and handoff token");
				token.updateNumberOfFish(this.snapshot.getNumberOfFish());
				forwarder.sendSnapshotToken(this.leftNeighbor, token);

				this.snapshot = null;
				this.snapshotTokenHandOff = null;
			});
			this.snapshotTokenHandOff.start();
		}
		this.snapshotToken = null;
	}

	public void initiateSnapshot() {
		this.initiator = true;
		this.snapshot = new Snapshot(fishies.size());
		this.snapshotMode = SnapshotMode.BOTH;
		// this.snapshotToken = new SnapshotToken();
		forwarder.sendSnapshotMarker(leftNeighbor);
		forwarder.sendSnapshotMarker(rightNeighbor);
	}

	public void sendLocationUpdate(InetSocketAddress address, String requestId) {
		forwarder.sendLocationUpdate(address, requestId);
	}

	public void updateLocation(InetSocketAddress address, String fishId) {
		homeAgent.put(fishId, address);
	}

	public void locateFishGlobally(String fishId) {
		if (homeAgent.get(fishId) == null) {
			locateFishLocally(fishId);
		} else {
			forwarder.sendLocationRequest(homeAgent.get(fishId), fishId);
		}
	}

	public void locateFishLocally(String fishId) {
		fishies.stream().iterator().forEachRemaining(fish -> {
			if (fish.getId().equals(fishId)) {
				fish.toggle();
			}
		});
	}

	synchronized boolean hasToken() {
		return token;
	}

	public String getId() {
		return id;
	}

	public synchronized int getFishCounter() {
		return fishCounter;
	}

	public synchronized Iterator<FishModel> iterator() {
		return fishies.iterator();
	}

	private synchronized void updateFishies() {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge()) {
				if (token) {
					if (fish.getDirection() == Direction.LEFT) {
						forwarder.handOff(fish, leftNeighbor);
					} else {
						forwarder.handOff(fish, rightNeighbor);
					}
				} else {
					fish.reverse();
				}
            }

			if (fish.disappears())
				it.remove();
		}
	}

	private synchronized void update() {
		updateFishies();
		setChanged();
		notifyObservers();
	}

	protected void run() {
		forwarder.register();

		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}

	public synchronized void finish() {
		forwarder.deregister(id);
	}
}