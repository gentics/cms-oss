package com.gentics.contentnode.mcp.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.Test;

import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.filters.RequiredPerm;

/**
 * Unit tests for {@link RestPermissions}. The granted path needs a CMS transaction and is covered by the live check.
 */
public class RestPermissionsTest {
	/**
	 * Main sources of the MCP package, relative to the cms-core module
	 */
	private static final Path MCP_SOURCES = Path.of("src/main/java/com/gentics/contentnode/mcp");

	/**
	 * Construction of a REST resource implementation
	 */
	private static final Pattern NEW_RESOURCE_IMPL = Pattern.compile("new \\w+ResourceImpl\\(");

	/**
	 * Test resource interface
	 */
	public interface TestResource {
		/**
		 * Annotated in the implementation
		 * @return value
		 */
		String restricted();

		/**
		 * Not annotated
		 * @return value
		 */
		String open();

		/**
		 * Not annotated, fails
		 * @return never
		 * @throws Exception always
		 */
		String failing() throws Exception;
	}

	/**
	 * Test resource implementation, recording the calls
	 */
	public static class TestResourceImpl implements TestResource {
		List<String> calls = new ArrayList<>();

		@Override
		@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
		public String restricted() {
			calls.add("restricted");
			return "restricted";
		}

		@Override
		public String open() {
			calls.add("open");
			return "open";
		}

		@Override
		public String failing() throws Exception {
			throw new IOException("delegate failed");
		}
	}

	@Test
	public void testAnnotatedMethodCheckedBeforeDelegate() {
		TestResourceImpl impl = new TestResourceImpl();
		TestResource guarded = RestPermissions.guard(TestResource.class, impl);

		// without a CMS transaction the check cannot be granted, so it fails closed
		assertThatThrownBy(guarded::restricted).isNotNull();
		assertThat(impl.calls).isEmpty();
	}

	@Test
	public void testUnannotatedMethodPassesThrough() {
		TestResourceImpl impl = new TestResourceImpl();

		assertThat(RestPermissions.guard(TestResource.class, impl).open()).isEqualTo("open");
		assertThat(impl.calls).containsExactly("open");
	}

	@Test
	public void testDelegateExceptionUnwrapped() {
		TestResource guarded = RestPermissions.guard(TestResource.class, new TestResourceImpl());

		assertThatThrownBy(guarded::failing).isExactlyInstanceOf(IOException.class).hasMessage("delegate failed");
	}

	@Test
	public void testToolsOnlyUseGuardedResources() throws IOException {
		assertThat(MCP_SOURCES).isDirectory();
		List<String> unguarded = new ArrayList<>();
		try (Stream<Path> files = Files.walk(MCP_SOURCES)) {
			for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
				String source = Files.readString(file);
				Matcher matcher = NEW_RESOURCE_IMPL.matcher(source);
				while (matcher.find()) {
					if (!source.substring(0, matcher.start()).endsWith(".class, ")) {
						unguarded.add("%s: %s".formatted(file.getFileName(), matcher.group()));
					}
				}
			}
		}

		assertThat(unguarded).as("REST resource implementations constructed outside RestPermissions.guard").isEmpty();
	}
}
