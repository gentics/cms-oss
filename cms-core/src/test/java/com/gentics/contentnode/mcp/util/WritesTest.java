package com.gentics.contentnode.mcp.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.PageResource;

/**
 * Unit tests for {@link Writes}, without a DB/CMS instance
 */
public class WritesTest {
	@Test
	public void testPageInNodeWithoutNodeId() throws Exception {
		Map<String, Object> arguments = Map.of("pageId", 4711);
		assertThat(Writes.pageInNode("publish_page", arguments, "pageId")).isSameAs(arguments);
	}

	@Test
	public void testPageInNodeRejectsInvalidNodeId() {
		assertThatThrownBy(() -> Writes.pageInNode("publish_page", Map.of("pageId", 4711, "nodeId", 0), "pageId"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("nodeId");
		assertThatThrownBy(() -> Writes.pageInNode("publish_page", Map.of("pageId", 0, "nodeId", 1), "pageId"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("pageId");
	}

	@Test
	public void testIdempotencyKeyTooLong() {
		assertThatThrownBy(() -> Writes.logIdempotencyKey("create_page", 1, Map.of("idempotencyKey", "x".repeat(129))))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatCode(() -> Writes.logIdempotencyKey("create_page", 1, Map.of("idempotencyKey", "k")))
				.doesNotThrowAnyException();
	}

	@Test
	public void testReadBackFlags() throws Exception {
		List<Object> args = new ArrayList<>();
		Page page = new Page();
		page.setId(7);
		PageResource resource = (PageResource) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { PageResource.class }, (proxy, method, methodArgs) -> {
					args.addAll(Arrays.asList(methodArgs));
					return new PageLoadResponse(null, new ResponseInfo(ResponseCode.OK, ""), page);
				});

		assertThat(Writes.readBack(resource, 7, null)).isSameAs(page);
		// id, update, template, folder, langvars, pagevars, workflow, translationstatus, versioninfo, disinherited,
		// construct, nodeId, package
		assertThat(args).containsExactly("7", false, false, true, true, false, true, true, true, false, false, null,
				null);
	}

	@Test
	public void testReadBackFailure() {
		PageResource resource = (PageResource) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { PageResource.class },
				(proxy, method, methodArgs) -> new PageLoadResponse(null,
						new ResponseInfo(ResponseCode.NOTFOUND, ""), null));

		assertThatThrownBy(() -> Writes.readBack(resource, 7, null)).hasMessageContaining("Page 7");
	}
}
