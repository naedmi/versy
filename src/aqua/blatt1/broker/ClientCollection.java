package aqua.blatt1.broker;

import java.util.ArrayList;
import java.util.List;

/*
 * This class is not thread-safe and hence must be used in a thread-safe way, e.g. thread confined or 
 * externally synchronized. 
 */

public class ClientCollection<T> {
	private class Client {
		final String id;
		final T client;
		long timestamp;

		Client(String id, T client, long timestamp) {
			this.id = id;
			this.client = client;
			this.timestamp = timestamp;
		}

		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}
	}

	private final List<Client> clients;

	public ClientCollection() {
		clients = new ArrayList<>();
	}

	public void add(String id, T client, long timestamp) {
		clients.add(new Client(id, client, timestamp));
	}

	public void remove(int index) {
		clients.remove(index);
	}

	public int indexOf(String id) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).id.equals(id))
				return i;
		return -1;
	}

	public int indexOf(T client) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).client.equals(client))
				return i;
		return -1;
	}

	public T getClient(int index) {
		return clients.get(index).client;
	}

	public int size() {
		return clients.size();
	}

	public T getLeftNeighborOf(int index) {
		return index == 0 ? clients.get(clients.size() - 1).client : clients.get(index - 1).client;
	}

	public T getRightNeighborOf(int index) {
		return index < clients.size() - 1 ? clients.get(index + 1).client : clients.get(0).client;
	}

	public void setTimestamp(int index, long timestamp) {
		clients.get(index).setTimestamp(timestamp);
	}

	public long getTimestamp(int index) {
		return clients.get(index).timestamp;
	}

}
