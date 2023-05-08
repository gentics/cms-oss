package com.gentics.contentnode.tests.feature.niceurl;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.TreeSet;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectWithAlternateUrls;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.tests.utils.TestedType;
import com.gentics.testutils.GenericTestUtils;

/**
 * Abstract base class for nice URL and alternate URL tests
 */
public abstract class AbstractNiceURLTest {
	/**
	 * Create tested object with given nice URL
	 * @param type tested type
	 * @param folder folder
	 * @param template template
	 * @param niceUrl nice URL
	 * @return tested object
	 * @throws NodeException
	 */
	protected NodeObjectWithAlternateUrls createObjectWithNiceUrl(TestedType type, Folder folder, Template template, String niceUrl) throws NodeException {
		NodeObjectWithAlternateUrls object = null;
		switch (type) {
		case page:
			object = supply(() -> create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder.getId());
				p.setName("Testpage");
				p.setNiceUrl(niceUrl);
			}));
			break;
		case file:
			object = supply(() -> create(File.class, f -> {
				f.setFolderId(folder.getId());
				f.setName("testfile.txt");
				f.setFileStream(new ByteArrayInputStream("contents".getBytes()));
				f.setNiceUrl(niceUrl);
			}));
			break;
		case image:
			object = supply(() -> create(ImageFile.class, f -> {
				f.setFolderId(folder.getId());
				f.setName("testfile.txt");
				f.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
				f.setNiceUrl(niceUrl);
			}));
			break;
		default:
			fail("Cannot run test with type " + type);
		}
		return object;
	}

	/**
	 * Create an object with the given filename
	 * @param type object type
	 * @param folder folder
	 * @param template template
	 * @param filename filename
	 * @return object
	 * @throws NodeException
	 */
	protected NodeObject createObjectWithFilename(TestedType type, Folder folder, Template template, String filename) throws NodeException {
		NodeObject object = null;
		switch (type) {
		case page:
			object = create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder.getId());
				p.setName("Page1");
				p.setFilename(filename);
			});
			break;
		case file:
			object = supply(() -> create(File.class, f -> {
				f.setFolderId(folder.getId());
				f.setName(filename);
				f.setFileStream(new ByteArrayInputStream("contents".getBytes()));
			}));
			break;
		case image:
			object = supply(() -> create(ImageFile.class, f -> {
				f.setFolderId(folder.getId());
				f.setName(filename);
				f.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
			}));
			break;
		default:
			fail("Cannot run test with type " + type);
		}

		return object;
	}

	/**
	 * Create tested object with the given alternate URLs
	 * @param type object type
	 * @param folder folder
	 * @param template template
	 * @param alternateUrls alternate URLs
	 * @return object
	 * @throws NodeException
	 */
	protected NodeObjectWithAlternateUrls createObjectWithAlternateUrls(TestedType type, Folder folder, Template template, String...alternateUrls) throws NodeException {
		NodeObjectWithAlternateUrls object = null;
		switch (type) {
		case page:
			object = supply(() -> create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder.getId());
				p.setName("Testpage");
				p.setAlternateUrls(alternateUrls);
			}));
			break;
		case file:
			object = supply(() -> create(File.class, f -> {
				f.setFolderId(folder.getId());
				f.setName("testfile.txt");
				f.setFileStream(new ByteArrayInputStream("contents".getBytes()));
				f.setAlternateUrls(alternateUrls);
			}));
			break;
		case image:
			object = supply(() -> create(ImageFile.class, f -> {
				f.setFolderId(folder.getId());
				f.setName("testfile.txt");
				f.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
				f.setAlternateUrls(alternateUrls);
			}));
			break;
		default:
			fail("Cannot run test with type " + type);
		}
		return object;
	}

	/**
	 * Assert that the version of the page has the expected alternate URLs
	 * @param page page
	 * @param timestamp version timestamp
	 * @param expected expected alternate URLs
	 * @throws NodeException
	 */
	protected void assertPageVersionAlternateUrls(Page page, int timestamp, String... expected) throws NodeException {
		consume(p -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page version = t.getObject(Page.class, p.getId(), timestamp);
			assertThat(version).as("Page @" + timestamp).hasFieldOrPropertyWithValue("alternateUrls",
					new TreeSet<>(Arrays.asList(expected)));
		}, page);
	}
}
