package aqua.blatt1.broker;

import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class Broker {
    private final Endpoint endpoint = new Endpoint(4711);
    private final ClientCollection<InetSocketAddress> clients = new ClientCollection<>();
    private int nextId = 0;
    private boolean done = false;

    public void broker() {
        while (!done) {
            Message message = this.endpoint.blockingReceive();
            Serializable request = message.getPayload();
            InetSocketAddress sender = message.getSender();

            if (request instanceof RegisterRequest) {
                register(sender);
            } else if (request instanceof DeregisterRequest) {
                deregister(sender);
            } else if (request instanceof HandoffRequest) {
                handoffFish(sender, (HandoffRequest) request);
            } else {
                System.out.println("Unknown request: " + request);
            }
        }
    }

    private void register(InetSocketAddress sender) {
        if (clients.indexOf(sender) != -1) {
            System.out.println("Client already registered: " + sender);
            return;
        }
        clients.add("tank" + nextId, sender);
        endpoint.send(sender, new RegisterResponse("tank" + nextId++));
    }

    private void deregister(InetSocketAddress sender) {
        if (clients.indexOf(sender) == -1) {
            System.out.println("Client not registered: " + sender);
            return;
        }
        if (clients.size() == 1) {
            done = true;
        }
        clients.remove(clients.indexOf(sender));
    }

    private void handoffFish(InetSocketAddress sender, HandoffRequest request) {
        int vector = request.getFish().getDirection().getVector();
        int index = clients.indexOf(sender);

        switch (vector) {
            case -1:
                sendLeft(index, request);
                break;
            case 1:
                sendRight(index, request);
                break;
            default:
                System.out.println("Unknown vector: " + vector);
                break;
        }
    }

    private void sendLeft(int index, HandoffRequest request) {
        InetSocketAddress leftNeighbor = clients.getLeftNeighborOf(index);
        endpoint.send(leftNeighbor, request);
    }

    private void sendRight(int index, HandoffRequest request) {
        InetSocketAddress rightNeighbor = clients.getRightNeighborOf(index);
        endpoint.send(rightNeighbor, request);
    }

    public static void main(String[] args) {
        new Broker().broker();
    }

}
