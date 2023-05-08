package com.gentics.contentnode.tests.parttype;

import static org.junit.Assert.assertEquals;

import java.util.regex.Pattern;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueContainer;
import com.gentics.contentnode.object.parttype.TextPartType;
import com.gentics.contentnode.rest.model.Property.Type;

public class TextPartTypeTest {
	
	@Test
	public void testEmptyString() throws NodeException {
		convertsLikePrev("");
	}
	
	@Test
	public void testNoConversionOfSimpleString() throws NodeException {
		convertsLikePrev("x");
	}
	
	@Test
	public void testSingleNewline() throws NodeException {
		convertsLikePrev("\n");
	}
	
	@Test
	public void testDoubleNewline() throws NodeException {
		convertsLikePrev("\n\n");
	}
	
	@Test
	public void testNewlineInTheMiddle() throws NodeException {
		convertsLikePrev("some\ntext");
	}
	
	@Test
	public void testNewlineAtTheEnd() throws NodeException {
		convertsLikePrev("sometext\n");
	}
	
	@Test
	public void testCarriageReturnsAreStripped() throws NodeException {
		convertsLikePrev("some\r\ntext\rwith\rcarriage\rreturns");
	}
	
	@Test
	public void testwhitespaceBeforeNewline() throws NodeException {
		convertsLikePrev("sometext\n    \t\t  \n   \n\t\nsometext  \n");
	}
	
	@Test
	public void testSingleTagWithNewline() throws NodeException {
		convertsLikePrev("<anyTag someAttr=''/>\n");
	}
	
	@Test
	public void testMultipleTagsSomeWithNewline() throws NodeException {
		convertsLikePrev("<anyTag someAttr=''/><anotherTag someAttr='<node tag>\n'>\n</anotherTag>\n");
	}
	
	@Test
	public void testTagsAfterWhichBrsAreInserted() throws NodeException {
		convertsLikePrev("sometext<b>\nsomtext</b>\nsometext<strong></strong>\n<i>\n<font>\n<div>\n<span>\n<br>\n<node>\n");
	}

	@Test
	public void testNewlinesInNestedTags() throws NodeException {
		String expected = "some<tag \n\n with='<node\n tag>' \n>text";
		String actual = textnl2br(expected);

		assertEquals("Newlines inside tags are only converted upto the first" + " closing angle bracket,", "some<tag \n\n with='<node\n tag>'<br />\n>text",
				actual);
	}
	
	@Test
	public void testNewlineInBrTag() throws NodeException {
		String actual = textnl2br("<span \n class=''>\n");
		
		assertEquals("Newlines in tags should be left untouched," + " newlines after tags should be processed normally,", "<span \n class=''><br />\n", actual);
	}

	@Test
	public void testWhitespaceBeforeNewlinesInTag() throws NodeException {
		String expected = "<node \n sometag\t  \n";
		String actual = textnl2br(expected);

		assertEquals("Newlines in tags should be left untouched," + " even if the tag is unclosed", expected, actual);
	}

	@Test
	public void testStrippedNewlineJoinsAttributes() throws NodeException {
		String expected = "<node attr1 \nattr2='value2' />";
		String actual = textnl2br(expected);

		assertEquals("Newlines in tags should be left untouched," + " and contrary to the old behaviour, shouldn't join" + " attributes together", expected,
				actual);
	}

	@Test
	public void testBrTagInNonBrTag() throws NodeException {
		String markup = "<img src=\"/GenticsImageStore/<node width>/<node height>/prop/<node img>\" />\n";

		convertsLikePrev(markup);
	}

	@Test
	public void testTagStartsWithBrNameButIsNotBrItself() throws NodeException {
		// <b> is a br tag <body> starts with b, but must not be seen as a br tag
		
		String expected = "<body>\n</body>\n";
		String actual = textnl2br(expected);

		assertEquals("Body tags aren't br tags,", expected, actual);
	}
	
	public void convertsLikePrev(String str) throws NodeException {
		String result = textnl2br(str);
		String expected = textnl2br_previous(str);

		assertEquals("the new newline-to-br transformation doesn't behave as expected,", expected, result);
	}
	
	private String textnl2br(String text) throws NodeException {
		return new MockTextPartType(text).getText();
	}

	/**
	 * This is an implementation of the {@link TextPartType#textnl2br(String)}
	 * method, before rewriting it to use no regular expressions.
	 * 
	 * The behaviour of this method will be tested against, so that we can be
	 * sure not to change the way content is rendered.
	 * 
	 * @param str string to be transformed
	 * @return converted string
	 */
	private String textnl2br_previous(String str) {
        
		str = str.replaceAll("\r", "");
        
		str = str.replaceAll(" *\n", "\n");
		str = str.replaceAll("(<[^>]*)\n", "$1");
        
		String[] brTags = new String[8];

		brTags[0] = "b";
		brTags[1] = "strong";
		brTags[2] = "i";
		brTags[3] = "font";
		brTags[4] = "div";
		brTags[5] = "span";
		brTags[6] = "br";
		brTags[7] = "node";
        
		String regexp = "";

		for (int i = 0; i < brTags.length; i++) {
			regexp += "(<" + brTags[i] + "[^>]*>)|(</" + brTags[i] + ">)|";
		}
		regexp = regexp.substring(0, regexp.length() - 1);
        
		str = Pattern.compile("(" + regexp + ")\n", Pattern.CASE_INSENSITIVE).matcher(str).replaceAll("$1<br />\n");
		str = str.replaceAll("(?<!(>))\n", "<br />\n");
        
		return str;
	}
	
	@SuppressWarnings("serial")
	private static class MockTextPartType extends TextPartType {
		public MockTextPartType(String value) throws NodeException {
			super(new MockValue(value), TextPartType.REPLACENL_EXTENDEDNL2BR);
		}

		@Override
		public Type getPropertyType() {
			return Type.STRING;
		}
	}
	
	@SuppressWarnings("serial")
	private static class MockValue extends Value {
		private String value;
		
		public MockValue(String value) {
			super(null, null);
			this.value = value;
		}
		
		@Override
		public String getValueText() {
			return value;
		}

		@Override
		public ValueContainer getContainer() throws NodeException {
			return null;
		}

		@Override
		public int getInfo() {
			return 0;
		}

		@Override
		public Part getPart(boolean checkForNull) throws NodeException {
			return null;
		}

		@Override
		public Integer getPartId() {
			return null;
		}

		@Override
		public int getValueRef() {
			return 0;
		}

		@Override
		public boolean isStatic() {
			return false;
		}

		@Override
		protected void performDelete() throws NodeException {}

		public NodeObject copy() throws NodeException {
			return null;
		}
	}
}
