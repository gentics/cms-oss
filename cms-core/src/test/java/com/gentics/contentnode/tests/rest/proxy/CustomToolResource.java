package com.gentics.contentnode.tests.rest.proxy;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

import fi.iki.santtu.md5.MD5;

@Path("tool")
public class CustomToolResource {
	@GET
	public String root() {
		return "Root Path";
	}

	@Path("hello")
	@GET
	public String hello() {
		return "Hello World!";
	}

	@Path("hello")
	@PUT
	public String put() {
		return "PUT should not be allowed";
	}

	@Path("headers")
	@GET
	public String headers(@QueryParam("return") List<String> returnedHeaders, @Context HttpHeaders httpHeaders) {
		return returnedHeaders.stream().map(header -> httpHeaders.getHeaderString(header)).collect(Collectors.joining(","));
	}

	@Path("queryParams")
	@GET
	public String queryParams(@Context UriInfo uriInfo) {
		return uriInfo.getQueryParameters().entrySet().stream().filter(entry -> !"sid".equals(entry.getKey()))
				.map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue())).collect(Collectors.joining(","));
	}

	@Path("post")
	@POST
	public String post(String postedData) {
		return MD5.asHex(postedData.getBytes());
	}

	@Path("variable/{var}/path")
	@GET
	public String variable(@PathParam("var") String var) {
		return var;
	}

	@Path("path/{path: .*}")
	@GET
	public String path(@PathParam("path") String path) {
		return path;
	}
}
