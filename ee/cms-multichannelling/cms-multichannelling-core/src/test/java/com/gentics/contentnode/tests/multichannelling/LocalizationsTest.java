package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.testutils.Creator.createUser;
import static com.gentics.contentnode.testutils.Creator.createUsergroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.rest.model.response.LocalizationsResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.tests.utils.MCTestedType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for getting localizations
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
@RunWith(value = Parameterized.class)
public class LocalizationsTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test node
	 */
	private static Node node;

	/**
	 * Test channel
	 */
	private static Node channel;

	/**
	 * Subchannel
	 */
	private static Node subChannel;

	/**
	 * Other test channel
	 */
	private static Node otherChannel;

	/**
	 * Folder
	 */
	private static Folder folder;

	private static UserGroup group;

	private static SystemUser user;

	private static Template template;

	@Parameter(0)
	public MCTestedType type;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: type {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (MCTestedType type : MCTestedType.values()) {
			data.add(new Object[] { type });
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
		node = supply(() -> createNode());
		channel = supply(() -> createChannel(node, "Channel", "channel", "/"));
		subChannel = supply(() -> createChannel(channel, "Subchannel", "subchannel", "/"));
		otherChannel = supply(() -> createChannel(node, "Otherchannel", "otherchannel", "/"));

		folder = supply(() -> createFolder(node.getFolder(), "Folder"));

		template = supply(() -> createTemplate(folder, "Template"));

		// create group with permission
		UserGroup nodeGroup = supply(t -> t.getObject(UserGroup.class, 2));
		group = supply(() -> createUsergroup("Testgroup", "", nodeGroup));
		operate(() -> {
			String perms = new Permission(
					PermHandler.PERM_FOLDER_CREATE,
					PermHandler.PERM_FOLDER_UPDATE,
					PermHandler.PERM_PAGE_VIEW,
					PermHandler.PERM_PAGE_UPDATE,
					PermHandler.PERM_VIEW)
			.toString();

			setPermissions(Folder.TYPE_FOLDER, folder.getId(), Arrays.asList(group), perms);
		});

		// create user
		user = supply(() -> createUser("test", "test", "test", "test", "", Arrays.asList(group)));

		// restrict membership of the user
		user = execute(u -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			SystemUser editable = t.getObject(u, true);
			editable.getGroupNodeRestrictions().put(group.getId(), new HashSet<>(Arrays.asList(node.getId(), channel.getId(), subChannel.getId())));
			editable.save();
			return t.getObject(u);
		}, user);
	}

	@Test
	public void testMaster() throws NodeException {
		LocalizableNodeObject<?> master = supply(() -> type.create(folder, template));
		LocalizableNodeObject<?> channelVariant = execute(m -> type.localize(m, channel), master);
		LocalizableNodeObject<?> subChannelVariant = execute(m -> type.localize(m, subChannel), master);
		execute(m -> type.localize(m, otherChannel), master);

		try (Trx trx = new Trx(user)) {
			LocalizationsResponse localizations = type.localizations(master);
			assertResponseOK(localizations);
			assertThat(localizations.getMasterId()).as("Master ID").isEqualTo(master.getId());
			assertThat(localizations.getMasterNodeId()).as("Master Node ID").isEqualTo(node.getId());
			assertThat(localizations.getTotal()).as("Total count").isEqualTo(3);
			assertThat(localizations.getNodeIds()).as("Map").containsOnly(entry(channelVariant.getId(), channel.getId()),
					entry(subChannelVariant.getId(), subChannel.getId()));
			trx.success();
		}
	}

	@Test
	public void testChannel() throws NodeException {
		LocalizableNodeObject<?> master = supply(() -> type.create(folder, null, template, channel));
		LocalizableNodeObject<?> subChannelVariant = execute(m -> type.localize(m, subChannel), master);

		try (Trx trx = new Trx(user)) {
			LocalizationsResponse localizations = type.localizations(master);
			assertResponseOK(localizations);
			assertThat(localizations.getMasterId()).as("Master ID").isEqualTo(master.getId());
			assertThat(localizations.getMasterNodeId()).as("Master Node ID").isEqualTo(node.getId());
			assertThat(localizations.getTotal()).as("Total count").isEqualTo(1);
			assertThat(localizations.getNodeIds()).as("Map").containsOnly(entry(subChannelVariant.getId(), subChannel.getId()));
			trx.success();
		}
	}

	@Test
	public void testOtherChannel() throws NodeException {
		LocalizableNodeObject<?> master = supply(() -> type.create(folder, null, template, otherChannel));

		try (Trx trx = new Trx(user)) {
			LocalizationsResponse localizations = type.localizations(master);
			assertResponse(localizations, ResponseCode.PERMISSION);
			trx.success();
		}
	}

	@Test
	public void testNotExistingId() throws NodeException {
		try (Trx trx = new Trx(user)) {
			LocalizationsResponse localizations = type.localizations(4711);
			assertResponse(localizations, ResponseCode.NOTFOUND);
			trx.success();
		}
	}
}
