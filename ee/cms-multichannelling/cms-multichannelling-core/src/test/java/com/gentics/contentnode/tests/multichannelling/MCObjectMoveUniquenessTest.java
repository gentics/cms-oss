package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.BiConsumer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.tests.utils.MCTestedType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for uniqueness checks while moving objects
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
@RunWith(value = Parameterized.class)
public class MCObjectMoveUniquenessTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext(true);
	private static Node targetNode;
	private static Node targetChannel;
	private static Folder targetFolder;
	private static Folder sideFolder;
	private static Node sourceNode;
	private static Folder sourceFolder;
	private static Template template;
	private static SystemUser user;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		targetNode = supply(() -> createNode());
		targetChannel = supply(() -> createChannel(targetNode, "Channel", "channel", "/"));
		targetFolder = supply(() -> createFolder(targetNode.getFolder(), "Target"));
		sideFolder = supply(() -> createFolder(targetNode.getFolder(), "Sidefolder"));

		sourceNode = supply(() -> createNode());
		sourceFolder = supply(() -> createFolder(sourceNode.getFolder(), "Source"));

		template = supply(() -> createTemplate(sourceFolder, "Template"));

		user = supply(t -> t.getObject(SystemUser.class, 1));
	}

	@Parameters(name = "{index}: move {0} into {1}, conflict with {2}, same folder {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (MCTestedType testedType : MCTestedType.values()) {
			for (Target into : Target.values()) {
				for (Target conflict : Target.values()) {
					for (boolean sameFolder : Arrays.asList(true, false)) {
						data.add(new Object[] {testedType, into, conflict, sameFolder});
					}
				}
			}
		}
//		data.add(new Object[] {MCTestedType.folder, Target.master, Target.master, true});
		return data;
	}

	@Parameter(0)
	public MCTestedType testedType;

	@Parameter(1)
	public Target into;

	@Parameter(2)
	public Target conflict;

	@Parameter(3)
	public boolean sameFolder;

	@Before
	public void setup() throws NodeException {
		BiConsumer<Folder, Node> clean = (folder, node) -> {
			folder = folder.reload();
			try (ChannelTrx cTrx = new ChannelTrx(node)) {
				for (Folder f : folder.getChildFolders()) {
					f.delete(true);
				}
				for (Page p : folder.getPages()) {
					p.delete(true);
				}
				for (File f : folder.getFilesAndImages()) {
					f.delete(true);
				}
			}
		};

		consume(clean, sourceFolder, sourceNode);
		consume(clean, targetFolder, targetNode);
		consume(clean, targetFolder, targetChannel);
		consume(clean, sideFolder, targetNode);
		consume(clean, sideFolder, targetChannel);
	}

	/**
	 * Test that moving an object fails (with proper error message), if the target folder contains a conflicting object
	 * @throws NodeException
	 */
	@Test
	public void testMove() throws NodeException {
		Node conflictNode = null;
		switch (conflict) {
		case master:
			conflictNode = targetNode;
			break;
		case channel:
			conflictNode = targetChannel;
			break;
		}
		Node moveTo = (into == Target.master ? null : targetChannel);
		LocalizableNodeObject<?> movedObject = supply(() -> testedType.create(sourceFolder, "movedobject", template));
		LocalizableNodeObject<?> conflictObject = execute(n -> testedType.create(sameFolder ? targetFolder : sideFolder, "movedobject", template, n),
				conflictNode);

		consume(o -> {
			assertThat(o.getName()).as("Name before moving").isEqualTo(conflictObject.getName());
		}, movedObject);

		try (Trx trx = new Trx(user)) {
			GenericResponse response = testedType.move(movedObject, targetFolder, moveTo);
			// moving a folder into a target folder, where the "conflicting" folder exists in sibling of the target, is possible
			if (testedType == MCTestedType.folder && !sameFolder) {
				assertResponseCodeOk(response);
			} else {
				assertResponseCode(response, ResponseCode.FAILURE);
				assertErrorMessages(response, (NodeObject) conflictObject);
			}
			trx.success();
		}
	}

	/**
	 * Assert that the response contains the expected error message
	 * @param response response
	 * @param conflictObject conflicting object
	 */
	protected void assertErrorMessages(GenericResponse response, NodeObject conflictObject) throws NodeException {
		Message expected = null;
		switch (testedType) {
		case file:
		case image:
			expected = new Message(Type.WARNING, String.format(
					"Das Verschieben ist nicht möglich, da der neue Publizierungspfad '/nowhere/movedobject' bereits von der Datei '%s' verwendet wird.",
					I18NHelper.getPath(conflictObject)));
			break;
		case folder:
			expected = new Message(Type.CRITICAL, "Im Zielordner existiert bereits ein Ordner mit diesem Namen.");
			break;
		case page:
			expected = new Message(Type.WARNING, String.format(
					"Das Verschieben ist nicht möglich, da der neue Publizierungspfad '/nowhere/movedobject.html' bereits von der Seite '%s' verwendet wird.",
					I18NHelper.getPath(conflictObject)));
			break;
		}

		assertThat(response.getMessages()).isNotNull().usingElementComparatorOnFields("type", "message").containsOnly(expected);
	}

	/**
	 * Possible target nodes
	 */
	public static enum Target {
		master,
		channel
	}
}
