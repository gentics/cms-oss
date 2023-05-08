/**
 *
 */
package com.gentics.node.tests.utils;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.util.FileUtil;

/**
 * Tests for FileUtil.
 *
 */
@RunWith(Parameterized.class)
@Category(BaseLibTest.class)
public class FileUtilsSanitizeTest {

	/**
	 * Name to test.
	 */
	private String name;

	/**
	 * Expected Path name.
	 */
	private String expectedPathName;

	/**
	 * Expected file name.
	 */
	private String expectedFileName;

	/**
	 * Sanitize characters
	 */
	private static Map<String, String> sanitizeCharacters;

	/**
	 * The standard replacement character
	 */
	private String replacementCharacter="_";

	@BeforeClass
	public static void setUpBeforeClass() {
		sanitizeCharacters = new HashMap<String, String>();

		sanitizeCharacters.put("ä", "ae");
		sanitizeCharacters.put("ü", "ue");
		sanitizeCharacters.put("ö", "oe");
		sanitizeCharacters.put("Ü", "Ue");
		sanitizeCharacters.put("Ä", "Ae");
		sanitizeCharacters.put("Ö", "Oe");
		sanitizeCharacters.put("ß", "ss");
		sanitizeCharacters.put("/", "s");
		sanitizeCharacters.put("|", "A");
		sanitizeCharacters.put("&", "1");

	}

	/**
	 * Creates input values for the constructor.
	 * @return
	 */
	@Parameters
	public static Collection<Object[]> createInputValues() {
		return Arrays.asList(new Object[][] {
				{ "/new/folder", "/new/folder", "snewsfolder" },
				{ getLongFolderName(FileUtil.MAX_PATH_LENGTH + 20) + ".de", getLongFolderName(FileUtil.MAX_PATH_LENGTH), getLongFolderName(FileUtil.MAX_FILE_NAME_LENGTH).replaceAll("/", "s").substring(0, FileUtil.MAX_FILE_NAME_LENGTH - 3) + ".de" },
				{ "ABCDEFGHI." + getLongFolderName(FileUtil.MAX_PATH_LENGTH + 20), "ABCDEFGHI." + getLongFolderName(FileUtil.MAX_PATH_LENGTH - 10), "ABCDEFGHI." + getLongFolderName(FileUtil.MAX_FILE_NAME_LENGTH - 10).replaceAll("/", "s") },
				{ "/äö/äï/name my", "/aeoe/ae_/name_my", "saeoesae_sname_my" },
				{ "/SSSS", "/SSSS", "sSSSS" },
				{ "/SS|SS", "/SSASS", "sSSASS" },
				{ "/SS|S S", "/SSAS_S", "sSSAS_S" },
				{ "/great.name.and-more.txt$", "/great.name.and-more.txt$", "sgreat.name.and-more.txt$" },
				{ "my- filename.jpg", "my-_filename.jpg", "my-_filename.jpg" },
				{ "my(f)i[l]e$name,.jpg;", "my(f)i[l]e$name_.jpg_", "my(f)i[l]e$name_.jpg_" },
				{ "my{filename.docx", "my{filename.docx", "my{filename.docx" },
				{ "     ", "1", "1" },
				{ ".hidden", "1.hidden", "1.hidden" } ,
				{ "..hid.den", "1..hid.den", "1..hid.den" },
				{ ".hidd.en", "1.hidd.en", "1.hidd.en" },
				{ ".", "1.", "1." },
				{ "/äö/äï/name####my####", "/aeoe/ae_/name_my_", "saeoesae_sname_my_" },
				{ "/äö/äï/name    my    ", "/aeoe/ae_/name_my", "saeoesae_sname_my" }
			});
	}

	/**
	 * Constructs the test with the parameter entries.
	 * @param name Name to test
	 * @param expectedPathName Expected Path name
	 * @param exptectedFileName Expected File name
	 */
	public FileUtilsSanitizeTest(String name, String expectedPathName, String exptectedFileName) {
		this.name = name;
		this.expectedPathName = expectedPathName;
		this.expectedFileName = exptectedFileName;
	}

	/**
	 * Tests sanitizing of folder path.
	 * @throws NodeException
	 */
	@Test
	public void testSanitizeFolderPath() throws NodeException {
		assertEquals(expectedPathName, FileUtil.sanitizeFolderPath(name, sanitizeCharacters, replacementCharacter, null));
	}

	/**
	 * Create a long folder name
	 *
	 * @return String
	 */
	private static String getLongFolderName(int length) {
		StringBuffer pathName = new StringBuffer();
		for (int i = 0; i < length; i++) {
			pathName.append(i % 10);
		}
		return pathName.toString().replaceAll("0", "/");
	}

	/**
	 * Tests sanitizing of a file name.
	 * @throws NodeException
	 */
	@Test
	public void testSanitizeFilename() throws NodeException {
		assertEquals(expectedFileName, FileUtil.sanitizeName(name, sanitizeCharacters, replacementCharacter, null));
	}
}
