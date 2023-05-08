package com.gentics.contentnode.tests.rest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.response.FileLoadResponse;
import com.gentics.contentnode.tests.multichannelling.AbstractMultichannellingSandboxTest;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.Creator;

public class FileLoadMCSandboxTest extends AbstractMultichannellingSandboxTest {

	private Node master;
	private Node secondChannel;

	@Test
	public void testLoadFile() throws Exception {
		File fileInMaster = null;
		File fileInSecondChannel = null;
		try (Trx trx = new Trx()) {
			master = ContentNodeTestDataUtils.createNode("Master", "masterhost", "/", null, false, false, false);
			Node firstChannel = ContentNodeTestDataUtils.createChannel(master, "1. Channel", "firstchannel", "/");
			secondChannel = ContentNodeTestDataUtils.createChannel(firstChannel, "2. Channel", "secondchannel", "/");
			fileInMaster = Creator.createFile(master.getFolder(), "Test", "content".getBytes(), master);
			fileInSecondChannel = Creator.createFile(secondChannel.getFolder(), "Test2", "content2".getBytes(), secondChannel);
			trx.success();
		}

		try (Trx trx = new Trx()) {
			FileLoadResponse response = ContentNodeRESTUtils.getFileResource().load(fileInMaster.getId().toString(), false, false, master.getId(), null);
			ContentNodeTestUtils.assertResponseCodeOk(response);
			assertEquals("Master", response.getFile().getInheritedFrom());
			assertEquals(master.getId(), response.getFile().getInheritedFromId());
			assertEquals("The node name did not match.", master.getFolder().getName(), response.getFile().getMasterNode());
			assertEquals("The node master id did not match.", master.getId(), response.getFile().getMasterNodeId());

			response = ContentNodeRESTUtils.getFileResource().load(fileInSecondChannel.getId().toString(), false, false, secondChannel.getId(), null);
			ContentNodeTestUtils.assertResponseCodeOk(response);
			assertEquals("2. Channel", response.getFile().getInheritedFrom());
			assertEquals(secondChannel.getId(), response.getFile().getInheritedFromId());
			assertEquals("The node name did not match.", secondChannel.getFolder().getName(), response.getFile().getMasterNode());
			assertEquals("The node id id did not match.", secondChannel.getId(), response.getFile().getMasterNodeId());
		}
	}

}
