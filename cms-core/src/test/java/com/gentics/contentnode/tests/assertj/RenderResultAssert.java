package com.gentics.contentnode.tests.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.assertj.core.api.AbstractAssert;

import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.render.RenderResult;

/**
 * Assertions for {@link RenderResult} instances
 */
public class RenderResultAssert extends AbstractAssert<RenderResultAssert, RenderResult> {
	protected RenderResultAssert(RenderResult actual) {
		super(actual, RenderResultAssert.class);
	}

	/**
	 * Assert that the {@link RenderResult} does not contain error messages
	 * @return fluent API
	 */
	public RenderResultAssert doesNotContainErrors() {
		Optional.ofNullable(actual.getMessages()).ifPresent(messages -> {
			List<NodeMessage> errorMessages = messages.stream()
					.filter(msg -> msg.getLevel().isMoreSpecificThan(Level.WARN)).collect(Collectors.toList());
			assertThat(errorMessages).as(descriptionText() + " errors").isEmpty();
		});
		return this;
	}
}
