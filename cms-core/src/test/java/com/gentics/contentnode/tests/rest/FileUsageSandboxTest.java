package com.gentics.contentnode.tests.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.rest.model.response.TotalUsageInfo;
import com.gentics.contentnode.rest.model.response.TotalUsageResponse;
import com.gentics.contentnode.rest.resource.FileResource;
import com.gentics.contentnode.rest.resource.ImageResource;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.ImageResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;

public class FileUsageSandboxTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static final int PAGE_ID = 45;

	public static final int FILE_ID = 34;

	public static final int TEMPLATE_ID = 81;

	public static final int IMAGE_ID = 33;

	public static final int FOLDER_ID = 70;

	public static final int FILE_URL_CONSTRUCT = 11;

	/**
	 * Assert that various dependencies to a file are correctly counted.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTotalCountForFile() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		ContentFile file = t.getObject(ContentFile.class, FILE_ID);
		testUsage(file);
	}

	/**
	 * Assert that various dependencies to an image are correctly counted.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTotalCountForImage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		ImageFile image = t.getObject(ImageFile.class, IMAGE_ID);
		testUsage(image);
	}

	private void testUsage(File file) throws Exception {
		ContentNodeTestDataUtils.createObjectPropertyDefinition(Page.TYPE_PAGE, FILE_URL_CONSTRUCT, "Page File URL", "fileurl");
		ContentNodeTestDataUtils.createObjectPropertyDefinition(Folder.TYPE_FOLDER, FILE_URL_CONSTRUCT, "Folder File URL", "fileurl");
		ContentNodeTestDataUtils.createObjectPropertyDefinition(ContentFile.TYPE_FILE, FILE_URL_CONSTRUCT, "File File URL", "fileurl");
		ContentNodeTestDataUtils.createObjectPropertyDefinition(ContentFile.TYPE_IMAGE, FILE_URL_CONSTRUCT, "Image File URL", "fileurl");
		ContentNodeTestDataUtils.createObjectPropertyDefinition(Template.TYPE_TEMPLATE, FILE_URL_CONSTRUCT, "Template File URL", "fileurl");

		// No reference
		TotalUsageResponse response = fetchFromRest(file);
		TotalUsageInfo firstInfo = response.getInfos().values().iterator().next();
		assertEquals("The file should not be referenced anywhere.", 0, firstInfo.getTotal());

		// Page reference
		addFileReferenceToPage(file);
		response = fetchFromRest(file);
		firstInfo = response.getInfos().values().iterator().next();
		assertEquals("The file should have one reference.", 1, firstInfo.getTotal());
		assertEquals("The file should be referenced by one page.", 1, firstInfo.getPages().intValue());

		// Folder reference
		addFileReferenceToFolder(file);
		response = fetchFromRest(file);
		firstInfo = response.getInfos().values().iterator().next();
		assertEquals("The file should now have two references.", 2, firstInfo.getTotal());
		assertEquals("The file should be referenced by one folder.", 1, firstInfo.getFolders().intValue());
		assertEquals("The file should be referenced by one page.", 1, firstInfo.getPages().intValue());

		// Template reference
		addFileReferenceToTemplate(file);
		response = fetchFromRest(file);
		firstInfo = response.getInfos().values().iterator().next();
		assertEquals("The file should have one reference.", 3, firstInfo.getTotal());
		assertEquals("The file should be referenced by one template.", 1, firstInfo.getTemplates().intValue());
		assertEquals("The file should be referenced by one page.", 1, firstInfo.getPages().intValue());
		assertEquals("The file should be referenced by one folder.", 1, firstInfo.getFolders().intValue());

		// Image reference
		addFileReferenceToImage(file);
		response = fetchFromRest(file);
		firstInfo = response.getInfos().values().iterator().next();
		assertEquals("The file should have one reference.", 4, firstInfo.getTotal());
		assertEquals("The file should be referenced by one image.", 1, firstInfo.getImages().intValue());
		assertEquals("The file should be referenced by one template.", 1, firstInfo.getTemplates().intValue());
		assertEquals("The file should be referenced by one page.", 1, firstInfo.getPages().intValue());
		assertEquals("The file should be referenced by one folder.", 1, firstInfo.getFolders().intValue());

		// File reference
		addFileReferenceToFile(file);
		response = fetchFromRest(file);
		firstInfo = response.getInfos().values().iterator().next();
		assertEquals("The file should have one reference.", 5, firstInfo.getTotal());
		assertEquals("The file should be referenced by one image.", 1, firstInfo.getImages().intValue());
		assertEquals("The file should be referenced by one template.", 1, firstInfo.getTemplates().intValue());
		assertEquals("The file should be referenced by one page.", 1, firstInfo.getPages().intValue());
		assertEquals("The file should be referenced by one file.", 1, firstInfo.getFiles().intValue());
		assertEquals("The file should be referenced by one folder.", 1, firstInfo.getFolders().intValue());
	}

	/**
	 * Query the file specific resource {@link ImageResource} for images and
	 * {@link FileResource} for files.
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	private TotalUsageResponse fetchFromRest(File file) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		TotalUsageResponse response;
		if (file.isImage()) {
			ImageResourceImpl resource = new ImageResourceImpl();
			resource.setTransaction(t);
			response = resource.getTotalFileUsageInfo(Arrays.asList(file.getId()), file.getNode().getId());
		} else {
			FileResourceImpl resource = new FileResourceImpl();
			resource.setTransaction(t);
			response = resource.getTotalUsageInfo(Arrays.asList(file.getId()), file.getNode().getId());
		}
		ContentNodeTestUtils.assertResponseCodeOk(response);
		return response;
	}

	private void addFileReferenceToPage(File file) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page page = t.getObject(Page.class, PAGE_ID, true);
		ObjectTag sourceTag = page.getObjectTags().get("fileurl");
		addReferenceToTag(sourceTag, file);
		page.save();
		t.commit(false);
	}

	private void addFileReferenceToFolder(File file) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder folder = t.getObject(Folder.class, FOLDER_ID, true);
		ObjectTag sourceTag = folder.getObjectTags().get("fileurl");
		addReferenceToTag(sourceTag, file);
		folder.save();
		t.commit(false);
	}

	private void addFileReferenceToTemplate(File file) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Template template = t.getObject(Template.class, TEMPLATE_ID, true);
		ObjectTag sourceTag = template.getObjectTags().get("fileurl");
		addReferenceToTag(sourceTag, file);
		template.save();
		t.commit(false);
	}

	private void addFileReferenceToImage(File file) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		ImageFile image = t.getObject(ImageFile.class, IMAGE_ID, true);
		ObjectTag sourceTag = image.getObjectTags().get("fileurl");
		addReferenceToTag(sourceTag, file);
		image.save();
		t.commit(false);
	}

	private void addFileReferenceToFile(File file) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		File depFile = t.getObject(File.class, FILE_ID, true);
		ObjectTag sourceTag = depFile.getObjectTags().get("fileurl");
		addReferenceToTag(sourceTag, file);
		depFile.save();
		t.commit(false);
	}

	private void addReferenceToTag(ObjectTag sourceTag, File file) throws NodeException {
		sourceTag.setEnabled(true);
		assertNotNull("Check whether object tag exists", sourceTag);
		PartType sourcePartType = sourceTag.getValues().iterator().next().getPartType();

		if (sourcePartType instanceof FileURLPartType) {
			((FileURLPartType) sourcePartType).setTargetFile(file);
		} else {
			fail("Type is expected to be " + FileURLPartType.class + ", but was " + sourcePartType.getClass() + " instead");
		}
	}
}
