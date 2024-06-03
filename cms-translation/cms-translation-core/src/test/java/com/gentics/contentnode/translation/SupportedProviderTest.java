package com.gentics.contentnode.translation;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

import com.gentics.api.lib.exception.NodeException;
import org.junit.jupiter.api.Test;

class SupportedProviderTest {

	@Test
	public void givenSupportedProvider_shouldMapToEnum() throws NodeException {
		SupportedProvider provider = SupportedProvider.fromString("deepl");

		assertThat(provider).isEqualTo(SupportedProvider.DEEPL);
	}

	@Test
	public void givenUnSupportedProvider_shouldThrowException() {
		assertThatThrownBy(
				() -> SupportedProvider.valueOf("non-existing-provider")
		);
	}

}
