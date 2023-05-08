package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.util.Objects;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.MapPreferences;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Node.UrlRenderWay;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.i18n.LanguageProviderFactory;

import io.reactivex.Flowable;

/**
 * Sandbox test for rendering page URLs in different szenarios
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY }, unset = { Feature.TAG_IMAGE_RESIZER })
public class PageUrlRenderSandboxTest {
	/**
	 * Static preferences used by the context
	 */
	protected static MapPreferences preferences;

	@ClassRule
	public static DBTestContext testContext = new DBTestContext().config(prefs -> {
		preferences = prefs;
	});

	/**
	 * ID of the pageurl construct
	 */
	public final static int PAGEURL_CONSTRUCT_ID = 9;

	private static Node targetNode;

	private static Node sourceNode;

	private static ContentRepository cr;

	private static ContentRepository meshCr;

	private static Template template;

	private static Page crossTargetPage;

	private static Page sameTargetPage;

	private static Page crossSourcePage;

	private static Page sameSourcePage;

	/**
	 * Linkway
	 */
	@Parameter(0)
	public LinkWay linkWay;

	/**
	 * Cross node
	 */
	@Parameter(1)
	public boolean crossNode;

	/**
	 * Source node publish option
	 */
	@Parameter(2)
	public Pair<PublishTarget, Boolean> source;

	@Parameter(3)
	public Node.UrlRenderWay sourceRenderWay;

	/**
	 * Target node publish option
	 */
	@Parameter(4)
	public Pair<PublishTarget, Boolean> target;

	@Parameter(5)
	public Node.UrlRenderWay targetRenderWay;

	/**
	 * Source page
	 */
	protected Page sourcePage;

	/**
	 * Target page
	 */
	protected Page targetPage;

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: linkway {0}, crossnode {1}, source {2}, sourceRenderWay {3}, target {4}, targetRenderWay {5}")
	public static Collection<Object[]> data() {
		Collection<Object[]> testData = new Vector<Object[]>();

		for (LinkWay linkWay : Arrays.asList(LinkWay.AUTO)) {
			for (boolean crossNode : Arrays.asList(false, true)) {
				if (crossNode) {
					for (Pair<PublishTarget, Boolean> source : publishOptions()) {
						for (UrlRenderWay sourceRenderWay : Node.UrlRenderWay.values()) {
							for (Pair<PublishTarget, Boolean> target : publishOptions()) {
								for (UrlRenderWay targetRenderWay : Node.UrlRenderWay.values()) {
									testData.add(new Object[] { linkWay, crossNode, source, sourceRenderWay, target, targetRenderWay });
	}
							}
						}
					}
				} else {
					for (Pair<PublishTarget, Boolean> sourceAndTarget : publishOptions()) {
						for (UrlRenderWay renderWay : Node.UrlRenderWay.values()) {
							testData.add(new Object[] { linkWay, crossNode, sourceAndTarget, renderWay, sourceAndTarget, renderWay });
						}
					}
				}
			}
		}
//		testData.add(new Object[] { LinkWay.AUTO, true, Pair.of(PublishTarget.CONTENTREPOSITORY, false), UrlRenderWay.STATIC_DYNAMIC, Pair.of(PublishTarget.CONTENTREPOSITORY, true), UrlRenderWay.AUTO });
		return testData;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		targetNode = supply(() -> createNode("target", "Target Node", PublishTarget.NONE));
		sourceNode = supply(() -> createNode("source", "Source Node", PublishTarget.NONE));

		cr = supply(() -> create(ContentRepository.class, cr -> {
			cr.setCrType(Type.cr);
			cr.setDbType("hsql");
			cr.setName("CR");
			cr.setUrl("bla");
		}));

		meshCr = supply(() -> create(ContentRepository.class, cr -> {
			cr.setCrType(Type.mesh);
			cr.setName("Mesh CR");
			cr.setUrl("bla");
			cr.setProjectPerNode(true);
		}));

		template = supply(() -> create(Template.class, tmpl -> {
			tmpl.setName("Template linking to a page");
			tmpl.setMlId(1);
			tmpl.setSource("<node pageurl>");
			tmpl.setFolderId(sourceNode.getId());

			tmpl.getTemplateTags().put("pageurl", create(TemplateTag.class, tag -> {
				tag.setConstructId(PAGEURL_CONSTRUCT_ID);
				tag.setEnabled(true);
				tag.setPublic(true);
				tag.setName("pageurl");
			}, false));
		}));

		// create the target pages
		crossTargetPage = supply(() -> createPage("target", targetNode.getFolder(), template, null));
		sameTargetPage = supply(() -> createPage("target", sourceNode.getFolder(), template, null));

		// create the source pages
		crossSourcePage = supply(() -> createPage("target", sourceNode.getFolder(), template, crossTargetPage));
		sameSourcePage = supply(() -> createPage("target", sourceNode.getFolder(), template, sameTargetPage));
	}

	/**
	 * Create a page, possibly linking to another page. The page is published and the transaction is commited (but not closed)
	 * 
	 * @param folder
	 *            folder
	 * @param template
	 *            template
	 * @param linkTarget
	 *            link target (may be null)
	 * @return created page
	 * @throws Exception
	 */
	public static Page createPage(String name, Folder folder, Template template, Page linkTarget) throws NodeException {
		Page p = create(Page.class, page -> {
			page.setName(name);
			page.setFolderId(folder.getId());
			page.setTemplateId(template.getId(), true);

			Tag tag = page.getContent().getTag("pageurl");
			Value value = tag.getValues().iterator().next();
			PartType partType = value.getPartType();
			if (partType instanceof PageURLPartType) {
				((PageURLPartType) partType).setTargetPage(linkTarget);
		} else {
				fail("Part type is of wrong type");
		}
		});

		return update(p, Page::publish);
	}

	public static List<Pair<PublishTarget, Boolean>> publishOptions() {
		return Flowable.fromArray(PublishTarget.values()).flatMap(pt -> {
			if (pt.isPublishCR()) {
				return Flowable.just(Pair.of(pt, true), Pair.of(pt, false));
		} else {
				return Flowable.just(Pair.of(pt, false));
		}
		}).toList().blockingGet();
	}

	public static Node updateNode(Node node, Pair<PublishTarget, Boolean> option, UrlRenderWay urlRenderWay) throws NodeException {
		return update(node, n -> {
			n.setPublishContentmap(option.getLeft().isPublishCR());
			n.setPublishFilesystem(option.getLeft().isPublishFS());
			n.setUrlRenderWayPages(urlRenderWay.getValue());

			if (option.getLeft().isPublishCR()) {
				n.setContentrepositoryId(option.getRight() ? meshCr.getId() : cr.getId());
			} else {
				n.setContentrepositoryId(0);
			}
		});
	}

	@Before
	public void setup() throws Exception {
		LanguageProviderFactory.reset();
		setLinkWay(linkWay);

		if (crossNode) {
			// setup the source and target node
			sourceNode = updateNode(sourceNode, source, sourceRenderWay);
			targetNode = updateNode(targetNode, target, targetRenderWay);

			sourcePage = crossSourcePage;
			targetPage = crossTargetPage;
		} else {
			targetNode = updateNode(targetNode, target, targetRenderWay);

			sourcePage = sameSourcePage;
			targetPage = sameTargetPage;
			}
	}

	/**
	 * Test rendering a page URL
	 * 
	 * @throws NodeException
	 */
	@Test
	public void testRenderPageURL() throws NodeException {
		// render the page in publish mode
		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
			assertThat(sourcePage.render()).as("Rendered page content").isEqualTo(getExpectedPageURL());
			trx.success();
				}
			}

	protected LinkWay getEffectiveLinkWay(Node node) throws NodeException {
		int pageLinkWay = node.getLinkwayPages();

		if (pageLinkWay <= 0) {
			if (node.doPublishContentmap() && !node.doPublishFilesystem()) {
				return LinkWay.PORTAL;
			} else {
				return linkWay;
	}
		} else {
			return LinkWay.get(pageLinkWay);
		}
	}

	/**
	 * Get the expected page URL
	 * 
	 * @return expected page URL
	 */
	protected String getExpectedPageURL() throws NodeException {
		boolean portalUrl = false;
		boolean includeHost = false;
		Node sourceNode = sourcePage.getFolder().getNode();
		Node targetNode = targetPage.getFolder().getNode();

		LinkWay effectiveLinkWay = getEffectiveLinkWay(targetNode);

		switch (effectiveLinkWay) {
		case REL:
		case ABS:
			break;

		case AUTO:
			if (!sourceNode.equals(targetNode)) {
				includeHost = true;
			}
			break;

		case HOST:
			includeHost = true;
			break;

		case PORTAL:
			portalUrl = true;
			break;
		}

		if (portalUrl) {
			if (Objects.areEqual(targetNode.getContentRepository(), meshCr)) {
				if (Objects.areEqual(sourceNode, targetNode) || Objects.areEqual(sourceNode.getContentRepository(), targetNode.getContentRepository())) {
					return String.format("{{mesh.link(%s, en, Target_Node)}}", MeshPublisher.getMeshUuid(targetPage));
				} else {
					return String.format("http://%s%s%s%s", targetNode.getHostname(), targetNode.getPublishDir(), targetPage.getFolder().getPublishDir(), targetPage.getFilename());
				}
			} else {
			if (crossNode && targetNode.isChannel()) {
				return "<plink id=\"10007." + targetPage.getId() + "\" channelid=\"" + targetNode.getId() + "\" />";
			} else {
				return "<plink id=\"10007." + targetPage.getId() + "\" />";
			}
			}
		} else {
			StringBuffer url = new StringBuffer();

			if (includeHost) {
				url.append("http://").append(targetNode.getHostname());
			}
			url.append(targetNode.getPublishDir()).append(targetPage.getFolder().getPublishDir()).append(targetPage.getFilename());
			return url.toString();
		}
	}

	/**
	 * Set a new linkway
	 * 
	 * @param linkWay
	 *            linkway
	 * @throws NodeException
	 */
	public void setLinkWay(LinkWay linkWay) throws NodeException {
		preferences.set("contentnode.linkway", linkWay.toString().toLowerCase());
		NodeConfigRuntimeConfiguration.reset();
		NodeConfigRuntimeConfiguration.getDefault();
	}

	/**
	 * Create the template
	 * 
	 * @param folder
	 *            folder where to create the template
	 * @return template
	 * @throws Exception
	 */
	public Template createTemplate(Folder folder) throws NodeException {
		return create(Template.class, template -> {
		template.setName("Template linking to a page");
		template.setMlId(1);
		template.setSource("<node pageurl>");
			template.setFolderId(folder.getId());

			template.getTemplateTags().put("pageurl", create(TemplateTag.class, tag -> {
		tag.setConstructId(PAGEURL_CONSTRUCT_ID);
		tag.setEnabled(true);
		tag.setPublic(true);
		tag.setName("pageurl");
			}, false));
		});
	}

	/**
	 * Possible linkway settings
	 */
	public static enum LinkWay {
		/**
		 * Absolute URLs
		 */
		ABS(RenderUrl.LINKWAY_ABS),

	/**
		 * Relative URLs
	 */
		REL(RenderUrl.LINK_REL),

		/**
		 * URLs including hostname
		 */
		HOST(RenderUrl.LINKWAY_HOST),

		/**
		 * Portal URLs
		 */
		PORTAL(RenderUrl.LINKWAY_PORTAL),

		/**
		 * Auto detection
		 */
		AUTO(RenderUrl.LINKWAY_AUTO);

		protected int linkWay;

		public static LinkWay get(int linkWay) throws NodeException {
			for (LinkWay lw : values()) {
				if (lw.linkWay == linkWay) {
					return lw;
	}
			}

			throw new NodeException(String.format("Invalid linkWay %d", linkWay));
}

		LinkWay(int linkWay) {
			this.linkWay = linkWay;
		}
	}
}
