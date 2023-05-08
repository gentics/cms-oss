package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.db.DBUtils.select;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createDatasource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.exception.DuplicateValueException;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.SingleSelectPartType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.DatasourceEntryModel;
import com.gentics.contentnode.rest.model.DatasourceType;
import com.gentics.contentnode.rest.model.response.ConstructList;
import com.gentics.contentnode.rest.model.response.DatasourceEntryListResponse;
import com.gentics.contentnode.rest.model.response.DatasourceEntryResponse;
import com.gentics.contentnode.rest.model.response.DatasourceLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PagedDatasourceListResponse;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.DatasourceResource;
import com.gentics.contentnode.rest.resource.impl.DatasourceResourceImpl;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.tests.utils.Expected;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for the {@link DatasourceResource}
 */
public class DatasourceResourceTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static UserGroup group;
	private static SystemUser user;

	@Rule
	public ExceptionChecker exceptionChecker = new ExceptionChecker();

	private static Set<Integer> constructIds;

	private static Node node;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		group = supply(() -> createUserGroup("TestGroup", NODE_GROUP_ID));
		user = supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(group)));

		node = supply(() -> createNode());

		constructIds = supply(() -> DBUtils.select("SELECT id FROM construct", DBUtils.IDS));
	}

	@Before
	public void setup() throws NodeException {
		operate(t -> {
			for (Construct construct : t.getObjects(Construct.class, select("SELECT id FROM construct", DBUtils.IDS))) {
				if (constructIds.contains(construct.getId())) {
					continue;
				}
				construct.delete();
			}
		});

		operate(t -> {
			for (Datasource datasource : t.getObjects(Datasource.class, select("SELECT id FROM datasource", DBUtils.IDS))) {
				datasource.delete();
			}
		});
	}

	@Test
	public void testList() throws NodeException {
		for (String name : Arrays.asList("First Datasource", "Second Datasource", "Third Datasource")) {
			operate(() -> {
				assertResponseOK(new DatasourceResourceImpl().create(new com.gentics.contentnode.rest.model.Datasource()
						.setType(DatasourceType.STATIC).setName(name)));
			});
		}

		PagedDatasourceListResponse response = new DatasourceResourceImpl().list(null, null, null);

		assertThat(response.getItems().stream().map(com.gentics.contentnode.rest.model.Datasource::getName)
				.collect(Collectors.toList())).as("Datasource names").containsOnly("First Datasource",
						"Second Datasource", "Third Datasource");
	}

	@Test
	public void testCreate() throws NodeException {
		DatasourceLoadResponse response = assertRequiredPermissions(group, user, () -> {
			com.gentics.contentnode.rest.model.Datasource datasource = new com.gentics.contentnode.rest.model.Datasource()
					.setType(DatasourceType.STATIC).setName("Test DS");
			return new DatasourceResourceImpl().create(datasource);
		}, Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(Datasource.TYPE_DATASOURCE, 1, PermHandler.PERM_VIEW));

		assertThat(response.getDatasource()).as("Created datasource").hasFieldOrPropertyWithValue("name", "Test DS")
				.hasFieldOrPropertyWithValue("type", DatasourceType.STATIC);
	}

	@Test
	@Expected(ex = RestMappedException.class, message = "Das Feld 'Name' darf nicht leer sein.")
	public void testCreateNoName() throws NodeException {
		operate(() -> {
			com.gentics.contentnode.rest.model.Datasource datasource = new com.gentics.contentnode.rest.model.Datasource()
					.setType(DatasourceType.STATIC);
			assertResponseOK(new DatasourceResourceImpl().create(datasource));
		});
	}

	@Test
	@Expected(ex = RestMappedException.class, message = "Das Feld 'Typ' darf nicht leer sein.")
	public void testCreateNoType() throws NodeException {
		operate(() -> {
			com.gentics.contentnode.rest.model.Datasource datasource = new com.gentics.contentnode.rest.model.Datasource()
					.setName("Datasource Name");
			assertResponseOK(new DatasourceResourceImpl().create(datasource));
		});
	}

	@Test
	public void testCreateDuplicateName() throws NodeException {
		operate(() -> {
			com.gentics.contentnode.rest.model.Datasource datasource = new com.gentics.contentnode.rest.model.Datasource()
					.setType(DatasourceType.STATIC).setName("Duplicate Name");
			assertResponseOK(new DatasourceResourceImpl().create(datasource));
		});

		exceptionChecker.expect(DuplicateValueException.class,
				"Das Feld 'Name' darf nicht den Wert 'Duplicate Name' haben, weil dieser Wert bereits verwendet wird.");
		operate(() -> {
			com.gentics.contentnode.rest.model.Datasource datasource = new com.gentics.contentnode.rest.model.Datasource()
					.setType(DatasourceType.STATIC).setName("Duplicate Name");
			new DatasourceResourceImpl().create(datasource);
		});
	}

	@Test
	public void testRead() throws NodeException {
		int datasourceId = supply(() -> {
			com.gentics.contentnode.rest.model.Datasource datasource = new com.gentics.contentnode.rest.model.Datasource()
					.setType(DatasourceType.STATIC).setName("Datasource Name");
			DatasourceLoadResponse response = new DatasourceResourceImpl().create(datasource);
			assertResponseOK(response);
			return response.getDatasource().getId();
		});

		DatasourceLoadResponse response = assertRequiredPermissions(group, user, () -> {
			return new DatasourceResourceImpl().get(Integer.toString(datasourceId));
		}, Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(Datasource.TYPE_DATASOURCE, 1, PermHandler.PERM_VIEW));

		assertThat(response.getDatasource()).as("Created datasource").hasFieldOrPropertyWithValue("name", "Datasource Name")
				.hasFieldOrPropertyWithValue("type", DatasourceType.STATIC);
	}

	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Die Datenquelle '4711' wurde nicht gefunden.")
	public void testReadUnknown() throws NodeException {
		operate(() -> {
			new DatasourceResourceImpl().get(Integer.toString(4711));
		});
	}

	@Test
	public void testUpdate() throws NodeException {
		int datasourceId = supply(() -> {
			com.gentics.contentnode.rest.model.Datasource datasource = new com.gentics.contentnode.rest.model.Datasource()
					.setType(DatasourceType.STATIC).setName("Original Name");
			DatasourceLoadResponse response = new DatasourceResourceImpl().create(datasource);
			assertResponseOK(response);
			return response.getDatasource().getId();
		});

		DatasourceLoadResponse response = assertRequiredPermissions(group, user, () -> {
			return new DatasourceResourceImpl().update(Integer.toString(datasourceId), new com.gentics.contentnode.rest.model.Datasource().setName("New Name"));
		}, Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(Datasource.TYPE_DATASOURCE, 1, PermHandler.PERM_VIEW));

		assertThat(response.getDatasource()).as("Updated datasource")
				.hasFieldOrPropertyWithValue("name", "New Name")
				.hasFieldOrPropertyWithValue("type", DatasourceType.STATIC);
	}

	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Die Datenquelle '4711' wurde nicht gefunden.")
	public void testUpdateUnknown() throws NodeException {
		operate(() -> {
			com.gentics.contentnode.rest.model.Datasource datasource = new com.gentics.contentnode.rest.model.Datasource()
					.setName("Modified Name");
			new DatasourceResourceImpl().update(Integer.toString(4711), datasource);
		});
	}

	@Test
	public void testUpdateDuplicate() throws NodeException {
		int datasourceId = supply(() -> {
			DatasourceLoadResponse response = new DatasourceResourceImpl()
					.create(new com.gentics.contentnode.rest.model.Datasource().setType(DatasourceType.STATIC)
							.setName("Original Name"));
			assertResponseOK(response);
			return response.getDatasource().getId();
		});

		operate(() -> {
			DatasourceLoadResponse response = new DatasourceResourceImpl()
					.create(new com.gentics.contentnode.rest.model.Datasource().setType(DatasourceType.STATIC)
							.setName("Duplicate Name"));
			assertResponseOK(response);
		});

		exceptionChecker.expect(DuplicateValueException.class,
				"Das Feld 'Name' darf nicht den Wert 'Duplicate Name' haben, weil dieser Wert bereits verwendet wird.");
		operate(() -> {
			com.gentics.contentnode.rest.model.Datasource datasource = new com.gentics.contentnode.rest.model.Datasource()
					.setName("Duplicate Name");
			new DatasourceResourceImpl().update(Integer.toString(datasourceId), datasource);
		});
	}

	@Test
	public void testDelete() throws NodeException {
		int datasourceId = supply(() -> {
			DatasourceLoadResponse response = new DatasourceResourceImpl()
					.create(new com.gentics.contentnode.rest.model.Datasource().setType(DatasourceType.STATIC)
							.setName("To Delete"));
			assertResponseOK(response);
			return response.getDatasource().getId();
		});

		assertRequiredPermissions(group, user, () -> {
			new DatasourceResourceImpl().delete(Integer.toString(datasourceId));
			return new GenericResponse(null, ResponseInfo.ok(""));
		}, Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(Datasource.TYPE_DATASOURCE, 1, PermHandler.PERM_VIEW));

		operate(t -> {
			assertThat(t.getObject(Datasource.class, Integer.toString(datasourceId))).as("Deleted datasource").isNull();
		});
	}

	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Die Datenquelle '4711' wurde nicht gefunden.")
	public void testDeleteUnknown() throws NodeException {
		operate(() -> new DatasourceResourceImpl().delete(Integer.toString(4711)));
	}

	@Test
	@Expected(ex = RestMappedException.class, message = "Die Datenquelle konnte nicht gelöscht werden, da sie noch in Tagtypen verwendung findet.")
	public void testDeleteUsed() throws NodeException {
		int constructId = supply(() -> createConstruct(node, SingleSelectPartType.class, "construct", "part"));
		int datasourceId = supply(t -> {
			Construct construct = t.getObject(Construct.class, constructId);
			return getPartType(SingleSelectPartType.class, construct, "part").getDatasourceId();
		});

		operate(() -> new DatasourceResourceImpl().delete(Integer.toString(datasourceId)));
	}

	@Test
	public void testGetConstructs() throws NodeException {
		int constructId = supply(() -> createConstruct(node, SingleSelectPartType.class, "construct", "part"));
		int datasourceId = supply(t -> {
			Construct construct = t.getObject(Construct.class, constructId);
			return getPartType(SingleSelectPartType.class, construct, "part").getDatasourceId();
		});

		operate(() -> PermHandler.setPermissions(Construct.TYPE_CONSTRUCT, Arrays.asList(group), PermHandler.EMPTY_PERM));
		ConstructList constructs = assertRequiredPermissions(group, user,
				() -> new DatasourceResourceImpl().constructs(Integer.toString(datasourceId), null, null, null),
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(Datasource.TYPE_DATASOURCE, 1, PermHandler.PERM_VIEW));

		// expect list to be empty, because user has no permission on constructs
		assertThat(constructs.getItems().stream().map(com.gentics.contentnode.rest.model.Construct::getName)
				.collect(Collectors.toList())).as("Construct Names").isEmpty();

		// grant permission to see the node and constructs to the user
		operate(() -> PermHandler.setPermissions(Construct.TYPE_CONSTRUCTS_INTEGER, Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		try (Trx trx = new Trx(user)) {
			constructs = new DatasourceResourceImpl().constructs(Integer.toString(datasourceId), null, null, null);
			trx.success();
		}

		assertThat(constructs.getItems().stream().map(com.gentics.contentnode.rest.model.Construct::getName)
				.collect(Collectors.toList())).as("Construct Names").contains("construct");
	}

	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Die Datenquelle '4711' wurde nicht gefunden.")
	public void testGetConstructsUnknown() throws NodeException {
		operate(() -> new DatasourceResourceImpl().constructs(Integer.toString(4711), null, null, null));
	}

	@Test
	public void testListEntries() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));

		DatasourceEntryListResponse response = assertRequiredPermissions(group, user,
				() -> new DatasourceResourceImpl().listEntries(Integer.toString(datasource.getId())),
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(Datasource.TYPE_DATASOURCE, 1, PermHandler.PERM_VIEW));

		assertThat(response.getItems()).as("Datasource entries").usingElementComparatorOnFields("key", "value", "dsId")
				.containsExactly(new DatasourceEntryModel().setDsId(1).setKey("first").setValue("First Entry"),
						new DatasourceEntryModel().setDsId(2).setKey("second").setValue("Second Entry"),
						new DatasourceEntryModel().setDsId(3).setKey("third").setValue("Third Entry"));
	}

	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Die Datenquelle '4711' wurde nicht gefunden.")
	public void testListEntriesUnknown() throws NodeException {
		operate(() -> new DatasourceResourceImpl().listEntries(Integer.toString(4711)));
	}

	@Test
	public void testLoadEntry() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);
		int entryId = execute(d -> d.getEntries().get(1).getId(), datasource);
		String globalEntryId = execute(d -> d.getEntries().get(1).getGlobalId().toString(), datasource);

		DatasourceEntryResponse response = assertRequiredPermissions(group, user,
				() -> new DatasourceResourceImpl().getEntry(Integer.toString(datasourceId), Integer.toString(entryId)),
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(Datasource.TYPE_DATASOURCE, 1, PermHandler.PERM_VIEW));

		assertThat(response.getEntry()).as("Loaded Entry").isEqualToComparingFieldByField(new DatasourceEntryModel()
				.setId(entryId).setGlobalId(globalEntryId).setDsId(2).setKey("second").setValue("Second Entry"));
	}

	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Die Datenquelle '4711' wurde nicht gefunden.")
	public void testLoadEntryUnknownDatasource() throws NodeException {
		operate(() -> new DatasourceResourceImpl().getEntry(Integer.toString(4711), Integer.toString(4711)));
	}

	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Der Datenquellen Eintrag '4711' wurde nicht gefunden.")
	public void testLoadUnknownEntry() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);

		operate(() -> new DatasourceResourceImpl().getEntry(Integer.toString(datasourceId), Integer.toString(4711)));
	}

	@Test
	public void testCreateEntry() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);

		assertRequiredPermissions(group, user, () -> {
			new DatasourceResourceImpl().addEntry(Integer.toString(datasourceId),
					new DatasourceEntryModel().setDsId(7).setKey("fourth").setValue("Fourth Entry"));
			return new DatasourceResourceImpl().addEntry(Integer.toString(datasourceId),
					new DatasourceEntryModel().setKey("fifth").setValue("Fifth Entry"));
		}, Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(Datasource.TYPE_DATASOURCE, 1, PermHandler.PERM_VIEW));

		DatasourceEntryListResponse response = supply(
				() -> new DatasourceResourceImpl().listEntries(Integer.toString(datasourceId)));
		assertThat(response.getItems()).as("Datasource entries").usingElementComparatorOnFields("key", "value", "dsId")
				.containsExactly(new DatasourceEntryModel().setDsId(1).setKey("first").setValue("First Entry"),
						new DatasourceEntryModel().setDsId(2).setKey("second").setValue("Second Entry"),
						new DatasourceEntryModel().setDsId(3).setKey("third").setValue("Third Entry"),
						new DatasourceEntryModel().setDsId(7).setKey("fourth").setValue("Fourth Entry"),
						new DatasourceEntryModel().setDsId(8).setKey("fifth").setValue("Fifth Entry"));
	}

	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Die Datenquelle '4711' wurde nicht gefunden.")
	public void testCreateEntryUnknownDatasource() throws NodeException {
		operate(() -> new DatasourceResourceImpl().addEntry(Integer.toString(4711), new DatasourceEntryModel().setKey("key").setValue("value")));
	}

	@Test
	@Expected(ex = RestMappedException.class, message = "Das Feld 'key' darf nicht leer sein.")
	public void testCreateEntryNoKey() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);

		operate(() -> new DatasourceResourceImpl().addEntry(Integer.toString(datasourceId), new DatasourceEntryModel().setValue("value")));
	}

	@Test
	@Expected(ex = RestMappedException.class, message = "Das Feld 'value' darf nicht leer sein.")
	public void testCreateEntryNoValue() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);

		operate(() -> new DatasourceResourceImpl().addEntry(Integer.toString(datasourceId), new DatasourceEntryModel().setKey("key")));
	}

	@Test
	@Expected(ex = DuplicateValueException.class, message = "Das Feld 'key' darf nicht den Wert 'first' haben, weil dieser Wert bereits verwendet wird.")
	public void testCreateEntryDuplicateKey() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);

		operate(() -> new DatasourceResourceImpl().addEntry(Integer.toString(datasourceId), new DatasourceEntryModel().setKey("first").setValue("Another First Entry")));
	}

	@Test
	@Expected(ex = DuplicateValueException.class, message = "Das Feld 'value' darf nicht den Wert 'First Entry' haben, weil dieser Wert bereits verwendet wird.")
	public void testCreateEntryDuplicateValue() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);

		operate(() -> new DatasourceResourceImpl().addEntry(Integer.toString(datasourceId), new DatasourceEntryModel().setKey("first-again").setValue("First Entry")));
	}

	@Test
	@Expected(ex = DuplicateValueException.class, message = "Das Feld 'dsId' darf nicht den Wert '2' haben, weil dieser Wert bereits verwendet wird.")
	public void testCreateEntryDuplicateDsId() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);

		operate(() -> new DatasourceResourceImpl().addEntry(Integer.toString(datasourceId), new DatasourceEntryModel().setKey("key").setValue("value").setDsId(2)));
	}

	@Test
	public void testUpdateEntry() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);
		int entryId = execute(d -> d.getEntries().get(1).getId(), datasource);

		assertRequiredPermissions(group, user,
				() -> new DatasourceResourceImpl().updateEntry(Integer.toString(datasourceId),
						Integer.toString(entryId),
						new DatasourceEntryModel().setKey("updated").setValue("Updated Entry")),
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(Datasource.TYPE_DATASOURCE, 1, PermHandler.PERM_VIEW));

		DatasourceEntryListResponse response = supply(
				() -> new DatasourceResourceImpl().listEntries(Integer.toString(datasourceId)));
		assertThat(response.getItems()).as("Datasource Entries").usingElementComparatorOnFields("dsId", "key", "value")
				.containsExactly(new DatasourceEntryModel().setDsId(1).setKey("first").setValue("First Entry"),
						new DatasourceEntryModel().setDsId(2).setKey("updated").setValue("Updated Entry"),
						new DatasourceEntryModel().setDsId(3).setKey("third").setValue("Third Entry"));
	}

	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Die Datenquelle '4711' wurde nicht gefunden.")
	public void testUpdateEntryUnknownDatasource() throws NodeException {
		operate(() -> new DatasourceResourceImpl().updateEntry(Integer.toString(4711), Integer.toString(4711), new DatasourceEntryModel()));
	}

	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Der Datenquellen Eintrag '4711' wurde nicht gefunden.")
	public void testUpdateUnknownEntry() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);

		operate(() -> new DatasourceResourceImpl().updateEntry(Integer.toString(datasourceId), Integer.toString(4711), new DatasourceEntryModel()));
	}

	@Test
	@Expected(ex = RestMappedException.class, message = "Das Feld 'key' darf nicht leer sein.")
	public void testUpdateEntryBlankKey() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);
		int entryId = execute(d -> d.getEntries().get(1).getId(), datasource);

		operate(() -> new DatasourceResourceImpl().updateEntry(Integer.toString(datasourceId),
				Integer.toString(entryId), new DatasourceEntryModel().setKey("")));
	}

	@Test
	@Expected(ex = RestMappedException.class, message = "Das Feld 'value' darf nicht leer sein.")
	public void testUpdateEntryBlankValue() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);
		int entryId = execute(d -> d.getEntries().get(1).getId(), datasource);

		operate(() -> new DatasourceResourceImpl().updateEntry(Integer.toString(datasourceId),
				Integer.toString(entryId), new DatasourceEntryModel().setValue("")));
	}

	@Test
	@Expected(ex = DuplicateValueException.class, message = "Das Feld 'key' darf nicht den Wert 'first' haben, weil dieser Wert bereits verwendet wird.")
	public void testUpdateEntryDuplicateKey() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);
		int entryId = execute(d -> d.getEntries().get(1).getId(), datasource);

		operate(() -> new DatasourceResourceImpl().updateEntry(Integer.toString(datasourceId),
				Integer.toString(entryId), new DatasourceEntryModel().setKey("first")));
	}

	@Test
	@Expected(ex = DuplicateValueException.class, message = "Das Feld 'value' darf nicht den Wert 'Third Entry' haben, weil dieser Wert bereits verwendet wird.")
	public void testUpdateEntryDuplicateValue() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);
		int entryId = execute(d -> d.getEntries().get(1).getId(), datasource);

		operate(() -> new DatasourceResourceImpl().updateEntry(Integer.toString(datasourceId),
				Integer.toString(entryId), new DatasourceEntryModel().setValue("Third Entry")));
	}

	@Test
	public void testDeleteEntry() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);
		int entryId = execute(d -> d.getEntries().get(1).getId(), datasource);

		assertRequiredPermissions(group, user, () -> {
			new DatasourceResourceImpl().deleteEntry(Integer.toString(datasourceId), Integer.toString(entryId));
			return new GenericResponse(null, ResponseInfo.ok(""));
		}, Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(Datasource.TYPE_DATASOURCE, 1, PermHandler.PERM_VIEW));

		DatasourceEntryListResponse response = supply(
				() -> new DatasourceResourceImpl().listEntries(Integer.toString(datasourceId)));
		assertThat(response.getItems()).as("Datasource Entries").usingElementComparatorOnFields("dsId", "key", "value")
				.containsExactly(new DatasourceEntryModel().setDsId(1).setKey("first").setValue("First Entry"),
						new DatasourceEntryModel().setDsId(3).setKey("third").setValue("Third Entry"));
	}

	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Die Datenquelle '4711' wurde nicht gefunden.")
	public void testDeleteEntryUnknownDatasource() throws NodeException {
		operate(() -> new DatasourceResourceImpl().deleteEntry(Integer.toString(4711), Integer.toString(4711)));
	}

	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Der Datenquellen Eintrag '4711' wurde nicht gefunden.")
	public void testDeleteUnknownEntry() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);

		operate(() -> new DatasourceResourceImpl().deleteEntry(Integer.toString(datasourceId), Integer.toString(4711)));
	}

	@Test
	public void testUpdateEntries() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);

		// get original entries, reverse list, update first entry, remove second and add a new entry at the beginning
		List<DatasourceEntryModel> items = supply(() -> new DatasourceResourceImpl().listEntries(Integer.toString(datasourceId))).getItems();
		Collections.reverse(items);
		items.get(0).setKey("updated");
		items.remove(1);
		items.add(0, new DatasourceEntryModel().setKey("new").setValue("New Entry"));

		DatasourceEntryListResponse response = assertRequiredPermissions(group, user,
				() -> new DatasourceResourceImpl().updateEntryList(Integer.toString(datasourceId), items),
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(Datasource.TYPE_DATASOURCE, 1, PermHandler.PERM_VIEW));

		assertThat(response.getItems()).as("Updated Datasource Entries")
				.usingElementComparatorOnFields("dsId", "key", "value")
				.containsExactly(new DatasourceEntryModel().setDsId(4).setKey("new").setValue("New Entry"),
						new DatasourceEntryModel().setDsId(3).setKey("updated").setValue("Third Entry"),
						new DatasourceEntryModel().setDsId(1).setKey("first").setValue("First Entry"));
	}

	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Die Datenquelle '4711' wurde nicht gefunden.")
	public void testUpdateEntriesUnknownDatasource() throws NodeException {
		operate(() -> new DatasourceResourceImpl().updateEntryList(Integer.toString(4711),
				Arrays.asList(new DatasourceEntryModel().setKey("key").setValue("value"))));
	}

	@Test
	@Expected(ex = RestMappedException.class, message = "Das Feld 'key' darf nicht leer sein.")
	public void testUpdateEntriesBlankKey() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);

		List<DatasourceEntryModel> items = supply(() -> new DatasourceResourceImpl().listEntries(Integer.toString(datasourceId))).getItems();
		items.get(0).setKey("");

		operate(() -> new DatasourceResourceImpl().updateEntryList(Integer.toString(datasourceId), items));
	}

	@Test
	@Expected(ex = RestMappedException.class, message = "Das Feld 'value' darf nicht leer sein.")
	public void testUpdateEntriesBlankValue() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);

		List<DatasourceEntryModel> items = supply(() -> new DatasourceResourceImpl().listEntries(Integer.toString(datasourceId))).getItems();
		items.get(1).setValue("");

		operate(() -> new DatasourceResourceImpl().updateEntryList(Integer.toString(datasourceId), items));
	}

	@Test
	@Expected(ex = RestMappedException.class, message = "Das Feld 'key' darf nicht leer sein.")
	public void testUpdateEntriesNoKey() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);

		List<DatasourceEntryModel> items = supply(() -> new DatasourceResourceImpl().listEntries(Integer.toString(datasourceId))).getItems();
		items.add(new DatasourceEntryModel().setValue("value"));

		operate(() -> new DatasourceResourceImpl().updateEntryList(Integer.toString(datasourceId), items));
	}

	@Test
	@Expected(ex = RestMappedException.class, message = "Das Feld 'value' darf nicht leer sein.")
	public void testUpdateEntriesNoValue() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);

		List<DatasourceEntryModel> items = supply(() -> new DatasourceResourceImpl().listEntries(Integer.toString(datasourceId))).getItems();
		items.add(new DatasourceEntryModel().setKey("key"));

		operate(() -> new DatasourceResourceImpl().updateEntryList(Integer.toString(datasourceId), items));
	}

	@Test
	@Expected(ex = DuplicateValueException.class, message = "Das Feld 'key' darf nicht den Wert 'first' haben, weil dieser Wert bereits verwendet wird.")
	public void testUpdateEntriesDuplicateKey() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);

		List<DatasourceEntryModel> items = supply(() -> new DatasourceResourceImpl().listEntries(Integer.toString(datasourceId))).getItems();
		items.get(1).setKey("first");

		operate(() -> new DatasourceResourceImpl().updateEntryList(Integer.toString(datasourceId), items));
	}

	@Test
	@Expected(ex = DuplicateValueException.class, message = "Das Feld 'value' darf nicht den Wert 'Second Entry' haben, weil dieser Wert bereits verwendet wird.")
	public void testUpdateEntriesDuplicateValue() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);

		List<DatasourceEntryModel> items = supply(() -> new DatasourceResourceImpl().listEntries(Integer.toString(datasourceId))).getItems();
		items.get(2).setValue("Second Entry");

		operate(() -> new DatasourceResourceImpl().updateEntryList(Integer.toString(datasourceId), items));
	}

	@Test
	@Expected(ex = DuplicateValueException.class, message = "Das Feld 'dsId' darf nicht den Wert '3' haben, weil dieser Wert bereits verwendet wird.")
	public void testUpdateEntriesDuplicateDsId() throws NodeException {
		Datasource datasource = supply(() -> createDatasource("Datasource with entries", Pair.of("first", "First Entry"),
				Pair.of("second", "Second Entry"), Pair.of("third", "Third Entry")));
		int datasourceId = execute(Datasource::getId, datasource);

		List<DatasourceEntryModel> items = supply(() -> new DatasourceResourceImpl().listEntries(Integer.toString(datasourceId))).getItems();
		items.add(new DatasourceEntryModel().setDsId(3).setKey("key").setValue("value"));

		operate(() -> new DatasourceResourceImpl().updateEntryList(Integer.toString(datasourceId), items));
	}
}
