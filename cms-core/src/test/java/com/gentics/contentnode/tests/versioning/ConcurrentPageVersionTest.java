package com.gentics.contentnode.tests.versioning;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.assertj.core.api.SoftAssertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.PageFactory;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.request.ContentTagCreateRequest;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;
import com.gentics.contentnode.rest.model.response.TagCreateResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Reproducer for SUP-20172: concurrent version creating requests (page/save with createVersion and page/publish)
 * for the same page by the same user. Mirrors the browser console reproducer:
 * <ol>
 * <li>page/newtag creates a new tag (committed with empty values)</li>
 * <li>page/save (filling the tag, createVersion=true, unlock=true) and page/publish are sent in parallel</li>
 * <li>the published version is rendered and must contain the value that was saved, unless the page was published before it was saved
 * (then the latest version must be a newer, unpublished version containing the value)</li>
 * <li>the published version must not render differently after clearing the object cache</li>
 * <li>finally the page versions must have unique numbers and timestamps</li>
 * </ol>
 */
public class ConcurrentPageVersionTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Number of iterations
	 */
	private final static int ITERATIONS = 20;

	/**
	 * Pause between iterations (the JS reproducer waits 300 ms)
	 */
	private final static long PAUSE_MS = 300;

	private static Node node;

	private static Template template;

	private static Integer constructId;

	private static SystemUser user;

	private static ExecutorService executor;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = Trx.supply(() -> createNode());
		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));
		constructId = Trx.supply(() -> createConstruct(node, ShortTextPartType.class, "link", "url"));
		user = Trx.supply(t -> t.getObject(SystemUser.class, 1));

		executor = Executors.newFixedThreadPool(2);
	}

	@AfterClass
	public static void tearDownOnce() {
		if (executor != null) {
			executor.shutdownNow();
		}
	}

	/**
	 * Run save (with createVersion) and publish concurrently and check the published version and the version numbers
	 * @throws Exception
	 */
	@Test
	public void testConcurrentSaveAndPublish() throws Exception {
		Trx.operate(() -> {
			System.out.println("DB isolation level: " + getIsolationLevel());
		});

		Page page = Trx.supply(user, () -> createPage(node.getFolder(), template, "Test"));
		String pageId = Integer.toString(page.getId());

		// publish once, so that the page has a published version
		publish(pageId);

		// lock the page (like it was opened in the editor)
		Trx.operate(user, t -> {
			t.getObject(Page.class, page.getId(), true);
		});

		Map<String, String> brokenTags = new LinkedHashMap<>();
		// tags, which rendered differently before and after clearing the object cache (tag name -> "before -> after")
		Map<String, String> cacheDependentTags = new LinkedHashMap<>();

		for (int i = 0; i < ITERATIONS; i++) {
			// 1. create a new tag (committed with empty values)
			Tag tag = newTag(pageId);

			// 2. fill the tag and run save and publish in parallel
			String url = "https://example.com/ui-repro-" + i;
			tag.getProperties().get("url").setStringValue(url);

			com.gentics.contentnode.rest.model.Page restPage = new com.gentics.contentnode.rest.model.Page();
			restPage.setId(page.getId());
			restPage.setTags(Collections.singletonMap(tag.getName(), tag));
			PageSaveRequest saveRequest = new PageSaveRequest(restPage);
			saveRequest.setUnlock(true);
			saveRequest.setCreateVersion(true);

			CyclicBarrier barrier = new CyclicBarrier(2);
			Future<GenericResponse> saveFuture = executor.submit(concurrently(barrier, () -> {
				try (Trx trx = new Trx(user)) {
					GenericResponse response = new PageResourceImpl().save(pageId, saveRequest);
					trx.success();
					return response;
				}
			}));
			Future<GenericResponse> publishFuture = executor.submit(concurrently(barrier, () -> {
				try (Trx trx = new Trx(user)) {
					PagePublishRequest request = new PagePublishRequest();
					request.setAlllang(false);
					request.setAt(0);
					GenericResponse response = new PageResourceImpl().publish(pageId, null, request);
					trx.success();
					return response;
				}
			}));

			assertResponseOK(saveFuture.get(1, TimeUnit.MINUTES));
			assertResponseOK(publishFuture.get(1, TimeUnit.MINUTES));

			// 3. render the published version of the tag (like the JS reproducer)
			String rendered = renderPublished(pageId, tag.getName());
			// inspect the published version, as it is currently seen (possibly from the object cache)
			String inspected = inspectPublishedVersion(page.getId(), tag.getName());

			// clear the object cache and render/inspect again, to see whether stale cached objects are involved
			Trx.operate(t -> t.clearNodeObjectCache());
			String renderedAfterClear = renderPublished(pageId, tag.getName());
			String inspectedAfterClear = inspectPublishedVersion(page.getId(), tag.getName());

			// if publish was done before save, the published version does not contain the value, but the latest (unpublished) version must
			boolean publishedBeforeSave = !url.equals(rendered) && latestVersionContains(page.getId(), tag.getName(), url);
			boolean broken = !url.equals(rendered) && !publishedBeforeSave;
			if (broken) {
				brokenTags.put(tag.getName(), rendered);
			}
			if (!Objects.equals(rendered, renderedAfterClear)) {
				cacheDependentTags.put(tag.getName(), String.format("'%s' -> '%s'", rendered, renderedAfterClear));
			}
			System.out.println(String.format("[%d] %s %s", i, broken ? "BROKEN" : (publishedBeforeSave ? "ok (published before save)" : "ok"), tag.getName()));
			// details only for problems
			if (broken || !Objects.equals(rendered, renderedAfterClear)) {
				System.out.println(String.format("  working copy url                = '%s'", url));
				System.out.println(String.format("  rendered (publish)              = '%s'", rendered));
				System.out.println(String.format("  published version               : %s", inspected));
				System.out.println(String.format("  rendered (publish, cache clear) = '%s'", renderedAfterClear));
				System.out.println(String.format("  published version (cache clear) : %s", inspectedAfterClear));
				dumpVersionedValues(tag.getId());
			}

			Thread.sleep(PAUSE_MS);
		}

		publish(pageId);

		// 4. check the page versions
		List<NodeObjectVersion> versions = Trx.supply(() -> {
			List<NodeObjectVersion> list = new ArrayList<>();
			Collections.addAll(list, PageFactory.loadPageVersions(page.getId()));
			return list;
		});
		List<String> duplicateNumbers = duplicates(versions.stream().map(NodeObjectVersion::getNumber).collect(Collectors.toList()));
		List<Integer> duplicateTimestamps = duplicates(
				versions.stream().map(v -> v.getDate().getIntTimestamp()).collect(Collectors.toList()));
		if (!duplicateNumbers.isEmpty() || !duplicateTimestamps.isEmpty()) {
			dumpNodeversions(page.getId());
		}

		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(brokenTags).as("Tags rendering wrong in published version (tag name -> rendered content)").isEmpty();
		softly.assertThat(cacheDependentTags).as("Tags rendering differently after clearing the object cache (tag name -> before -> after)").isEmpty();
		softly.assertThat(duplicateNumbers).as("Duplicate page version numbers").isEmpty();
		softly.assertThat(duplicateTimestamps).as("Duplicate page version timestamps").isEmpty();
		softly.assertAll();
	}

	/**
	 * Test creating a page version in a transaction, whose snapshot does not contain the page version, which was created (and committed)
	 * by another transaction in the meantime (with isolation level REPEATABLE READ, the snapshot of the transaction is stale).
	 * Unlike {@link #testConcurrentSaveAndPublish()}, this does not depend on timing.
	 * @throws Exception
	 */
	@Test
	public void testCreateVersionWithStaleSnapshot() throws Exception {
		int base = (int) (System.currentTimeMillis() / 1000) + 3600;
		Page page = Trx.supply(user, () -> createPage(node.getFolder(), template, "Stale"));
		List<String> tagNames = Trx.supply(user, t -> {
			Page editablePage = t.getObject(page, true);
			ContentTag first = editablePage.getContent().addContentTag(constructId);
			first.setEnabled(true);
			ContentTag second = editablePage.getContent().addContentTag(constructId);
			second.setEnabled(true);
			editablePage.save();
			return Arrays.asList(first.getName(), second.getName());
		});
		String thisTag = tagNames.get(0);
		String otherTag = tagNames.get(1);

		try (Trx trx = new Trx(user)) {
			Transaction t = trx.getTransaction();
			t.setTimestamp((base + 30) * 1000L);

			// establish the snapshot of this transaction (with a consistent read of an InnoDB table)
			System.out.println("DB isolation level: " + getIsolationLevel());
			int versionCount = DBUtils.select("SELECT COUNT(*) c FROM nodeversion WHERE o_type = ? AND o_id = ?", ps -> {
				ps.setInt(1, Page.TYPE_PAGE);
				ps.setInt(2, page.getId());
			}, rs -> rs.next() ? rs.getInt("c") : 0);
			System.out.println("Page versions seen by this transaction before the other transaction: " + versionCount);

			// another transaction changes the other tag and creates a version (and is committed)
			executor.submit(() -> {
				Trx.operate(user, t2 -> {
					t2.setTimestamp((base + 20) * 1000L);
					Page editablePage = t2.getObject(page, true);
					getPartType(ShortTextPartType.class, editablePage.getContentTag(otherTag), "url").setText("other");
					editablePage.save();
				});
				return null;
			}).get(1, TimeUnit.MINUTES);

			// this transaction changes this tag and creates a version
			Page editablePage = t.getObject(page, true);
			getPartType(ShortTextPartType.class, editablePage.getContentTag(thisTag), "url").setText("this");
			editablePage.save();
			trx.success();
		}

		dumpNodeversions(page.getId());

		Trx.operate(user, t -> {
			Page current = t.getObject(Page.class, page.getId());
			NodeObjectVersion[] versions = current.getVersions();
			List<String> numbers = Arrays.stream(versions).map(NodeObjectVersion::getNumber).collect(Collectors.toList());
			assertThat(duplicates(numbers)).as("Duplicate page version numbers").isEmpty();
			assertThat(versions[0].getDate().getIntTimestamp()).as("Timestamp of latest version").isEqualTo(base + 30);

			Page latest = t.getObject(Page.class, page.getId(), versions[0].getDate().getIntTimestamp());
			assertThat(getUrl(latest, thisTag)).as("Value of this tag in the latest version").isEqualTo("this");
			for (String tagName : tagNames) {
				assertThat(getUrl(latest, tagName)).as("Value of tag " + tagName + " in the latest version").isEqualTo(getUrl(current, tagName));
			}
		});
	}

	/**
	 * Get the url value of the tag in the page
	 * @param page page
	 * @param tagName tag name
	 * @return url value (null if tag or value not found)
	 * @throws NodeException
	 */
	protected String getUrl(Page page, String tagName) throws NodeException {
		ContentTag tag = page.getContentTag(tagName);
		Value value = tag != null ? tag.getValues().getByKeyname("url") : null;
		return value != null ? value.getValueText() : null;
	}

	/**
	 * Wrap the callable, so that it waits at the barrier before being executed
	 * @param barrier barrier
	 * @param callable wrapped callable
	 * @return callable
	 */
	protected <T> Callable<T> concurrently(CyclicBarrier barrier, Callable<T> callable) {
		return () -> {
			barrier.await(1, TimeUnit.MINUTES);
			return callable.call();
		};
	}

	/**
	 * Create a new tag in the page (like page/newtag)
	 * @param pageId page ID
	 * @return created tag
	 * @throws NodeException
	 */
	protected Tag newTag(String pageId) throws NodeException {
		try (Trx trx = new Trx(user)) {
			ContentTagCreateRequest request = new ContentTagCreateRequest();
			request.setConstructId(constructId);
			TagCreateResponse response = new PageResourceImpl().createTag(pageId, null, null, request);
			assertResponseOK(response);
			trx.success();
			return response.getTag();
		}
	}

	/**
	 * Publish the page
	 * @param pageId page ID
	 * @throws NodeException
	 */
	protected void publish(String pageId) throws NodeException {
		try (Trx trx = new Trx(user)) {
			PagePublishRequest request = new PagePublishRequest();
			request.setAlllang(false);
			request.setAt(0);
			assertResponseOK(new PageResourceImpl().publish(pageId, null, request));
			trx.success();
		}
	}

	/**
	 * Render the tag in the published version of the page (like page/render?publish=true)
	 * @param pageId page ID
	 * @param tagName tag name
	 * @return rendered content
	 * @throws NodeException
	 */
	protected String renderPublished(String pageId, String tagName) throws NodeException {
		try (Trx trx = new Trx(user)) {
			PageRenderResponse response = new PageResourceImpl().render(pageId, null, "<node " + tagName + ">", false, null, LinksType.backend, false,
					false, true, null);
			assertResponseOK(response);
			trx.success();
			return response.getContent();
		}
	}

	/**
	 * Inspect the published version of the page: get the versioned page object for the published version
	 * and check whether it contains the tag and which value the tag has (for diagnostics)
	 * @param pageId page ID
	 * @param tagName tag name
	 * @return description of the published version
	 * @throws NodeException
	 */
	protected String inspectPublishedVersion(int pageId, String tagName) throws NodeException {
		return Trx.supply(user, t -> {
			Page current = t.getObject(Page.class, pageId);
			NodeObjectVersion published = current.getPublishedVersion();
			if (published == null) {
				return "no published version";
			}
			int timestamp = published.getDate().getIntTimestamp();
			String versionInfo = String.format("%s@%d", published.getNumber(), timestamp);

			Page versioned = t.getObject(Page.class, pageId, timestamp);
			if (versioned == null) {
				return versionInfo + ", versioned page NOT found";
			}

			// identities of the versioned objects (same identity = same (cached) instance) and number of tags of the versioned
			// content compared to the number of tags, which would be loaded from the DB now (same query as FactoryContent.loadContentTagIds())
			Content versionedContent = versioned.getContent();
			int cachedTagCount = versionedContent.getContentTags().size();
			int dbTagCount = DBUtils.select(
					"SELECT COUNT(DISTINCT id) c FROM contenttag_nodeversion WHERE content_id = ? AND (nodeversionremoved > ? OR nodeversionremoved = 0) AND nodeversiontimestamp <= ?",
					ps -> {
						ps.setInt(1, versionedContent.getId());
						ps.setInt(2, timestamp);
						ps.setInt(3, timestamp);
					}, rs -> rs.next() ? rs.getInt("c") : -1);
			versionInfo = String.format("%s [Page@T #%08x, Content@T #%08x, tags in Content@T = %d, tags in DB at T = %d]", versionInfo,
					System.identityHashCode(versioned), System.identityHashCode(versionedContent), cachedTagCount, dbTagCount);

			ContentTag versionedTag = versioned.getContentTag(tagName);
			if (versionedTag == null) {
				return versionInfo + ", tag NOT contained in version";
			}
			Value value = versionedTag.getValues().getByKeyname("url");
			return String.format("%s, tag contained in version (enabled = %b), url value = '%s'", versionInfo, versionedTag.isEnabled(),
					value != null ? value.getValueText() : "<no value>");
		});
	}

	/**
	 * Check whether the latest version of the page is not the published version and contains the tag with the given url
	 * @param pageId page ID
	 * @param tagName tag name
	 * @param url expected url
	 * @return true iff the latest version is unpublished and contains the url
	 * @throws NodeException
	 */
	protected boolean latestVersionContains(int pageId, String tagName, String url) throws NodeException {
		return Trx.supply(user, t -> {
			NodeObjectVersion[] versions = t.getObject(Page.class, pageId).getVersions();
			if (versions.length == 0 || versions[0].isPublished()) {
				return false;
			}
			Page latest = t.getObject(Page.class, pageId, versions[0].getDate().getIntTimestamp());
			ContentTag tag = latest != null ? latest.getContentTag(tagName) : null;
			Value value = tag != null ? tag.getValues().getByKeyname("url") : null;
			return value != null && url.equals(value.getValueText());
		});
	}

	/**
	 * Get the duplicate entries of the list
	 * @param list list
	 * @return list of entries, that occur more than once
	 */
	protected <T> List<T> duplicates(List<T> list) {
		return list.stream().filter(e -> Collections.frequency(list, e) > 1).distinct().collect(Collectors.toList());
	}

	/**
	 * Get the transaction isolation level of the DB connection (for diagnostics)
	 * @return isolation level
	 */
	protected static String getIsolationLevel() {
		for (String variable : List.of("@@SESSION.transaction_isolation", "@@SESSION.tx_isolation")) {
			try {
				return DBUtils.select("SELECT " + variable + " level", rs -> rs.next() ? rs.getString("level") : null);
			} catch (NodeException e) {
				// try next variable
			}
		}
		return "unknown";
	}

	/**
	 * Print the nodeversion entries of the page (for diagnostics)
	 * @param pageId page ID
	 * @throws NodeException
	 */
	protected void dumpNodeversions(int pageId) throws NodeException {
		Trx.operate(() -> {
			System.out.println("nodeversion entries of page " + pageId + " (id | timestamp | nodeversion | published):");
			DBUtils.select("SELECT id, timestamp, nodeversion, published FROM nodeversion WHERE o_type = ? AND o_id = ? ORDER BY timestamp, id", ps -> {
				ps.setInt(1, Page.TYPE_PAGE);
				ps.setInt(2, pageId);
			}, rs -> {
				while (rs.next()) {
					System.out.println(String.format("  %d | %d | %s | %b", rs.getInt("id"), rs.getInt("timestamp"), rs.getString("nodeversion"),
							rs.getBoolean("published")));
				}
				return null;
			});
		});
	}

	/**
	 * Print the versioned values of the tag (for diagnostics)
	 * @param tagId tag ID
	 * @throws NodeException
	 */
	protected void dumpVersionedValues(int tagId) throws NodeException {
		Trx.operate(() -> {
			System.out.println("  value_nodeversion of contenttag " + tagId + " (auto_id | id | value_text | timestamp | latest | removed):");
			DBUtils.select(
					"SELECT auto_id, id, value_text, nodeversiontimestamp, nodeversionlatest, nodeversionremoved FROM value_nodeversion WHERE contenttag_id = ? ORDER BY auto_id",
					ps -> ps.setInt(1, tagId), rs -> {
						while (rs.next()) {
							System.out.println(String.format("    %d | %d | '%s' | %d | %d | %d", rs.getInt("auto_id"), rs.getInt("id"),
									rs.getString("value_text"), rs.getInt("nodeversiontimestamp"), rs.getInt("nodeversionlatest"),
									rs.getInt("nodeversionremoved")));
						}
						return null;
					});
		});
	}
}
