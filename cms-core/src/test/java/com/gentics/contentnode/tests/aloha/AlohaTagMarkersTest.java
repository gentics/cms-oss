package com.gentics.contentnode.tests.aloha;

import static com.gentics.contentnode.tests.utils.ContentNodeMockUtils.content;
import static com.gentics.contentnode.tests.utils.ContentNodeMockUtils.contentTag;
import static com.gentics.contentnode.tests.utils.ContentNodeMockUtils.page;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.testutils.DBTestContext;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Tests for annotating the tags in readonly mode with the rendertype parameter {@link AlohaRenderer#TAG_MARKERS}:
 * which tags {@link AlohaRenderer#block(String, com.gentics.contentnode.parser.tag.ParserTag, RenderResult)} annotates.
 * How the code is annotated is tested in {@link TagMarkersTest}.
 */
public class AlohaTagMarkersTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Annotations of {@link #tag}
	 */
	private final static String ANNOTATIONS = "data-gcn-pageid=\"556\" data-gcn-tagid=\"124\" data-gcn-tagname=\"dummy124\"";

	/**
	 * Marker, which {@link #tag} gets, when it renders only text
	 */
	private final static String MARKER_START = "<!--gtxtagmarker " + ANNOTATIONS + "-->";

	private final static String MARKER_END = "<!--/gtxtagmarker-->";

	/**
	 * Editable tag of the page
	 */
	private static ContentTag tag;

	/**
	 * Tag of the page, which is not editable (e.g. inherited)
	 */
	private static ContentTag notEditableTag;

	/**
	 * Editable tag, which does not belong to the page
	 */
	private static ContentTag otherPageTag;

	private int editMode;

	private boolean copyTags;

	@BeforeClass
	public static void setUp() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Page page = page(556, content(667));
		when(page.getFolder()).thenReturn(mock(Folder.class));

		tag = contentTag(124, null, "dummy124", page.getContent());
		when(tag.isEditable()).thenReturn(true);
		when(tag.isAlohaBlock()).thenReturn(true);

		notEditableTag = contentTag(125, null, "dummy125", page.getContent());
		when(notEditableTag.isEditable()).thenReturn(false);

		otherPageTag = contentTag(126, null, "dummy126", content(668));
		when(otherPageTag.isEditable()).thenReturn(true);

		t.getRenderType().push(page);
	}

	@Before
	public void setReadOnlyWithTagMarkers() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

		editMode = renderType.getEditMode();
		copyTags = prefs.isFeature(Feature.COPY_TAGS);
		renderType.setEditMode(RenderType.EM_ALOHA_READONLY);
		prefs.setFeature(Feature.COPY_TAGS.toString().toLowerCase(), false);
		renderType.setParameter(AlohaRenderer.TAG_MARKERS, true);
	}

	@After
	public void reset() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();

		renderType.setEditMode(editMode);
		t.getNodeConfig().getDefaultPreferences().setFeature(Feature.COPY_TAGS.toString().toLowerCase(), copyTags);
		renderType.unsetParameter(AlohaRenderer.TAG_MARKERS);
	}

	/**
	 * Render the given code as output of the given tag
	 */
	private String block(String code, ContentTag contentTag) throws Exception {
		return new AlohaRenderer().block(code, contentTag, new RenderResult());
	}

	@Test
	public void editableTag() throws Exception {
		assertEquals("<img " + ANNOTATIONS + " src=\"/a.jpg\">", block("<img src=\"/a.jpg\">", tag));
	}

	@Test
	public void editableTagWithTextOnly() throws Exception {
		assertEquals(MARKER_START + "Some text" + MARKER_END, block("Some text", tag));
	}

	@Test
	public void notEditableTag() throws Exception {
		assertEquals("<img src=\"/a.jpg\">", block("<img src=\"/a.jpg\">", notEditableTag));
	}

	@Test
	public void tagOfOtherPage() throws Exception {
		assertEquals("<img src=\"/a.jpg\">", block("<img src=\"/a.jpg\">", otherPageTag));
	}

	@Test
	public void withoutTagMarkers() throws Exception {
		TransactionManager.getCurrentTransaction().getRenderType().unsetParameter(AlohaRenderer.TAG_MARKERS);

		assertEquals("<img src=\"/a.jpg\">", block("<img src=\"/a.jpg\">", tag));
	}
}
