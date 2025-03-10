package com.gentics.contentnode.tests.rest.client;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.perm.TypePerms.admin;
import static com.gentics.contentnode.perm.TypePerms.contentadmin;
import static com.gentics.contentnode.perm.TypePerms.contentrepository;
import static com.gentics.contentnode.perm.TypePerms.contentrepositoryadmin;
import static com.gentics.contentnode.perm.TypePerms.groupadmin;
import static com.gentics.contentnode.rest.model.perm.PermType.read;
import static com.gentics.contentnode.rest.model.perm.PermType.setperm;
import static com.gentics.contentnode.rest.model.perm.PermType.setuserperm;
import static com.gentics.contentnode.rest.util.MiscUtils.setPermissions;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.attribute;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.QueueEntry;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.model.CRElasticsearchModel;
import com.gentics.contentnode.rest.model.ContentRepositoryListResponse;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.PasswordType;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.rest.model.TagmapEntryListResponse;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.perm.TypePermissionItem;
import com.gentics.contentnode.rest.model.request.TypePermissionRequest;
import com.gentics.contentnode.rest.model.response.ContentRepositoryResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.TypePermissionResponse;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.testutils.infrastructure.TestEnvironment;

/**
 * Test cases for dealing with CRs using the REST Client
 */
@RunWith(value = Parameterized.class)
public class ContentRepositoryClientTest {
	/**
	 * Root path to the CR Resource
	 */
	private static final String CR_PATH = "contentrepositories";

	/**
	 * Test context
	 */
	private static DBTestContext testContext = new DBTestContext();

	/**
	 * REST App context
	 */
	private static RESTAppContext restContext = new RESTAppContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	/**
	 * Login of the test user
	 */
	private final static String LOGIN = "test";

	/**
	 * Password of the test user
	 */
	private final static String PASSWORD = "test";

	/**
	 * ID of an unknown ContentRepository
	 */
	private final static String UNKNOWN_ID = "4711";

	/**
	 * Test group
	 */
	private static UserGroup nodeGroup;

	/**
	 * Sub Group
	 */
	private static UserGroup subGroup;

	@Parameters(name = "{index}: mediatype {0}, permission {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (MediaType mediaType : Arrays.asList(MediaType.APPLICATION_JSON_TYPE /*, MediaType.APPLICATION_XML_TYPE */)) {
			for (boolean permission : Arrays.asList(true, false)) {
				data.add(new Object[] { mediaType, permission });
			}
		}
		return data;
	}

	/**
	 * Create static test data
	 * 
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setUpOnce() throws NodeException {
		if (TransactionManager.getCurrentTransactionOrNull() != null) {
			TransactionManager.getCurrentTransaction().commit();
		}
		// load group
		nodeGroup = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<UserGroup> groups = t.getObjects(UserGroup.class, Arrays.asList(2));
			assertThat(groups).as("User Groups").hasSize(1);
			return groups.get(0);
		});

		// create test user
		Trx.operate(() -> {
			Creator.createUser(LOGIN, PASSWORD, "Tester", "Tester", "", Arrays.asList(nodeGroup));
		});

		// create sub group
		subGroup = supply(() -> createUserGroup("Subgroup", nodeGroup.getId()));
	}

	/**
	 * Tested mediatype
	 */
	@Parameter(0)
	public MediaType mediaType;

	/**
	 * True when testing with sufficient permissions
	 */
	@Parameter(1)
	public boolean permission;

	/**
	 * Truncate systemsession table for each test and remove all CRs
	 * 
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		Trx.operate(() -> DBUtils.executeStatement("DELETE FROM systemsession", Transaction.UPDATE_STATEMENT));
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			for (ContentRepository cr : t.getObjects(ContentRepository.class, DBUtils.select("SELECT id FROM contentrepository", DBUtils.IDS))) {
				cr.delete(true);
			}
		});
		// reset permissions
		Trx.operate(() -> PermHandler.setPermissions(ContentRepository.TYPE_CONTENTREPOSITORIES, Arrays.asList(nodeGroup), PermHandler.FULL_PERM));
	}

	/**
	 * Test creating a CR
	 * @throws RestException
	 * @throws NodeException
	 */
	@Test
	public void testCreate() throws RestException, NodeException {
		if (!permission) {
			Trx.operate(() -> {
				PermHandler.setPermissions(ContentRepository.TYPE_CONTENTREPOSITORIES, Arrays.asList(nodeGroup), PermHandler.EMPTY_PERM);
			});
		}

		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			ContentRepositoryModel crModel = new ContentRepositoryModel();
			crModel.setCrType(Type.cr);
			crModel.setDbType("mysql");
			crModel.setName("Test CR");

			Response response = client.get().base().path(CR_PATH).request(mediaType).post(Entity.entity(crModel, mediaType));
			if (permission) {
				ContentRepositoryResponse createResponse = response.readEntity(ContentRepositoryResponse.class);
				client.get().assertResponse(createResponse);
				ContentRepositoryModel responseModel = createResponse.getContentRepository();
				assertThat(responseModel).as("Created CR").isNotNull();

				Trx.consume(id -> {
					Transaction t = TransactionManager.getCurrentTransaction();
					ContentRepository cr = t.getObject(ContentRepository.class, id);
					assertThat(cr).as("CR")
						.isNotNull()
						.has(responseModel.getId())
						.has(new GlobalId(responseModel.getGlobalId()))
						.has(attribute("name", "Test CR"))
						.has(attribute("crType", Type.cr))
						.has(attribute("dbType", "mysql"));

					try {
						// insert the static tagmap entries with cr_id 0, this statement was used in the PHP implementation to add the default tagmap entries
						DBUtils.executeUpdate(StringUtils.readStream(getClass().getResourceAsStream("static_tagmap_entries.sql")), null);
						// clear the "reserved" flag for some of the unnecessary tagmap entries
						DBUtils.executeUpdate("UPDATE tagmap SET static = 0 WHERE contentrepository_id = 0 AND mapname IN ('createtimestamp', 'creator', 'creatoremail', 'editor', 'editoremail', 'edittimestamp', 'priority', 'publisher', 'publishermail', 'publishtimestamp')", null);
					} catch (IOException e) {
						throw new NodeException(e);
					}
					List<TagmapEntry> expected = t.getObjects(TagmapEntry.class,
							DBUtils.select("SELECT id FROM tagmap WHERE contentrepository_id = 0", DBUtils.IDS));
					assertThat(expected).as("Expected static tagmap Entries").isNotEmpty();

					assertThat(cr.getEntries())
							.as("Tagmap Entries").usingElementComparatorOnFields("tagname", "mapname", "object", "targetType", "attributetype", "multivalue",
									"static", "optimized", "filesystem", "foreignlinkAttribute", "foreignlinkAttributeRule", "category")
							.containsOnlyElementsOf(expected);

				}, createResponse.getContentRepository().getGlobalId());

			} else {
				assertThat(response.getStatusInfo()).as("Response status").isEqualTo(Status.FORBIDDEN);
				assertThat(response.readEntity(GenericResponse.class)).as("Response").hasCode(ResponseCode.PERMISSION)
						.containsMessage(Message.Type.CRITICAL, "rest.permission.required");
			}
		}
	}

	/**
	 * Test getting a single CR
	 * @throws RestException
	 * @throws NodeException
	 */
	@Test
	public void testGet() throws RestException, NodeException {
		ContentRepository nodeCR = create(ContentRepository.class, cr -> {
			cr.setName("CR Name");
			cr.setCrType(Type.mccr);
			cr.setDbType("oracle");
			cr.setBasepath("basepath-value");
			cr.setUrl("url-value");
			cr.setUsername("username-value");
			cr.setPassword("password-value");
			cr.setInstantPublishing(true);
			cr.setLanguageInformation(true);
			cr.setPermissionInformation(true);
			cr.setDiffDelete(true);
		});

		if (permission) {
			Trx.operate(() -> {
				String perms = new Permission(PermHandler.PERM_VIEW).toString();
				PermHandler.setPermissions(ContentRepository.TYPE_CONTENTREPOSITORY, nodeCR.getId(), Arrays.asList(nodeGroup), perms);
			});
		}

		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			for (String id : Arrays.asList(Integer.toString(nodeCR.getId()), nodeCR.getGlobalId().toString())) {
				Response response = client.get().base().path(CR_PATH).path(id).request(mediaType).get();
				if (permission) {
					ContentRepositoryResponse getResponse = response.readEntity(ContentRepositoryResponse.class);
					client.get().assertResponse(getResponse);
					ContentRepositoryModel responseModel = getResponse.getContentRepository();
					assertThat(responseModel).as("CR fetched with %s", id)
						.isNotNull()
						.has(attribute("name", "CR Name"))
						.has(attribute("crType", Type.mccr))
						.has(attribute("dbType", "oracle"))
						.has(attribute("basepath", "basepath-value"))
						.has(attribute("url", "url-value"))
						.has(attribute("username", "username-value"))
						.has(attribute("password", null))
						.has(attribute("instantPublishing", true))
						.has(attribute("languageInformation", true))
						.has(attribute("permissionInformation", true))
						.has(attribute("diffDelete", true));
				} else {
					assertThat(response.getStatusInfo()).as("Response status").isEqualTo(Status.FORBIDDEN);
					assertThat(response.readEntity(GenericResponse.class)).as("Response").hasCode(ResponseCode.PERMISSION)
							.containsMessage(Message.Type.CRITICAL, "contentrepository.nopermission", id);
				}
			}
		}
	}

	/**
	 * Test getting an unknown CR
	 * @throws RestException
	 * @throws NodeException
	 */
	@Test
	public void testGetUnknown() throws RestException, NodeException {
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			Response response = client.get().base().path(CR_PATH).path(UNKNOWN_ID).request(mediaType).get();
			assertThat(response.getStatusInfo()).as("Response status").isEqualTo(Status.NOT_FOUND);
			assertThat(response.readEntity(GenericResponse.class)).as("Response").hasCode(ResponseCode.NOTFOUND)
					.containsMessage(Message.Type.WARNING, "contentrepository.notfound", UNKNOWN_ID);
		}
	}

	/**
	 * Test listing CRs
	 * @throws RestException
	 * @throws NodeException
	 */
	@Test
	public void testList() throws RestException, NodeException {
		ContentRepository firstCR = create(ContentRepository.class, cr -> {
			cr.setName("First");
			cr.setCrType(Type.mccr);
			cr.setDbType("oracle");
		});
		ContentRepository secondCR = create(ContentRepository.class, cr -> {
			cr.setName("Second");
			cr.setCrType(Type.mccr);
			cr.setDbType("oracle");
		});
		ContentRepository thirdCR = create(ContentRepository.class, cr -> {
			cr.setName("Third");
			cr.setCrType(Type.mccr);
			cr.setDbType("oracle");
		});
		create(ContentRepository.class, cr -> {
			cr.setName("No Permission");
			cr.setCrType(Type.mccr);
			cr.setDbType("oracle");
		});

		if (permission) {
			Trx.operate(() -> {
				String perms = new Permission(PermHandler.PERM_VIEW).toString();
				PermHandler.setPermissions(ContentRepository.TYPE_CONTENTREPOSITORY, firstCR.getId(), Arrays.asList(nodeGroup), perms);
				PermHandler.setPermissions(ContentRepository.TYPE_CONTENTREPOSITORY, secondCR.getId(), Arrays.asList(nodeGroup), perms);
				PermHandler.setPermissions(ContentRepository.TYPE_CONTENTREPOSITORY, thirdCR.getId(), Arrays.asList(nodeGroup), perms);
			});
		}

		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			ContentRepositoryListResponse listResponse = client.get().base().path(CR_PATH).request(mediaType)
					.get(ContentRepositoryListResponse.class);
			client.get().assertResponse(listResponse);
			List<ContentRepositoryModel> itemList = listResponse.getItems();
			assertThat(itemList).as("Item list").isNotNull();

			if (permission) {
				List<ContentRepositoryModel> expected = new ArrayList<>();

				for (ContentRepository cr : Arrays.asList(firstCR, secondCR, thirdCR)) {
					ContentRepositoryResponse getResponse = client.get().base().path(CR_PATH).path(Integer.toString(cr.getId()))
							.request(mediaType).get(ContentRepositoryResponse.class);
					client.get().assertResponse(getResponse);
					expected.add(getResponse.getContentRepository());
				}
				assertThat(itemList).as("Item list").usingFieldByFieldElementComparator().containsOnlyElementsOf(expected);
			} else {
				assertThat(itemList).as("Item list").isEmpty();
			}
		};
	}

	/**
	 * Test updating a CR
	 * @throws RestException
	 * @throws NodeException
	 */
	@Test
	public void testUpdate() throws RestException, NodeException {
		ContentRepository nodeCR = create(ContentRepository.class, cr -> {
			cr.setName("Original Name");
			cr.setCrType(Type.mccr);
			cr.setDbType("oracle");
			cr.setBasepath("basepath-value");
			cr.setUrl("url-value");
			cr.setUsername("username-value");
			cr.setPassword("password-value");
			cr.setInstantPublishing(true);
			cr.setLanguageInformation(true);
			cr.setPermissionInformation(true);
			cr.setDiffDelete(true);
			cr.setProjectPerNode(false);
		});

		if (permission) {
			Trx.consume(id -> {
				String perms = new Permission(PermHandler.PERM_VIEW, PermHandler.PERM_CONTENTREPOSITORY_UPDATE).toString();
				PermHandler.setPermissions(ContentRepository.TYPE_CONTENTREPOSITORY, id, Arrays.asList(nodeGroup), perms);
			}, nodeCR.getId());
		}

		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			ContentRepositoryModel crModel = new ContentRepositoryModel();
			crModel.setName("Test CR");
			crModel.setCrType(Type.cr);
			crModel.setDbType("mysql");
			crModel.setBasepath("new basepath-value");
			crModel.setUrl("new url-value");
			crModel.setUsername("new username-value");
			crModel.setPassword("new password-value");
			crModel.setPasswordType(PasswordType.value);
			crModel.setInstantPublishing(false);
			crModel.setLanguageInformation(false);
			crModel.setPermissionInformation(false);
			crModel.setDiffDelete(false);
			crModel.setProjectPerNode(true);
			String crId = Integer.toString(nodeCR.getId());

			Response response = client.get().base().path(CR_PATH).path(crId).request(mediaType).put(Entity.entity(crModel, mediaType));
			if (permission) {
				ContentRepositoryResponse updateResponse = response.readEntity(ContentRepositoryResponse.class);
				client.get().assertResponse(updateResponse);
				ContentRepositoryModel responseModel = updateResponse.getContentRepository();
				assertThat(responseModel).as("Updated CR").isNotNull();

				nodeCR = Trx.execute(c -> TransactionManager.getCurrentTransaction().getObject(c), nodeCR);
				Trx.consume(cr -> {
					assertThat(cr).as("CR")
						.isNotNull()
						.has(responseModel.getId())
						.has(new GlobalId(responseModel.getGlobalId()))
						.has(attribute("name", "Test CR"))
						.has(attribute("crType", Type.cr))
						.has(attribute("dbType", "mysql"))
						.has(attribute("basepath", "new basepath-value"))
						.has(attribute("url", "new url-value"))
						.has(attribute("username", "new username-value"))
						.has(attribute("password", "new password-value"))
						.has(attribute("instantPublishing", false))
						.has(attribute("languageInformation", false))
						.has(attribute("permissionInformation", false))
						.has(attribute("diffDelete", false))
						.has(attribute("projectPerNode", true));
				}, nodeCR);
			} else {
				assertThat(response.getStatusInfo()).as("Response status").isEqualTo(Status.FORBIDDEN);
				assertThat(response.readEntity(GenericResponse.class)).as("Response").hasCode(ResponseCode.PERMISSION)
						.containsMessage(Message.Type.CRITICAL, "contentrepository.nopermission", crId);
			}
		}
	}

	/**
	 * Test updating an unknown CR
	 * @throws RestException
	 * @throws NodeException
	 */
	@Test
	public void testUpdateUnknown() throws RestException, NodeException {
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			ContentRepositoryModel crModel = new ContentRepositoryModel();
			crModel.setName("Test CR");

			Response response = client.get().base().path(CR_PATH).path(UNKNOWN_ID).request(mediaType).put(Entity.entity(crModel, mediaType));
			assertThat(response.getStatusInfo()).as("Response status").isEqualTo(Status.NOT_FOUND);
			assertThat(response.readEntity(GenericResponse.class)).as("Response").hasCode(ResponseCode.NOTFOUND)
					.containsMessage(Message.Type.WARNING, "contentrepository.notfound", UNKNOWN_ID);
		}
	}

	/**
	 * Test deleting a CR
	 * @throws RestException
	 * @throws NodeException
	 */
	@Test
	public void testDelete() throws RestException, NodeException {
		ContentRepository nodeCR = create(ContentRepository.class, cr -> {
			cr.setName("Original Name");
			cr.setCrType(Type.mccr);
			cr.setDbType("oracle");
		});

		if (permission) {
			Trx.consume(id -> {
				String perms = new Permission(PermHandler.PERM_VIEW, PermHandler.PERM_CONTENTREPOSITORY_DELETE).toString();
				PermHandler.setPermissions(ContentRepository.TYPE_CONTENTREPOSITORY, id, Arrays.asList(nodeGroup), perms);
			}, nodeCR.getId());
		}

		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			String crId = Integer.toString(nodeCR.getId());
			Response deleteResponse = client.get().base().path(CR_PATH).path(crId).request(mediaType).delete();
			assertThat(deleteResponse).as("Delete response").isNotNull();
			if (permission) {
				assertThat(deleteResponse.getStatusInfo()).as("Response status").isEqualTo(Status.NO_CONTENT);
			} else {
				assertThat(deleteResponse.getStatusInfo()).as("Response status").isEqualTo(Status.FORBIDDEN);
				assertThat(deleteResponse.readEntity(GenericResponse.class)).as("Response").hasCode(ResponseCode.PERMISSION).containsMessage(Message.Type.CRITICAL,
						"contentrepository.nopermission", crId);
			}
		}

		Trx.operate(t -> {
			if (permission) {
				assertThat(t.getObject(nodeCR)).as("Deleted CR").isNull();
			} else {
				assertThat(t.getObject(nodeCR)).as("Not deleted CR").isNotNull();
			}
		});
	}

	/**
	 * Test deleting an unknown CR
	 * @throws RestException
	 * @throws NodeException
	 */
	@Test
	public void testDeleteUnknown() throws RestException, NodeException {
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			Response response = client.get().base().path(CR_PATH).path(UNKNOWN_ID).request(mediaType).delete();
			assertThat(response.getStatusInfo()).as("Response status").isEqualTo(Status.NOT_FOUND);
			assertThat(response.readEntity(GenericResponse.class)).as("Response").hasCode(ResponseCode.NOTFOUND)
					.containsMessage(Message.Type.WARNING, "contentrepository.notfound", UNKNOWN_ID);
		}
	}

	/**
	 * Test checking CR structure
	 * @throws RestException
	 * @throws NodeException
	 */
	@Test
	public void testCheck() throws RestException, NodeException {
		checkStructure(false);
	}

	/**
	 * Test checking CR structure of an unknown CR
	 * @throws RestException
	 * @throws NodeException
	 */
	@Test
	public void testCheckUnknown() throws RestException, NodeException {
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			Response response = client.get().base().path(CR_PATH).path(UNKNOWN_ID).path("structure").path("check").request(mediaType)
					.put(Entity.entity("", mediaType));
			assertThat(response.getStatusInfo()).as("Response status").isEqualTo(Status.NOT_FOUND);
			assertThat(response.readEntity(GenericResponse.class)).as("Response").hasCode(ResponseCode.NOTFOUND)
					.containsMessage(Message.Type.WARNING, "contentrepository.notfound", UNKNOWN_ID);
		}
	}

	/**
	 * Test repairing CR structure
	 * @throws RestException
	 * @throws NodeException
	 */
	@Test
	public void testRepair() throws RestException, NodeException {
		checkStructure(true);
	}

	/**
	 * Test repairing CR structure of an unknown CR
	 * @throws RestException
	 * @throws NodeException
	 */
	@Test
	public void testRepairUnknown() throws RestException, NodeException {
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			Response response = client.get().base().path(CR_PATH).path(UNKNOWN_ID).path("structure").path("repair").request(mediaType)
					.put(Entity.entity("", mediaType));
			assertThat(response.getStatusInfo()).as("Response status").isEqualTo(Status.NOT_FOUND);
			assertThat(response.readEntity(GenericResponse.class)).as("Response").hasCode(ResponseCode.NOTFOUND)
					.containsMessage(Message.Type.WARNING, "contentrepository.notfound", UNKNOWN_ID);
		}
	}

	/**
	 * Check Structure
	 * @param repair true to repair
	 * @throws RestException
	 * @throws NodeException
	 */
	protected void checkStructure(boolean repair) throws RestException, NodeException {
		String path = repair ? "repair" : "check";

		ContentRepository nodeCR = create(ContentRepository.class, cr -> {
			cr.setName("CR");
			cr.setCrType(Type.cr);
			cr.setDbType("hsql");
			cr.setUrl(String.format("jdbc:hsqldb:mem:%s;shutdown=true", TestEnvironment.getRandomHash(10)));
			cr.setUsername("sa");
			cr.setPassword("");
		});

		assertThat(nodeCR).as("Unchecked CR")
			.has(attribute("checkStatus", ContentRepositoryModel.Status.unchecked.code()))
			.has(attribute("checkDate", new ContentNodeDate(0)))
			.has(attribute("checkResult", null));

		if (permission) {
			Trx.consume(id -> {
				String perms = new Permission(PermHandler.PERM_VIEW, PermHandler.PERM_CONTENTREPOSITORY_UPDATE).toString();
				PermHandler.setPermissions(ContentRepository.TYPE_CONTENTREPOSITORY, id, Arrays.asList(nodeGroup), perms);
			}, nodeCR.getId());
		}

		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			String crId = Integer.toString(nodeCR.getId());
			Response checkResponse = client.get().base().path(CR_PATH).path(crId).path("structure").path(path).request(mediaType)
					.put(Entity.entity("", mediaType));
			assertThat(checkResponse).as("Response").isNotNull();
			if (permission) {
				assertThat(checkResponse.getStatusInfo()).as("Response Status").isEqualTo(Status.OK);
				ContentRepositoryResponse crResponse = checkResponse.readEntity(ContentRepositoryResponse.class);
				ContentRepositoryModel crModel = crResponse.getContentRepository();
				assertThat(crModel).as("Checked CR").isNotNull()
				.has(attribute("checkStatus", repair ? ContentRepositoryModel.Status.ok : ContentRepositoryModel.Status.error))
					.doesNotHave(attribute("checkDate", 0))
					.doesNotHave(attribute("checkResult", null));
			} else {
				assertThat(checkResponse.getStatusInfo()).as("Response Status").isEqualTo(Status.FORBIDDEN);
			}
		}
	}

	/**
	 * Test queuing data check
	 * @throws RestException
	 * @throws NodeException
	 */
	@Test
	public void testCheckData() throws RestException, NodeException {
		checkData(false);
	}

	/**
	 * Test queuing data check for an unknown CR
	 * @throws RestException
	 * @throws NodeException
	 */
	@Test
	public void testCheckDataUnknown() throws RestException, NodeException {
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			Response response = client.get().base().path(CR_PATH).path(UNKNOWN_ID).path("data").path("check").request(mediaType)
					.put(Entity.entity("", mediaType));
			assertThat(response.getStatusInfo()).as("Response status").isEqualTo(Status.NOT_FOUND);
			assertThat(response.readEntity(GenericResponse.class)).as("Response").hasCode(ResponseCode.NOTFOUND)
					.containsMessage(Message.Type.WARNING, "contentrepository.notfound", UNKNOWN_ID);
		}
	}

	/**
	 * Test queuing data repair
	 * @throws RestException
	 * @throws NodeException
	 */
	@Test
	public void testRepairData() throws RestException, NodeException {
		checkData(true);
	}

	/**
	 * Test queuing data repair for an unknown CR
	 * @throws RestException
	 * @throws NodeException
	 */
	@Test
	public void testRepairDataUnknown() throws RestException, NodeException {
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			Response response = client.get().base().path(CR_PATH).path(UNKNOWN_ID).path("data").path("repair").request(mediaType)
					.put(Entity.entity("", mediaType));
			assertThat(response.getStatusInfo()).as("Response status").isEqualTo(Status.NOT_FOUND);
			assertThat(response.readEntity(GenericResponse.class)).as("Response").hasCode(ResponseCode.NOTFOUND)
					.containsMessage(Message.Type.WARNING, "contentrepository.notfound", UNKNOWN_ID);
		}
	}

	@Test
	public void testCopyCr() throws Exception {
		copyTest(create -> {
			create.setCrType(Type.cr);
			create.setDbType("mysql");
			create.setInstantPublishing(true);
			create.setLanguageInformation(true);
			create.setPermissionInformation(true);
		}, update -> {});
	}

	@Test
	public void testCopyMccr() throws Exception {
		copyTest(create -> {
			create.setCrType(Type.mccr);
			create.setDbType("mysql");
		}, update -> {});
	}

	@Test
	public void testCopyMeshCr() throws Exception {
		copyTest(create -> {
			create.setCrType(Type.mesh);
			CRElasticsearchModel es = new CRElasticsearchModel();
			ObjectMapper objectMapper = new ObjectMapper();
			es.setPage(objectMapper.createObjectNode());
			create.setElasticsearch(es);
			create.setProjectPerNode(true);
			create.setPermissionProperty("object.roles");
		}, update -> {});
	}

	/**
	 * Do the copy test
	 * @param crCreator
	 * @param updater
	 * @throws RestException
	 * @throws NodeException
	 * @throws JsonProcessingException
	 */
	protected void copyTest(Consumer<ContentRepositoryModel> crCreator, Consumer<ContentRepositoryModel> updater) throws RestException, NodeException, JsonProcessingException {
		String name = "Test CR";
		ContentRepositoryModel cr = null;
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			ContentRepositoryModel crModel = new ContentRepositoryModel();
			crModel.setName(name);
			crCreator.accept(crModel);

			ContentRepositoryResponse createResponse = client.get().base().path(CR_PATH).request(mediaType).post(Entity.entity(crModel, mediaType),
					ContentRepositoryResponse.class);
			client.get().assertResponse(createResponse);
			cr = createResponse.getContentRepository();

			if (updater != null) {
				updater.accept(cr);
			}
		}

		if (!permission) {
			Trx.operate(() -> {
				PermHandler.setPermissions(ContentRepository.TYPE_CONTENTREPOSITORIES, Arrays.asList(nodeGroup), PermHandler.EMPTY_PERM);
			});
		}

		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {

			Response response = client.get().base().path(CR_PATH).path(cr.getGlobalId()).path("copy").request(mediaType).put(Entity.entity("", mediaType));
			if (permission) {
				ContentRepositoryResponse createResponse = response.readEntity(ContentRepositoryResponse.class);
				client.get().assertResponse(createResponse);
				ContentRepositoryModel responseModel = createResponse.getContentRepository();
				String newName = Trx.execute(tmpCr -> {
					CNI18nString nameI18n = new CNI18nString("copy_of");
					nameI18n.addParameter("");
					nameI18n.addParameter(tmpCr.getName());
					return nameI18n.toString();
				}, cr);
				assertThat(responseModel).as("Created CR").isNotNull()
					.has(attribute("name", newName))
					.isEqualToIgnoringGivenFields(cr, "id", "globalId", "name", "elasticsearch");

				ObjectMapper mapper = new ObjectMapper();
				String elasticsearch = mapper.writeValueAsString(responseModel.getElasticsearch());
				String expectedES = mapper.writeValueAsString(cr.getElasticsearch());
				assertThat(elasticsearch).as("Elasticsearch").isEqualTo(expectedES);

				TagmapEntryListResponse expected = client.get().base().path(CR_PATH).path(cr.getGlobalId()).path("entries").request()
						.get(TagmapEntryListResponse.class);
				TagmapEntryListResponse entries = client.get().base().path(CR_PATH).path(responseModel.getGlobalId()).path("entries").request()
						.get(TagmapEntryListResponse.class);

				assertThat(entries.getItems()).as("Copied tagmap entries").usingElementComparatorIgnoringFields("id", "globalId")
						.containsOnlyElementsOf(expected.getItems());
			} else {
				assertThat(response.getStatusInfo()).as("Response status").isEqualTo(Status.FORBIDDEN);
				assertThat(response.readEntity(GenericResponse.class)).as("Response").hasCode(ResponseCode.PERMISSION)
						.containsMessage(Message.Type.CRITICAL, "rest.permission.required");
			}
		}
	}

	/**
	 * Check data
	 * @param repair true to also repair
	 * @throws RestException
	 * @throws NodeException
	 */
	protected void checkData(boolean repair) throws RestException, NodeException {
		String path = repair ? "repair" : "check";

		ContentRepository nodeCR = create(ContentRepository.class, cr -> {
			cr.setName("CR");
			cr.setCrType(Type.cr);
			cr.setDbType("hsql");
			cr.setUrl(String.format("jdbc:hsqldb:mem:%s;shutdown=true", TestEnvironment.getRandomHash(10)));
			cr.setUsername("sa");
			cr.setPassword("");
		});
		Trx.operate(t -> {
			assertThat(nodeCR.checkStructure(true)).as("Structure check").isTrue();
		});

		assertThat(nodeCR).as("Unchecked CR")
			.has(attribute("dataStatus", ContentRepositoryModel.Status.unchecked.code()))
			.has(attribute("dataCheckResult", null));

		if (permission) {
			Trx.consume(id -> {
				String perms = new Permission(PermHandler.PERM_VIEW, PermHandler.PERM_CONTENTREPOSITORY_UPDATE).toString();
				PermHandler.setPermissions(ContentRepository.TYPE_CONTENTREPOSITORY, id, Arrays.asList(nodeGroup), perms);
			}, nodeCR.getId());
		}

		Trx.operate(() -> {
			DBUtils.executeUpdate("DELETE FROM dirtqueue", null);
		});

		String crId = Integer.toString(nodeCR.getId());
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			Response checkResponse = client.get().base().path(CR_PATH).path(crId).path("data").path(path).request(mediaType)
					.put(Entity.entity("", mediaType));
			assertThat(checkResponse).as("Response").isNotNull();
			if (permission) {
				assertThat(checkResponse.getStatusInfo()).as("Response Status").isEqualTo(Status.OK);
				ContentRepositoryResponse crResponse = checkResponse.readEntity(ContentRepositoryResponse.class);
				ContentRepositoryModel crModel = crResponse.getContentRepository();
				assertThat(crModel).as("Checked CR").isNotNull()
					.has(attribute("dataStatus", ContentRepositoryModel.Status.queued))
					.has(attribute("dataCheckResult", null));
			} else {
				assertThat(checkResponse.getStatusInfo()).as("Response Status").isEqualTo(Status.FORBIDDEN);
			}
		}

		Trx.operate(t -> {
			List<QueueEntry> entries = DBUtils.select("SELECT * FROM dirtqueue", rs -> {
				List<QueueEntry> tmpEntries = new ArrayList<>();
				while (rs.next()) {
					tmpEntries.add(new QueueEntry(rs));
				}
				return tmpEntries;
			});
			if (permission) {
				assertThat(entries).as("Queue Entries").usingElementComparatorOnFields("objId", "objType", "eventMask", "property")
						.contains(new QueueEntry(0, -1, -1, Events.DATACHECK_CR, new String[] { crId, Boolean.toString(repair) }, 0, null));
			} else {
				assertThat(entries).as("Queue Entries").isEmpty();
			}
		});
	}

	/**
	 * Test setting permission
	 * @throws NodeException
	 * @throws RestException
	 */
	@Test
	public void testSetPermission() throws NodeException, RestException {
		ContentRepository cr = create(ContentRepository.class, create -> {
			create.setName("CR");
			create.setCrType(Type.cr);
		});

		if (permission) {
			consume(id -> {
				setPermissions(admin, null, Arrays.asList(nodeGroup), new Permission(read.getBit()).toString(), false);
				setPermissions(groupadmin, null, Arrays.asList(nodeGroup), new Permission(read.getBit(), setuserperm.getBit()).toString(), false);
				setPermissions(contentadmin, null, Arrays.asList(nodeGroup), new Permission(read.getBit()).toString(), false);
				setPermissions(contentrepositoryadmin, null, Arrays.asList(nodeGroup), new Permission(read.getBit()).toString(), false);
				setPermissions(contentrepository, id, Arrays.asList(nodeGroup), new Permission(read.getBit(), setperm.getBit()).toString(), false);
			}, cr.getId());
		} else {
			consume(id -> {
				setPermissions(admin, null, Arrays.asList(nodeGroup), new Permission(read.getBit()).toString(), false);
				setPermissions(groupadmin, null, Arrays.asList(nodeGroup), new Permission(read.getBit(), setuserperm.getBit()).toString(), false);
				setPermissions(contentadmin, null, Arrays.asList(nodeGroup), new Permission(read.getBit()).toString(), false);
				setPermissions(contentrepositoryadmin, null, Arrays.asList(nodeGroup), new Permission(read.getBit()).toString(), false);
				setPermissions(contentrepository, id, Arrays.asList(nodeGroup), PermHandler.EMPTY_PERM, false);
			}, cr.getId());
		}

		if (permission) {
			try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
				// read permissions
				TypePermissionResponse readResponse = client.get().base().path("group").path(Integer.toString(subGroup.getId())).path("perms")
						.path("contentrepository").path(Integer.toString(cr.getId())).request()
						.get(TypePermissionResponse.class);
				assertResponseOK(readResponse);
				assertThat(readResponse.getPerms()).as("Permissions before update")
						.usingElementComparatorOnFields("type", "value")
						.contains(new TypePermissionItem().setType(PermType.read).setValue(false));

				TypePermissionRequest request = new TypePermissionRequest()
						.setPerms(Arrays.asList(new TypePermissionItem().setType(PermType.read).setValue(true)));
				GenericResponse setResponse = client.get().base().path("group").path(Integer.toString(subGroup.getId())).path("perms")
						.path("contentrepository").path(Integer.toString(cr.getId())).request()
						.post(Entity.entity(request, mediaType), GenericResponse.class);
				assertResponseOK(setResponse);

				readResponse = client.get().base().path("group").path(Integer.toString(subGroup.getId())).path("perms")
						.path("contentrepository").path(Integer.toString(cr.getId())).request()
						.get(TypePermissionResponse.class);
				assertResponseOK(readResponse);
				assertThat(readResponse.getPerms()).as("Permissions after update")
						.usingElementComparatorOnFields("type", "value")
						.contains(new TypePermissionItem().setType(PermType.read).setValue(true));
			}
		} else {
			try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
				// read permissions
				client.get().base().path("group").path(Integer.toString(subGroup.getId())).path("perms")
						.path("contentrepository").path(Integer.toString(cr.getId())).request()
						.get(TypePermissionResponse.class);
				fail("Request should have failed");
			} catch (ForbiddenException expected) {
			}
		}
	}
}
