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

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.util.FileUtil;

/**
 * Test for preserving filename extensions for FileUtil.sanitizeName(..)
 * @author drainer
 *
 */
@RunWith(Parameterized.class)
@Category(BaseLibTest.class)
public class SanitizeFilenameExtensionTest {

	/**
	 * Name to test.
	 */
	private String name;

	/**
	 * Expected Path name.
	 */
	private String extension;

	/**
	 * Expected file name.
	 */
	private String expectedFileName;

	/**
	 * preserved characters
	 */
	private String[] preservedCharacters;

	/**
	 * Sanitize characters
	 */
	private static Map<String, String> sanitizeCharacters;

	/**
	 * The standard replacement character
	 */
	private String replacementCharacter="_";

	/**
	 * If true, the dot should be replaced with underscore
	 */
	private boolean sanitizeDot;

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
		sanitizeCharacters.put("$", "_");
		sanitizeCharacters.put("|", "A");
		sanitizeCharacters.put("#c#", "©");
		sanitizeCharacters.put(" ", "_");
		sanitizeCharacters.put("{", "_");
	}

	/**
	 * Creates input values for the constructor.
	 *
	 * @return
	 */
	@Parameters(name = "{index}: name \"{0}\", extension \"{1}\", expected \"{2}\", sanitizeDot \"{3}\", preservedCharacters \"{4}\"")
	public static Collection<Object[]> createInputValues() {
		return Arrays.asList(new Object[][] {
				{ " Something. blah .ext$ ", "ext$", "Something__blah_.ext_",true, "" },
				{ " Something .ext$ ", ".ext$", "Something_.ext_",true, "" },
				{ " Something. blah .ext$ ", ".ext$", "Something__blah_.ext_", true, "" },
				{ " Something. blah .ext$ ", null, "Something__blah_.ext_", true, "" },
				{ " Something.blah.ext$.ext$ ", ".ext$.ext$", "Something_blah.ext_.ext_", true, "" },
				{ " Something.blah.ext$.ext$ ", "ext$.ext$", "Something_blah.ext_.ext_", true, "" },
				{ " Something.blah.ext$.ext$ ", ".ext$.ext$", "Something.blah.ext_.ext_", false, "" },
				{ " Something.blah.ext$.ext$ ", "ext$.ext$", "Something.blah.ext_.ext_", false, "" },
				{ " Something.blah.ext$.ext$ ", "", "Something_blah_ext_.ext_", true, "" },
				{ " Something.blah.ext$.ext$ ", null, "Something_blah_ext_.ext_", true, "" },
				{ " Something.blah.ext$.ext$ ", "", "Something.blah.ext_.ext_", false, "" },
				{ " Something.blah.ext$.ext$ ", "", "Something.blah.ext_.ext_", false, "" },
				{ " Something.blah..html", "", "Something.blah..html", false, "" },
				{ " Something.blah..html", "", "Something_blah_.html", true, "" },
				{ " .ext$ ", ".ext$", "1.ext_" ,true, "" },
				{ ".", null, "_", true, "" },
				{ "   .   ", "", "_", true, "" },
				{ "Something.", ".", "Something.", false, "" },
				{ ".hidden.ext", ".ext", "1.hidden.ext", false, "" },
				{ ".hidden..ext", "ext", "1.hidden..ext", false, "" },
				{ ".hidden..ext", ".ext", "1.hidden..ext", false, "" },
				{ ".hidden..ext", "..ext", "1.hidden..ext", false, "" },
				{ "#c# 2014", "", "__2014", false, "" },
				{ "#c# 2014", "", "©_2014", false, "©" },
				{ "龍", "", "龍", false, "龍" },
				{ "{", "", "_", false, "" },
				{ "³,.½¬{[]¼}.³$", ".³$", "_,.½¬_[]¼}.__", false, ",.½¬{[]¼}" }, //fails with jdk < 1.6 execution environment
				{ ",.½¬{[]¼}.³$", "", ",.½¬_[]¼}.__", false, ",.½¬{[]¼}" }, //fails with jdk < 1.6 execution environment
				{ getLongFileName(64) + ".ext.ext", ".ext", getLongFileName(60) + ".ext", true, "" },
				{ getLongFileName(64) + ".ext.ext", ".ext", getLongFileName(60) + ".ext", false, "" },
				{ getLongFileName(64) + ".ext.ext", "ext", getLongFileName(60) + ".ext", true, "" },
				{ getLongFileName(64) + ".ext.ext", "ext", getLongFileName(60) + ".ext", false, "" },
				{ getLongFileName(60) + ".ext.ext", ".ext", getLongFileName(60) + ".ext", true, "" },
				{ getLongFileName(60) + ".ext.ext", ".ext", getLongFileName(60) + ".ext", false, "" },
				{ getLongFileName(60) + ".ext.ext", "ext", getLongFileName(60) + ".ext", true, "" },
				{ getLongFileName(60) + ".ext.ext", "ext", getLongFileName(60) + ".ext", false, "" },
				{ getLongFileName(60) + ".01234576890.01234576890", ".01234576890", getLongFileName(64 - 12) + ".01234576890", false, "" },
				{ "BeCharmed_Wholesale_en.html", "en.html", "BeCharmed_Wholesale_en.html", false, "" },
				{ "BeCharmed_Wholesale_en.html", ".en.html", "BeCharmed_Wholesale_en.html", false, "" }
		});
	}

	/**
	 * Test sanitize filenames with extensions
	 * @param name
	 * @param extension
	 * @param expectedFileName
	 * @param sanitizeDot true if the dot should be sanitized
	 * @param preservedCharacters the characters that schould be preserved
	 */
	public SanitizeFilenameExtensionTest(String name, String extension, String expectedFileName, boolean sanitizeDot, String preservedCharacters) {
		this.name = name;
		this.extension = extension;
		this.expectedFileName = expectedFileName;
		this.sanitizeDot = sanitizeDot;
		if (!ObjectTransformer.isEmpty(preservedCharacters)) {
			this.preservedCharacters = preservedCharacters.split("");
		}
	}

	/**
	 * Tests sanitizing of a file name.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testSanitizeFilename() throws NodeException {
		if (sanitizeDot) {
			if (!sanitizeCharacters.containsKey(".")) {
				sanitizeCharacters.put(".", "_");
			}
		} else {
			if (sanitizeCharacters.containsKey(".")) {
				sanitizeCharacters.remove(".");
			}
		}
		assertEquals(expectedFileName, FileUtil.sanitizeName(name, extension, sanitizeCharacters, replacementCharacter, preservedCharacters));
	}

	/**
	 * Create a long folder name
	 * @return String
	 */
	private static String getLongFileName(int length) {
		StringBuffer pathName = new StringBuffer();
		for (int i = 0; i < length; i++) {
			pathName.append(i % 10);
		}
		return pathName.toString();
	}

}
