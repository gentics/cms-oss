package com.gentics.contentnode.tests.rendering;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.render.RenderType;

/**
 * Resolve tests for the rendertype
 */
@RunWith(value = Parameterized.class)
public class RenderTypeResolveTest extends AbstractRenderTypeTest {
	/**
	 * Map of resolved properties -> objects
	 */
	protected final static Map<String, Object> RESOLVED = new HashMap<>();

	@Parameters(name = "{index}: resolve: {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (String key : Arrays.asList("page", "page.name", "folder", "folder.name", "html")) {
			data.add(new Object[] { key });
		}

		return data;
	}

	@BeforeClass
	public static void setup() throws NodeException {
		AbstractRenderTypeTest.setup();
		Trx.operate(() -> {
			RESOLVED.put("page", page1);
			RESOLVED.put("page.name", page1.getName());
			RESOLVED.put("folder", folder);
			RESOLVED.put("folder.name", folder.getName());
			RESOLVED.put("html", contentTag.getValues().getByKeyname("html"));
		});
	}

	@Parameter(0)
	public String key;

	/**
	 * Test resolving
	 * @throws NodeException
	 */
	@Test
	public void testResolve() throws NodeException {
		RenderType renderType = new RenderType();
		renderType.push(folder);
		renderType.push(page2);
		renderType.push(page1);
		renderType.push(contentTag);

		Object resolvedObject = Trx.supply(() -> {
			TransactionManager.getCurrentTransaction().setRenderType(renderType);
			return renderType.getStack().resolve(key);
		});
		assertEquals("Check resolving '"+key+"'", RESOLVED.get(key), resolvedObject);
	}
}
