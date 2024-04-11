package com.gentics.contentnode.testutils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.factory.object.UserLanguageFactory;
import com.gentics.contentnode.i18n.CNDictionary;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.UserLanguage;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.object.Value;
import com.gentics.testutils.TestFileProvider;


/**
 * Helper class for creating objects.
 * All objects created here are also immediately saved or written to the database by other means.
 * Where available, return values are writable objects.
 * @author escitalopram
 */
public class Creator {

	/**
	 * Creates a role.
	 * @param name Name of the role
	 * @param languagePermissions Page permissions for different languages
	 * @param pagePerms general page permissions
	 * @param filePerms permissions on files and images
	 * @return id of the role
	 * @throws NodeException
	 */
	public static int createRole(String name, Map<ContentLanguage, String> languagePermissions, String pagePerms, String filePerms) throws NodeException {
		int outputid = CNDictionary.createNewOutputId();
		List<Integer> idList = UserLanguageFactory.getActive().stream().map(UserLanguage::getId).collect(Collectors.toList());
		for (Integer id : idList) {
			CNDictionary.saveDicUserEntry(outputid, id, name);
		}
		int roleId = DBUtils.executeInsert("insert into role (name_id, description_id) values (?, ?)", new Object[] { outputid, outputid }).get(0);

		for (Entry<ContentLanguage, String> entry : languagePermissions.entrySet()) {
			int rpId = DBUtils.executeInsert("insert into roleperm (role_id, perm) values (?, ?)", new Object[] { roleId, entry.getValue() }).get(0);
			DBUtils.executeInsert("insert into roleperm_obj (roleperm_id, obj_type, obj_id) values (?, ?, ?)", new Object[] { rpId,
					ContentLanguage.TYPE_CONTENTGROUP, entry.getKey().getId() });
			DBUtils.executeInsert("insert into roleperm_obj (roleperm_id, obj_type) values (?, ?)", new Object[] { rpId, Page.TYPE_PAGE });
		}

		if (pagePerms != null) {
			int rpId = DBUtils.executeInsert("insert into roleperm (role_id, perm) values (?, ?)", new Object[] { roleId, pagePerms }).get(0);
			DBUtils.executeInsert("insert into roleperm_obj (roleperm_id, obj_type) values (?, ?)", new Object[] { rpId, Page.TYPE_PAGE });
		}

		if (filePerms != null) {
			int rpId = DBUtils.executeInsert("insert into roleperm (role_id, perm) values (?, ?)", new Object[] { roleId, filePerms }).get(0);
			DBUtils.executeInsert("insert into roleperm_obj (roleperm_id, obj_type) values (?, ?)", new Object[] { rpId, File.TYPE_FILE });
		}

		return roleId;
	}

	/**
	 * Creates a usergroup.
	 * @param name name of the group
	 * @param description description of the group
	 * @param parent parent group
	 * @return the newly created group
	 * @throws NodeException
	 */
	public static UserGroup createUsergroup(String name, String description, UserGroup parent) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		int groupId = DBUtils.executeInsert("insert into usergroup (name, mother, creator, editor, description) values (?, ?, 1, 1, ?)",
				new Object[] { name, parent.getId(), description }).get(0);
		UserGroup result = t.getObject(UserGroup.class, groupId);
		return result;
	}

	/**
	 * Creates a new user.
	 * @param login login name
	 * @param password password
	 * @param firstname first name
	 * @param lastname last name
	 * @param email email address
	 * @param groups list of groups the user should be contained in
	 * @return the newly created user
	 * @throws NodeException
	 */
	public static SystemUser createUser(String login, String password, String firstname, String lastname, String email, List<UserGroup> groups) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		SystemUser result = t.createObject(SystemUser.class);
		result.setLogin(login);
		result.setPassword("todo");
		result.setFirstname(firstname);
		result.setLastname(lastname);
		result.setEmail(email);
		result.setActive(true);
		for (UserGroup g : groups) {
			result.getUserGroups().add(g);
		}
		result.save();
		//set the encrypted password, salted with the userId
		int userId = ObjectTransformer.getInt(result.getId(), -1);
		result.setPassword(SystemUserFactory.hashPassword(password, userId));
		result.save();
		return result;
	}

	/**
	 * Creates a language.
	 * @param name name of the language
	 * @param code language code
	 * @return the newly created language
	 * @throws NodeException
	 */
	public static ContentLanguage createLanguage(String name, String code) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		ContentLanguage result = t.createObject(ContentLanguage.class);
		result.setName(name);
		result.setCode(code);
		result.save();
		return result;
	}

	/**
	 * Creates a new node including its root folder.
	 * @param name name of the node
	 * @param hostname hostname of the node
	 * @param nodePubdir publication directory of the node
	 * @param folderPubdir publication directory of the folder
	 * @param languages languages that should be used in the folder
	 * @return the newly created node
	 * @throws NodeException
	 */
	public static Node createNode(String name, String hostname, String nodePubdir, String folderPubdir, List<ContentLanguage> languages) throws NodeException {
		return createNode(name, hostname, nodePubdir, folderPubdir, languages, null);
	}

	/**
	 * Creates a new node including its root folder.
	 * @param name name of the node
	 * @param hostname hostname of the node
	 * @param nodePubdir publication directory of the node
	 * @param folderPubdir publication directory of the folder
	 * @param languages languages that should be used in the folder
	 * @param masterNode create a channel of this node, create master node if null
	 * @return the newly created node
	 * @throws NodeException
	 */
	public static Node createNode(String name, String hostname, String nodePubdir, String folderPubdir, List<ContentLanguage> languages, Node masterNode) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node result = t.createObject(Node.class);
		result.setHostname(hostname);
		result.setPublishDir(nodePubdir);

		Folder rootFolder = t.createObject(Folder.class);
		rootFolder.setName(name);
		rootFolder.setPublishDir(folderPubdir);

		if (masterNode != null) {
			rootFolder.setChannelMaster(masterNode.getFolder());
		}

		result.setFolder(rootFolder);
		result.save();

		int position = 1;
		if (languages != null) {
			for (ContentLanguage lang : languages) {
			DBUtils.executeInsert("insert into node_contentgroup (node_id, contentgroup_id, sortorder) values (?, ?, ?)",
					new Object[] { result.getId(), lang.getId(), position++ });
			}
		}
		return result;
	}

	/**
	 * Creates a folder.
	 * @param parent parent folder
	 * @param name name of the folder
	 * @param publishDir publishdir of the folder
	 * @return the newly created folder
	 * @throws NodeException
	 */
	public static Folder createFolder(Folder parent, String name, String publishDir) throws NodeException {
		return createFolder(parent, name, publishDir, null);
	}

	/**
	 * Create a folder
	 * @param parent parent folder
	 * @param name name of the folder
	 * @param publishDir publishdir of the folder
	 * @param channel the channel where the folder should be created
	 * @return the newly created folder
	 * @throws NodeException
	 */
	public static Folder createFolder(Folder parent, String name, String publishDir, Node channel) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder result = t.createObject(Folder.class);
		result.setName(name);
		result.setPublishDir(publishDir);
		result.setMotherId(parent.getId());
		if (channel != null && channel.isChannel()) {
			result.setChannelInfo(channel.getId(), result.getChannelSetId());
		}
		result.save();
		return result;
	}

	/**
	 * Creates a template without template tags.
	 * @param name name of the template
	 * @param source source code of the template
	 * @param folder initial owning folder of the template
	 * @return the newly created template
	 * @throws NodeException
	 */
	public static Template createTemplate(String name, String source, Folder folder) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Template template = t.createObject(Template.class);
		template.setFolderId(folder.getId());
		template.setSource(source);
		template.setName(name);
		template.save();
		return template;
	}

	/**
	 * Creates a new file.
	 *
	 * @param folder the folder where to create the file
	 * @param name the name of the new file
	 * @param data the data to be contained in the file
	 * @param channel the channel where to create the file
	 * @return the newly created file
	 * @throws Exception
	 */
	public static File createFile(Folder folder, String name, byte[] data, Node channel) throws Exception {
		File intermediate = ContentNodeTestDataUtils.createFile(folder, name, data, channel);
		Transaction t = TransactionManager.getCurrentTransaction();
		return t.getObject(File.class, intermediate.getId(), true);
	}

	/**
	 * Creates a new image.
	 *
	 * @param folder the folder where to create the image
	 * @param name the name of the new image
	 * @param channel the channel to create the image in
	 * @return the newly created image.
	 * @throws Exception
	 */
	public static ImageFile createImage(Folder folder, String name, Node channel) throws Exception {
		byte[] imgdata = IOUtils.toByteArray(TestFileProvider.getTestJPG1());
		return createImage(folder, name, channel, imgdata);
	}

	/**
	 * Creates a new image.
	 *
	 * @param folder the folder where to create the image
	 * @param name the name of the new image
	 * @param channel the channel to create the image in
	 * @return the newly created image.
	 * @throws Exception
	 */
	public static ImageFile createImage(Folder folder, String name, Node channel, byte[] imgdata) throws Exception {
		File intermediate = ContentNodeTestDataUtils.createFile(folder, name, imgdata, channel);
		Transaction t = TransactionManager.getCurrentTransaction();
		return t.getObject(ImageFile.class, intermediate.getId(), true);
	}

	/**
	 * Creates a page without additional content tags.
	 * @param name name of the page. can be null
	 * @param parent parent folder of the page
	 * @param template template of the page
	 * @param language the language of the page. can be null
	 * @return the newly created page
	 * @throws NodeException
	 */
	public static Page createPage(String name, Folder parent, Template template, ContentLanguage language) throws NodeException {
		return createPage(name, parent, template, language, null);
	}

	/**
	 * Creates a page without additional content tags.
	 * @param name name of the page. can be null
	 * @param parent parent folder of the page
	 * @param template template of the page
	 * @param language the language of the page. can be null
	 * @param channel create page in this channel
	 * @return the newly created page
	 * @throws NodeException
	 */
	public static Page createPage(String name, Folder parent, Template template, ContentLanguage language, Node channel) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page result = t.createObject(Page.class);
		result.setFolderId(parent.getId());
		result.setTemplateId(template.getId());
		if (name != null) {
			result.setName(name);
		}
		if (channel != null && channel.isChannel()) {
			result.setChannelInfo(channel.getId(), result.getChannelSetId());
		}
		if (language != null) {
			result.setLanguage(language);
		}
		result.save();
		return result;
	}
	/**
	 * Creates a page.
	 * @param name the name of the page
	 * @param parent the folder where to create the page
	 * @param template the template to use
	 * @return the newly created page
	 * @throws NodeException
	 */
	public static Page createPage(String name, Folder parent, Template template) throws NodeException {
		return createPage(name, parent, template, null, null);
	}

	/**
	 * Creates a language variant using the source page and the target language.
	 * @param sourcePage the page to create the language variant for
	 * @param targetLanguage the language of the new variant
	 * @return the newyly created page variant
	 * @throws NodeException
	 */
	public static Page createLanguageVariant(Page sourcePage, ContentLanguage targetLanguage) throws NodeException {
		Page pageVariant = (Page) sourcePage.copy();
		pageVariant.setContentsetId(sourcePage.getContentsetId());
		pageVariant.setLanguage(targetLanguage);
		pageVariant.save();
		return pageVariant;
	}

	/**
	 * Creates a construct.
	 * @param keyword keyword of the construct
	 * @param iconName iconname for the construct
	 * @param name name of the construct
	 * @param parts list of parts
	 * @return the construct
	 * @throws NodeException
	 */
	public static Construct createConstruct(String keyword, String iconName, String name, List<Part> parts) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Construct result = t.createObject(Construct.class);
		result.setKeyword(keyword);
		result.setName(name, 1);
		if (parts != null) {
			result.getParts().addAll(parts);
		}
		result.save();
		return result;
	}

	/**
	 * Creates a part and <i>doesn't</i> save it
	 * @param keyword keyword of the new part
	 * @param partType part type of the new part
	 * @param editable editable value of the new part
	 * @param defaultValue default value of the new part
	 * @return the newly created part
	 * @throws NodeException
	 */
	public static Part createTextPartUnsaved(String keyword, int partType, int editable, String defaultValue) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Part result = t.createObject(Part.class);
		result.setKeyname(keyword);
		result.setPartTypeId(partType);
		result.setEditable(editable);
		if (defaultValue != null) {
			Value val = t.createObject(Value.class);
			val.setPart(result);
			val.setValueText(defaultValue);
			result.setDefaultValue(val);
		}
		return result;
	}
	
	/**
	 * Create a simple overview part for constructs
	 * 
	 * @param keyword keyword of the new part
	 * @param editable editable value of the new part
	 * @return the newly created part
	 * @throws NodeException
	 */
	public static Part createOverviewPart(String keyword, int editable) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Part result = t.createObject(Part.class);
		result.setKeyname(keyword);
		result.setPartTypeId(Part.OVERVIEW);
		result.setEditable(editable);
		return result;
	}

	/**
	 * Create a simple part without a default value.
	 *
	 * @param partId The type ID of the new part
	 * @param keyword The parts keyword
	 * @param editable Whether the part should be editable
	 *
	 * @see {@ Part}
	 * @return A simple part without a default value
	 * @throws NodeException On errors
	 */
	public static Part createSimplePart(int typeId, String keyword, int editable) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Part result = t.createObject(Part.class);

		result.setPartTypeId(typeId);
		result.setKeyname(keyword);
		result.setEditable(editable);

		return result;
	}
}
