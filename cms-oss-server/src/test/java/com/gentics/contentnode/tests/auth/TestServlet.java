package com.gentics.contentnode.tests.auth;

import java.io.IOException;
import java.util.stream.Stream;

import org.apache.commons.lang3.Strings;

import com.gentics.contentnode.factory.SessionToken;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class TestServlet extends HttpServlet {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1269551028048894117L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String sessionSecret = Stream.of(req.getCookies()).filter(c -> Strings.CI.equals(c.getName(), SessionToken.SESSION_SECRET_COOKIE_NAME))
				.findFirst().map(Cookie::getValue).orElse("");
		resp.getWriter().println(sessionSecret);
	}
}
