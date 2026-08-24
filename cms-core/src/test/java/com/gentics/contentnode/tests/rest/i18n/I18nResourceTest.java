package com.gentics.contentnode.tests.rest.i18n;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertSuccess;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getI18nResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.request.SetLanguageRequest;
import com.gentics.contentnode.rest.resource.I18nResource;
import com.gentics.contentnode.tests.utils.Auth;
import com.gentics.contentnode.tests.utils.Auth.AuthType;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Tests for the {@link I18nResource}.
 */
@RunWith(Parameterized.class)
public class I18nResourceTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	protected static Auth auth1;

	protected static Auth auth2;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		UserGroup nodeGroup = supply(t -> t.getObject(UserGroup.class, NODE_GROUP_ID));

		auth1 = new Auth(create(SystemUser.class, u -> {
			u.setLogin("testuser1");
			u.getUserGroups().add(nodeGroup);
		}).build());

		auth2 = new Auth(create(SystemUser.class, u -> {
			u.setLogin("testuser2");
			u.getUserGroups().add(nodeGroup);
		}).build());

		auth2.withAuth(AuthType.LOGIN, () -> {
			getI18nResource().setLanguage(new SetLanguageRequest().setCode("en"));
		});
	}

	@Parameters(name = "{index}: auth {0}, key {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		for (AuthType authType : List.of(AuthType.LOGIN, AuthType.TOKEN)) {
			data.add(new Object[] { authType, "rest.file.copy.success", new String[] { "4711", "0815" },
					"Die Datei mit der id 4711 wurde erfolgreich kopiert. Id der neuen Datei ist 0815.",
					"The file with id 4711 was successfully copied. The new fileId is 0815." });
		}

		return data;
	}

	/**
	 * Delete all systemsessions for every test case
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		operate(() -> DBUtils.update("DELETE FROM systemsession"));
	}

	/**
	 * Authentication type
	 */
	@Parameter(0)
	public AuthType authType;

	/**
	 * Translation key
	 */
	@Parameter(1)
	public String key;

	/**
	 * Translation parameters
	 */
	@Parameter(2)
	public String[] parameters;

	/**
	 * Expected translation in "de"
	 */
	@Parameter(3)
	public String expectedDe;

	/**
	 * Expected translation in "en"
	 */
	@Parameter(4)
	public String expectedEn;

	/**
	 * Test translation, when the language was set to "de" before
	 * @throws NodeException
	 */
	@Test
	public void testDe() throws NodeException {
		// set language to "de" for user1
		assertSuccess(() -> auth1.withAuth(authType,
				() -> getI18nResource().setLanguage(new SetLanguageRequest().setCode("de"))), null);

		// expect that translation for user1 is in "de"
		String translation = auth1.withAuth(authType, () -> getI18nResource().translateFromParam(key, List.of(parameters)));
		assertThat(translation).as("Translation for user1").isEqualTo(expectedDe);

		// expect that translation for user2 is in "en"
		translation = auth2.withAuth(authType, () -> getI18nResource().translateFromParam(key, List.of(parameters)));
		assertThat(translation).as("Translation for user2").isEqualTo(expectedEn);
	}

	/**
	 * Test translation, when the language was set to "en" before
	 * @throws NodeException
	 */
	@Test
	public void testEn() throws NodeException {
		// set language to "en" for user1
		assertSuccess(() -> auth1.withAuth(authType,
				() -> getI18nResource().setLanguage(new SetLanguageRequest().setCode("en"))), null);

		// expect that translation for user1 is in "en"
		String translation = auth1.withAuth(authType, () -> getI18nResource().translateFromParam(key, List.of(parameters)));
		assertThat(translation).as("Translation for user1").isEqualTo(expectedEn);

		// expect that translation for user2 is in "en"
		translation = auth2.withAuth(authType, () -> getI18nResource().translateFromParam(key, List.of(parameters)));
		assertThat(translation).as("Translation for user2").isEqualTo(expectedEn);
	}
}
