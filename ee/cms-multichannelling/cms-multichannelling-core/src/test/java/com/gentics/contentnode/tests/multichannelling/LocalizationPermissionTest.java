package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertError;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertSuccess;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFileResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getImageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.glassfish.jersey.media.multipart.MultiPart;
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
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.Image;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.request.FileSaveRequest;
import com.gentics.contentnode.rest.model.request.FolderCreateRequest;
import com.gentics.contentnode.rest.model.request.FolderSaveRequest;
import com.gentics.contentnode.rest.model.request.ImageSaveRequest;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PageLocalizeRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.request.UnlocalizeRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCFileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCFolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCImageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCPageResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.MCTestedType;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Test cases for localization with restricted permissions
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
@RunWith(value = Parameterized.class)
public class LocalizationPermissionTest {
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

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: type {0}, restricted {1}, channelwithperm {2}, create {3}, delete {4}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (MCTestedType type : MCTestedType.values()) {
			for (Restricted restricted : Restricted.values()) {
				for (boolean channelWithPerm : Arrays.asList(true, false)) {
					// if the user is not restricted to the channel, it does not make any difference in which channel the test is done
					if (restricted != Restricted.channel && channelWithPerm == false) {
						continue;
					}
					for (boolean create : Arrays.asList(true, false)) {
						for (boolean delete : Arrays.asList(true, false)) {
							data.add(new Object[] { type, restricted, channelWithPerm, create, delete });
						}
					}
				}
			}
		}
//		data.add(new Object[] {MCTestedType.file, Restricted.channel, true, true, true});
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
		node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		channel = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(node, "Channel", "channel", "/"));
		otherChannel = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(node, "Otherchannel", "otherchannel", "/"));

		folder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder"));

		template = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(folder, "Template"));

		// create group with permission
		UserGroup nodeGroup = Trx.supply(() -> TransactionManager.getCurrentTransaction().getObject(UserGroup.class, 2));
		group = Trx.supply(() -> Creator.createUsergroup("Testgroup", "", nodeGroup));
		Trx.operate(() -> {
			String perms = new Permission(
					PermHandler.PERM_FOLDER_CREATE,
					PermHandler.PERM_FOLDER_UPDATE,
					PermHandler.PERM_PAGE_VIEW,
					PermHandler.PERM_PAGE_UPDATE,
					PermHandler.PERM_VIEW)
			.toString();

			PermHandler.setPermissions(Folder.TYPE_FOLDER, folder.getId(), Arrays.asList(group), perms);
		});

		// create user
		user = Trx.supply(() -> Creator.createUser("test", "test", "test", "test", "", Arrays.asList(group)));
	}

	@Parameter(0)
	public MCTestedType type;

	@Parameter(1)
	public Restricted restricted;

	@Parameter(2)
	public boolean channelWithPerm;

	@Parameter(3)
	public boolean create;

	@Parameter(4)
	public boolean delete;

	@Before
	public void before() throws NodeException {
		Permission perm = new Permission(PermHandler.PERM_VIEW);

		switch (type) {
		case file:
		case image:
		case page:
			perm.mergeBits(new Permission(PermHandler.PERM_PAGE_VIEW).toString());
			if (create) {
				perm.mergeBits(new Permission(PermHandler.PERM_PAGE_CREATE, PermHandler.PERM_PAGE_UPDATE).toString());
			}
			if (delete) {
				perm.mergeBits(new Permission(PermHandler.PERM_PAGE_DELETE).toString());
			}
			break;
		case folder:
			if (create) {
				perm.mergeBits(new Permission(PermHandler.PERM_FOLDER_CREATE, PermHandler.PERM_FOLDER_UPDATE).toString());
			}
			if (delete) {
				perm.mergeBits(new Permission(PermHandler.PERM_FOLDER_DELETE).toString());
			}
			break;
		}

		Trx.operate(() -> {
			PermHandler.setPermissions(Folder.TYPE_FOLDER, folder.getId(), Arrays.asList(group), perm.toString());
		});

		// restrict user membership to the channel
		user = Trx.execute(u -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			SystemUser editable = t.getObject(u, true);
			switch (restricted) {
			case master:
				editable.getGroupNodeRestrictions().put(group.getId(), Collections.singleton(node.getId()));
				break;
			case channel:
				editable.getGroupNodeRestrictions().put(group.getId(), Collections.singleton(channel.getId()));
				break;
			case none:
				editable.getGroupNodeRestrictions().put(group.getId(), Collections.emptySet());
				break;
			}
			editable.save();
			return t.getObject(u);
		}, user);
	}

	/**
	 * Test localizing
	 * @throws NodeException
	 */
	@Test
	public void testLocalize() throws NodeException {
		Node testChannel = channelWithPerm ? channel : otherChannel;
		LocalizableNodeObject<?> master = Trx.supply(() -> type.create(folder, template));

		try (Trx trx = new Trx(user)) {
			PageLocalizeRequest request = new PageLocalizeRequest();
			request.setChannelId(testChannel.getId());
			GenericResponse response = null;

			switch (type) {
			case folder:
				response = new MCFolderResourceImpl().localize(master.getId(), request);
				break;
			case file:
				response = new MCFileResourceImpl().localize(master.getId(), request);
				break;
			case image:
				response = new MCImageResourceImpl().localize(master.getId(), request);
				break;
			case page:
				response = new MCPageResourceImpl().localize(master.getId(), request);
				break;
			}

			if (create && (restricted == Restricted.none || (restricted == Restricted.channel && testChannel.equals(channel)))) {
				ContentNodeRESTUtils.assertResponseOK(response);
			} else {
				ContentNodeRESTUtils.assertResponse(response, ResponseCode.PERMISSION,
						new CNI18nString(String.format("%s.nopermission", type.toString())).toString());
			}
			trx.success();
		}
	}

	/**
	 * Test unlocalizing
	 * @throws NodeException
	 */
	@Test
	public void testUnlocalize() throws NodeException {
		Node testChannel = channelWithPerm ? channel : otherChannel;
		LocalizableNodeObject<?> master = Trx.supply(() -> type.create(folder, template));
		LocalizableNodeObject<?> localized = Trx.execute(o -> type.localize(o, testChannel), master);

		try (Trx trx = new Trx(user)) {
			UnlocalizeRequest request = new UnlocalizeRequest();
			request.setChannelId(testChannel.getId());
			request.setRecursive(false);
			request.setTypes(Arrays.asList(UnlocalizeRequest.Type.file, UnlocalizeRequest.Type.image, UnlocalizeRequest.Type.page,
					UnlocalizeRequest.Type.folder));
			GenericResponse response = null;

			switch (type) {
			case folder:
				response = new MCFolderResourceImpl().unlocalize(localized.getId(), request);
				break;
			case file:
				response = new MCFileResourceImpl().unlocalize(localized.getId(), request);
				break;
			case image:
				response = new MCImageResourceImpl().unlocalize(localized.getId(), request);
				break;
			case page:
				response = new MCPageResourceImpl().unlocalize(localized.getId(), request);
				break;
			}

			if (delete && (restricted == Restricted.none || (restricted == Restricted.channel && testChannel.equals(channel)))) {
				ContentNodeRESTUtils.assertResponseOK(response);
			} else {
				ContentNodeRESTUtils.assertResponse(response, ResponseCode.PERMISSION,
						new CNI18nString(String.format("%s.nopermission", type.toString())).toString());
			}
			trx.success();
		}
	}

	/**
	 * Test create local objects
	 * @throws NodeException
	 * @throws ParseException
	 */
	@Test
	public void testCreate() throws NodeException, ParseException {
		Node testChannel = channelWithPerm ? channel : otherChannel;

		try (Trx trx = new Trx(user)) {
			Supplier<GenericResponse> method = null;

			switch (type) {
			case folder:
				FolderCreateRequest folderCreate = new FolderCreateRequest();
				folderCreate.setName("Testfolder");
				folderCreate.setMotherId(Integer.toString(folder.getId()));
				folderCreate.setNodeId(testChannel.getId());
				method = () -> getFolderResource().create(folderCreate);
				break;
			case file:
			case image:
				method = () -> {
					try (MultiPart fileMultiPart =  ContentNodeTestDataUtils.createRestFileUploadMultiPart("testfile.txt",
							folder.getId(), testChannel.getId(), "", false, "testcontent")) {
						return new FileResourceImpl().create(fileMultiPart);
					} catch (ParseException | IOException e) {
						throw new NodeException(e);
					}
				};
				break;
			case page:
				PageCreateRequest pageCreate = new PageCreateRequest();
				pageCreate.setFolderId(Integer.toString(folder.getId()));
				pageCreate.setNodeId(testChannel.getId());
				method = () -> getPageResource().create(pageCreate);
				break;
			}

			if (create && (restricted == Restricted.none || (restricted == Restricted.channel && testChannel.equals(channel)))) {
				assertSuccess(method, null);
			} else if (!create && (restricted == Restricted.none || (restricted == Restricted.channel && testChannel.equals(channel)))) {
				switch (type) {
				case folder:
					assertError(method, InsufficientPrivilegesException.class, ResponseCode.PERMISSION, "Insufficient permissions to create folder");
					break;
				case page:
					assertError(method, InsufficientPrivilegesException.class, ResponseCode.PERMISSION, "Insufficient permission to create pages in folder %d".formatted(folder.getId()));
					break;
				case file:
				case image:
					assertError(method, InsufficientPrivilegesException.class, ResponseCode.FAILURE, "You don't have permission to create files in the folder with id %d.".formatted(folder.getId()));
					break;
				}
			} else {
				switch (type) {
				case folder:
					assertError(method, InsufficientPrivilegesException.class, ResponseCode.FAILURE, "Error while creating new folder: " + I18NHelper.get("%s.nopermission".formatted(type)));
					break;
				case page:
					assertError(method, InsufficientPrivilegesException.class, ResponseCode.PERMISSION, "Insufficient permission to create pages in folder %d".formatted(folder.getId()));
					break;
				case file:
				case image:
					assertError(method, InsufficientPrivilegesException.class, ResponseCode.PERMISSION, I18NHelper.get("folder.nopermission"));
					break;
				}
			}
			trx.success();
		}
	}

	/**
	 * Test updating local objects
	 * @throws NodeException
	 */
	@Test
	public void testUpdate() throws NodeException {
		Node testChannel = channelWithPerm ? channel : otherChannel;
		LocalizableNodeObject<?> localObject = Trx.supply(() -> type.create(folder, null, template, testChannel));
		Trx.consume(LocalizableNodeObject::unlock, localObject);

		try (Trx trx = new Trx(user)) {
			Supplier<GenericResponse> method = null;

			switch (type) {
			case folder:
				com.gentics.contentnode.rest.model.Folder folderModel = new com.gentics.contentnode.rest.model.Folder();
				folderModel.setName("modified " + localObject.getName());
				FolderSaveRequest folderSave = new FolderSaveRequest();
				folderSave.setFolder(folderModel);
				method = () -> getFolderResource().save(Integer.toString(localObject.getId()), folderSave);
				break;
			case file:
				File file = new File();
				file.setName("modified_" + localObject.getName());
				FileSaveRequest fileSave = new FileSaveRequest();
				fileSave.setFile(file);
				method = () -> getFileResource().save(localObject.getId(), fileSave);
				break;
			case image:
				Image image = new Image();
				image.setName("modified_" + localObject.getName());
				ImageSaveRequest imageSave = new ImageSaveRequest();
				imageSave.setImage(image);
				method = () -> getImageResource().save(localObject.getId(), imageSave);
				break;
			case page:
				Page page = new Page();
				page.setName("modified " + localObject.getName());
				PageSaveRequest pageSave = new PageSaveRequest();
				pageSave.setPage(page);
				method = () -> getPageResource().save(Integer.toString(localObject.getId()), pageSave);
				break;
			}

			if (create && (restricted == Restricted.none || (restricted == Restricted.channel && testChannel.equals(channel)))) {
				assertSuccess(method, null);
			} else if (!create && (restricted == Restricted.none || (restricted == Restricted.channel && testChannel.equals(channel)))) {
				switch (type) {
				case folder:
					assertError(method, InsufficientPrivilegesException.class, ResponseCode.PERMISSION, String.format("Insufficient permissions to save Folder %d", localObject.getId()));
					break;
				case page:
				case file:
				case image:
					assertError(method, InsufficientPrivilegesException.class, ResponseCode.PERMISSION, I18NHelper.get("%s.nopermission".formatted(type)));
					break;
				}
			} else {
				switch (type) {
				case folder:
					assertError(method, InsufficientPrivilegesException.class, ResponseCode.PERMISSION, "Insufficient permissions to save Folder %d".formatted(localObject.getId()));
					break;
				case page:
				case file:
				case image:
					assertError(method, InsufficientPrivilegesException.class, ResponseCode.PERMISSION, I18NHelper.get("%s.nopermission".formatted(type)));
					break;
				}
			}
			trx.success();
		}
	}

	/**
	 * Test deleting local objects
	 * @throws NodeException
	 */
	@Test
	public void testDelete() throws NodeException {
		Node testChannel = channelWithPerm ? channel : otherChannel;
		LocalizableNodeObject<?> localObject = Trx.supply(() -> type.create(folder, null, template, testChannel));
		Trx.consume(LocalizableNodeObject::unlock, localObject);

		try (Trx trx = new Trx(user)) {
			Supplier<GenericResponse> method = null;

			switch (type) {
			case folder:
				method = () -> getFolderResource().delete(Integer.toString(localObject.getId()), testChannel.getId(), null);
				break;
			case file:
				method = () -> getFileResource().delete(Integer.toString(localObject.getId()), testChannel.getId(), null);
				break;
			case image:
				method = () -> getImageResource().delete(Integer.toString(localObject.getId()), testChannel.getId(), null);
				break;
			case page:
				method = () -> getPageResource().delete(Integer.toString(localObject.getId()), testChannel.getId(), null);
				break;
			}

			if (delete && (restricted == Restricted.none || (restricted == Restricted.channel && testChannel.equals(channel)))) {
				assertSuccess(method, null);
			} else {
				assertError(method, InsufficientPrivilegesException.class, ResponseCode.PERMISSION, I18NHelper.get("%s.nopermission".formatted(type)));
			}
			trx.success();
		}
	}

	@Test
	public void testDeleteMaster() throws NodeException {
		Node testChannel = channelWithPerm ? channel : otherChannel;
		LocalizableNodeObject<?> masterObject = supply(() -> type.create(folder, template));
		consume(LocalizableNodeObject::unlock, masterObject);

		supply(() -> type.localize(masterObject, testChannel));

		try (Trx trx = new Trx(user)) {
			Supplier<GenericResponse> method = null;

			switch (type) {
			case folder:
				method = () -> getFolderResource().delete(Integer.toString(masterObject.getId()), null, null);
				break;
			case file:
				method = () -> getFileResource().delete(Integer.toString(masterObject.getId()), null, null);
				break;
			case image:
				method = () -> getImageResource().delete(Integer.toString(masterObject.getId()), null, null);
				break;
			case page:
				method = () -> getPageResource().delete(Integer.toString(masterObject.getId()), null, null);
				break;
			}

			if (delete && restricted == Restricted.none) {
				assertSuccess(method, null);
			} else {
				assertError(method, InsufficientPrivilegesException.class, ResponseCode.PERMISSION, I18NHelper.get("%s.nopermission".formatted(type)));
			}
			trx.success();
		}
	}

	/**
	 * Possible restriction options
	 */
	public static enum Restricted {
		/**
		 * User is restricted to the master
		 */
		master,

		/**
		 * User is restricted to the channel
		 */
		channel,

		/**
		 * User is not restricted
		 */
		none
	}
}
