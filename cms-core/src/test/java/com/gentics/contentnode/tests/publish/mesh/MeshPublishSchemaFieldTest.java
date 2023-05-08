package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

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

/**
 * Test cases for transformation of tagmap entries to Mesh schemas
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY })
public class MeshPublishSchemaFieldTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@Parameters(name = "{index}: objecttype {0}, attributetype {1}, multivalue {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (int objectType : Arrays.asList(Page.TYPE_PAGE, Folder.TYPE_FOLDER, File.TYPE_FILE)) {
			for (AttributeType attributeType : AttributeType.values()) {
				// filter out attribute types, that are not supported
				if (!attributeType.validFor(Type.mesh)) {
					continue;
				}
				for (boolean multivalue : Arrays.asList(true, false)) {
					// binary attribute types do not support multivalue
					if (attributeType == AttributeType.binary && multivalue) {
						continue;
					}
					data.add(new Object[] { objectType, attributeType, multivalue });
				}
			}
		}
		return data;
	}

	@Parameter(0)
	public int objectType;

	@Parameter(1)
	public AttributeType attributeType;

	@Parameter(2)
	public boolean multivalue;

	@Before
	public void setup() throws NodeException {
		Trx.operate(trx -> {
			for (ContentRepository cr : trx.getObjects(ContentRepository.class, DBUtils.select("SELECT id FROM contentrepository", DBUtils.IDS))) {
				cr.delete(true);
			}
		});
	}

	@Test
	public void test() throws NodeException {
		ContentRepository contentRepository = Trx.supply(() -> ContentNodeTestDataUtils.create(ContentRepository.class, cr -> {
			cr.setCrType(Type.mesh);
			cr.setUrl("localhost:1234/demo");
			cr.setDbType("");
			cr.setName("Test CR");
			cr.addEntry("tagname", "mapname", objectType, objectType, attributeType, multivalue, false, false, false, false);
			cr.addEntry("name", "name", objectType, 0, AttributeType.text, false, false, true, true, false);
		}));

		Trx.operate(trx -> {
			try (MeshPublisher mp = new MeshPublisher(contentRepository, false)) {
				SchemaModel schema = mp.getSchema(objectType, new SchemaModelImpl());
				schema.validate();
				switch (objectType) {
				case Folder.TYPE_FOLDER:
					assertThat(schema.getName()).as("Schema name").isEqualTo("demo_folder");
					assertThat(schema.getContainer()).as("Schema container flag").isTrue();
					break;
				case Page.TYPE_PAGE:
					assertThat(schema.getName()).as("Schema name").isEqualTo("demo_content");
					assertThat(schema.getContainer()).as("Schema container flag").isFalse();
					break;
				case File.TYPE_FILE:
					assertThat(schema.getName()).as("Schema name").isEqualTo("demo_binary_content");
					assertThat(schema.getContainer()).as("Schema container flag").isFalse();
					break;
				}

				assertThat(schema.getFields()).as("Schema fields").hasSize(2);
			}
		});
	}
}
