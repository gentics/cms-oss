package com.gentics.node.tests.utils;

import static org.junit.Assert.assertEquals;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.apache.commons.io.input.CountingInputStream;
import org.junit.Test;

import com.gentics.testutils.GenericTestUtils;

import fi.iki.santtu.md5.MD5;
import fi.iki.santtu.md5.MD5InputStream;
import org.junit.experimental.categories.Category;

/**
 * This test will test the md5 hash functions and counting tools to determine the byte size of images
 * @author johannes2
 *
 */
@Category(BaseLibTest.class)
public class MD5HashTest {

	/**
	 * Generate a data file that contains the specified string
	 * @param data
	 * @return a stream that was created by using file data
	 * @throws IOException
	 */
	private InputStream generateDataFile(String data) throws IOException {
		FileOutputStream fos;
		DataOutputStream dos;

		java.io.File file = new java.io.File(System.getProperty("java.io.tmpdir"), "TEMP");

		file.delete();

		fos = new FileOutputStream(file);
		dos = new DataOutputStream(fos);
		dos.writeChars(data);
		dos.flush();
		dos.close();

		return new FileInputStream(file);

	}

	/**
	 * Test the counting from inputstream
	 * @throws Exception
	 */
	@Test
	public void testInputStreamCounter() throws Exception {
		InputStream in = GenericTestUtils.getPictureResource("garbage.data");

		MD5InputStream in2 = new MD5InputStream(in);
		CountingInputStream in3 = new CountingInputStream(in2);

		int b;

		while ((b = in3.read()) != -1) {
			;
		}
		String hash = MD5.asHex(in2.hash());

		assertEquals("dfeedb671e6d645ca29e9fc6ba166af2", hash);
		assertEquals(512000, in3.getByteCount());

	}

	/**
	 * Test the md5 hashing function
	 * @throws Exception
	 */
	@Test
	public void testMD5Hashing() throws Exception {
		InputStream in = generateDataFile("Blub");

		MD5InputStream in2 = new MD5InputStream(in);
		int b;

		while ((b = in2.read()) != -1 && b != '\r' && b != '\n') {
			;
		}

		// String hash = MD5.asHex(in2.hash());

		// System.out.println(hash);
	}

	@Test
	public void testMD5HashString() throws Exception {
		String dummyString = "Just a dummy string?";
		String referenceHash = "63e3a041ffd326ba09581a50a2a0e844";
		MD5 md5 = new MD5();

		md5.Init();
		md5.Update(dummyString);
		String hash = md5.asHex();

		assertEquals(referenceHash, hash);
	}

}
