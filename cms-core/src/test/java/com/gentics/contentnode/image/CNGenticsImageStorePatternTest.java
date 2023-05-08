package com.gentics.contentnode.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class CNGenticsImageStorePatternTest {
	/**
	 * Tested URL
	 */
	protected String url;

	/**
	 * Expected host
	 */
	protected String host;

	/**
	 * Expected image URL
	 */
	protected String imageUrl;

	/**
	 * Expected transform
	 */
	protected String transform;

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: URL {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();

		for (List<String> host : Arrays.asList(Arrays.asList("https://www.gentics.com", "www.gentics.com"),
				Arrays.asList("http://www.gentics.com", "www.gentics.com"), Arrays.asList("//www.gentics.com", "www.gentics.com"), Arrays.asList("", null))) {
			for (String width : Arrays.asList("123", "auto")) {
				for (String height : Arrays.asList("456", "auto")) {
					for (String mode : Arrays.asList("prop", "force", "smart", "cropandresize/prop/10/10/20/20", "cropandresize/force/10/10/20/20",
							"cropandresize/smart/10/10/20/20")) {
						String transform = width + "/" + height + "/" + mode;
						for (String imageUrl : Arrays.asList("/image.jpg", "/image", "/path/to/my/image.jpg", "/path/to/my/image")) {
							String url = host.get(0) + "/GenticsImageStore/" + transform + imageUrl;
							data.add(new Object[] { url, host.get(1), imageUrl, transform });
						}
					}
				}
			}
		}
		return data;
	}

	/**
	 * Create a test instance
	 * @param url url to test
	 * @param host expected host
	 * @param imageUrl expected image URL
	 * @param transform expected transform part
	 */
	public CNGenticsImageStorePatternTest(String url, String host, String imageUrl, String transform) {
		this.url = url;
		this.host = host;
		this.imageUrl = imageUrl;
		this.transform = transform;
	}

	@Test
	public void testMatch() throws Exception {
		Matcher m = CNGenticsImageStore.SANE_IMAGESTORE_URL_PATTERN.matcher(url);
		assertTrue("URL does not match", m.matches());

		assertEquals("Check host", host, m.group("host"));
		assertEquals("Check imageUrl", imageUrl, m.group("imageurl"));
		assertEquals("Check transform", transform, m.group("transform"));
	}
}
