package com.gentics.contentnode.tests.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.I18nNamedNodeObject;
import com.gentics.contentnode.object.NamedNodeObject;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.PublishWorkflow;
import com.gentics.contentnode.object.PublishWorkflowStep;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for the I18NHelper
 */
public class I18NHelperTest {
	/**
	 * Set containing all classes of {@link NodeObject} which are known to neither implement {@link NamedNodeObject} nor {@link I18nNamedNodeObject}
	 */
	public final static Set<Class<? extends NodeObject>> classesWithoutNames = new HashSet<>(
			Arrays.asList(Content.class, PublishWorkflow.class, PublishWorkflowStep.class, Value.class));

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test getting a page name (page as representative for a {@link NamedNodeObject})
	 * @throws NodeException
	 */
	@Test
	public void testGetPageName() throws NodeException {
		Page mockedPage = mock(Page.class);
		when(mockedPage.getName()).thenReturn("Mocked Page Name");

		assertThat(I18NHelper.getName(mockedPage)).as("Page name").isEqualTo("Mocked Page Name");
	}

	/**
	 * Test getting a construct name (construct as representative for a {@link I18nNamedNodeObject})
	 * @throws NodeException
	 */
	@Test
	public void testGetConstructName() throws NodeException {
		I18nString mockedName = mock(I18nString.class);
		when(mockedName.toString()).thenReturn("Mocked Construct Name");

		Construct mockedConstruct = mock(Construct.class);
		when(mockedConstruct.getName()).thenReturn(mockedName);

		assertThat(I18NHelper.getName(mockedConstruct)).as("Construct name").isEqualTo("Mocked Construct Name");
	}

	/**
	 * Check that all classes of objects having ttypes (and registered to the
	 * {@link NodeFactory}) implement either {@link NamedNodeObject} or
	 * {@link I18nNamedNodeObject} (except the classes collected in {@link #classesWithoutNames}).
	 */
	@Test
	public void testAllNodeObjectsHaveNames() {
		Set<Class<? extends NodeObject>> classes = new HashSet<>();

		// checking for all ttypes between 1 and 100000
		for (int type = 1; type < 100_000; type++) {
			Class<? extends NodeObject> clazz = NodeFactory.getInstance().getClass(type);
			if (clazz != null) {
				if (!NamedNodeObject.class.isAssignableFrom(clazz) && !I18nNamedNodeObject.class.isAssignableFrom(clazz)
						&& !classesWithoutNames.contains(clazz)) {
					classes.add(clazz);
				}
			}
		}

		assertThat(classes).as(String.format("Classes which implement neither %s nor %s", NamedNodeObject.class,
				I18nNamedNodeObject.class)).isEmpty();
	}
}
