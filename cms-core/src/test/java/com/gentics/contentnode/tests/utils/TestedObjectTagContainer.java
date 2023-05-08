package com.gentics.contentnode.tests.utils;

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.ObjectTagContainer;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.lib.util.FileUtil;
import com.gentics.testutils.GenericTestUtils;

public enum TestedObjectTagContainer {
	folder(Folder.TYPE_FOLDER),
	page(Page.TYPE_PAGE),
	file(File.TYPE_FILE),
	image(ImageFile.TYPE_IMAGE),
	template(Template.TYPE_TEMPLATE);

	/**
	 * TType
	 */
	protected int ttype;

	/**
	 * Create an instance with given ttype
	 * @param ttype ttype
	 */
	private TestedObjectTagContainer(int ttype) {
		this.ttype = ttype;
	}

	/**
	 * Get the ttype
	 * @return ttype
	 */
	public int getTType() {
		return ttype;
	}

	/**
	 * Create an instance of the tested type
	 * @param folder where to create the object
	 * @param template template used to create pages
	 * @return instance
	 * @throws NodeException
	 */
	public ObjectTagContainer create(Folder folder, Template template) throws NodeException {
		return create(folder, null, template);
	}

	/**
	 * Create an instance of the tested type
	 * @param folder where to create the object
	 * @param name optional name
	 * @param template template used to create pages
	 * @return instance
	 * @throws NodeException
	 */
	public ObjectTagContainer create(Folder folder, String name, Template template) throws NodeException {
		switch (this) {
		case folder:
			return ContentNodeTestDataUtils.createFolder(folder, ObjectTransformer.getString(name, "Folder"));
		case page:
			return ContentNodeTestDataUtils.createPage(folder, template, ObjectTransformer.getString(name, "Page"));
		case file:
			return ContentNodeTestDataUtils.createFile(folder, ObjectTransformer.getString(name, "testfile.txt"), "Contents".getBytes());
		case image:
			try {
				ByteArrayOutputStream data = new ByteArrayOutputStream();
				FileUtil.inputStreamToOutputStream(GenericTestUtils.getPictureResource("blume.jpg"), data);
				return ContentNodeTestDataUtils.createImage(folder, ObjectTransformer.getString(name, "blume.jpg"), data.toByteArray());
			} catch (IOException e) {
				throw new NodeException(e);
			}
		case template:
			return ContentNodeTestDataUtils.createTemplate(folder, ObjectTransformer.getString(name, "Template"));
		default:
			fail("Cannot generate object of unknown type " + this);
			return null;
		}
	}

	/**
	 * Reload the object from the given transaction
	 * @param instance instance
	 * @param editable true for getting an editable instance
	 * @return fresh instance
	 * @throws NodeException
	 */
	public ObjectTagContainer reload(ObjectTagContainer instance, boolean editable) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		switch (this) {
		case folder:
			return t.getObject((Folder)instance, editable);
		case page:
			return t.getObject((Page)instance, editable);
		case file:
			return t.getObject((File)instance, editable);
		case image:
			return t.getObject((ImageFile)instance, editable);
		case template:
			return t.getObject((Template)instance, editable);
		default:
			fail("Cannot edit object of unknown type " + this);
			return null;
		}
	}
}
