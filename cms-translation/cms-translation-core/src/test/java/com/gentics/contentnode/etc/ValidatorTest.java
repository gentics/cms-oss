package com.gentics.contentnode.etc;

import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import java.util.regex.Pattern;
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


	@Test
	void givenCleanedMarkup_hasNoErrors() throws NodeException {
		final Pattern NODE_TAGS_PATTERN = Pattern.compile("(<node [a-z]+\\d*>)",
				Pattern.CASE_INSENSITIVE);
		var html = """
				<h1>Eine Inhaltsseite</h1><p>Diese Seite hat externe <node gtxalohapagelink1>.</p><p>Es sollen auch Inhalte von <node gtxalohapagelink2> übersetzt werden können:</p><p><br/></p><table><tbody><tr><td><lt-highlighter contenteditable="false" data-lt-linked="1" style="display: none; z-index: 100000 !important;"><lt-div spellcheck="false" class="lt-highlighter__wrapper" style="width: 28.4531px !important; height: 18px !important; transform: none !important; transform-origin: 14.2266px 9px !important; zoom: 1 !important;"><lt-div class="lt-highlighter__scroll-element" style="top: 0px !important; left: 0px !important; width: 28px !important; height: 18px !important;"></lt-div></lt-div></lt-highlighter>Eins</td><td><lt-highlighter contenteditable="false" data-lt-linked="1" style="display: none; z-index: 100000 !important;"><lt-div spellcheck="false" class="lt-highlighter__wrapper" style="width: 32.875px !important; height: 18px !important; transform: none !important; transform-origin: 16.4375px 9px !important; zoom: 1 !important;"><lt-div class="lt-highlighter__scroll-element" style="top: 0px !important; left: 0px !important; width: 32.875px !important; height: 18px !important;"></lt-div></lt-div></lt-highlighter>Zwei</td><td><lt-highlighter contenteditable="false" data-lt-linked="1" style="display: none; z-index: 100000 !important;"><lt-div spellcheck="false" class="lt-highlighter__wrapper" style="width: 37.3281px !important; height: 18px !important; transform: none !important; transform-origin: 18.6641px 9px !important; zoom: 1 !important;"><lt-div class="lt-highlighter__scroll-element" style="top: 0px !important; left: 0px !important; width: 37px !important; height: 18px !important;"></lt-div></lt-div></lt-highlighter>Drei</td></tr><tr><td><lt-highlighter contenteditable="false" data-lt-linked="1" style="display: none; z-index: 100000 !important;"><lt-div spellcheck="false" class="lt-highlighter__wrapper" style="width: 28.4531px !important; height: 18px !important; transform: none !important; transform-origin: 14.2266px 9px !important; zoom: 1 !important;"><lt-div class="lt-highlighter__scroll-element" style="top: 0px !important; left: 0px !important; width: 28px !important; height: 18px !important;"></lt-div></lt-div></lt-highlighter>Vier</td><td><lt-highlighter contenteditable="false" data-lt-linked="1" style="display: none; z-index: 100000 !important;"><lt-div spellcheck="false" class="lt-highlighter__wrapper" style="width: 32.875px !important; height: 18px !important; transform: none !important; transform-origin: 16.4375px 9px !important; zoom: 1 !important;"><lt-div class="lt-highlighter__scroll-element" style="top: 0px !important; left: 0px !important; width: 32.875px !important; height: 18px !important;"></lt-div></lt-div></lt-highlighter>Fünf</td><td><lt-highlighter contenteditable="false" data-lt-linked="1" style="display: none; z-index: 100000 !important;"><lt-div spellcheck="false" class="lt-highlighter__wrapper" style="width: 37.3281px !important; height: 18px !important; transform: none !important; transform-origin: 18.6641px 9px !important; zoom: 1 !important;"><lt-div class="lt-highlighter__scroll-element" style="top: 0px !important; left: 0px !important; width: 37px !important; height: 18px !important;"></lt-div></lt-div></lt-highlighter>Sechs</td></tr></tbody></table><p><br/></p><p>Auch Bilder sollen eingefügt werden können:d</p><node manimage2><p><br/></p><node mantitle1><p><br/></p><p>Hier steht der Footer</p>
						""";
		var cleanHtml = html.replaceAll("(<node [a-z]+[0-9]*>)", "");

		var errors = this.validator.validateAndCollectErrors(cleanHtml);
		assertThat(errors).isEmpty();
	}
}