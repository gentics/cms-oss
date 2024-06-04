package com.gentics.contentnode.translation;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import com.gentics.api.lib.exception.NodeException;
import org.junit.jupiter.api.Test;

class SupportedProviderTest {

	@Test
	public void givenSupportedProvider_shouldMapToEnum() throws NodeException {
		SupportedProvider provider = SupportedProvider.fromString("deepl");

		assertThat(provider).isEqualTo(SupportedProvider.DEEPL);
	}

	@Test
	public void givenUnsupportedProvider_shouldThrowException() {
		Throwable exception = catchThrowable(
				() -> SupportedProvider.fromString("non-existing-provider"));
		assertThat(exception).isInstanceOf(NodeException.class);
		assertThat(exception).hasMessageContaining("Invalid provider value");
	}

}
