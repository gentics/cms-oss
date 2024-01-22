package com.gentics.contentnode.tests.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.HTMLTextPartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.tools.GlobalIdSync;
import com.gentics.contentnode.tools.GlobalIdSync.ObjectType;

/**
 * Test cases for the {@link GlobalIdSync} tool
 */
public class GlobalIdSyncTest {
	@Rule
	public DBTestContext testContext = new DBTestContext();

	@Before
	public void setup() throws NodeException {
		Set<Integer> constructIds = DBUtils.select("SELECT DISTINCT construct_id id FROM part LEFT JOIN type ON part.type_id = type.id WHERE type.id IS NULL", DBUtils.IDS);
		Transaction t = TransactionManager.getCurrentTransaction();

		for (int id : constructIds) {
			Construct construct = t.getObject(Construct.class, id);
			if (construct != null) {
				construct.delete();
			}
			t.commit(false);
		}
	}

	/**
	 * Test synchronization of identically generated constructs
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSyncConstruct() throws Exception {
		new ConstructTestCase() {
			@Override
			protected Construct createOriginal() throws Exception {
				return create(LongHTMLPartType.class);
			}

			@Override
			protected Construct createCopy() throws Exception {
				return createOriginal();
			}
		}.execute();
	}

	/**
	 * Test synchronization of a construct, where the parts have different type
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSyncConstructChangedPart() throws Exception {
		new ConstructTestCase() {
			@Override
			protected Construct createOriginal() throws Exception {
				return create(LongHTMLPartType.class);
			}

			@Override
			protected Construct createCopy() throws Exception {
				return create(HTMLTextPartType.class);
			}
		}.execute();
	}

	/**
	 * Test synchronization of a construct, which has an additional part (in the
	 * target system)
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSyncConstructAddPart() throws Exception {
		new ConstructTestCase() {
			@Override
			protected Construct createOriginal() throws Exception {
				return create(LongHTMLPartType.class);
			}

			@Override
			protected Construct createCopy() throws Exception {
				return create(LongHTMLPartType.class, LongHTMLPartType.class);
			}
		}.execute();
	}

	/**
	 * Test synchronization of a construct, which has viewer parts (in the
	 * target system)
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSyncConstructRemovePart() throws Exception {
		new ConstructTestCase() {
			@Override
			protected Construct createOriginal() throws Exception {
				return create(LongHTMLPartType.class, LongHTMLPartType.class);
			}

			@Override
			protected Construct createCopy() throws Exception {
				return create(LongHTMLPartType.class);
			}
		}.execute();
	}

	/**
	 * Test synchronization of a datasource
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSyncDatasource() throws Exception {
		new DatasourceTestCase() {
			@Override
			protected Datasource createOriginal() throws Exception {
				return ContentNodeTestDataUtils.createDatasource("datasource", Arrays.asList("One", "Two", "Three"));
			}

			@Override
			protected Datasource createCopy() throws Exception {
				return createOriginal();
			}
		}.execute();
	}

	/**
	 * Test synchronization of a datasource with an additional entry
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSyncDatasourceAddEntry() throws Exception {
		new DatasourceTestCase() {
			@Override
			protected Datasource createOriginal() throws Exception {
				return ContentNodeTestDataUtils.createDatasource("datasource", Arrays.asList("One", "Two", "Three"));
			}

			@Override
			protected Datasource createCopy() throws Exception {
				return ContentNodeTestDataUtils.createDatasource("datasource", Arrays.asList("Null", "One", "Two", "Three"));
			}
		}.execute();
	}

	/**
	 * Test synchronization of a datasource with a removed entry
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSyncDatasourceRemoveEntry() throws Exception {
		new DatasourceTestCase() {
			@Override
			protected Datasource createOriginal() throws Exception {
				return ContentNodeTestDataUtils.createDatasource("datasource", Arrays.asList("Null", "One", "Two", "Three"));
			}

			@Override
			protected Datasource createCopy() throws Exception {
				return ContentNodeTestDataUtils.createDatasource("datasource", Arrays.asList("One", "Two", "Three"));
			}
		}.execute();
	}

	/**
	 * Test synchronization of an object tag definition
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSyncObjectTagDefinition() throws Exception {
		new ObjectTagDefinitionTestCase() {
			protected int constructId;

			@Override
			protected void init() throws Exception {
				Node testNode = ContentNodeTestDataUtils.createNode();
				constructId = ContentNodeTestDataUtils.createConstruct(testNode, LongHTMLPartType.class, "construct", "part");
			}

			@Override
			protected ObjectTagDefinition createOriginal() throws Exception {
				return ContentNodeTestDataUtils.createObjectPropertyDefinition(
						Folder.TYPE_FOLDER, constructId, "synctest", "synctest");
			}

			@Override
			protected ObjectTagDefinition createCopy() throws Exception {
				return createOriginal();
			}
		}.execute();
	}

	/**
	 * Abstract class for a Testcase
	 * 
	 * @param <T>
	 *            class that is tested
	 */
	protected static abstract class TestCase<T extends NodeObject> {
		/**
		 * Tested class
		 */
		protected Class<T> clazz;

		/**
		 * Tested type
		 */
		protected ObjectType type;

		/**
		 * Create an instance
		 * 
		 * @param clazz
		 *            tested class
		 * @param type
		 *            tested type
		 */
		public TestCase(Class<T> clazz, ObjectType type) {
			this.clazz = clazz;
			this.type = type;
		}

		/**
		 * Initialize the testcase
		 * 
		 * @throws Exception
		 */
		protected void init() throws Exception {
		}

		/**
		 * Create the original object
		 * 
		 * @return original object
		 * @throws Exception
		 */
		protected abstract T createOriginal() throws Exception;

		/**
		 * Create the copy (which will be synchronized)
		 * 
		 * @return copy
		 * @throws Exception
		 */
		protected abstract T createCopy() throws Exception;

		/**
		 * Collect all globalIds of this object and all its subobjects. The keys
		 * of the map should be meaningful and chosen in a way, that objects,
		 * that will be synchonized have the same keys
		 * 
		 * @param object
		 *            object
		 * @return map holding all global IDs
		 * @throws Exception
		 */
		protected abstract Map<String, GlobalId> collectGlobalIds(T object) throws Exception;

		/**
		 * Execute the test.
		 * <ol>
		 * <li>Create the original object (via {@link #createOriginal()})</li>
		 * <li>Collect the globalIds of the original {via
		 * {@link #collectGlobalIds(NodeObject)}</li>
		 * <li>Read the object info</li>
		 * <li>Delete the original object</li>
		 * <li>Create the copy (via {@link #createCopy()})</li>
		 * <li>Match the objects and transform the global IDs</li>
		 * <li>Collect the globalIds of the copy and check, whether object was
		 * matched</li>
		 * </ol>
		 * 
		 * @throws Exception
		 */
		public void execute() throws Exception {
			init();

			Transaction t = TransactionManager.getCurrentTransaction();
			T original = createOriginal();
			Map<String, GlobalId> globalIds = collectGlobalIds(original);

			StringWriter definition = new StringWriter();
			GlobalIdSync.readObjectInfo(definition, type);

			// delete the original
			original.delete();
			t.commit(false);

			// check that the construct is gone
			assertNull("Original object must have been deleted", t.getObject(clazz, original.getId()));

			// create the copy
			T copy = createCopy();

			Map<String, GlobalId> newGlobalIds = collectGlobalIds(copy);

			// check that the new object uses just new globalIds
			for (String key : newGlobalIds.keySet()) {
				assertNotEquals(key + " must have a new globalId", globalIds.get(key), newGlobalIds.get(key));
			}

			// now match the constructs
			StringWriter match = new StringWriter();
			GlobalIdSync.matchObjects(new StringReader(definition.toString()), match);

			// generate and execute the sql statements
			StringWriter sql = new StringWriter();
			GlobalIdSync.createUpdateSQL(new StringReader(match.toString()), new PrintWriter(sql), true);
			t.commit(false);

			// clear caches
			t.clearNodeObjectCache();

			// check whether the global IDs have been changed
			copy = t.getObject(clazz, copy.getId());
			Map<String, GlobalId> changedGlobalIds = collectGlobalIds(copy);

			for (String key : changedGlobalIds.keySet()) {
				if (globalIds.containsKey(key)) {
					assertEquals(key + " must have the same globalId", globalIds.get(key), changedGlobalIds.get(key));
				}
			}
		}
	}

	/**
	 * Abstract TestCase class for constructs
	 */
	protected static abstract class ConstructTestCase extends TestCase<Construct> {
		/**
		 * Test node
		 */
		protected Node testNode;

		/**
		 * Create an instance
		 */
		public ConstructTestCase() {
			super(Construct.class, ObjectType.construct);
		}

		@Override
		protected void init() throws Exception {
			testNode = ContentNodeTestDataUtils.createNode();
		}

		/**
		 * Create a construct with at least one part (of PartType
		 * {@link LongHTMLPartType})
		 * 
		 * @param additionalPartTypeClasses
		 *            optional list of parttype classes for additional parts
		 * @return construct
		 * @throws Exception
		 */
		protected Construct create(Class<?>... additionalPartTypeClasses) throws Exception {
			Transaction t = TransactionManager.getCurrentTransaction();

			int constructId = ContentNodeTestDataUtils.createConstruct(testNode, LongHTMLPartType.class, "construct", "part1");

			if (!ObjectTransformer.isEmpty(additionalPartTypeClasses)) {
				Construct construct = t.getObject(Construct.class, constructId, true);
				int partCounter = 2;
				for (Class<?> partTypeClass : additionalPartTypeClasses) {
					String keyWord = "part" + partCounter++;
					Part part = t.createObject(Part.class);
					part.setEditable(1);
					part.setHidden(false);
					part.setKeyname(keyWord);
					part.setName(keyWord, 1);
					part.setPartTypeId(ContentNodeTestDataUtils.getPartTypeId(partTypeClass));
					part.setDefaultValue(t.createObject(Value.class));
					construct.getParts().add(part);
				}
				construct.save();
				t.commit(false);
			}

			return t.getObject(Construct.class, constructId);
		}

		@Override
		protected Map<String, GlobalId> collectGlobalIds(Construct object) throws Exception {
			Map<String, GlobalId> globalIds = new HashMap<>();
			globalIds.put("construct", object.getGlobalId());
			for (Part part : object.getParts()) {
				globalIds.put("part " + part.getKeyname(), part.getGlobalId());
				globalIds.put("value " + part.getKeyname(), part.getDefaultValue().getGlobalId());
			}
			return globalIds;
		}
	}

	/**
	 * Abstract TestCase class for datasources
	 */
	protected static abstract class DatasourceTestCase extends TestCase<Datasource> {
		/**
		 * Create an instance
		 */
		public DatasourceTestCase() {
			super(Datasource.class, ObjectType.datasource);
		}

		@Override
		protected Map<String, GlobalId> collectGlobalIds(Datasource object) throws Exception {
			Map<String, GlobalId> globalIds = new HashMap<>();
			globalIds.put("datasource", object.getGlobalId());
			for (DatasourceEntry entry : object.getEntries()) {
				globalIds.put("entry " + entry.getKey(), entry.getGlobalId());
			}
			return globalIds;
		}
	}

	/**
	 * Abstract TestCase for object tag definitions
	 */
	protected static abstract class ObjectTagDefinitionTestCase extends TestCase<ObjectTagDefinition> {
		/**
		 * Create an instance
		 */
		public ObjectTagDefinitionTestCase() {
			super(ObjectTagDefinition.class, ObjectType.objprop);
		}

		@Override
		protected Map<String, GlobalId> collectGlobalIds(ObjectTagDefinition object) throws Exception {
			Map<String, GlobalId> globalIds = new HashMap<>();

			globalIds.put("objtag", object.getObjectTag().getGlobalId());
			globalIds.put("objprop", GlobalId.getGlobalId("objprop", object.getObjectPropId()));

			return globalIds;
		}
	}
}
