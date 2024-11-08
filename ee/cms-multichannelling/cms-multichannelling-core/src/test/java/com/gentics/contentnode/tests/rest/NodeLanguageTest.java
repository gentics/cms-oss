package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getNodeResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.clearNodeObjectCache;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.ContentLanguage;
import com.gentics.contentnode.rest.model.response.LanguageList;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.tests.utils.Expected;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for assigning content languages to nodes
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class NodeLanguageTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public ExceptionChecker exceptionChecker = new ExceptionChecker();

	private static Node node;
	private static Node channel;
	private static Template template;
	private static UserGroup group;
	private static SystemUser user;

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();
		node = supply(() -> createNode());
		channel = supply(() -> createChannel(node, "Channel", "channel", "/"));

		template = supply(() -> createTemplate(node.getFolder(), "Template"));

		group = supply(() -> createUserGroup("Testgroup", NODE_GROUP_ID));
		user = supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(group)));
	}

	@Before
	public void setup() throws NodeException, PortalCacheException {
		operate(() -> DBUtils.executeUpdate("DELETE FROM node_contentgroup", null));
		clearNodeObjectCache();
		node = execute(Node::reload, node);
		channel = execute(Node::reload, channel);

		operate(() -> clear(node));
		operate(() -> clear(channel));
	}

	/**
	 * Test adding a language by language code
	 * @throws NodeException
	 */
	@Test
	public void testAddByCode() throws NodeException {
		ContentLanguage french = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("fr")));
		operate(() -> assertResponseCodeOk(getNodeResource().addLanguage(Integer.toString(node.getId()), french.getCode())));

		operate(() -> {
			LanguageList response = getNodeResource().languages(Integer.toString(node.getId()), null, null);
			assertResponseCodeOk(response);
			assertThat(response.getItems()).as("Assigned languages").usingFieldByFieldElementComparator().containsExactly(french);
		});
	}

	/**
	 * Test adding a language by local ID
	 * @throws NodeException
	 */
	@Test
	public void testAddByLocalId() throws NodeException {
		ContentLanguage danish = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("da")));
		operate(() -> assertResponseCodeOk(getNodeResource().addLanguage(Integer.toString(node.getId()), Integer.toString(danish.getId()))));

		operate(() -> {
			LanguageList response = getNodeResource().languages(Integer.toString(node.getId()), null, null);
			assertResponseCodeOk(response);
			assertThat(response.getItems()).as("Assigned languages").usingFieldByFieldElementComparator().containsExactly(danish);
		});
	}

	/**
	 * Test adding a language by global ID
	 * @throws NodeException
	 */
	@Test
	public void testAddByGlobalId() throws NodeException {
		String globalId = supply(() -> getLanguage("bg").getGlobalId().toString());
		ContentLanguage bulgarian = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("bg")));
		operate(() -> assertResponseCodeOk(getNodeResource().addLanguage(Integer.toString(node.getId()), globalId)));

		operate(() -> {
			LanguageList response = getNodeResource().languages(Integer.toString(node.getId()), null, null);
			assertResponseCodeOk(response);
			assertThat(response.getItems()).as("Assigned languages").usingFieldByFieldElementComparator().containsExactly(bulgarian);
		});
	}

	/**
	 * Test that adding a language adds it at the end of the language list
	 * @throws NodeException
	 */
	@Test
	public void testAddingOrder() throws NodeException {
		ContentLanguage hungarian = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("hu")));
		ContentLanguage italian = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("it")));
		ContentLanguage spanish = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("es")));
		operate(() -> assertResponseCodeOk(getNodeResource().addLanguage(Integer.toString(node.getId()), hungarian.getCode())));
		operate(() -> assertResponseCodeOk(getNodeResource().addLanguage(Integer.toString(node.getId()), italian.getCode())));
		operate(() -> assertResponseCodeOk(getNodeResource().addLanguage(Integer.toString(node.getId()), spanish.getCode())));

		operate(() -> {
			LanguageList response = getNodeResource().languages(Integer.toString(node.getId()), null, null);
			assertResponseCodeOk(response);
			assertThat(response.getItems()).as("Assigned languages").usingFieldByFieldElementComparator().containsExactly(hungarian, italian, spanish);
		});
	}

	/**
	 * Test that adding a language adds it at the end of the language list
	 * @throws NodeException
	 */
	@Test
	public void testChannelLanguages() throws NodeException {
		operate(() -> {
			LanguageList response = getNodeResource().languages(Integer.toString(channel.getId()), null, null);
			assertResponseCodeOk(response);
			assertThat(response.getItems()).as("Assigned languages").usingFieldByFieldElementComparator().isEmpty();
		});
		ContentLanguage french = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("fr")));
		ContentLanguage danish = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("da")));
		ContentLanguage bulgarian = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("bg")));
		operate(() -> assertResponseCodeOk(getNodeResource().addLanguage(Integer.toString(node.getId()), french.getCode())));
		operate(() -> assertResponseCodeOk(getNodeResource().addLanguage(Integer.toString(node.getId()), danish.getCode())));
		operate(() -> assertResponseCodeOk(getNodeResource().addLanguage(Integer.toString(node.getId()), bulgarian.getCode())));

		operate(() -> {
			LanguageList response = getNodeResource().languages(Integer.toString(node.getId()), null, null);
			assertResponseCodeOk(response);
			assertThat(response.getItems()).as("Assigned languages").usingFieldByFieldElementComparator().containsExactly(french, danish, bulgarian);
		});
		operate(() -> {
			LanguageList response = getNodeResource().languages(Integer.toString(channel.getId()), null, null);
			assertResponseCodeOk(response);
			assertThat(response.getItems()).as("Assigned languages").usingFieldByFieldElementComparator().containsExactly(french, danish, bulgarian);
		});
	}


	/**
	 * Test that adding a language twice does not change the language order
	 * @throws NodeException
	 */
	@Test
	public void testAddingTwice() throws NodeException {
		ContentLanguage arabic = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("ar")));
		ContentLanguage polish = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("pl")));
		ContentLanguage albanian = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("sq")));

		operate(() -> assertResponseCodeOk(getNodeResource().addLanguage(Integer.toString(node.getId()), arabic.getCode())));
		operate(() -> assertResponseCodeOk(getNodeResource().addLanguage(Integer.toString(node.getId()), polish.getCode())));
		operate(() -> assertResponseCodeOk(getNodeResource().addLanguage(Integer.toString(node.getId()), arabic.getCode())));
		operate(() -> assertResponseCodeOk(getNodeResource().addLanguage(Integer.toString(node.getId()), albanian.getCode())));

		operate(() -> {
			LanguageList response = getNodeResource().languages(Integer.toString(node.getId()), null, null);
			assertResponseCodeOk(response);
			assertThat(response.getItems()).as("Assigned languages").usingFieldByFieldElementComparator().containsExactly(arabic, polish, albanian);
		});
	}

	/**
	 * Test adding an unknown language
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Die Sprache unknown konnte nicht gefunden werden.")
	public void testAddUnknown() throws NodeException {
		operate(() -> getNodeResource().addLanguage(Integer.toString(node.getId()), "unknown"));
	}

	/**
	 * Test defining the order by setting the languages
	 * @throws NodeException
	 */
	@Test
	public void testOrder() throws NodeException {
		ContentLanguage croatian = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("hr")));
		ContentLanguage japanese = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("ja")));
		ContentLanguage finish = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("fi")));
		operate(() -> assertResponseCodeOk(getNodeResource().addLanguage(Integer.toString(node.getId()), croatian.getCode())));
		operate(() -> assertResponseCodeOk(getNodeResource().addLanguage(Integer.toString(node.getId()), japanese.getCode())));
		operate(() -> assertResponseCodeOk(getNodeResource().addLanguage(Integer.toString(node.getId()), finish.getCode())));

		operate(() -> {
			LanguageList response = getNodeResource().languages(Integer.toString(node.getId()), null, null);
			assertResponseCodeOk(response);
			assertThat(response.getItems()).as("Assigned languages").usingFieldByFieldElementComparator().containsExactly(croatian, japanese, finish);
		});

		operate(() -> assertResponseCodeOk(getNodeResource().setLanguages(Integer.toString(node.getId()), Arrays.asList(finish, japanese, croatian))));

		operate(() -> {
			LanguageList response = getNodeResource().languages(Integer.toString(node.getId()), null, null);
			assertResponseCodeOk(response);
			assertThat(response.getItems()).as("Assigned languages").usingFieldByFieldElementComparator().containsExactly(finish, japanese, croatian);
		});
	}

	/**
	 * Test setting an unknown language
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Die Sprache 4711 konnte nicht gefunden werden.")
	public void testSetUnknown() throws NodeException {
		operate(() -> getNodeResource().setLanguages(Integer.toString(node.getId()), Arrays.asList(new ContentLanguage().setId(4711))));
	}

	/**
	 * Test removing a language by code
	 * @throws NodeException
	 */
	@Test
	public void testRemoveByCode() throws NodeException {
		ContentLanguage french = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("fr")));
		ContentLanguage danish = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("da")));
		ContentLanguage bulgarian = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("bg")));
		operate(() -> assertResponseCodeOk(getNodeResource().setLanguages(Integer.toString(node.getId()), Arrays.asList(french, danish, bulgarian))));

		operate(() -> {
			LanguageList response = getNodeResource().languages(Integer.toString(node.getId()), null, null);
			assertResponseCodeOk(response);
			assertThat(response.getItems()).as("Assigned languages").usingFieldByFieldElementComparator().containsExactly(french, danish, bulgarian);
		});

		operate(() -> assertResponseCodeOk(getNodeResource().removeLanguage(Integer.toString(node.getId()), danish.getCode())));

		operate(() -> {
			LanguageList response = getNodeResource().languages(Integer.toString(node.getId()), null, null);
			assertResponseCodeOk(response);
			assertThat(response.getItems()).as("Assigned languages").usingFieldByFieldElementComparator().containsExactly(french, bulgarian);
		});
	}

	/**
	 * Test removing a language by local ID
	 * @throws NodeException
	 */
	@Test
	public void testRemoveByLocalId() throws NodeException {
		ContentLanguage french = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("fr")));
		ContentLanguage danish = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("da")));
		ContentLanguage bulgarian = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("bg")));
		operate(() -> assertResponseCodeOk(getNodeResource().setLanguages(Integer.toString(node.getId()), Arrays.asList(french, danish, bulgarian))));

		operate(() -> {
			LanguageList response = getNodeResource().languages(Integer.toString(node.getId()), null, null);
			assertResponseCodeOk(response);
			assertThat(response.getItems()).as("Assigned languages").usingFieldByFieldElementComparator().containsExactly(french, danish, bulgarian);
		});

		operate(() -> assertResponseCodeOk(getNodeResource().removeLanguage(Integer.toString(node.getId()), Integer.toString(danish.getId()))));

		operate(() -> {
			LanguageList response = getNodeResource().languages(Integer.toString(node.getId()), null, null);
			assertResponseCodeOk(response);
			assertThat(response.getItems()).as("Assigned languages").usingFieldByFieldElementComparator().containsExactly(french, bulgarian);
		});
	}

	/**
	 * Test removing a language by global ID
	 * @throws NodeException
	 */
	@Test
	public void testRemoveByGlobalId() throws NodeException {
		String globalId = supply(() -> getLanguage("da").getGlobalId().toString());
		ContentLanguage french = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("fr")));
		ContentLanguage danish = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("da")));
		ContentLanguage bulgarian = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("bg")));
		operate(() -> assertResponseCodeOk(getNodeResource().setLanguages(Integer.toString(node.getId()), Arrays.asList(french, danish, bulgarian))));

		operate(() -> {
			LanguageList response = getNodeResource().languages(Integer.toString(node.getId()), null, null);
			assertResponseCodeOk(response);
			assertThat(response.getItems()).as("Assigned languages").usingFieldByFieldElementComparator().containsExactly(french, danish, bulgarian);
		});

		operate(() -> assertResponseCodeOk(getNodeResource().removeLanguage(Integer.toString(node.getId()), globalId)));

		operate(() -> {
			LanguageList response = getNodeResource().languages(Integer.toString(node.getId()), null, null);
			assertResponseCodeOk(response);
			assertThat(response.getItems()).as("Assigned languages").usingFieldByFieldElementComparator().containsExactly(french, bulgarian);
		});
	}

	/**
	 * Test that removing a language, which is still used, does not work (throws an error)
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = RestMappedException.class, message = "Die Sprache kann nicht deaktiviert werden, weil sich in diesem Node noch 1 Seiten in dieser Sprache (0 davon im Papierkorb) befinden.")
	public void testRemoveUsed() throws NodeException {
		com.gentics.contentnode.object.ContentLanguage french = supply(() -> getLanguage("fr"));
		ContentLanguage frenchModel = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(french));
		operate(() -> assertResponseCodeOk(getNodeResource().addLanguage(Integer.toString(node.getId()), frenchModel.getCode())));

		supply(() -> createPage(node.getFolder(), template, "Page", null, french));

		operate(() -> getNodeResource().removeLanguage(Integer.toString(node.getId()), frenchModel.getCode()));
	}

	/**
	 * Test that removing a language, which is still used, by setting languages does not work (throws an error)
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = RestMappedException.class, message = "Die Sprache kann nicht deaktiviert werden, weil sich in diesem Node noch 1 Seiten in dieser Sprache (0 davon im Papierkorb) befinden.")
	public void testRemoveUsedBySetting() throws NodeException {
		com.gentics.contentnode.object.ContentLanguage french = supply(() -> getLanguage("fr"));
		ContentLanguage frenchModel = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(french));
		ContentLanguage englishModel = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("en")));
		operate(() -> assertResponseCodeOk(getNodeResource().setLanguages(Integer.toString(node.getId()), Arrays.asList(englishModel, frenchModel))));

		supply(() -> createPage(node.getFolder(), template, "Page", null, french));
		operate(() -> getNodeResource().setLanguages(Integer.toString(node.getId()), Arrays.asList(englishModel)));
	}

	/**
	 * Test required permissions to add a language
	 * @throws NodeException
	 */
	@Test
	public void testAddPermission() throws NodeException {
		ContentLanguage french = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("fr")));
		Integer rootFolderId = supply(() -> node.getFolder().getId());
		assertRequiredPermissions(group, user, () -> getNodeResource().addLanguage(Integer.toString(node.getId()), french.getCode()),
				Triple.of(Node.TYPE_NODE, rootFolderId, PermHandler.PERM_VIEW), Triple.of(Node.TYPE_NODE, rootFolderId, PermHandler.PERM_FOLDER_UPDATE));
	}

	/**
	 * Test required permissions to remove a language
	 * @throws NodeException
	 */
	@Test
	public void testRemovePermission() throws NodeException {
		ContentLanguage french = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("fr")));
		Integer rootFolderId = supply(() -> node.getFolder().getId());
		operate(() -> assertResponseCodeOk(getNodeResource().addLanguage(Integer.toString(node.getId()), french.getCode())));
		assertRequiredPermissions(group, user, () -> getNodeResource().removeLanguage(Integer.toString(node.getId()), french.getCode()),
				Triple.of(Node.TYPE_NODE, rootFolderId, PermHandler.PERM_VIEW), Triple.of(Node.TYPE_NODE, rootFolderId, PermHandler.PERM_FOLDER_UPDATE));
	}

	/**
	 * Test required permissions to set languages
	 * @throws NodeException
	 */
	@Test
	public void testSetPermission() throws NodeException {
		ContentLanguage french = supply(() -> com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(getLanguage("fr")));
		Integer rootFolderId = supply(() -> node.getFolder().getId());
		assertRequiredPermissions(group, user, () -> getNodeResource().setLanguages(Integer.toString(node.getId()), Arrays.asList(french)),
				Triple.of(Node.TYPE_NODE, rootFolderId, PermHandler.PERM_VIEW), Triple.of(Node.TYPE_NODE, rootFolderId, PermHandler.PERM_FOLDER_UPDATE));
	}
}
