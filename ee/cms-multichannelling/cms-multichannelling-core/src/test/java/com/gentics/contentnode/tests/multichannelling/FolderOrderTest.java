package com.gentics.contentnode.tests.multichannelling;


import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.utility.FolderComparator;

/**
 * Test for ordering folders.
 * @author najor
 */
public class FolderOrderTest {

	/**
	 * Tests Name comparator
	 * @throws ReadOnlyException
	 */
	@Test
	public void testNameComparator() throws ReadOnlyException {
		Comparator<Folder> comparator = new FolderComparator("name", "asc");

		List<Folder> folders = createListFolders();
		folders = new Vector<Folder>(folders);
		Collections.sort(folders, comparator);
		
		assertEquals("0", folders.get(0).getName());
		assertEquals("1", folders.get(1).getName());
		assertEquals("5", folders.get(5).getName());
	}
	
	/**
	 * Tests Master Node Comparator
	 * @throws NodeException
	 */
	@Test
	public void testMasterNodeComparator() throws NodeException {
		Comparator<Folder> comparator = new FolderComparator("masterNode", "asc");

		List<Folder> folders = createListFolders();
		folders = new Vector<Folder>(folders);
		Collections.sort(folders, comparator);
		
		assertEquals("Master D", folders.get(0).getMasterNodeFolderName());
		assertEquals("Master D", folders.get(1).getMasterNodeFolderName());
		assertEquals("Master E", folders.get(5).getMasterNodeFolderName());
	}
	
	/**
	 * Create a list of folder
	 * @return
	 * @throws ReadOnlyException
	 */
	private List<Folder> createListFolders() throws ReadOnlyException {
		List<Folder> folders = new ArrayList<Folder>();
		
		folders.add(createFolder("4", "Master D"));
		folders.add(createFolder("0", "Master D"));
		folders.add(createFolder("2", "Master D"));
		folders.add(createFolder("1", "Master E"));
		folders.add(createFolder("5", "Master D"));
		folders.add(createFolder("3", "Master D"));
		
		return folders ;
	}
	
	/**
	 * Creates a folder with name 'name' and master folder name 'masterFolderName'
	 * @param name
	 * @param masterFolderName
	 * @return
	 * @throws ReadOnlyException
	 */
	private Folder createFolder(String name, String masterFolderName) throws ReadOnlyException {
		DummyFolder folder = new DummyFolder(0, null);
		folder.setName(name);
		folder.setMasterNodeName(masterFolderName);
		
		return folder;
	}
}
