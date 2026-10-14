package com.gentics.contentnode.tests.contentstaging;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.StageableNodeObject;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.SingleSelectPartType;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

import jakarta.ws.rs.core.Response.Status;

/**
 * Test cases for checking the implementation entities for equality during an import
 */
@GCNFeature(set = { Feature.CONTENT_STAGING })
public class ContentStagingImplementationCheckTest implements ContentStagingMethods {
	/**
	 * Name of the test package
	 */
	public final static String PACKAGE_NAME = "TestPackage";

	/**
	 * Object mapper
	 */
	public static ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

	/**
	 * Database context
	 */
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();
	}

	/**
	 * Node for testing
	 */
	protected Node node;

	/**
	 * Root folder of the node
	 */
	protected Folder nodeRootFolder;

	@Before
	public void setup() throws NodeException, IOException {
		makeClean();

		// create a test node
		node = create(Node.class, n -> {
			Folder rootFolder = create(Folder.class, f -> {
				f.setName("Staged Node");
				f.setPublishDir("/");
			}).doNotSave().build();

			n.setFolder(rootFolder);
			n.setHostname("staged.node");
			n.setPublishDir("/");
			n.getLanguages().add(getLanguage("de"));
			n.getLanguages().add(getLanguage("en"));
		}).build();

		nodeRootFolder = supply(() -> node.getFolder());
	}

	/**
	 * Test adding a template tag
	 * @throws NodeException
	 */
	@Test
	public void testAddTemplateTag() throws NodeException {
		Integer constructId = supply(() -> createConstruct(node, LongHTMLPartType.class, "construct", "part"));
		String tagName = "templatetag";

		testImplementationChange(Page.class, () -> {
			// create a template
			return create(Template.class, t -> {
				t.setName("Tested Template");
				t.setSource("");
			}).build();
		}, template -> {
			// create a page with the template
			return create(Page.class, p -> {
				p.setFolderId(nodeRootFolder.getId());
				p.setTemplateId(template.getId());
				p.setName("Staged Page");
			}).build();
		}, template -> {
			// add a new tag to the template
			return update(template, t -> {
				t.getTags().put(tagName, create(TemplateTag.class, tag -> {
					tag.setConstructId(constructId);
					tag.setName(tagName);
				}).doNotSave().build());
			}).build();
		}, template -> {
			// remove the new tag from the template
			return update(template, t -> {
				t.getTags().remove(tagName);
			}).build();
		});
	}

	/**
	 * Test removing a template tag
	 * @throws NodeException
	 */
	@Test
	public void testRemoveTemplateTag() throws NodeException {
		Integer constructId = supply(() -> createConstruct(node, LongHTMLPartType.class, "construct", "part"));
		String tagName = "templatetag";
		AtomicReference<GlobalId> globalTagId = new AtomicReference<>();

		testImplementationChange(Page.class, () -> {
			// create a template
			return create(Template.class, t -> {
				t.setName("Tested Template");
				t.setSource("");
				t.getTags().put(tagName, create(TemplateTag.class, tag -> {
					tag.setConstructId(constructId);
					tag.setName(tagName);
				}).doNotSave().build());
			}).build();
		}, template -> {
			// create a page with the template
			return create(Page.class, p -> {
				p.setFolderId(nodeRootFolder.getId());
				p.setTemplateId(template.getId());
				p.setName("Staged Page");
			}).build();
		}, template -> {
			// remove the tag from the template
			return update(template, t -> {
				globalTagId.set(t.getTag(tagName).getGlobalId());
				t.getTags().remove(tagName);
			}).build();
		}, template -> {
			// re-add the tag to the template
			return update(template, t -> {
				t.getTags().put(tagName, create(TemplateTag.class, tag -> {
					tag.setGlobalId(globalTagId.get());
					tag.setConstructId(constructId);
					tag.setName(tagName);
				}).doNotSave().build());
			}).build();
		});
	}

	/**
	 * Test changing the template source
	 * @throws NodeException
	 */
	@Test
	public void testChangeTemplateSource() throws NodeException {
		testImplementationChange(Page.class, () -> {
			// create a template
			return create(Template.class, t -> {
				t.setName("Tested Template");
				t.setSource("Original template source");
			}).build();
		}, template -> {
			// create a page with the template
			return create(Page.class, p -> {
				p.setFolderId(nodeRootFolder.getId());
				p.setTemplateId(template.getId());
				p.setName("Staged Page");
			}).build();
		}, template -> {
			// change the template source
			return update(template, t -> {
				t.setSource("Modified template source");
			}).build();
		}, template -> {
			// change the template source back
			return update(template, t -> {
				t.setSource("Original template source");
			}).build();
		});
	}

	/**
	 * Test changing a template tag (value)
	 * @throws NodeException
	 */
	@Test
	public void testChangeTemplateTag() throws NodeException {
		Integer constructId = supply(() -> createConstruct(node, LongHTMLPartType.class, "construct", "part"));
		String tagName = "templatetag";

		testImplementationChange(Page.class, () -> {
			// create a template
			return create(Template.class, t -> {
				t.setName("Tested Template");
				t.setSource("");
				t.getTags().put(tagName, create(TemplateTag.class, tag -> {
					tag.setConstructId(constructId);
					tag.setName(tagName);
					getPartType(LongHTMLPartType.class, tag, "part").setText("Original part value");
				}).doNotSave().build());
			}).build();
		}, template -> {
			// create a page with the template
			return create(Page.class, p -> {
				p.setFolderId(nodeRootFolder.getId());
				p.setTemplateId(template.getId());
				p.setName("Staged Page");
			}).build();
		}, template -> {
			// change the tag value
			return update(template, t -> {
				getPartType(LongHTMLPartType.class, t.getTag(tagName), "part").setText("Modified part value");
			}).build();
		}, template -> {
			// change the tag value back
			return update(template, t -> {
				getPartType(LongHTMLPartType.class, t.getTag(tagName), "part").setText("Original part value");
			}).build();
		});
	}

	/**
	 * Test deleting the template
	 * @throws NodeException
	 */
	@Test
	public void testDeleteTemplate() throws NodeException {
		testImplementationDelete(Page.class, () -> {
			// create a template
			return create(Template.class, t -> {
				t.setName("Tested Template");
				t.setSource("Original template source");
			}).build();
		}, template -> {
			// create a page with the template
			return create(Page.class, p -> {
				p.setFolderId(nodeRootFolder.getId());
				p.setTemplateId(template.getId());
				p.setName("Staged Page");
			}).build();
		});
	}

	/**
	 * Test adding a construct part
	 * @throws NodeException
	 */
	@Test
	public void testAddConstructPart() throws NodeException {
		Template template = create(Template.class, t -> {
			t.setName("Template");
			t.setSource("");
		}).build();
		String partName = "part2";

		testImplementationChange(Page.class, () -> {
			// create a construct with one part
			return create(Construct.class, c -> {
				c.setKeyword("test_construct");
				c.setName("Construct", 1);
				c.setAutoEnable(true);

				c.getParts().add(create(Part.class, p -> {
					p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
					p.setKeyname("part1");
					p.setName("Part 1", 1);
				}).doNotSave().build());
			}).build();
		}, construct -> {
			// create page with a tag using the construct
			return create(Page.class, p -> {
				p.setName("Page");
				p.setFolderId(nodeRootFolder.getId());
				p.setTemplateId(template.getId());
				p.getContent().addContentTag(construct.getId());
			}).build();
		}, construct -> {
			return update(construct, c -> {
				c.getParts().add(create(Part.class, p -> {
					p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
					p.setKeyname(partName);
					p.setName(partName, 1);
				}).doNotSave().build());
			}).build();
		}, construct -> {
			return update(construct, c -> {
				c.getParts().removeIf(p -> StringUtils.equals(p.getKeyname(), partName));
			}).build();
		});
	}

	/**
	 * Test removing a template part
	 * @throws NodeException
	 */
	@Test
	public void testRemoveConstructPart() throws NodeException {
		Template template = create(Template.class, t -> {
			t.setName("Template");
			t.setSource("");
		}).build();
		String partName = "part";
		AtomicReference<GlobalId> globalPartId = new AtomicReference<>();

		testImplementationChange(Page.class, () -> {
			// create a construct with one part
			return create(Construct.class, c -> {
				c.setKeyword("test_construct");
				c.setName("Construct", 1);
				c.setAutoEnable(true);

				c.getParts().add(create(Part.class, p -> {
					p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
					p.setKeyname(partName);
					p.setName(partName, 1);
				}).doNotSave().build());
			}).build();
		}, construct -> {
			// create page with a tag using the construct
			return create(Page.class, p -> {
				p.setName("Page");
				p.setFolderId(nodeRootFolder.getId());
				p.setTemplateId(template.getId());
				p.getContent().addContentTag(construct.getId());
			}).build();
		}, construct -> {
			// remove the part
			return update(construct, c -> {
				Optional<Part> optionalPart = c.getParts().stream().filter(p -> StringUtils.equals(p.getKeyname(), partName)).findFirst();
				if (optionalPart.isPresent()) {
					Part part = optionalPart.get();
					globalPartId.set(part.getGlobalId());
					c.getParts().remove(part);
				}
			}).build();
		}, construct -> {
			// re-add the part (with identical implementation)
			return update(construct, c -> {
				c.getParts().add(create(Part.class, p -> {
					p.setGlobalId(globalPartId.get());
					p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
					p.setKeyname(partName);
					p.setName(partName, 1);
				}).doNotSave().build());
			}).build();
		});
	}

	/**
	 * Test changing a construct part (value)
	 * @throws NodeException
	 */
	@Test
	public void testChangeConstructPart() throws NodeException {
		Template template = create(Template.class, t -> {
			t.setName("Template");
			t.setSource("");
		}).build();
		String partName = "part";

		testImplementationChange(Page.class, () -> {
			// create a construct with one part
			Construct construct = create(Construct.class, c -> {
				c.setKeyword("test_construct");
				c.setName("Construct", 1);
				c.setAutoEnable(true);

				c.getParts().add(create(Part.class, p -> {
					p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
					p.setKeyname(partName);
					p.setName(partName, 1);
					p.setDefaultValue(create(Value.class, v -> {
					}).doNotSave().build());
				}).doNotSave().build());
			}).build();

			return update(construct, upd -> {
				getPartType(LongHTMLPartType.class, upd, partName).setText("Original text");
			}).build();
		}, construct -> {
			// create page with a tag using the construct
			return create(Page.class, p -> {
				p.setName("Page");
				p.setFolderId(nodeRootFolder.getId());
				p.setTemplateId(template.getId());
				p.getContent().addContentTag(construct.getId());
			}).build();
		}, construct -> {
			// update the part text
			return update(construct, upd -> {
				getPartType(LongHTMLPartType.class, upd, partName).setText("Modified text");
			}).build();
		}, construct -> {
			// update the part text back
			return update(construct, upd -> {
				getPartType(LongHTMLPartType.class, upd, partName).setText("Original text");
			}).build();
		});
	}

	/**
	 * Test deleting a used construct
	 * @throws NodeException
	 */
	@Test
	public void testDeleteConstruct() throws NodeException {
		Template template = create(Template.class, t -> {
			t.setName("Template");
			t.setSource("");
		}).build();

		testImplementationDelete(Page.class, () -> {
			// create a construct
			return create(Construct.class, c -> {
				c.setKeyword("test_construct");
				c.setName("Construct", 1);
				c.setAutoEnable(true);
			}).build();
		}, construct -> {
			// create page with a tag using the construct
			return create(Page.class, p -> {
				p.setName("Page");
				p.setFolderId(nodeRootFolder.getId());
				p.setTemplateId(template.getId());
				p.getContent().addContentTag(construct.getId());
			}).build();
		});
	}

	/**
	 * Test changing the object property so that it uses a different construct
	 * @throws NodeException
	 */
	@Test
	public void testChangeObjectPropertyConstruct() throws NodeException {
		Integer originalConstructId = supply(() -> createConstruct(node, LongHTMLPartType.class, "construct", "part"));
		Integer modifiedConstructId = supply(() -> createConstruct(node, LongHTMLPartType.class, "modifiedconstruct", "part"));

		testImplementationChange(Folder.class, () -> {
			// create object property for folder with the original construct
			return create(ObjectTagDefinition.class, prop -> {
				ObjectTag tag = prop.getObjectTag();
				tag.setConstructId(originalConstructId);
				tag.setName("object.property");
				tag.setObjType(Folder.TYPE_FOLDER);
				prop.setName("Object Property", 1);
				prop.setDescription("Object Property Description", 1);
			}).build();
		}, prop -> {
			// create a folder
			return create(Folder.class, f -> {
				f.setMotherId(nodeRootFolder.getId());
				f.setName("Staged Folder");
				f.setPublishDir("/");
			}).build();
		}, prop -> {
			// update object property to use a different construct
			return update(prop, p -> {
				ObjectTag tag = p.getObjectTag();
				tag.setConstructId(modifiedConstructId);
			}).build();
		}, prop -> {
			// update object property back to use the original construct
			return update(prop, p -> {
				ObjectTag tag = p.getObjectTag();
				tag.setConstructId(originalConstructId);
			}).build();
		});
	}

	/**
	 * Test deleting an object property
	 * @throws NodeException
	 */
	@Test
	public void testDeleteObjectProperty() throws NodeException {
		Integer constructId = supply(() -> createConstruct(node, LongHTMLPartType.class, "construct", "part"));

		testImplementationDelete(Folder.class, () -> {
			// create object property for folder
			return create(ObjectTagDefinition.class, prop -> {
				ObjectTag tag = prop.getObjectTag();
				tag.setConstructId(constructId);
				tag.setName("object.property");
				tag.setObjType(Folder.TYPE_FOLDER);
				prop.setName("Object Property", 1);
				prop.setDescription("Object Property Description", 1);
			}).build();
		}, prop -> {
			// create a folder
			return create(Folder.class, f -> {
				f.setMotherId(nodeRootFolder.getId());
				f.setName("Staged Folder");
				f.setPublishDir("/");
			}).build();
		});
	}

	/**
	 * Test adding an entry to a datasource
	 * @throws NodeException
	 */
	@Test
	public void testAddDatasourceEntry() throws NodeException {
		AtomicInteger constructId = new AtomicInteger();

		Template template = create(Template.class, t -> {
			t.setName("Template");
			t.setSource("");
		}).build();

		testImplementationChange(Page.class, () -> {
			constructId.set(supply(() -> createConstruct(node, SingleSelectPartType.class, "construct", "part")));

			Construct construct = supply(t -> t.getObject(Construct.class, constructId.get()));
			return supply(() -> getPartType(SingleSelectPartType.class, construct, "part").getDatasource());
		}, datasource -> {
			Construct construct = supply(t -> t.getObject(Construct.class, constructId.get()));

			return create(Page.class, p -> {
				p.setFolderId(nodeRootFolder.getId());
				p.setTemplateId(template.getId());
				p.setName("Staged Page");
				p.getContent().addContentTag(construct.getId());
			}).build();
		}, datasource -> {
			return update(datasource, upd -> {
				upd.getEntries().add(create(DatasourceEntry.class, e -> {
					e.setKey("delete");
					e.setValue("me");
				}).doNotSave().build());
			}).build();
		}, datasource -> {
			return update(datasource, upd -> {
				upd.getEntries().removeIf(e -> StringUtils.equals(e.getKey(), "delete"));
			}).build();
		});
		supply(() -> createConstruct(node, SingleSelectPartType.class, "construct", "part"));
	}

	/**
	 * Test removing an entry from a datasource
	 * @throws NodeException
	 */
	@Test
	public void testRemoveDatasourceEntry() throws NodeException {
		AtomicInteger constructId = new AtomicInteger();
		AtomicReference<GlobalId> globalDatasourceEntryId = new AtomicReference<>();

		Template template = create(Template.class, t -> {
			t.setName("Template");
			t.setSource("");
		}).build();

		testImplementationChange(Page.class, () -> {
			constructId.set(supply(() -> createConstruct(node, SingleSelectPartType.class, "construct", "part")));

			Construct construct = supply(t -> t.getObject(Construct.class, constructId.get()));
			Datasource datasource = supply(() -> getPartType(SingleSelectPartType.class, construct, "part").getDatasource());
			return update(datasource, upd -> {
				upd.getEntries().add(create(DatasourceEntry.class, e -> {
					e.setKey("delete");
					e.setValue("me");
				}).doNotSave().build());
			}).build();
		}, datasource -> {
			Construct construct = supply(t -> t.getObject(Construct.class, constructId.get()));

			return create(Page.class, p -> {
				p.setFolderId(nodeRootFolder.getId());
				p.setTemplateId(template.getId());
				p.setName("Staged Page");
				p.getContent().addContentTag(construct.getId());
			}).build();
		}, datasource -> {
			return update(datasource, upd -> {
				Optional<DatasourceEntry> optionalEntry = upd.getEntries().stream()
						.filter(e -> StringUtils.equals(e.getKey(), "delete")).findFirst();
				if (optionalEntry.isPresent()) {
					DatasourceEntry entry = optionalEntry.get();
					globalDatasourceEntryId.set(entry.getGlobalId());
					upd.getEntries().remove(entry);
				}
			}).build();
		}, datasource -> {
			return update(datasource, upd -> {
				upd.getEntries().add(create(DatasourceEntry.class, e -> {
					e.setKey("delete");
					e.setValue("me");
					e.setGlobalId(globalDatasourceEntryId.get());
				}).doNotSave().build());
			}).build();
		});
	}

	/**
	 * Test deleting a used datasource
	 * @throws NodeException
	 */
	@Test
	public void testDeleteDatasource() throws NodeException {
		AtomicInteger constructId = new AtomicInteger();

		Template template = create(Template.class, t -> {
			t.setName("Template");
			t.setSource("");
		}).build();

		testImplementationDelete(Page.class, () -> {
			constructId.set(supply(() -> createConstruct(node, SingleSelectPartType.class, "construct", "part")));

			Construct construct = supply(t -> t.getObject(Construct.class, constructId.get()));
			Datasource datasource = supply(() -> getPartType(SingleSelectPartType.class, construct, "part").getDatasource());
			return update(datasource, upd -> {
				upd.getEntries().add(create(DatasourceEntry.class, e -> {
					e.setKey("delete");
					e.setValue("me");
				}).doNotSave().build());
			}).build();
		}, datasource -> {
			Construct construct = supply(t -> t.getObject(Construct.class, constructId.get()));

			return create(Page.class, p -> {
				p.setFolderId(nodeRootFolder.getId());
				p.setTemplateId(template.getId());
				p.setName("Staged Page");
				p.getContent().addContentTag(construct.getId());
			}).build();
		});
	}

	/**
	 * Generic method to perform the implementation check
	 * @param <T> type of tested implementation entity
	 * @param <U> type of staged object
	 * @param clazz class of the staged object
	 * @param createTested create the tested entity
	 * @param createStaged create the staged object
	 * @param modifyTested modify the tested entity
	 * @param modifyTestedBack modify the tesed entity back
	 * @throws NodeException
	 */
	protected <T extends NodeObject, U extends StageableNodeObject> void testImplementationChange(Class<U> clazz,
			Supplier<T> createTested, Function<T, U> createStaged, Function<T, T> modifyTested,
			Function<T, T> modifyTestedBack) throws NodeException {
		// create tested entity
		T testedEntity = createTested.supply();

		// create exported object
		U stagedObject = createStaged.apply(testedEntity);
		GlobalId objectGlobalId = execute(NodeObject::getGlobalId, stagedObject);

		// create package and export object
		createContentPackage(PACKAGE_NAME);
		addObjectToContentPackage(PACKAGE_NAME, stagedObject);

		// delete exported object
		consume(NodeObject::delete, stagedObject, true);
		stagedObject = execute(NodeObject::reload, stagedObject);
		assertThat(stagedObject).as("Deleted object").isNull();

		// modify tested entity
		testedEntity = modifyTested.apply(testedEntity);

		// try import (assert that import fails with a proper message and that object is not imported)
		String message = execute(obj -> {
			return I18NHelper.get("content.staging.import.errors", PACKAGE_NAME, "1");
		}, testedEntity);
		tryImportContentPackage(PACKAGE_NAME, Status.INTERNAL_SERVER_ERROR, ResponseCode.FAILURE, "Error while importing", new Message(Message.Type.CRITICAL, message));
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));
		assertThat(stagedObject).as("Staged object").isNull();

		// modify tested entity back
		testedEntity = modifyTestedBack.apply(testedEntity);

		// import (assert that import succeeds and that object is imported)
		importContentPackage(PACKAGE_NAME);
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));
		assertThat(stagedObject).as("Staged object").isNotNull();
	}

	/**
	 * Generic method to perform the check for missing implementation objects
	 * @param <T> type of tested implementation entity
	 * @param <U> type of staged object
	 * @param clazz class of staged object
	 * @param createTested create the tested entity
	 * @param createStaged create the staged object
	 * @throws NodeException
	 */
	protected <T extends NodeObject, U extends StageableNodeObject> void testImplementationDelete(Class<U> clazz,
			Supplier<T> createTested, Function<T, U> createStaged) throws NodeException {
		// create tested entity
		T testedEntity = createTested.supply();
		GlobalId globalEntityId = execute(NodeObject::getGlobalId, testedEntity);
		String entityTable = execute(obj -> {
			return TransactionManager.getCurrentTransaction().getTable(obj.getObjectInfo().getObjectClass());
		}, testedEntity);

		// create exported object
		U stagedObject = createStaged.apply(testedEntity);

		// create package and export object
		createContentPackage(PACKAGE_NAME);
		addObjectToContentPackage(PACKAGE_NAME, stagedObject);

		// delete exported object
		consume(NodeObject::delete, stagedObject, true);
		stagedObject = execute(NodeObject::reload, stagedObject);
		assertThat(stagedObject).as("Deleted object").isNull();

		// delete tested entity
		consume(NodeObject::delete, testedEntity, true);
		testedEntity = execute(NodeObject::reload, testedEntity);
		assertThat(testedEntity).as("Deleted entity").isNull();

		// try import (assert that import fails with a proper message)
		String responseMessage = supply(() -> {
			return String.format("Could not find %s with id %s", entityTable, globalEntityId.toString());
		});
		String message = supply(() -> {
			return I18NHelper.get(String.format("%s.notfound", entityTable), globalEntityId.toString());
		});
		// the EntityNotFoundException is mapped to a response with status 404 and response code NOTFOUND
		Throwable thrown = catchThrowable(() -> new ContentStagingResourceImpl().importIntoCms(PACKAGE_NAME, 0, false));
		assertThat(thrown).as("Import error").isInstanceOf(EntityNotFoundException.class).hasMessage(responseMessage);
		assertThat(supply(() -> thrown.getLocalizedMessage())).as("Localized import error").isEqualTo(message);
	}
}
