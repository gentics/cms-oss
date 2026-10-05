package com.gentics.contentnode.mcp.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.mcp.util.SearchUnavailableException;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Unit tests for {@link Problem}: the mapping of every failure the tools see to the problem types
 * of the contract's error mapping.
 */
public class ProblemTest {
	private static final String TOOL = "update_page_properties";

	@Test
	public void testLockedPage() {
		Problem problem = Problem.of(TOOL, new ReadOnlyException(
				"Could not lock {Page 7} for user {3}, since it is locked for user {5}", "page.readonly.locked",
				"Home"));

		assertProblem(problem, "object-locked", 423, true);
		assertThat(problem.detail()).contains("locked for user {5}");
		assertThat(problem.cms()).isNull();
	}

	@Test
	public void testLockedTemplateAndForm() {
		assertProblem(Problem.of(TOOL, new ReadOnlyException("x", "template.readonly.locked", "T")), "object-locked",
				423, true);
		assertProblem(Problem.of(TOOL, new ReadOnlyException("x", "form.readonly.locked", "F")), "object-locked", 423,
				true);
	}

	@Test
	public void testSearchUnavailable() {
		Problem problem = Problem.of(TOOL, new InvocationTargetException(
				new SearchUnavailableException("The search module is not installed", new ClassNotFoundException())));

		assertProblem(problem, "search-unavailable", 503, false);
		assertThat(problem.detail()).isEqualTo("The search module is not installed");
		assertThat(problem.cms()).isNull();
	}

	@Test
	public void testLockWrappedInInvocationTargetException() {
		Exception cause = new InvocationTargetException(
				new ReadOnlyException("locked", "page.readonly.locked", "Home"));

		assertProblem(Problem.of(TOOL, cause), "object-locked", 423, true);
	}

	@Test
	public void testOtherReadOnly() {
		Problem problem = Problem.of(TOOL, new ReadOnlyException("Workflow of the page does not allow to edit the page",
				"page.readonly.workflow"));

		assertProblem(problem, "permission-denied", 403, false);
		assertThat(problem.cms().responseCode()).isEqualTo(ResponseCode.PERMISSION);
	}

	@Test
	public void testReadOnlyWithoutMessageKey() {
		assertProblem(
				Problem.of(TOOL, new ReadOnlyException("Object instance {Page 7} is readonly and cannot be modified")),
				"permission-denied", 403, false);
	}

	@Test
	public void testInsufficientPrivileges() {
		Problem problem = Problem.of(TOOL, new InsufficientPrivilegesException("No permission to edit page 7", null,
				(List<String>) null, 10007, 7, PermType.update));

		assertProblem(problem, "permission-denied", 403, false);
		assertThat(problem.detail()).isEqualTo("No permission to edit page 7");
		assertThat(problem.cms().responseCode()).isEqualTo(ResponseCode.PERMISSION);
	}

	@Test
	public void testEntityNotFound() {
		Problem problem = Problem.of(TOOL, new EntityNotFoundException("Page 7 not found"));

		assertProblem(problem, "not-found", 404, false);
		assertThat(problem.cms().responseCode()).isEqualTo(ResponseCode.NOTFOUND);
	}

	@Test
	public void testRestMappedPerResponseCode() {
		assertRestMapped(ResponseCode.PERMISSION, "permission-denied", 403, false);
		assertRestMapped(ResponseCode.NOTFOUND, "not-found", 404, false);
		assertRestMapped(ResponseCode.INVALIDDATA, "invalid-data", 422, false);
		assertRestMapped(ResponseCode.LOCKED, "object-locked", 423, true);
		assertRestMapped(ResponseCode.MAINTENANCEMODE, "maintenance-mode", 503, true);
		assertRestMapped(ResponseCode.NOTLICENSED, "feature-not-licensed", 403, false);
		assertRestMapped(ResponseCode.FAILURE, "cms-failure", 500, true);
		assertRestMapped(ResponseCode.OK, "cms-failure", 500, true);
		assertRestMapped(ResponseCode.AUTHREQUIRED, "cms-failure", 500, true);
	}

	@Test
	public void testRestMappedMessages() {
		GenericResponse response = new GenericResponse(
				new Message(Message.Type.CRITICAL, "name", "The name is already in use."),
				new ResponseInfo(ResponseCode.INVALIDDATA, "Duplicate name"));

		Problem problem = Problem.of(TOOL, new IllegalArgumentException(
				"Page 7 was not saved: The name is already in use.", new RestMappedException(response)));

		assertProblem(problem, "invalid-data", 422, false);
		assertThat(problem.detail()).isEqualTo("The name is already in use.");
		assertThat(problem.cms().responseCode()).isEqualTo(ResponseCode.INVALIDDATA);
		assertThat(problem.cms().messages())
				.containsExactly(new Problem.CmsMessage(Message.Type.CRITICAL, "name", "The name is already in use."));
	}

	@Test
	public void testRestMappedWithoutMessages() {
		Problem problem = Problem.of(TOOL, new RestMappedException(new GenericResponse(null,
				new ResponseInfo(ResponseCode.NOTFOUND, "Could not find workflow of page 7"))));

		assertProblem(problem, "not-found", 404, false);
		assertThat(problem.detail()).isEqualTo("Could not find workflow of page 7");
	}

	@Test
	public void testRestMappedWithoutResponseInfo() {
		assertProblem(Problem.of(TOOL, new RestMappedException(new GenericResponse())), "cms-failure", 500, true);
	}

	@Test
	public void testInvalidArgument() {
		Problem problem = Problem.of(TOOL, new IllegalArgumentException("Argument 'priority' must be at most 100"));

		assertProblem(problem, "invalid-data", 422, false);
		assertThat(problem.detail()).isEqualTo("Argument 'priority' must be at most 100");
		assertThat(problem.cms()).isNull();
	}

	@Test
	public void testUnexpectedFailure() {
		Problem problem = Problem.of(TOOL, new NodeException("Database is gone"));

		assertProblem(problem, "cms-failure", 500, true);
		assertThat(problem.detail()).isEqualTo("Database is gone");
	}

	@Test
	public void testFailureWithoutMessage() {
		Problem problem = Problem.of(TOOL, new NullPointerException());

		assertProblem(problem, "cms-failure", 500, true);
		assertThat(problem.detail()).isEqualTo("CMS failure");
	}

	@Test
	public void testSpecificFailureWinsOverInvalidArgument() {
		Exception cause = new IllegalArgumentException("wrapped", new EntityNotFoundException("Page 7 not found"));

		assertProblem(Problem.of(TOOL, cause), "not-found", 404, false);
	}

	@Test
	public void testNoStackTrace() throws Exception {
		Exception cause = new RuntimeException("boom", new IllegalStateException("inner"));

		String json = new ObjectMapper().writeValueAsString(Problem.of(TOOL, cause));

		assertThat(json).doesNotContain("\tat ").doesNotContain("java.lang.");
	}

	@Test
	public void testSerialization() throws Exception {
		String json = new ObjectMapper()
				.writeValueAsString(Problem.of(TOOL, new EntityNotFoundException("Page 7 not found")));

		assertThat(json).isEqualTo("{\"type\":\"https://cms.gentics.com/problems/not-found\","
				+ "\"title\":\"Not found\",\"status\":404,\"detail\":\"Page 7 not found\","
				+ "\"tool\":\"update_page_properties\",\"retryable\":false,\"cms\":{\"responseCode\":\"NOTFOUND\"}}");
	}

	private static void assertRestMapped(ResponseCode code, String slug, int status, boolean retryable) {
		GenericResponse response = new GenericResponse(null, new ResponseInfo(code, "message"));

		assertProblem(Problem.of(TOOL, new RestMappedException(response)), slug, status, retryable);
	}

	private static void assertProblem(Problem problem, String slug, int status, boolean retryable) {
		assertThat(problem.type()).isEqualTo(Problem.TYPE_PREFIX + slug);
		assertThat(problem.status()).isEqualTo(status);
		assertThat(problem.retryable()).isEqualTo(retryable);
		assertThat(problem.tool()).isEqualTo(TOOL);
		assertThat(problem.title()).isNotBlank();
		assertThat(problem.detail()).isNotBlank();
	}
}
