package com.gentics.contentnode.tests.message;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.messaging.Message;

/**
 * Test cases for parsing messages.
 */
@RunWith(Parameterized.class)
public class MessageParsingSandboxTest extends AbstractMessagingSandboxTest {
	private String message;

	private String expected;

	@Parameters
	public static Collection<String[]> getMessages() throws Exception {
		return Arrays.asList(new String[] { "<userid 1>", "Gentics .Node"}, new String[] { "<userid 2>", "Gentics Support"},
				new String[] { "<userid 1>, <userid 2>", "Gentics .Node, Gentics Support"}, new String[] { "prefix<userid 1>", "prefixGentics .Node"},
				new String[] { "<userid 1>suffix", "Gentics .Nodesuffix"}, new String[] { "prefix<userid 1>suffix", "prefixGentics .Nodesuffix"},
				new String[] { "prefix<userid 1> <userid 2>suffix", "prefixGentics .Node Gentics Supportsuffix"}, new String[] { "<userid 999999999>", "--"},
				new String[] { "<pageid 1>", "DirtingTests/Target/Pages/SimplePage (1)"},
				new String[] {
			"<pageid 1> <pageid 2>",
			"DirtingTests/Target/Pages/SimplePage (1) DirtingTests/Target/Pages/Target-Files[Garbage.data]/Target[Garbage.data].name (2)"},
				new String[] { "<pageid 999999999>", "--"},
				new String[] {
			"<userid 1> möchte die Seite <pageid 1> veröffentlichen.",
			"Gentics .Node möchte die Seite DirtingTests/Target/Pages/SimplePage (1) veröffentlichen."},
				new String[] {
			"|<userid 2>|<pageid 1>|<userid 1>|<pageid 2>|",
			"|Gentics Support|DirtingTests/Target/Pages/SimplePage (1)|Gentics .Node|DirtingTests/Target/Pages/Target-Files[Garbage.data]/Target[Garbage.data].name (2)|"});
	}

	/**
	 * Create an instance
	 * @param message message to parse
	 * @param expected expected parsed message
	 */
	public MessageParsingSandboxTest(String message, String expected) {
		this.message = message;
		this.expected = expected;
	}

	@Test
	public void testParse() throws Exception {
		Message msg = new Message(1, 1, message);

		assertEquals("Check parsed message of " + message, expected, msg.getParsedMessage());
	}
}
