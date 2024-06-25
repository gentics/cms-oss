package com.gentics.contentnode.etc;

import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import org.junit.jupiter.api.Test;

class ValidatorTest {

	Validator validator = new JsoupValidator();

	@Test
	void givenValidHtmlMarkup_isValid() throws NodeException {
		final var html = """
				<h1>My blog is great</h1>
				<p>This is some random text</p>
				""";

		assertThat(this.validator.isValid(html)).isTrue();
	}

	@Test
	void givenValidPartialMarkup_isValid() throws NodeException {
		final var html = """
				<div>Some text</div>
				""";

		assertThat(this.validator.isValid(html)).isTrue();
	}


	@Test
	void givenCorruptMarkup_isNotValid() throws NodeException {
		final var html = """
				<!DOCTYPE html>
				<title>Page title</title>
				     
				<h1>My blog is great <!-- something is missing here -->
									
				<p>Some Text</p>
				""";

		assertThat(this.validator.isValid(html)).isFalse();
	}

	@Test
	void givenMessedupMarkup_isNotValid() throws NodeException {
		final var html = """
					<h1>This is my first page</h1></div><div>Here's the introduction.
					</div><div>
				""";

		assertThat(this.validator.isValid(html)).isFalse();
	}

	@Test
	void givenCorruptMarkup_isNotValid2() throws NodeException {
		final var html = """
				<html>
				<title>Page title</title>
				Simple text
									
				<p>Some Text.
				""";

		assertThat(this.validator.isValid(html)).isFalse();
	}

	@Test
	void givenSimpleText_isValid() throws NodeException {
		final var html = "just some plain text";

		assertThat(this.validator.isValid(html)).isTrue();
	}

	@Test
	void givenValidHtmlMarkup_hasNoErrors() throws NodeException {
		final var html = """
				<!DOCTYPE html>
				<title>Page title</title>
				     
				<h1>My blog is great </h1>
				<p>Ich habe einen deutschen Text, den ich übersetzen will</p>
				""";
		var errors = this.validator.validateAndCollectErrors(html);
		assertThat(errors).isEmpty();
	}

	@Test
	void givenMessedupMarkup_hasErrors() throws NodeException {
		final var html = """
					<h1>This is my first page</h1></div><div>Here's the introduction.
					</div><div>
				""";

		var errors = this.validator.validateAndCollectErrors(html);
		assertThat(errors).isNotEmpty();
	}

}