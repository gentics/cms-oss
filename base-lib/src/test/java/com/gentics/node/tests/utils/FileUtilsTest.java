package com.gentics.node.tests.utils;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.contentnode.tests.category.BaseLibTest;
import com.gentics.lib.util.FileUtil;
import com.gentics.testutils.GenericTestUtils;

/**
 * Tests for FileUtil.
 *
 */
@Category(BaseLibTest.class)
public class FileUtilsTest {
	/**
	 * Test mimetype detection with various filetypes
	 * @throws Exception
	 */
	@Test
	public void testMimeTypeDetection() throws Exception {
		Map<String, String> fileTypes = new HashMap<String, String>();

		// List of file extensions which we have test files for and their expected mimetypes

		// Documents
		fileTypes.put("doc",  "application/msword");
		fileTypes.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		fileTypes.put("odt",  "application/vnd.oasis.opendocument.text");
		fileTypes.put("pdf",  "application/pdf");
		fileTypes.put("ppt",  "application/vnd.ms-powerpoint");
		fileTypes.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
		fileTypes.put("sxw",  "application/vnd.sun.xml.writer");
		fileTypes.put("txt",  "text/plain");
		fileTypes.put("xls",  "application/vnd.ms-excel");
		fileTypes.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

		// Images
		fileTypes.put("ai",   "application/postscript");
		fileTypes.put("bmp",  "image/bmp");
		fileTypes.put("eps",  "application/postscript");
		fileTypes.put("gif",  "image/gif");
		fileTypes.put("ico",  "image/vnd.microsoft.icon");
		fileTypes.put("jpg",  "image/jpeg");
		fileTypes.put("png",  "image/png");
		fileTypes.put("psd",  "image/vnd.adobe.photoshop");
		fileTypes.put("raw",  "image/x-raw-panasonic");
		fileTypes.put("svg",  "image/svg+xml");
		fileTypes.put("tif",  "image/tiff");

		// Multimedia
		fileTypes.put("avi",  "video/x-msvideo");
		fileTypes.put("flv",  "video/x-flv");
		fileTypes.put("mov",  "video/quicktime");
		fileTypes.put("mp3",  "audio/mpeg");
		fileTypes.put("swf",  "application/x-shockwave-flash");
		fileTypes.put("wav",  "audio/vnd.wave");

		// Other
		fileTypes.put("blihblahblub", "application/octet-stream"); // Random file extension
		fileTypes.put("exe",  "application/x-msdownload; format=pe32");
		fileTypes.put("html", "text/html");
		fileTypes.put("rar",  "application/x-rar-compressed; version=4");
		fileTypes.put("zip",  "application/zip");

		// OpenOffice-generated
		fileTypes.put("oofdf.pdf",  "application/pdf"); // FDF content
		fileTypes.put("oodf.pdf",  "application/pdf"); // OO document content
		fileTypes.put("arc.pdf",  "application/pdf"); // Archive content

		// Loop trough all filetypes now and check if the mimetype matches
		for (Map.Entry<String, String> fileType : fileTypes.entrySet()) {
			String fileName = "file." + fileType.getKey();
			InputStream inputStream = GenericTestUtils.getFileResource(fileName);

			String mimeType = FileUtil.getMimeType(inputStream, fileName);
			//System.out.println("Filename: " + fileName + " Mimetype: " + mimeType);
			assertEquals("Mimetype for " + fileName + " has to match", fileType.getValue(), mimeType);
		}
	}

	/**
	 * Test mimetype detection with file extension with various filetypes
	 * @throws Exception
	 */
	@Test
	public void testMimeTypeDetectionByExtensionOnly() throws Exception {
		Map<String, String> fileTypes = new HashMap<String, String>();

		// List of file extensions which we have test files for and their expected mimetypes

		// Documents
		fileTypes.put("doc",  "application/msword");
		fileTypes.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		fileTypes.put("odt",  "application/vnd.oasis.opendocument.text");
		fileTypes.put("pdf",  "application/pdf");
		fileTypes.put("ppt",  "application/vnd.ms-powerpoint");
		fileTypes.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
		fileTypes.put("sxw",  "application/vnd.sun.xml.writer");
		fileTypes.put("txt",  "text/plain");
		fileTypes.put("xls",  "application/vnd.ms-excel");
		fileTypes.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

		// Images
		fileTypes.put("ai",   "application/postscript"); // Differs from content detection
		fileTypes.put("bmp",  "image/bmp");
		fileTypes.put("eps",  "application/postscript");
		fileTypes.put("gif",  "image/gif");
		fileTypes.put("ico",  "image/x-icon");
		fileTypes.put("jpg",  "image/jpeg");
		fileTypes.put("png",  "image/png");
		fileTypes.put("psd",  "image/vnd.adobe.photoshop");
		fileTypes.put("raw",  "application/octet-stream"); // Differs from content detection
		fileTypes.put("svg",  "image/svg+xml");
		fileTypes.put("tif",  "image/tiff");

		// Multimedia
		fileTypes.put("avi",  "video/x-msvideo");
		fileTypes.put("flv",  "video/x-flv");
		fileTypes.put("mov",  "video/quicktime");
		fileTypes.put("mp3",  "audio/mpeg");
		fileTypes.put("swf",  "application/x-shockwave-flash");
		fileTypes.put("wav",  "audio/x-wav");

		// Other
		fileTypes.put("blihblahblub", "application/octet-stream"); // Random file extension
		fileTypes.put("exe",  "application/x-msdownload"); // Differs slighty from content detection
		fileTypes.put("html", "text/html");
		fileTypes.put("rar",  "application/x-rar-compressed");
		fileTypes.put("zip",  "application/zip");

		// Loop trough all filetypes now and check if the mimetype matches
		for (Map.Entry<String, String> fileType : fileTypes.entrySet()) {
			String fileName = "file." + fileType.getKey();

			String mimeTypeByExtension = FileUtil.getMimeTypeByExtension(fileName);
			//System.out.println("Extension, filename: " + fileName + " Mimetype: " + mimeType);
			assertEquals("Mimetype for " + fileName + " determined by extension has to match", fileType.getValue(), mimeTypeByExtension);
		}
	}

	/**
	 * Test getting a mimetype from an extension
	 * @throws Exception
	 */
	@Test
	public void testGetExtensionByMimeType() {
		assertMimeTypeExtension(".jpg", "image/jpeg");
		assertMimeTypeExtension(".gif", "image/gif");
		assertMimeTypeExtension(".png", "image/png");
		assertMimeTypeExtension(".svg", "image/svg+xml");
		assertMimeTypeExtension(null, "image/invalidmimetype");
	}

	public void assertMimeTypeExtension(String extension, String mimeType) {
		assertEquals("Extension should match", extension, FileUtil.getExtensionByMimeType(mimeType));
	}
}
