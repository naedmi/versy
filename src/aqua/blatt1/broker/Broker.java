package aqua.blatt1.broker;

import aqua.blatt1.common.msgtypes.*;
import aqua.blatt2.broker.PoisonPill;
import messaging.Endpoint;
import messaging.Message;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static javax.swing.JOptionPane.showMessageDialog;

public class Broker {
    private static final int NUMTHREADS = 10;
    private final Endpoint endpoint = new Endpoint(4711);
    public ClientCollection<InetSocketAddress> clients = new ClientCollection<>();
    private final ExecutorService executor = newFixedThreadPool(NUMTHREADS);
    private int nextId = 0;
    private boolean stopRequested = false;

    public void broker() {
        Thread backgroundThread = new Thread(() -> {
            showMessageDialog(null, "Press OK button to stop server");
            stopRequested = true;
        });
        backgroundThread.start();

        while (!stopRequested) {
            Message message = this.endpoint.blockingReceive();
            Serializable request = message.getPayload();
            InetSocketAddress sender = message.getSender();
            executor.execute(new BrokerTask(request, sender));
        }
        executor.shutdown();
        System.exit(0);
    }

    public static void main(String[] args) {
        new Broker().broker();

    }

    private class BrokerTask implements Runnable {
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private final Serializable request;
        private final InetSocketAddress sender;

        public BrokerTask(Serializable request, InetSocketAddress sender) {
            this.request = request;
            this.sender = sender;
        }

        @Override
        public void run() {
            if (request instanceof RegisterRequest) {
                register(sender);
            } else if (request instanceof DeregisterRequest) {
                deregister(sender);
            } else if (request instanceof HandoffRequest) {
                handoffFish(sender, (HandoffRequest) request);
            } else if (request instanceof PoisonPill) {
                stopRequested = true;
            } else {
                System.out.println("Unknown request: " + request);
            }
        }

        private void register(InetSocketAddress sender) {
            lock.writeLock().lock();
            if (clients.indexOf(sender) != -1) {
                System.out.println("Client already registered: " + sender);
                return;
            }
            clients.add("tank" + nextId, sender);

            switch (clients.size()) {
                case 1:
                    endpoint.send(sender, new RegisterResponse("tank" + nextId++));
                    endpoint.send(sender, new NeighborUpdate(sender, sender));
                    endpoint.send(sender, new Token());
                    break;
                case 2:
                    endpoint.send(sender, new RegisterResponse("tank" + nextId++));
                    InetSocketAddress neighbor = clients.getLeftNeighborOf(clients.indexOf(sender));

                    endpoint.send(neighbor, new NeighborUpdate(sender, sender));
                    endpoint.send(sender, new NeighborUpdate(neighbor, neighbor));
                    break;
                default:
                    endpoint.send(sender, new RegisterResponse("tank" + nextId++));

                    InetSocketAddress leftNeighbor = clients.getLeftNeighborOf(clients.indexOf(sender));
                    InetSocketAddress rightNeighbor = clients.getRightNeighborOf(clients.indexOf(sender));

                    updateNeighbors(leftNeighbor);
                    updateNeighbors(rightNeighbor);
            }

            lock.writeLock().unlock();
        }

        private void updateNeighbors(InetSocketAddress neighbor) {
            int neighborIndex = clients.indexOf(neighbor);
            endpoint.send(neighbor, new NeighborUpdate(
                    clients.getLeftNeighborOf(neighborIndex),
                    clients.getRightNeighborOf(neighborIndex)));

        }

        private void deregister(InetSocketAddress sender) {
            lock.writeLock().lock();
            int index = clients.indexOf(sender);
            if (index == -1) {
                System.out.println("Client not registered: " + sender);
                return;
            }
            if (clients.size() == 1) {
                stopRequested = true;
            }

            InetSocketAddress leftNeighbor = clients.getLeftNeighborOf(index);
            InetSocketAddress rightNeighbor = clients.getRightNeighborOf(index);

            clients.remove(clients.indexOf(sender));

            updateNeighbors(leftNeighbor);
            updateNeighbors(rightNeighbor);

            lock.writeLock().unlock();
        }

        private void handoffFish(InetSocketAddress sender, HandoffRequest request) {
            lock.readLock().lock();
            int vector = request.getFish().getDirection().getVector();
            int index = clients.indexOf(sender);
            InetSocketAddress neighbor;

            switch (vector) {
                case -1:
                    neighbor = clients.getLeftNeighborOf(index);
                    break;
                case 1:
                    neighbor = clients.getRightNeighborOf(index);
                    break;
                default:
                    System.out.println("Unknown vector: " + vector);
                    return;
            }
            endpoint.send(neighbor, request);
            lock.readLock().unlock();
        }
    }
}
