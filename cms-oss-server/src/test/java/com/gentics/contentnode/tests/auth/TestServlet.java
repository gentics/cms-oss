package com.gentics.contentnode.tests.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestServlet extends HttpServlet {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1269551028048894117L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.getWriter().println(req.getParameter("sid"));
	}
}
