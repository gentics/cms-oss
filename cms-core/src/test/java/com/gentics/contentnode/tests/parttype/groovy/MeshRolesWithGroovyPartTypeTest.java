package com.gentics.contentnode.tests.parttype.groovy;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createDatasource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.parttype.MultiSelectPartType;
import com.gentics.contentnode.object.parttype.groovy.GroovyPartType;
import com.gentics.contentnode.publish.cr.MeshRoleRenderer;

/**
 * Specific test cases for usage of groovy scripts to calculate Mesh roles
 */
@RunWith(Parameterized.class)
public class MeshRolesWithGroovyPartTypeTest extends AbstractGroovyTest {
	public final static String SELECT_PART_NAME = "select";

	protected static Datasource rolesDatasource;

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return List.of(
				new Object[] { "literal", "def roles = [\"role_a\", \"role_b\", \"role_c\"]\nreturn roles",
						new String[] { "role_a", "role_b", "role_c" } },
				new Object[] { "resolved from tag", "return cms.tag.parts.keySet()",
						new String[] { GROOVY_PART_NAME, HBS_PART_NAME, PAGE_PART_NAME, SELECT_PART_NAME,
								"unique_tag_id" } },
				new Object[] { "resolved from select part",
						"return cms.tag.parts.%s.selection.key".formatted(SELECT_PART_NAME), new String[] { "role_1", "role_3" } });
	}

	@BeforeClass
	public static void setupOnce() throws NodeException, IOException {
		AbstractGroovyTest.setupOnce();

		rolesDatasource = supply(() -> createDatasource("roles", List.of("role_1", "role_2", "role_3")));

		groovyConstruct = update(groovyConstruct, c -> {
			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(MultiSelectPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname(SELECT_PART_NAME);
				p.setName("Select", 1);
				p.setInfoInt(rolesDatasource.getId());
			}).doNotSave().build());
		}).build();
	}

	@Parameter(0)
	public String testCase;

	@Parameter(1)
	public String script;

	@Parameter(2)
	public String[] expectedRoles;

	@Before
	public void setup() throws NodeException, IOException {
		testPage = update(testPage, p -> {
			ContentTag contentTag = p.getContentTag(GROOVY_TAGNAME);

			getPartType(GroovyPartType.class, contentTag, GROOVY_PART_NAME).setText(script);
			getPartType(MultiSelectPartType.class, contentTag, SELECT_PART_NAME)
					.setSelected(rolesDatasource.getEntries().get(0), rolesDatasource.getEntries().get(2));
		}).publish().unlock().build();
	}

	@Test
	public void test() throws NodeException {
		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish(testPage)) {
			Transaction t = trx.getTransaction();
			MeshRoleRenderer meshRoleRenderer = new MeshRoleRenderer(Page.TYPE_PAGE,
					"page.tags.%s.parts.%s.execute".formatted(GROOVY_TAGNAME, GROOVY_PART_NAME));

			Collection<?> rolesRaw = ObjectTransformer.getCollection(
					meshRoleRenderer.getRenderedTransformedValue(t.getRenderType(), t.getRenderResult(), null),
					Collections.emptyList());
			Set<String> roles = rolesRaw.stream().map(Object::toString).collect(Collectors.toSet());

			assertThat(roles).as("Roles").containsOnly(expectedRoles);

			trx.success();
		}
	}
}
