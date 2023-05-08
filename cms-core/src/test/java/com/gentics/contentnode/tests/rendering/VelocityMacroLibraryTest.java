package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createVelocityConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.render.velocity.SerializableVelocityTemplateWrapper;

/**
 * Test configuring and using a velocity macro library with a file resource loader
 */
public class VelocityMacroLibraryTest {
	/**
	 * Name of the macro library file
	 */
	public final static String VM_FILENAME = "gtx-vtl-macro-lib.vm";

	/**
	 * Name of the vtl tag
	 */
	public final static String TAG_NAME = "render";

	public final static int THREADS = 10;

	static {
		// Disable cache. This is necessary for the test, which runs garbage collections while the pages are rendered.
		// If the Velocity templates would be cached, they would never be garbage collected
		System.setProperty("com.gentics.portalnode.portalcache", "false");
	}

	/**
	 * VTL configuration has to be provided before the DBTestContext (and with it the Velocity Engine) is initialized
	 */
	@ClassRule
	public static DBTestContext testContext = new DBTestContext().config(map -> {
		try {
			File macroFile = new File(VelocityMacroLibraryTest.class.getResource(VM_FILENAME).toURI());
			Map<String, Object> velocityConfig = new HashMap<>();
			velocityConfig.put("resource.loader", "string,file");
			velocityConfig.put("file.resource.loader.path", macroFile.getParent());
			velocityConfig.put("file.resource.loader.cache", "true");
			velocityConfig.put("file.resource.loader.modificationCheckInterval", 1);
			velocityConfig.put("velocimacro.library", VM_FILENAME);
			map.set("velocity", velocityConfig);
		} catch (URISyntaxException e) {
			throw new NodeException(e);
		}
	});

	private static Node node;

	private static Page page;

	private static Page inlineMacroPage;

	@BeforeClass
	public static void setupOnce() throws NodeException, URISyntaxException, IOException {
		testContext.getContext().getTransaction().commit();
		node = supply(() -> createNode());
		page = createTestPage("#hello()##");
		inlineMacroPage = createTestPage(FileUtils.readFileToString(new File(VelocityMacroLibraryTest.class.getResource("inpage_macro.vm").toURI()), "UTF-8"));
	}

	/**
	 * Create a velocity construct with the given velocity template code, a cms template using the construct and a test page from the template
	 * @param vtlTemplate velocity template code
	 * @return page
	 * @throws NodeException
	 */
	private static Page createTestPage(String vtlTemplate) throws NodeException {
		Construct vtlConstruct = supply(() -> update(createVelocityConstruct(node), c -> {
			c.getValues().getByKeyname("template").setValueText(vtlTemplate);
		}));

		Template template = supply(() -> update(createTemplate(node.getFolder(), "Template"), t -> {
			t.getTemplateTags().put(TAG_NAME, create(TemplateTag.class, tag -> {
				tag.setConstructId(vtlConstruct.getId());
				tag.setEnabled(true);
				tag.setName(TAG_NAME);
				tag.setPublic(false);
			}, false));
			t.setSource("<node " + TAG_NAME + ">");
		}));

		return supply(() -> createPage(node.getFolder(), template, "Page"));
	}

	/**
	 * Return a runnable, which will render the page and check, whether the result
	 * contains a '#' (which would be an error, because this would mean that macros
	 * were not called)
	 * 
	 * @param page         page to be rendered
	 * @param counter      atomic counter for rendered pages
	 * @param errorCounter atomic counter for errors (unresolved macros)
	 * @return runnable
	 */
	private static Runnable render(Page page, AtomicLong counter, AtomicLong errorCounter) {
		return () -> {
			try {
				String renderedPage = supply(() -> {
					try (RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_PREVIEW)) {
						return page.render();
					}
				});

				counter.incrementAndGet();
				if (renderedPage.contains("#")) {
					errorCounter.incrementAndGet();
				}
			} catch (NodeException e) {
				throw new RuntimeException(e);
			}
		};
	}

	/**
	 * Return a runnable which will call System.gc()
	 * 
	 * @param counter atomic counter for invocations
	 * @return runnable
	 */
	private static Runnable runGc(AtomicLong counter) {
		return () -> {
			try {
				System.gc();
				counter.incrementAndGet();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}

	/**
	 * Test rendering the page
	 * @throws Exception
	 */
	@Test
	public void test() throws Exception {
		String renderedPage = supply(() -> {
			try (RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_PREVIEW)) {
				return page.render();
			}
		});

		assertThat(renderedPage).as("Rendered page").isEqualTo("Hello World!");
	}

	/**
	 * Test garbage collection while rendering velocity. This test reproduced the
	 * problem that instances of {@link SerializableVelocityTemplateWrapper}
	 * sometimes were garbage collected while the wrapped template was still
	 * rendered. This cause the template's namespace to be dumped so that inline
	 * macros could not be resolved any more.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGcWhileRendering() throws Exception {
		ScheduledExecutorService service = Executors.newScheduledThreadPool(THREADS + 1);

		LocalTime start = LocalTime.now();
		AtomicLong counter = new AtomicLong();
		AtomicLong errorCounter = new AtomicLong();
		AtomicLong runGcCounter = new AtomicLong();
		Set<ScheduledFuture<?>> futures = new HashSet<>();

		// render pages in multiple threads
		for (int i = 0; i < THREADS; i++) {
			futures.add(service.scheduleWithFixedDelay(render(inlineMacroPage, counter, errorCounter), 0, 1, TimeUnit.MILLISECONDS));
		}

		// run garbage collector
		futures.add(service.scheduleWithFixedDelay(runGc(runGcCounter), 10, 10, TimeUnit.MILLISECONDS));

		Duration dur = Duration.between(start, LocalTime.now());
		while (errorCounter.get() == 0 && dur.toMinutes() < 1) {
			Thread.sleep(1000);
			dur = Duration.between(start, LocalTime.now());
		}

		futures.forEach(future -> future.cancel(false));

 		service.shutdown();
 		service.awaitTermination(1, TimeUnit.SECONDS);

 		if (errorCounter.get() > 0) {
 			fail(String.format("%d pages failed to render correctly", errorCounter.get()));
 		}
	}
}
