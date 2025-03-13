package com.gentics.contentnode.tests.migration;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSession;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.api.contentnode.migration.IMigrationPreprocessor;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.migration.MigrationDBLogger;
import com.gentics.contentnode.migration.jobs.AbstractMigrationJob;
import com.gentics.contentnode.migration.jobs.TagTypeMigrationJob;
import com.gentics.contentnode.migration.jobs.TemplateMigrationJob;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.object.parttype.NormalTextPartType;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.migration.MigrationPartMapping;
import com.gentics.contentnode.rest.model.migration.MigrationPreProcessor;
import com.gentics.contentnode.rest.model.migration.TagTypeMigrationMapping;
import com.gentics.contentnode.rest.model.migration.TemplateMigrationEditableTagMapping;
import com.gentics.contentnode.rest.model.migration.TemplateMigrationMapping;
import com.gentics.contentnode.rest.model.request.migration.TagTypeMigrationRequest;
import com.gentics.contentnode.rest.model.request.migration.TemplateMigrationRequest;
import com.gentics.contentnode.rest.model.response.migration.MigrationJobEntry;
import com.gentics.contentnode.rest.model.response.migration.MigrationJobLogEntryItem;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for Tagtype migrations with Preprocessors
 */
@Ignore("Ignored since the feature is discontinued")
public class TTMPreprocessorTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Node to test in
	 */
	private static Node node;

	/**
	 * Construct to migrate from
	 */
	private static Construct fromConstruct;

	/**
	 * Construct to migrate to
	 */
	private static Construct toConstruct;

	/**
	 * Dummy template
	 */
	private static Template template;

	/**
	 * Template to migrate from
	 */
	private static Template fromTemplate;

	/**
	 * Template to migrate to
	 */
	private static Template toTemplate;

	/**
	 * Setup basic test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		node = Trx.supply(() -> createNode());
		fromConstruct = Trx.supply(() -> create(Construct.class, c -> {
			c.setKeyword("from");

			c.getParts().add(create(Part.class, p -> {
				p.setKeyname("text");
				p.setPartTypeId(getPartTypeId(NormalTextPartType.class));
				p.setEditable(1);
			}, false));
		}));

		toConstruct = Trx.supply(() -> create(Construct.class, c -> {
			c.setKeyword("to");

			c.getParts().add(create(Part.class, p -> {
				p.setKeyname("text");
				p.setPartTypeId(getPartTypeId(HTMLPartType.class));
				p.setEditable(1);
			}, false));
		}));

		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));

		fromTemplate = create(Template.class, t -> {
			t.setSource("");
			t.setName("From Template");
			t.addFolder(node.getFolder());

			for (String tagName : Arrays.asList("tag1", "tag2")) {
				t.getTags().put(tagName, create(TemplateTag.class, tag -> {
					tag.setConstructId(fromConstruct.getId());
					tag.setEnabled(true);
					tag.setPublic(true);
					tag.setName(tagName);
				}, false));
			}
		});

		toTemplate = create(Template.class, t -> {
			t.setSource("");
			t.setName("To Template");
			t.addFolder(node.getFolder());

			for (String tagName : Arrays.asList("tag1", "tag2")) {
				t.getTags().put(tagName, create(TemplateTag.class, tag -> {
					tag.setConstructId(toConstruct.getId());
					tag.setEnabled(true);
					tag.setPublic(true);
					tag.setName(tagName);
				}, false));
			}
		});
	}

	/**
	 * Reset the preprocessors
	 */
	@Before
	public void setup() {
		BrokenPreprocessor.reset();
		SkippingPreprocessor.reset();
		ModifyingPreprocessor.reset();
	}

	/**
	 * Test Preprocessor that returns null
	 * @throws NodeException
	 */
	@Test
	public void testReturnNull() throws NodeException {
		BrokenPreprocessor.nullFor("tag1");
		testBrokenPreprocessor();
	}

	/**
	 * Test Preprocessor that throws an exception
	 * @throws NodeException
	 */
	@Test
	public void testThrowException() throws NodeException {
		BrokenPreprocessor.exceptionFor("tag1");
		testBrokenPreprocessor();
	}

	/**
	 * Test Preprocessor that throws a runtime exception
	 * @throws NodeException
	 */
	@Test
	public void testThrowRuntimeException() throws NodeException {
		BrokenPreprocessor.runtimeExceptionFor("tag1");
		testBrokenPreprocessor();
	}

	/**
	 * Test Preprocessor that skips a tag
	 * @throws NodeException
	 */
	@Test
	public void testSkipTag() throws NodeException {
		SkippingPreprocessor.skipTags("tag2");

		Page page = testSkippingPreprocessor(AbstractMigrationJob.STATUS_COMPLETED, AbstractMigrationJob.STATUS_COMPLETED);
		Trx.operate(() -> {
			for (ContentTag tag : page.getContent().getContentTags().values()) {
				if ("tag2".equals(tag.getName())) {
					assertThat(tag).as("Tag " + tag.getName()).hasConstruct(fromConstruct.getGlobalId());
				} else {
					assertThat(tag).as("Tag " + tag.getName()).hasConstruct(toConstruct.getGlobalId());
				}
			}
		});
	}

	/**
	 * Test Preprocessor that skips an object
	 * @throws NodeException
	 */
	@Test
	public void testSkipObject() throws NodeException {
		SkippingPreprocessor.skipObject("tag2");

		Page page = testSkippingPreprocessor(AbstractMigrationJob.STATUS_COMPLETED_WITH_WARNINGS, AbstractMigrationJob.STATUS_SKIPPED);
		Trx.operate(() -> {
			for (ContentTag tag : page.getContent().getContentTags().values()) {
				assertThat(tag).as("Tag " + tag.getName()).hasConstruct(fromConstruct.getGlobalId());
			}
		});
	}

	/**
	 * Test Preprocessor that modifies a tag
	 * @throws NodeException
	 */
	@Test
	public void testModifyTag() throws NodeException {
		ModifyingPreprocessor.modify("tag2", tag -> {
			tag.getProperties().get("text").setStringValue("Modified by the Preprocessor");
		});

		Page page = Trx.supply(() -> createPage(node.getFolder(), template, "Page"));
		Trx.operate(() -> {
			update(page, p -> {
				p.getContent().getContentTags().put("tag1", create(ContentTag.class, t -> {
					t.setConstructId(fromConstruct.getId());
					t.setEnabled(true);
					t.setName("tag1");
				}, false));
				p.getContent().getContentTags().put("tag2", create(ContentTag.class, t -> {
					t.setConstructId(fromConstruct.getId());
					t.setEnabled(true);
					t.setName("tag2");
				}, false));
				p.getContent().getContentTags().put("tag3", create(ContentTag.class, t -> {
					t.setConstructId(fromConstruct.getId());
					t.setEnabled(true);
					t.setName("tag3");
				}, false));
			});
		});

		migrate(request(Page.class, Arrays.asList(page), Arrays.asList(ModifyingPreprocessor.class)));

		Trx.operate(() -> {
			for (ContentTag tag : page.getContent().getContentTags().values()) {
				assertThat(tag).as("Tag " + tag.getName()).hasConstruct(toConstruct.getGlobalId());
				if ("tag2".equals(tag.getName())) {
					assertThat(tag.getValues().getByKeyname("text")).as("Value [text]").hasText("Modified by the Preprocessor");
				} else {
					assertThat(tag.getValues().getByKeyname("text")).as("Value [text]").hasText("");
				}
			}
		});
	}

	/**
	 * Test that preprocessors are applied in the correct order
	 * @throws NodeException
	 */
	@Test
	public void testOrder() throws NodeException {
		ModifyingPreprocessor.modify("tag", tag -> {
			Property prop = tag.getProperties().get("text");
			prop.setStringValue(prop.getStringValue() + "-one");
		});
		ModifyingPreprocessor2.modify("tag", tag -> {
			Property prop = tag.getProperties().get("text");
			prop.setStringValue(prop.getStringValue() + "-two");
		});
		Page page1 = Trx.supply(() -> createPage(node.getFolder(), template, "Page1"));
		Page page2 = Trx.supply(() -> createPage(node.getFolder(), template, "Page2"));
		Page page3 = Trx.supply(() -> createPage(node.getFolder(), template, "Page3"));
		Page page4 = Trx.supply(() -> createPage(node.getFolder(), template, "Page4"));

		for (Page page : Arrays.asList(page1, page2, page3, page4)) {
			Trx.operate(() -> {
				update(page, p -> {
					p.getContent().getContentTags().put("tag", create(ContentTag.class, t -> {
						t.setConstructId(fromConstruct.getId());
						t.setEnabled(true);
						t.setName("tag");
						t.getValues().getByKeyname("text").setValueText("content");
					}, false));
				});
			});
		}

		migrate(request(Page.class, Arrays.asList(page1, page2), Arrays.asList(ModifyingPreprocessor.class, ModifyingPreprocessor2.class)));

		Trx.operate(() -> {
			for (Page page : Arrays.asList(page1, page2)) {
				assertThat(page.getContent().getContentTag("tag").getValues().getByKeyname("text")).as("Migrated value").hasText("content-one-two");
			}
		});

		migrate(request(Page.class, Arrays.asList(page3, page4), Arrays.asList(ModifyingPreprocessor2.class, ModifyingPreprocessor.class)));

		Trx.operate(() -> {
			for (Page page : Arrays.asList(page3, page4)) {
				assertThat(page.getContent().getContentTag("tag").getValues().getByKeyname("text")).as("Migrated value").hasText("content-two-one");
			}
		});
	}

	/**
	 * Test template migration with a modifying preprocessor
	 * @throws NodeException
	 */
	@Test
	public void testModifyingTemplateMigration() throws NodeException {
		ModifyingPreprocessor.modify("tag1", tag -> {
			tag.getProperties().get("text").setStringValue("Modified by the Preprocessor");
		});
		Page page1 = Trx.supply(() -> createPage(node.getFolder(), fromTemplate, "Page1"));
		Page page2 = Trx.supply(() -> createPage(node.getFolder(), fromTemplate, "Page2"));

		int jobId = migrate(request(fromTemplate, toTemplate, Arrays.asList("tag1"), Arrays.asList(ModifyingPreprocessor.class)));
		int page1Id = page1.getId();
		int page2Id = page2.getId();

		Trx.operate(() -> {
			MigrationDBLogger dbLogger = new MigrationDBLogger(MigrationDBLogger.DEFAULT_LOGGER);
			MigrationJobEntry jobEntry = dbLogger.getMigrationJobEntry(jobId);
			String log = null;
			try {
				log = FileUtils.readFileToString(dbLogger.getLogFileForJob(jobId), "UTF-8");
			} catch (IOException e) {
				throw new NodeException("Error while reading log file for migration job", e);
			}
			assertThat(jobEntry).as(String.format("Migration job (log: %s)", log)).isNotNull().hasStatus(AbstractMigrationJob.STATUS_COMPLETED);

			List<MigrationJobLogEntryItem> itemEntries = dbLogger.getMigrationJobItemEntries(jobId);
			assertThat(itemEntries).as("Migration items").usingFieldByFieldElementComparator()
					.containsOnly(
							new MigrationJobLogEntryItem(jobId, page1Id, Page.TYPE_PAGE, AbstractMigrationJob.STATUS_COMPLETED),
							new MigrationJobLogEntryItem(jobId, page2Id, Page.TYPE_PAGE, AbstractMigrationJob.STATUS_COMPLETED));
		});

		for (Page page : Arrays.asList(page1, page2)) {
			Trx.operate(() -> {
				assertThat(page.getContent().getContentTag("tag1").getValues().getByKeyname("text")).as("Migrated value").hasText("Modified by the Preprocessor");
			});
		}

		page1 = Trx.execute(p -> TransactionManager.getCurrentTransaction().getObject(p), page1);
		page2 = Trx.execute(p -> TransactionManager.getCurrentTransaction().getObject(p), page2);

		for (Page page : Arrays.asList(page1, page2)) {
			Trx.operate(() -> {
				assertThat(page.getTemplate()).as("Template of " + page).isEqualTo(toTemplate);
			});
		}

	}

	/**
	 * Test template migration with an object skipping preprocessor
	 * @throws NodeException
	 */
	@Test
	public void testObjectSkippingTemplateMigration() throws NodeException {
		SkippingPreprocessor.skipObject("tag1");

		Page page1 = Trx.supply(() -> createPage(node.getFolder(), fromTemplate, "Page1"));
		Page page2 = Trx.supply(() -> createPage(node.getFolder(), fromTemplate, "Page2"));

		int jobId = migrate(request(fromTemplate, toTemplate, Arrays.asList("tag1"), Arrays.asList(SkippingPreprocessor.class)));
		int page1Id = page1.getId();
		int page2Id = page2.getId();

		Trx.operate(() -> {
			MigrationDBLogger dbLogger = new MigrationDBLogger(MigrationDBLogger.DEFAULT_LOGGER);
			MigrationJobEntry jobEntry = dbLogger.getMigrationJobEntry(jobId);
			assertThat(jobEntry).as("Migration job").isNotNull().hasStatus(AbstractMigrationJob.STATUS_COMPLETED_WITH_WARNINGS);

			List<MigrationJobLogEntryItem> itemEntries = dbLogger.getMigrationJobItemEntries(jobId);
			assertThat(itemEntries).as("Migration items").usingFieldByFieldElementComparator()
					.containsOnly(
							new MigrationJobLogEntryItem(jobId, page1Id, Page.TYPE_PAGE, AbstractMigrationJob.STATUS_SKIPPED),
							new MigrationJobLogEntryItem(jobId, page2Id, Page.TYPE_PAGE, AbstractMigrationJob.STATUS_SKIPPED));
		});

		page1 = Trx.execute(p -> TransactionManager.getCurrentTransaction().getObject(p), page1);
		page2 = Trx.execute(p -> TransactionManager.getCurrentTransaction().getObject(p), page2);

		for (Page page : Arrays.asList(page1, page2)) {
			Trx.operate(() -> {
				assertThat(page.getTemplate()).as("Template of " + page).isEqualTo(fromTemplate);
			});
		}
	}

	/**
	 * Test template migration with a tag skipping preprocessor
	 * @throws NodeException
	 */
	@Test
	public void testTagSkippingTemplateMigration() throws NodeException {
		for (String tagName : Arrays.asList("tag1", "tag2")) {
			Page page1 = Trx.supply(() -> createPage(node.getFolder(), fromTemplate, "Page1"));
			Page page2 = Trx.supply(() -> createPage(node.getFolder(), fromTemplate, "Page2"));
			int page1Id = page1.getId();
			int page2Id = page2.getId();

			SkippingPreprocessor.reset();
			SkippingPreprocessor.skipTags(tagName);
			int jobId = migrate(request(fromTemplate, toTemplate, Arrays.asList("tag1", "tag2"), Arrays.asList(SkippingPreprocessor.class)));

			Trx.consume(id -> {
				MigrationDBLogger dbLogger = new MigrationDBLogger(MigrationDBLogger.DEFAULT_LOGGER);
				MigrationJobEntry jobEntry = dbLogger.getMigrationJobEntry(id);
				assertThat(jobEntry).as("Migration job").isNotNull().hasStatus(AbstractMigrationJob.STATUS_COMPLETED);

				List<MigrationJobLogEntryItem> itemEntries = dbLogger.getMigrationJobItemEntries(id);
				assertThat(itemEntries).as("Migration items").usingFieldByFieldElementComparator().containsOnly(
						new MigrationJobLogEntryItem(id, page1Id, Page.TYPE_PAGE, AbstractMigrationJob.STATUS_COMPLETED),
						new MigrationJobLogEntryItem(id, page2Id, Page.TYPE_PAGE, AbstractMigrationJob.STATUS_COMPLETED));
			}, jobId);

			page1 = Trx.execute(p -> TransactionManager.getCurrentTransaction().getObject(p), page1);
			page2 = Trx.execute(p -> TransactionManager.getCurrentTransaction().getObject(p), page2);

			for (Page page : Arrays.asList(page1, page2)) {
				Trx.operate(() -> {
					assertThat(page.getTemplate()).as("Template of " + page).isEqualTo(toTemplate);
					for (String tag : page.getContent().getContentTags().keySet()) {
						if (tag.equals(tagName)) {
							assertThat(page.getContentTag(tag)).as("Skipped Tag").hasConstruct(fromConstruct.getGlobalId());
						} else {
							assertThat(page.getContentTag(tag)).as("Migrated Tag").hasConstruct(toConstruct.getGlobalId());
						}
					}
				});
			}
		}
	}

	/**
	 * Test migration with a skipping preprocessor
	 * @param expectedJobStatus expected job status
	 * @param expectedPageStatus expected page status
	 * @return page
	 * @throws NodeException
	 */
	protected Page testSkippingPreprocessor(int expectedJobStatus, int expectedPageStatus) throws NodeException {
		Page page = Trx.supply(() -> createPage(node.getFolder(), template, "Page"));
		Trx.operate(() -> {
			update(page, p -> {
				p.getContent().getContentTags().put("tag1", create(ContentTag.class, t -> {
					t.setConstructId(fromConstruct.getId());
					t.setEnabled(true);
					t.setName("tag1");
				}, false));
				p.getContent().getContentTags().put("tag2", create(ContentTag.class, t -> {
					t.setConstructId(fromConstruct.getId());
					t.setEnabled(true);
					t.setName("tag2");
				}, false));
				p.getContent().getContentTags().put("tag3", create(ContentTag.class, t -> {
					t.setConstructId(fromConstruct.getId());
					t.setEnabled(true);
					t.setName("tag3");
				}, false));
			});
		});

		int jobId = migrate(request(Page.class, Arrays.asList(page), Arrays.asList(SkippingPreprocessor.class)));

		Trx.operate(() -> {
			MigrationDBLogger dbLogger = new MigrationDBLogger(MigrationDBLogger.DEFAULT_LOGGER);
			MigrationJobEntry jobEntry = dbLogger.getMigrationJobEntry(jobId);
			assertThat(jobEntry).as("Migration job").isNotNull().hasStatus(expectedJobStatus);
			List<MigrationJobLogEntryItem> itemEntries = dbLogger.getMigrationJobItemEntries(jobId);
			assertThat(itemEntries).as("List of item entries").hasSize(1);
			assertThat(itemEntries.get(0)).as("Item entry").isFor(page).hasStatus(expectedPageStatus);
		});
		return page;
	}

	/**
	 * Test migration with a broken preprocessor
	 * @throws NodeException
	 */
	protected void testBrokenPreprocessor() throws NodeException {
		Page page = Trx.supply(() -> createPage(node.getFolder(), template, "Page"));
		Trx.operate(() -> {
			update(page, p -> {
				p.getContent().getContentTags().put("tag1", create(ContentTag.class, t -> {
					t.setConstructId(fromConstruct.getId());
					t.setEnabled(true);
					t.setName("tag1");
				}, false));
			});
		});

		int jobId = migrate(request(Page.class, Arrays.asList(page), Arrays.asList(BrokenPreprocessor.class)));

		Trx.operate(() -> {
			MigrationDBLogger dbLogger = new MigrationDBLogger(MigrationDBLogger.DEFAULT_LOGGER);
			MigrationJobEntry jobEntry = dbLogger.getMigrationJobEntry(jobId);
			assertThat(jobEntry).as("Migration job").isNotNull().hasStatus(AbstractMigrationJob.STATUS_ERROR);
			List<MigrationJobLogEntryItem> itemEntries = dbLogger.getMigrationJobItemEntries(jobId);
			assertThat(itemEntries).as("List of item entries").hasSize(1);
			assertThat(itemEntries.get(0)).as("Item entry").isFor(page).hasStatus(AbstractMigrationJob.STATUS_ERROR);
		});
	}

	/**
	 * Get list of migration job entry ids
	 * @return list of ids
	 * @throws NodeException
	 */
	protected List<Integer> getJobs() throws NodeException {
		return new ArrayList<>(Trx.supply(() -> {
			MigrationDBLogger dbLogger = new MigrationDBLogger(MigrationDBLogger.DEFAULT_LOGGER);
			return dbLogger.getMigrationJobEntries().stream().map(MigrationJobEntry::getJobId).collect(Collectors.toList());
		}));
	}

	/**
	 * Create a request to migrate the given objects with some preprocessors
	 * @param clazz object class
	 * @param objects list of objects
	 * @param preprocessors list of preprocessors
	 * @return request
	 * @throws NodeException
	 */
	protected <T extends NodeObject> TagTypeMigrationRequest request(Class<T> clazz, List<T> objects,
			List<Class<? extends IMigrationPreprocessor>> preprocessors) throws NodeException {
		return Trx.supply(() -> {
			TagTypeMigrationMapping mapping = new TagTypeMigrationMapping();
			mapping.setFromTagTypeId(fromConstruct.getId());
			mapping.setToTagTypeId(toConstruct.getId());
			MigrationPartMapping partMapping = new MigrationPartMapping();
			partMapping.setFromPartId(fromConstruct.getParts().get(0).getId());
			partMapping.setToPartId(toConstruct.getParts().get(0).getId());
			mapping.setPartMappings(Arrays.asList(partMapping));

			TagTypeMigrationRequest request = new TagTypeMigrationRequest();
			request.setMappings(Arrays.asList(mapping));

			if (Page.class.isAssignableFrom(clazz)) {
				request.setType("page");
				request.setObjectIds(objects.stream().map(NodeObject::getId).collect(Collectors.toList()));
			} else if (Template.class.isAssignableFrom(clazz)) {
				request.setType("template");
				request.setObjectIds(objects.stream().map(NodeObject::getId).collect(Collectors.toList()));
			} else {
				throw new NodeException("Only page or template migration possible");
			}

			AtomicInteger counter = new AtomicInteger();
			request.setEnabledPreProcessors(preprocessors.stream().map(p -> {
				MigrationPreProcessor pre = new MigrationPreProcessor();
				pre.setOrderId(counter.getAndIncrement());
				pre.setClassName(p.getName());
				return pre;
			}).collect(Collectors.toList()));

			return request;
		});
	}

	/**
	 * Create a request for a template migration
	 * @param from template to migrate from
	 * @param to template to migrate to
	 * @param tagNames list of tagnames to migrate
	 * @param preprocessors list of preprocessors
	 * @return request instance
	 * @throws NodeException
	 */
	protected TemplateMigrationRequest request(Template from, Template to, List<String> tagNames, List<Class<? extends IMigrationPreprocessor>> preprocessors)
			throws NodeException {
		return Trx.supply(() -> {
			TemplateMigrationRequest request = new TemplateMigrationRequest();

			TemplateMigrationMapping mapping = new TemplateMigrationMapping();
			mapping.setFromTemplateId(from.getId());
			mapping.setToTemplateId(to.getId());
			mapping.setNodeId(node.getId());

			List<TemplateMigrationEditableTagMapping> tagMappings = new ArrayList<>();
			for (String tagName : tagNames) {
				TemplateMigrationEditableTagMapping tagMapping = new TemplateMigrationEditableTagMapping();
				tagMapping.setFromTagId(from.getTag(tagName).getId());
				tagMapping.setToTagId(to.getTag(tagName).getId());

				MigrationPartMapping partMapping = new MigrationPartMapping();
				partMapping.setFromPartId(fromConstruct.getParts().get(0).getId());
				partMapping.setToPartId(toConstruct.getParts().get(0).getId());
				tagMapping.setPartMappings(Arrays.asList(partMapping));

				tagMappings.add(tagMapping);
			}

			mapping.setEditableTagMappings(tagMappings);
			mapping.setNonEditableTagMappings(Collections.emptyList());
			request.setMapping(mapping);
			request.setOptions(new HashMap<>());

			// pre processors
			AtomicInteger counter = new AtomicInteger();
			request.setEnabledPreProcessors(preprocessors.stream().map(p -> {
				MigrationPreProcessor pre = new MigrationPreProcessor();
				pre.setOrderId(counter.getAndIncrement());
				pre.setClassName(p.getName());
				return pre;
			}).collect(Collectors.toList()));

			return request;
		});
	}

	/**
	 * Do the tagtype migration
	 * @param request request
	 * @return id of the finished job
	 * @throws NodeException
	 */
	protected int migrate(TagTypeMigrationRequest request) throws NodeException {
		List<Integer> oldEntries = getJobs();

		try (Trx trx = new Trx(createSession(), true)) {
			TagTypeMigrationJob job = new TagTypeMigrationJob()
					.setRequest(request)
					.setType(request.getType())
					.setObjectIds(request.getObjectIds())
					.setHandlePagesByTemplate(false)
					.setHandleAllNodes(false)
					.setPreventTriggerEvent(false);

			AtomicBoolean foreground = new AtomicBoolean(true);
			job.execute(1000, TimeUnit.SECONDS, () -> foreground.set(false));
			assertTrue("Expected the job to finish", foreground.get());
			trx.success();
		}

		List<Integer> newEntries = getJobs();
		newEntries.removeAll(oldEntries);

		assertThat(newEntries).as("List of new migration job entries").hasSize(1);
		return newEntries.get(0);
	}

	/**
	 * Do the template migration
	 * @param request request
	 * @return id of finished job
	 * @throws NodeException
	 */
	protected int migrate(TemplateMigrationRequest request) throws NodeException {
		List<Integer> oldEntries = getJobs();

		try (Trx trx = new Trx(createSession(), true)) {
			TemplateMigrationJob job = new TemplateMigrationJob()
					.setRequest(request);

			AtomicBoolean foreground = new AtomicBoolean(true);
			job.execute(1000, TimeUnit.SECONDS, () -> foreground.set(false));
			assertTrue("Expected the job to finish", foreground.get());
			trx.success();
		}

		List<Integer> newEntries = getJobs();
		newEntries.removeAll(oldEntries);

		assertThat(newEntries).as("List of new migration job entries").hasSize(1);
		return newEntries.get(0);
	}
}
