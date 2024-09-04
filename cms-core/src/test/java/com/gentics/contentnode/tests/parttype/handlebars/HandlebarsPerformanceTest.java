package com.gentics.contentnode.tests.parttype.handlebars;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Stopwatch;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.object.parttype.handlebars.HelperSource;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.cache.ConcurrentMapTemplateCache;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.helper.StringHelpers;

/**
 * Performance test for repeatedly parsing of a template (with cache)
 */
@RunWith(value = Parameterized.class)
public class HandlebarsPerformanceTest {
	/**
	 * Template source. The actually parsed template will contain this source multiple times
	 */
	public static String templateSource;

	/**
	 * Maximum allowed parse time in ms
	 */
	protected final static long tolerableParseDurationMs = 1000;

	@Parameters(name = "{index}: parse: {0}, repeat {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (int parse : Arrays.asList(1, 1000, 10000)) {
			for (int repeat : Arrays.asList(1, 10, 100)) {
				data.add(new Object[] {parse, repeat });
			}
		}
		return data;
	}

	@BeforeClass
	public final static void setupOnce() throws IOException {
		try (InputStream in = HandlebarsPerformanceTest.class.getResourceAsStream("template.hbs")) {
			templateSource = IOUtils.toString(in, "UTF-8");
		}
	}

	@Rule
	public Stopwatch stopWatch = new Stopwatch();

	/**
	 * How often will the template be parsed
	 */
	@Parameter(0)
	public int parse;

	/**
	 * How often is the template source repeated in the template (i.e. how large is the template)
	 */
	@Parameter(1)
	public int repeat;

	protected Handlebars handlebars;

	protected String template;

	@Before
	public void setup() {
		handlebars = new Handlebars().infiniteLoops(true).with(new ConcurrentMapTemplateCache().setReload(true));
		handlebars.registerHelpers(ConditionalHelpers.class);
		handlebars.registerHelpers(StringHelpers.class);
		handlebars.registerHelpers(HelperSource.class);

		template = StringUtils.repeat(templateSource, repeat);
	}

	@Test
	public void testParseTemplate() throws IOException {
		for (int i = 0; i < parse; i++) {
			handlebars.compileInline(template);
		}
		assertThat(stopWatch.runtime(TimeUnit.MILLISECONDS)).as("Test runtime (in ms)").isLessThanOrEqualTo(tolerableParseDurationMs);
	}
}
