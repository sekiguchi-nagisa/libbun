package libbun.util;

public final class SoftwareFault extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public SoftwareFault(String Message) {
		super(Message);
	}

	public SoftwareFault() {
		super();
	}

	public SoftwareFault(Throwable Cause){
		super(Cause);
	}

	public SoftwareFault(Object Message) {
		super(Message.toString());
	}
}