package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class RegisterResponse implements Serializable {
	private final String id;

	private long leaseTime = 1000;

	public RegisterResponse(String id, long leaseTime) {
		this.id = id;
		this.leaseTime = leaseTime;
	}

	public RegisterResponse(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public long getLeaseTime() {
		return leaseTime;
	}

}
