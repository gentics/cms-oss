package com.gentics.contentnode.etc;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Translation statistics
 */
public class TranslationStats {
	/**
	 * Counter for the number of translation requests
	 */
	protected AtomicInteger requests = new AtomicInteger();

	/**
	 * Counter for number or sent characters
	 */
	protected AtomicInteger charactersSent = new AtomicInteger();

	/**
	 * Counter for number of received characters
	 */
	protected AtomicInteger charactersReceived = new AtomicInteger();

	/**
	 * Add statistics for a single translation request
	 * @param sent characters sent
	 * @param received characters received
	 */
	public void addRequest(int sent, int received) {
		requests.addAndGet(1);
		charactersSent.addAndGet(sent);
		charactersReceived.addAndGet(received);
	}

	@Override
	public String toString() {
		return "Requests: %d, Characters sent: %d, Characters received: %d.".formatted(requests.get(),
				charactersSent.get(), charactersReceived.get());
	}
}
