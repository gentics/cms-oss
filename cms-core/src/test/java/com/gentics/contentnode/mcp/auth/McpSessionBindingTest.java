package com.gentics.contentnode.mcp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.After;
import org.junit.Test;

import com.gentics.api.lib.i18n.Language;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;

/**
 * Unit tests for {@link McpSessionBinding}. Uses a bare-bones {@link Session} fake (no DB
 * involved - only the {@link ContentNodeHelper} ThreadLocal is touched by the class under test).
 */
public class McpSessionBindingTest {
	private static Session fakeSession(int id, int userId, int languageId) {
		return new Session() {
			@Override
			public int getId() {
				return id;
			}

			@Override
			public int getUserId() {
				return userId;
			}

			@Override
			public int getLanguageId() {
				return languageId;
			}

			@Override
			public Language getLanguage() {
				return null;
			}
		};
	}

	@After
	public void resetContentNodeHelperState() {
		ContentNodeHelper.setSession(null);
		ContentNodeHelper.setLanguageId(-1);
	}

	@Test
	public void testBindsGivenSessionAndRestoresPreviousOnClose() {
		Session previous = fakeSession(1, 10, 1);
		ContentNodeHelper.setSession(previous);

		Session newSession = fakeSession(2, 20, 2);
		try (McpSessionBinding binding = new McpSessionBinding(newSession)) {
			assertThat(ContentNodeHelper.getSession()).isSameAs(newSession);
			assertThat(ContentNodeHelper.getLanguageId(-1)).isEqualTo(2);
		}

		assertThat(ContentNodeHelper.getSession()).isSameAs(previous);
		assertThat(ContentNodeHelper.getLanguageId(-1)).isEqualTo(1);
	}

	@Test
	public void testBindsFixedBackendLanguageWhenNoSessionGiven() {
		ContentNodeHelper.setSession(null);
		ContentNodeHelper.setLanguageId(-1);

		try (McpSessionBinding binding = new McpSessionBinding(null)) {
			assertThat(ContentNodeHelper.getSession()).isNull();
			assertThat(ContentNodeHelper.getLanguageId(-1)).isEqualTo(2);
		}

		assertThat(ContentNodeHelper.getSession()).isNull();
		assertThat(ContentNodeHelper.getLanguageId(-1)).isEqualTo(-1);
	}

	@Test
	public void testRestoresPreviousStateEvenWhenBodyThrows() {
		Session previous = fakeSession(1, 10, 1);
		ContentNodeHelper.setSession(previous);

		assertThatThrownBy(() -> {
			try (McpSessionBinding binding = new McpSessionBinding(fakeSession(2, 20, 2))) {
				throw new IllegalStateException("boom");
			}
		}).isInstanceOf(IllegalStateException.class);

		assertThat(ContentNodeHelper.getSession()).isSameAs(previous);
		assertThat(ContentNodeHelper.getLanguageId(-1)).isEqualTo(1);
	}
}
