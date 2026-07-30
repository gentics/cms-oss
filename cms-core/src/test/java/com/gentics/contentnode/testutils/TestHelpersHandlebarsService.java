package com.gentics.contentnode.testutils;

import com.gentics.contentnode.render.HandlebarsService;
import com.github.jknack.handlebars.Handlebars;

public class TestHelpersHandlebarsService implements HandlebarsService {

	@Override
	public void registerHelpers(Handlebars handlebars) {
		System.out.println("bla");
	}

}
