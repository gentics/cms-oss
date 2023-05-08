package com.gentics.contentnode.tests.rest.proxy;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

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
