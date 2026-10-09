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
	public void testRejectNodeId() {
		assertThatThrownBy(() -> Writes.rejectChannel("create_page", Map.of("nodeId", 2)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("create_page does not yet support nodeId (multichannelling); call it without nodeId, "
						+ "or wait for that support to be added.");
	}

	@Test
	public void testRejectChannelId() {
		assertThatThrownBy(() -> Writes.rejectChannel("translate_page", Map.of("channelId", 2)))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("support channelId");
	}

	@Test
	public void testNoChannel() {
		assertThatCode(() -> Writes.rejectChannel("create_page", Map.of("folderId", 2))).doesNotThrowAnyException();
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

		assertThat(Writes.readBack(resource, 7)).isSameAs(page);
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

		assertThatThrownBy(() -> Writes.readBack(resource, 7)).hasMessageContaining("Page 7");
	}
}
