package com.gentics.contentnode.tests.publish.mesh;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;

/**
 * Test cases for generation of Mesh schemas
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY })
public class MeshPublishSchemaTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext();

	protected static Map<Integer, String> typeMap = new HashMap<>();

	static {
		typeMap.put(Page.TYPE_PAGE, "page");
		typeMap.put(Folder.TYPE_FOLDER, "folder");
		typeMap.put(File.TYPE_FILE, "file");
	}

	@Parameters(name = "{index}: objecttype {0}, attributetype {1}, multivalue {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (int objectType : Arrays.asList(Page.TYPE_PAGE, Folder.TYPE_FOLDER, File.TYPE_FILE)) {
			data.add(new Object[] { objectType });
		}
		return data;
	}

	@Parameter(0)
	public int objectType;

	/**
	 * Tested CR
	 */
	private ContentRepository contentRepository;

	@Before
	public void setup() throws NodeException {
		Trx.operate(trx -> {
			for (ContentRepository cr : trx.getObjects(ContentRepository.class, DBUtils.select("SELECT id FROM contentrepository", DBUtils.IDS))) {
				cr.delete(true);
			}
		});

		contentRepository = Trx.supply(() -> ContentNodeTestDataUtils.create(ContentRepository.class, cr -> {
			cr.setCrType(Type.mesh);
			cr.setUrl("localhost:1234/demo");
			cr.setDbType("");
			cr.setName("Test CR");
		}));
	}

	/**
	 * Test not setting a displayfield
	 * @throws NodeException
	 */
	@Test(expected = NodeException.class)
	public void testMissingDisplayfield() throws NodeException {
		contentRepository = Trx.supply(() -> ContentNodeTestDataUtils.update(contentRepository, cr -> {
			cr.addEntry("tagname", "segmentfield", objectType, 0, AttributeType.text, false, false, true, false, false);
		}));

		Trx.operate(trx -> {
			try (MeshPublisher mp = new MeshPublisher(contentRepository, false)) {
				mp.getSchema(objectType, new SchemaModelImpl());
			}
		});
	}

	/**
	 * Test setting a displayfield
	 * @throws NodeException
	 */
	@Test
	public void testDisplayfield() throws NodeException {
		contentRepository = Trx.supply(() -> ContentNodeTestDataUtils.update(contentRepository, cr -> {
			cr.addEntry("tagname", "segmentfield", objectType, 0, AttributeType.text, false, false, true, false, false);
			cr.addEntry("tagname", "testfield", objectType, 0, AttributeType.text, false, false, false, true, false);
		}));

		Trx.operate(trx -> {
			try (MeshPublisher mp = new MeshPublisher(contentRepository, false)) {
				SchemaModel schema = mp.getSchema(objectType, new SchemaModelImpl());
				schema.validate();

				assertThat(schema.getDisplayField()).as("Schema displayfield").isEqualTo("testfield");
			}
		});
	}

	/**
	 * Test not setting a segment
	 * @throws NodeException
	 */
	@Test(expected = NodeException.class)
	public void testMissingSegmentfield() throws NodeException {
		contentRepository = Trx.supply(() -> ContentNodeTestDataUtils.update(contentRepository, cr -> {
			cr.addEntry("tagname", "displayfield", objectType, 0, AttributeType.text, false, false, false, true, false);
		}));

		Trx.operate(trx -> {
			try (MeshPublisher mp = new MeshPublisher(contentRepository, false)) {
				mp.getSchema(objectType, new SchemaModelImpl());
			}
		});
	}

	/**
	 * Test setting a segmentfield
	 * @throws NodeException
	 */
	@Test
	public void testSegmentfield() throws NodeException {
		contentRepository = Trx.supply(() -> ContentNodeTestDataUtils.update(contentRepository, cr -> {
			cr.addEntry("tagname", "testfield", objectType, 0, AttributeType.text, false, false, true, false, false);
			cr.addEntry("tagname", "displayfield", objectType, 0, AttributeType.text, false, false, false, true, false);
		}));

		Trx.operate(trx -> {
			try (MeshPublisher mp = new MeshPublisher(contentRepository, false)) {
				SchemaModel schema = mp.getSchema(objectType, new SchemaModelImpl());
				schema.validate();

				assertThat(schema.getSegmentField()).as("Schema segmentfield").isEqualTo("testfield");
			}
		});
	}

	/**
	 * Test not setting URL fields
	 * @throws NodeException
	 */
	@Test
	public void testNoUrlfields() throws NodeException {
		contentRepository = Trx.supply(() -> ContentNodeTestDataUtils.update(contentRepository, cr -> {
			cr.addEntry("tagname", "testfield", objectType, 0, AttributeType.text, false, false, true, true, false);
		}));

		Trx.operate(trx -> {
			try (MeshPublisher mp = new MeshPublisher(contentRepository, false)) {
				SchemaModel schema = mp.getSchema(objectType, new SchemaModelImpl());
				schema.validate();

				assertThat(schema.getUrlFields()).as("Schema url fields").isEmpty();
			}
		});
	}

	/**
	 * Test setting a single URL field
	 * @throws NodeException
	 */
	@Test
	public void testSingleUrlfield() throws NodeException {
		contentRepository = Trx.supply(() -> ContentNodeTestDataUtils.update(contentRepository, cr -> {
			cr.addEntry("tagname", "testfield", objectType, 0, AttributeType.text, false, false, true, true, false);
			cr.addEntry("tagname", "urlfield", objectType, 0, AttributeType.text, false, false, false, false, true);
		}));

		Trx.operate(trx -> {
			try (MeshPublisher mp = new MeshPublisher(contentRepository, false)) {
				SchemaModel schema = mp.getSchema(objectType, new SchemaModelImpl());
				schema.validate();

				assertThat(schema.getUrlFields()).as("Schema url fields").containsOnly("urlfield");
			}
		});
	}

	/**
	 * Test setting multiple URL fields
	 * @throws NodeException
	 */
	@Test
	public void testUrlFields() throws NodeException {
		contentRepository = Trx.supply(() -> ContentNodeTestDataUtils.update(contentRepository, cr -> {
			cr.addEntry("tagname", "testfield", objectType, 0, AttributeType.text, false, false, true, true, false);
			cr.addEntry("tagname", "urlfield1", objectType, 0, AttributeType.text, false, false, false, false, true);
			cr.addEntry("tagname", "urlfield2", objectType, 0, AttributeType.text, false, false, false, false, true);
			cr.addEntry("tagname", "urlfield3", objectType, 0, AttributeType.text, false, false, false, false, true);
		}));

		Trx.operate(trx -> {
			try (MeshPublisher mp = new MeshPublisher(contentRepository, false)) {
				SchemaModel schema = mp.getSchema(objectType, new SchemaModelImpl());
				schema.validate();

				assertThat(schema.getUrlFields()).as("Schema url fields").containsOnly("urlfield1", "urlfield2", "urlfield3");
			}
		});
	}

	/**
	 * Test unsetting the generic searchindex configuration
	 * @throws NodeException
	 */
	@Test
	public void testNullSearchindexConfiguration() throws NodeException {
		contentRepository = Trx.supply(() -> ContentNodeTestDataUtils.update(contentRepository, cr -> {
			cr.addEntry("tagname", "testfield", objectType, 0, AttributeType.text, false, false, true, true, false);
			cr.setElasticsearch(null);
		}));

		Trx.operate(trx -> {
			try (MeshPublisher mp = new MeshPublisher(contentRepository, false)) {
				SchemaModel schema = mp.getSchema(objectType, new SchemaModelImpl());
				schema.validate();

				assertThat(schema.getElasticsearch()).as("Elasticsearch config").isNotNull().isEqualTo(new JsonObject());
			}
		});
	}

	/**
	 * Test setting a generic searchindex configuration
	 * @throws NodeException
	 */
	@Test
	public void testSearchindexConfiguration() throws NodeException {
		JsonObject typeConfig = new JsonObject();
		typeConfig.put("test", true);

		contentRepository = Trx.supply(() -> ContentNodeTestDataUtils.update(contentRepository, cr -> {
			cr.addEntry("tagname", "testfield", objectType, 0, AttributeType.text, false, false, true, true, false);
			JsonObject elConfig = new JsonObject();
			elConfig.put(typeMap.get(objectType), typeConfig);

			cr.setElasticsearch(elConfig.toString());
		}));

		Trx.operate(trx -> {
			try (MeshPublisher mp = new MeshPublisher(contentRepository, false)) {
				SchemaModel schema = mp.getSchema(objectType, new SchemaModelImpl());
				schema.validate();

				assertThat(schema.getElasticsearch()).as("Elasticsearch config").isNotNull().isEqualTo(typeConfig);
			}
		});
	}

	/**
	 * Test setting an invalid generic searchindex configuration
	 * @throws NodeException
	 */
	@Test(expected = DecodeException.class)
	public void testInvalidSearchindexConfiguration() throws NodeException {
		contentRepository = Trx.supply(() -> ContentNodeTestDataUtils.update(contentRepository, cr -> {
			cr.addEntry("tagname", "testfield", objectType, 0, AttributeType.text, false, false, true, true, false);
			cr.setElasticsearch("This is not a JSON object");
		}));

		Trx.operate(trx -> {
			try (MeshPublisher mp = new MeshPublisher(contentRepository, false)) {
				SchemaModel schema = mp.getSchema(objectType, new SchemaModelImpl());
				schema.validate();

				assertThat(schema.getElasticsearch()).as("Elasticsearch config").isNotNull().isEqualTo(new JsonObject());
			}
		});
	}

	/**
	 * Test unsetting the searchindex configuration for a tagmap entry
	 * @throws NodeException
	 */
	@Test
	public void testEntryNullSearchindexConfiguration() throws NodeException {
		contentRepository = Trx.supply(() -> ContentNodeTestDataUtils.update(contentRepository, cr -> {
			cr.addEntry("tagname", "testfield", objectType, 0, AttributeType.text, false, false, true, true, false);
			cr.addEntry("tagname", "searchablefield", objectType, 0, AttributeType.text, false, false, false, false, false);

			cr.getEntryByMapName("searchablefield").setElasticsearch(null);
		}));

		Trx.operate(trx -> {
			try (MeshPublisher mp = new MeshPublisher(contentRepository, false)) {
				SchemaModel schema = mp.getSchema(objectType, new SchemaModelImpl());
				schema.validate();

				assertThat(schema.getField("searchablefield")).as("searchable field").isNotNull();
				assertThat(schema.getField("searchablefield").getElasticsearch()).as("searchable field config").isEqualTo(new JsonObject());
			}
		});
	}

	/**
	 * Test setting a searchindex configuration for a tagmap entry
	 * @throws NodeException
	 */
	@Test
	public void testEntrySearchindexConfiguration() throws NodeException {
		JsonObject entryConfig = new JsonObject();
		entryConfig.put("test", true);

		contentRepository = Trx.supply(() -> ContentNodeTestDataUtils.update(contentRepository, cr -> {
			cr.addEntry("tagname", "testfield", objectType, 0, AttributeType.text, false, false, true, true, false);
			cr.addEntry("tagname", "searchablefield", objectType, 0, AttributeType.text, false, false, false, false, false);

			cr.getEntryByMapName("searchablefield").setElasticsearch(entryConfig.toString());
		}));

		Trx.operate(trx -> {
			try (MeshPublisher mp = new MeshPublisher(contentRepository, false)) {
				SchemaModel schema = mp.getSchema(objectType, new SchemaModelImpl());
				schema.validate();

				assertThat(schema.getField("searchablefield")).as("searchable field").isNotNull();
				assertThat(schema.getField("searchablefield").getElasticsearch()).as("searchable field config").isEqualTo(entryConfig);
			}
		});
	}

	/**
	 * Test setting an invalid searchindex configuration for a tagmap entry
	 * @throws NodeException
	 */
	@Test(expected = DecodeException.class)
	public void testEntryInvalidSearchindexConfiguration() throws NodeException {
		contentRepository = Trx.supply(() -> ContentNodeTestDataUtils.update(contentRepository, cr -> {
			cr.addEntry("tagname", "testfield", objectType, 0, AttributeType.text, false, false, true, true, false);
			cr.addEntry("tagname", "searchablefield", objectType, 0, AttributeType.text, false, false, false, false, false);

			cr.getEntryByMapName("searchablefield").setElasticsearch("This is not a JSON object");
		}));

		Trx.operate(trx -> {
			try (MeshPublisher mp = new MeshPublisher(contentRepository, false)) {
				SchemaModel schema = mp.getSchema(objectType, new SchemaModelImpl());
				schema.validate();

				assertThat(schema.getField("searchablefield")).as("searchable field").isNotNull();
				assertThat(schema.getField("searchablefield").getElasticsearch()).as("searchable field config").isEqualTo(new JsonObject());
			}
		});
	}
}
