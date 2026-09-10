package com.gentics.contentnode.tests.dirting;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceNotAvailableException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.events.Dependency;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.datasource.CNWriteableDatasource;

/**
 * Tests checking the correct dirting of languagesets
 * @author escitalopram
 *
 */
@RunWith(Parameterized.class)
public class LanguagesetDirtingTest {
	@Parameterized.Parameters(name = "{index} instantPublish: {0}, multichannelling: {1}, mccr: {2}")
	public static Collection<Object[]> getParameters() {
		List<Object[]> result = new ArrayList<Object[]>();
		boolean[] bools = new boolean[] { false, true };
		for (boolean instantPublish : bools) {
			for (boolean multichannelling : bools) {
				for (boolean mccr : bools) {
					if (multichannelling || !mccr) {
						result.add(new Object[] { instantPublish, multichannelling, mccr });
					}
				}
			}
		}
		return result;
	}

	private final boolean instantPublishing;
	private final boolean multichannelling;
	private final boolean mccr;
	private Node node;

	public LanguagesetDirtingTest(boolean instantPublishing, boolean multichannelling, boolean mccr) {
		this.instantPublishing = instantPublishing;
		this.multichannelling = multichannelling;
		this.mccr = mccr;
	}

	@Rule
	public DBTestContext context = new DBTestContext();
	private ContentLanguage klingon;
	private ContentLanguage tengwar;
	private Template tpl;

	@Before
	public void setup() throws Exception {
		NodePreferences prefs = context.getContext().getNodeConfig().getDefaultPreferences();

		prefs.setFeature("mccr", mccr);
		prefs.setFeature(Feature.MULTICHANNELLING.toString().toLowerCase(), multichannelling);
		prefs.setFeature("contentnode.feature.instant_cr_publishing", instantPublishing);

		Transaction t = TransactionManager.getCurrentTransaction();
		t.commit(false);
		klingon = Creator.createLanguage("klingon", "tlh");
		tengwar = Creator.createLanguage("tengwar", "teng");
		node = ContentNodeTestDataUtils.createNode("asdtest.local", "asdtest", PublishTarget.CONTENTREPOSITORY,
				mccr, false, klingon, tengwar);

		ContentMap contentMap = node.getContentMap();
		ContentRepository cr = t.getObject(ContentRepository.class, contentMap.getId(), true);
		cr.setLanguageInformation(true);
		cr.save();
		t.commit(false);

		tpl = Creator.createTemplate("lsdtemplate", "asdf", node.getFolder());
	}

	@After
	public void tearDown() throws Exception {
		PortalConnectorFactory.destroy();
	}

	/**
	 * Check if languagesets are correctly updated when lange versions are deleted.
	 * @throws Exception
	 */
	@Test
	public void deleteLanguageVersionTest() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// create & publish a page with a translation
		Page originalPage = Creator.createPage("testpage", node.getFolder(), tpl, klingon);
		t.commit(false);
		PageLoadResponse translationResult = getPageResource().translate((Integer) originalPage.getId(), "teng", false, null);
		ContentNodeRESTUtils.assertResponseOK(translationResult);
		Page translatedPage = t.getObject(Page.class, translationResult.getPage().getId(), true);
		originalPage.publish();
		translatedPage.publish();
		t.commit(false);
		context.getContext().startDirtQueueWorker();
		context.getContext().publish(false, true);
		context.getContext().stopDirtQueueWorker();
		t.commit(false);

		// All languages must be in linked in the CR
		CNWriteableDatasource ds = node.getContentMap().getDatasource();
		assertPageIdInAttribute(originalPage, originalPage, "contentid_tlh");
		assertPageIdInAttribute(translatedPage, originalPage, "contentid_teng");
		assertPageIdInAttribute(originalPage, translatedPage, "contentid_tlh");
		assertPageIdInAttribute(translatedPage, translatedPage, "contentid_teng");

		// A dependency on the languageset must exist
		Dependency d = DependencyManager.createDependency(new DependencyObject(originalPage), "languageset", new DependencyObject(originalPage), Events.UPDATE | Events.DELETE);
		assertDependencyExistence(originalPage, d, true);

		// Delete a language variant & publish
		translatedPage.delete();
		t.commit(false);
		context.getContext().startDirtQueueWorker();
		context.getContext().publish(false, true);
		context.getContext().stopDirtQueueWorker();
		t.commit(false);
		originalPage = t.getObject(Page.class, originalPage.getId());
		// The remaining one must have its contentid_* updated
		assertPageIdInAttribute(originalPage, originalPage, "contentid_tlh");
		assertPageIdInAttribute(null, originalPage, "contentid_teng");
	}

	/**
	 * Check if languagesets are correctly updated when lange versions are added.
	 * @throws Exception
	 */
	@Test
	public void addLanguageVersionTest() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Create a page & publish
		Page originalPage = Creator.createPage("testpage", node.getFolder(), tpl, klingon);
		originalPage.publish();
		t.commit(false);
		context.getContext().startDirtQueueWorker();
		context.getContext().publish(false, true);
		context.getContext().stopDirtQueueWorker();
		t.commit(false);

		// create translation, publish it
		PageLoadResponse translationResult = getPageResource().translate((Integer) originalPage.getId(), "teng", false, null);
		ContentNodeRESTUtils.assertResponseOK(translationResult);
		Page translatedPage = t.getObject(Page.class, translationResult.getPage().getId(), true);
		translatedPage.publish();
		t.commit(false);
		context.getContext().startDirtQueueWorker();
		context.getContext().publish(false, true);
		context.getContext().stopDirtQueueWorker();
		t.commit(false);

		// original version must have it's contentid_* updated
		CNWriteableDatasource ds = node.getContentMap().getDatasource();
		assertPageIdInAttribute(originalPage, originalPage, "contentid_tlh");
		assertPageIdInAttribute(originalPage, translatedPage, "contentid_tlh");
	}

	/**
	 * Assert that a certain page or no one is referenced by a specified attribute of an object from a CR
	 * @param referencedPage the page that should be referenced or null for no page
	 * @param containingPage the page containing the reference
	 * @param attribute the attribute to be checked
	 * @throws DatasourceNotAvailableException
	 * @throws NodeException
	 */
	private void assertPageIdInAttribute(Page referencedPage, Page containingPage, String attribute) throws DatasourceNotAvailableException, NodeException {
		Datasource ds;
		if (mccr) {
			ds = node.getContentMap().getMCCRDatasource();
		} else {
			ds = node.getContentMap().getDatasource();
		}
		String containeroid = "10007." + containingPage.getId();
		String referenceoid = referencedPage == null ? null : ("10007." + referencedPage.getId());
		// Filter for objects with nonexisting referenced objects – must assert for empty
		String nonExistingFilter = "object.contentid == '" + containeroid + "' AND !isempty(object." + attribute + ")";
		// Filter for objects with existent referenced objects – must assert for nonempty
		String existingFilter = "object.contentid == '" + containeroid + "' AND object." + attribute + " == '" + referenceoid + "'";

		DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse(referencedPage == null ? nonExistingFilter : existingFilter));
		Collection<Resolvable> result = ds.getResult(filter, null);
		assertEquals("Wrong reference found", referencedPage == null, result.isEmpty());
	}

	/**
	 * Assert that a certain dependency exists
	 * @param changed check dependencies for this object
	 * @param dep the dependency to look for
	 * @param exist whether the dependency should exist
	 * @throws NodeException
	 */
	public void assertDependencyExistence(Page changed, Dependency dep, boolean exist) throws NodeException {
		Set<Dependency> deps = new HashSet<Dependency>(DependencyManager.getDependenciesForObject(changed, null, null));
		assertEquals("Dependency must " + (exist ? "" : "not") + " exist", exist, deps.contains(dep));
	}
}
