package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.db.DBUtils.select;
import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.exception.DuplicateValueException;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.response.ContentLanguageResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.LanguageList;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.LanguageResource;
import com.gentics.contentnode.rest.resource.impl.LanguageResourceImpl;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.tests.utils.Expected;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for the {@link LanguageResource}
 */
@GCNFeature(set = { Feature.WASTEBIN })
public class LanguageResourceTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static UserGroup group;
	private static SystemUser user;

	@Rule
	public ExceptionChecker exceptionChecker = new ExceptionChecker();

	private static Node node;

	private static Template template;

	private static Set<Integer> contentGroupIds;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		group = supply(() -> createUserGroup("TestGroup", NODE_GROUP_ID));
		user = supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(group)));

		node = supply(() -> createNode());
		template = supply(() -> createTemplate(node.getFolder(), "Template"));

		contentGroupIds = supply(() -> DBUtils.select("SELECT id FROM contentgroup", DBUtils.IDS));
	}

	@Before
	public void setup() throws NodeException {
		operate(() -> clear(node));

		operate(t -> {
			for (ContentLanguage language : t.getObjects(ContentLanguage.class, select("SELECT id FROM contentgroup", DBUtils.IDS))) {
				if (contentGroupIds.contains(language.getId())) {
					continue;
				}
				language.delete();
			}
		});
	}

	@Test
	public void testList() throws NodeException {
		LanguageList response = new LanguageResourceImpl().list(null, null, null);

		assertThat(response.getItems().stream().map(com.gentics.contentnode.rest.model.ContentLanguage::getCode)
				.collect(Collectors.toList())).as("Language codes").containsOnly("ar", "ar_EG", "bg", "bs", "cs", "da",
						"de", "en", "en_CA", "es", "et", "fa", "fi", "fr", "fr_CA", "hr", "hu", "it", "ja", "ko", "mk",
						"nl", "pl", "pt", "ro", "ru", "sk", "sl", "sq", "sr", "tr", "uk");
	}

	@Test
	public void testCreate() throws NodeException {
		ContentLanguageResponse response = assertRequiredPermissions(group, user, () -> {
			com.gentics.contentnode.rest.model.ContentLanguage language = new com.gentics.contentnode.rest.model.ContentLanguage()
					.setName("Klingonisch").setCode("tlh");
			return new LanguageResourceImpl().create(language);
		}, Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(ContentLanguage.TYPE_CONTENTLANGUAGE, 1, PermHandler.PERM_VIEW));

		assertThat(response.getLanguage()).as("Created language").hasFieldOrPropertyWithValue("name", "Klingonisch")
				.hasFieldOrPropertyWithValue("code", "tlh");
	}

	@Test
	@Expected(ex = RestMappedException.class, message = "Das Feld 'Name' darf nicht leer sein.")
	public void testCreateNoName() throws NodeException {
		operate(() -> {
			com.gentics.contentnode.rest.model.ContentLanguage language = new com.gentics.contentnode.rest.model.ContentLanguage()
					.setCode("tlh");
			assertResponseOK(new LanguageResourceImpl().create(language));
		});
	}

	@Test
	@Expected(ex = RestMappedException.class, message = "Das Feld 'Kürzel' darf nicht leer sein.")
	public void testCreateNoCode() throws NodeException {
		operate(() -> {
			com.gentics.contentnode.rest.model.ContentLanguage language = new com.gentics.contentnode.rest.model.ContentLanguage()
					.setName("Klingonisch");
			assertResponseOK(new LanguageResourceImpl().create(language));
		});
	}

	@Test
	public void testCreateDuplicateName() throws NodeException {
		operate(() -> {
			com.gentics.contentnode.rest.model.ContentLanguage language = new com.gentics.contentnode.rest.model.ContentLanguage()
					.setName("Klingonisch").setCode("tlh");
			assertResponseOK(new LanguageResourceImpl().create(language));
		});

		exceptionChecker.expect(DuplicateValueException.class,
				"Das Feld 'Name' darf nicht den Wert 'Klingonisch' haben, weil dieser Wert bereits verwendet wird.");
		operate(() -> {
			com.gentics.contentnode.rest.model.ContentLanguage language = new com.gentics.contentnode.rest.model.ContentLanguage()
					.setName("Klingonisch").setCode("tlh1");
			new LanguageResourceImpl().create(language);
		});
	}

	@Test
	public void testCreateDuplicateCode() throws NodeException {
		operate(() -> {
			com.gentics.contentnode.rest.model.ContentLanguage language = new com.gentics.contentnode.rest.model.ContentLanguage()
					.setName("Klingonisch").setCode("tlh");
			assertResponseOK(new LanguageResourceImpl().create(language));
		});

		exceptionChecker.expect(DuplicateValueException.class,
				"Das Feld 'Kürzel' darf nicht den Wert 'tlh' haben, weil dieser Wert bereits verwendet wird.");
		operate(() -> {
			com.gentics.contentnode.rest.model.ContentLanguage language = new com.gentics.contentnode.rest.model.ContentLanguage()
					.setName("Klingonisch1").setCode("tlh");
			new LanguageResourceImpl().create(language);
		});
	}

	@Test
	public void testRead() throws NodeException {
		int languageId = supply(() -> {
			com.gentics.contentnode.rest.model.ContentLanguage language = new com.gentics.contentnode.rest.model.ContentLanguage()
					.setName("Klingonisch").setCode("tlh");
			ContentLanguageResponse response = new LanguageResourceImpl().create(language);
			assertResponseOK(response);
			return response.getLanguage().getId();
		});

		ContentLanguageResponse response = assertRequiredPermissions(group, user, () -> {
			return new LanguageResourceImpl().get(Integer.toString(languageId));
		}, Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(ContentLanguage.TYPE_CONTENTLANGUAGE, 1, PermHandler.PERM_VIEW));

		assertThat(response.getLanguage()).as("Created language").hasFieldOrPropertyWithValue("name", "Klingonisch")
				.hasFieldOrPropertyWithValue("code", "tlh");
	}

	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Die Seitensprache '4711' wurde nicht gefunden.")
	public void testReadUnknown() throws NodeException {
		operate(() -> {
			new LanguageResourceImpl().get(Integer.toString(4711));
		});
	}

	@Test
	public void testUpdate() throws NodeException {
		int languageId = supply(() -> {
			com.gentics.contentnode.rest.model.ContentLanguage language = new com.gentics.contentnode.rest.model.ContentLanguage()
					.setName("Klingonisch").setCode("tlh");
			ContentLanguageResponse response = new LanguageResourceImpl().create(language);
			assertResponseOK(response);
			return response.getLanguage().getId();
		});

		ContentLanguageResponse response = assertRequiredPermissions(group, user, () -> {
			return new LanguageResourceImpl().update(Integer.toString(languageId),
					new com.gentics.contentnode.rest.model.ContentLanguage().setName("New Name").setCode("new"));
		}, Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(ContentLanguage.TYPE_CONTENTLANGUAGE, 1, PermHandler.PERM_VIEW));

		assertThat(response.getLanguage()).as("Updated language")
				.hasFieldOrPropertyWithValue("name", "New Name")
				.hasFieldOrPropertyWithValue("code", "new");
	}

	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Die Seitensprache '4711' wurde nicht gefunden.")
	public void testUpdateUnknown() throws NodeException {
		operate(() -> {
			com.gentics.contentnode.rest.model.ContentLanguage language = new com.gentics.contentnode.rest.model.ContentLanguage()
					.setName("Modified Name");
			new LanguageResourceImpl().update(Integer.toString(4711), language);
		});
	}

	@Test
	public void testUpdateDuplicateName() throws NodeException {
		int languageId = supply(() -> {
			ContentLanguageResponse response = new LanguageResourceImpl()
					.create(new com.gentics.contentnode.rest.model.ContentLanguage().setName("Klingonisch").setCode("tlh"));
			assertResponseOK(response);
			return response.getLanguage().getId();
		});

		operate(() -> {
			ContentLanguageResponse response = new LanguageResourceImpl()
					.create(new com.gentics.contentnode.rest.model.ContentLanguage().setName("Duplicate Name").setCode("dup"));
			assertResponseOK(response);
		});

		exceptionChecker.expect(DuplicateValueException.class,
				"Das Feld 'Name' darf nicht den Wert 'Duplicate Name' haben, weil dieser Wert bereits verwendet wird.");
		operate(() -> {
			com.gentics.contentnode.rest.model.ContentLanguage language = new com.gentics.contentnode.rest.model.ContentLanguage()
					.setName("Duplicate Name");
			new LanguageResourceImpl().update(Integer.toString(languageId), language);
		});
	}

	@Test
	public void testUpdateDuplicateCode() throws NodeException {
		int languageId = supply(() -> {
			ContentLanguageResponse response = new LanguageResourceImpl().create(
					new com.gentics.contentnode.rest.model.ContentLanguage().setName("Klingonisch").setCode("tlh"));
			assertResponseOK(response);
			return response.getLanguage().getId();
		});

		operate(() -> {
			ContentLanguageResponse response = new LanguageResourceImpl()
					.create(new com.gentics.contentnode.rest.model.ContentLanguage().setName("Duplicate Name")
							.setCode("dup"));
			assertResponseOK(response);
		});

		exceptionChecker.expect(DuplicateValueException.class,
				"Das Feld 'Kürzel' darf nicht den Wert 'dup' haben, weil dieser Wert bereits verwendet wird.");
		operate(() -> {
			com.gentics.contentnode.rest.model.ContentLanguage language = new com.gentics.contentnode.rest.model.ContentLanguage()
					.setCode("dup");
			new LanguageResourceImpl().update(Integer.toString(languageId), language);
		});
	}

	@Test
	public void testDelete() throws NodeException {
		int languageId = supply(() -> {
			ContentLanguageResponse response = new LanguageResourceImpl().create(
					new com.gentics.contentnode.rest.model.ContentLanguage().setName("Klingonisch").setCode("tlh"));
			assertResponseOK(response);
			return response.getLanguage().getId();
		});

		assertRequiredPermissions(group, user, () -> {
			new LanguageResourceImpl().delete(Integer.toString(languageId));
			return new GenericResponse(null, ResponseInfo.ok(""));
		}, Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(ContentLanguage.TYPE_CONTENTLANGUAGE, 1, PermHandler.PERM_VIEW));

		operate(t -> {
			assertThat(t.getObject(ContentLanguage.class, Integer.toString(languageId))).as("Deleted language").isNull();
		});
	}

	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Die Seitensprache '4711' wurde nicht gefunden.")
	public void testDeleteUnknown() throws NodeException {
		operate(() -> new LanguageResourceImpl().delete(Integer.toString(4711)));
	}

	@Test
	@Expected(ex = RestMappedException.class, message = "Die Seitensprache kann nicht gelöscht werden, da es noch 1 Seiten(n) in dieser Sprache gibt.")
	public void testDeleteUsed() throws NodeException {
		ContentLanguage language = supply(t -> {
			ContentLanguageResponse response = new LanguageResourceImpl().create(
					new com.gentics.contentnode.rest.model.ContentLanguage().setName("Klingonisch").setCode("tlh"));
			assertResponseOK(response);

			return t.getObject(ContentLanguage.class, response.getLanguage().getId());
		});

		operate(() -> createPage(node.getFolder(), template, "Page", null, language));

		operate(() -> new LanguageResourceImpl().delete(Integer.toString(language.getId())));
	}

	@Test
	@Expected(ex = RestMappedException.class, message = "Die Seitensprache kann nicht gelöscht werden, da es noch 1 Seiten(n) in dieser Sprache gibt (1 davon im Papierkorb).")
	public void testDeleteUsedWastebin() throws NodeException {
		ContentLanguage language = supply(t -> {
			ContentLanguageResponse response = new LanguageResourceImpl().create(
					new com.gentics.contentnode.rest.model.ContentLanguage().setName("Klingonisch").setCode("tlh"));
			assertResponseOK(response);

			return t.getObject(ContentLanguage.class, response.getLanguage().getId());
		});

		Page page = supply(() -> createPage(node.getFolder(), template, "Page", null, language));
		consume(Page::delete, page);

		operate(() -> new LanguageResourceImpl().delete(Integer.toString(language.getId())));
	}
}
