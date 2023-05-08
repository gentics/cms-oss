package com.gentics.contentnode.tests.multichannelling;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;
import org.junit.runners.Parameterized;

import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.ChannelInheritanceException;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;

/**
 * Tests visibility and name collisions in the context of multichannelling
 * restrictions (multichanelling exclusion and disinheriting)
 *
 * @author escitalopram
 *
 */
public class MultichannellingRestrictionSandboxTest extends AbstractMultichannellingRestrictionSandboxTest {

	@Parameterized.Parameters(name = "{index} Object: {0} useRootFolder: {1}")
	public static Collection<Object[]> parameters() {
		ArrayList<Object[]> result = new ArrayList<>();
		for (boolean masterFolder : new boolean[] { true, false }) {
			for (DisinheritableObjTypes type : DisinheritableObjTypes.values()) {
				result.add(new Object[] { type, masterFolder });
			}
		}
		return result;
	}

	public MultichannellingRestrictionSandboxTest(DisinheritableObjTypes objType, boolean useRootFolder) {
		super(objType, useRootFolder);
	}

	/**
	 * An object excluded from multichannelling must not exist in subchannels
	 * and its name should be reusable. The master object is created in a
	 * channel node.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testExcludeChannelLocal() throws NodeException {
		createObject(ml);
		excludeObject();
		assertExistsObject(false, ml);
		reuseName(mr);
		reuseName(mll);
		reuseName(m, true);
	}

	/**
	 * An object excluded from multichannelling must not exist in subchannels
	 * and its name should be reusable. The master object is created in the
	 * master node.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testExcludeInMasterNode() throws NodeException {
		createObject(m);
		excludeObject();
		assertExistsObject(false, m);
		reuseName(ml);
		reuseName(mrl);
	}

	/**
	 * A disinherited channel should not see the object and the name should be
	 * reusable. The master object is created in the master node.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testDisinheritInMaster() throws NodeException {
		createObject(m);
		disinheritObject(ml);
		assertExistsObject(false, m, mr, mrl, mrr);
		reuseName(mll);
		reuseName(mlr);
		reuseName(mr, true);
	}

	/**
	 * A disinherited channel should not see the object and the name should be
	 * reusable. The master object is created in a channel node.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testDisinheritChannelLocal() throws NodeException {
		createObject(ml);
		disinheritObject(mll);
		reuseName(mr);
		reuseName(mll);
		reuseName(mlr, true);
	}

	/**
	 * An excluded object that reuses the name of an excluded object should
	 * allow for the name to be reused.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testDoubleReuseExclude() throws NodeException {
		createObject(m);
		excludeObject();
		createObject(ml);
		excludeObject();
		reuseName(mll);
	}

	/**
	 * An object that reuses the name of an object in a disinherited channel,
	 * that in turn disinherits a channel should allow for the name to be reused
	 * in the disinherited channel.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testDoubleReuseDisinherit() throws NodeException {
		createObject(m);
		disinheritObject(ml);
		createObject(ml);
		disinheritObject(mll);
		reuseName(mlll);
	}

	/**
	 * Excluded objects must be listed in their channels.
	 *
	 * @throws InsufficientPrivilegesException
	 * @throws NodeException
	 */
	@Test
	public void testListExcludedInMaster() throws InsufficientPrivilegesException, NodeException {
		createObject(m);
		excludeObject();
		assertListedInFolder(m);
	}

	/**
	 * Excluded objects must be listed in their channels.
	 *
	 * @throws InsufficientPrivilegesException
	 * @throws NodeException
	 */
	@Test
	public void testListExcludedInChannel() throws InsufficientPrivilegesException, NodeException {
		createObject(mr);
		excludeObject();
		assertListedInFolder(mr);
	}

	/**
	 * Disinheriting a channel segment that contains a localization musts fail.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testLocalizationObstructionDisinherit() throws NodeException {
		createObject(m);
		localizeObject(mll);
		try {
			disinheritObject(ml);
			fail("Shouldn't be able to disinherit a channel with a localization.");
		} catch (ChannelInheritanceException e) {
		}
	}

	/**
	 * Excluding an object that has localizations from multichannelling must
	 * fail.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testLocalizationObstructionExclude() throws NodeException {
		createObject(m);
		localizeObject(mll);
		try {
			excludeObject();
			fail("Shouldn't be able to exclude an object with a localization");
		} catch (ChannelInheritanceException e) {
		}
	}

	/**
	 * A newly created object should be visible in its channel and its
	 * subchannels but not in the master node.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testExistence() throws NodeException {
		createObject(ml);
		assertExistsObject(true, m, mr, mrl, mrr);
	}

	/**
	 * Reincluding an object with a downstream object of the same name must fail
	 *
	 * @throws NodeException
	 */
	@Test
	public void testIncludeConflict() throws NodeException {
		createObject(ml);
		excludeObject();
		reuseName(mll);
		try {
			masterObject.changeMultichannellingRestrictions(false, Collections.<Node> emptySet(), false);
			fail("Shoudln't be able to reinclude a channel with an obstructing object with the same name");
		} catch (ChannelInheritanceException e) {
		}
	}

	/**
	 * Reinheriting a channel on an object with a downstream object of the same name must fail
	 *
	 * @throws NodeException
	 */
	@Test
	public void testReinheritConflict() throws NodeException {
		createObject(ml);
		disinheritObject(mll);
		reuseName(mll);
		try {
			masterObject.changeMultichannellingRestrictions(false, Collections.<Node> emptySet(), false);
			fail("Shoudln't be able to reinherit a channel with an obstructing object with the same name");
		} catch (ChannelInheritanceException e) {
		}
	}

	/**
	 * Cannot exclude recursively if there's an obstructing localization
	 * @throws NodeException
	 */
	@Test
	public void testExcludeRecursiveConflict() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		testFolder = Creator.createFolder(testFolder, "x" + name, "/asdf");

		createObject(m);
		localizeObject(ml);

		// reload test folder
		testFolder = t.getObject(Folder.class, testFolder.getId());

		// shouldn't be possible due to localization in ml
		try {
			testFolder.changeMultichannellingRestrictions(true, Collections.<Node> emptySet(), false);
			fail("Excluding a folder with a contained localized object should fail");
		} catch (ChannelInheritanceException e) {
		}
	}

	/**
	 * Cannot disinherit recursively if there's an obstructing localization
	 * @throws NodeException
	 */
	@Test
	public void testDisinheritRecursiveConflict() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		testFolder = Creator.createFolder(testFolder, "x" + name, "/asdf");

		createObject(m);
		localizeObject(ml);

		// reload test folder
		testFolder = t.getObject(Folder.class, testFolder.getId());

		// shouldn't be possible due to localization in ml
		try {
			testFolder.changeMultichannellingRestrictions(false, new HashSet<Node>(Arrays.asList(ml)), false);
			fail("Disinheriting a channel on a folder with a contained localized object should fail");
		} catch (ChannelInheritanceException e) {
		}

	}

	/**
	 * Disinheriting shouldn't be possible if a subobject exists locally in a channel
	 * @throws NodeException
	 */
	@Test
	public void testDisinheritWithObstructingSubchannelObject() throws NodeException {
		testFolder = Creator.createFolder(testFolder, "x"+name, "/");
		createObject(mr);
		try {
			testFolder.changeMultichannellingRestrictions(false, new HashSet<Node>(Arrays.asList(mr)), false);
			fail ("Shouldn't be able to disinherit a channel on a folder with local objects");
		} catch (ChannelInheritanceException e) {
		}
	}

	/**
	 * Excluding shouldn't be possible if a subobject exists locally in a channel
	 * @throws NodeException
	 */
	@Test
	public void testExcludeWithObstructingSubchannelObject() throws NodeException {
		testFolder = Creator.createFolder(testFolder, "x"+name, "/");
		createObject(mrr);
		try {
			testFolder.changeMultichannellingRestrictions(true, Collections.<Node> emptySet(), false);
			fail ("Shouldn't be able to exclude a folder with subobjects local to a channel");
		} catch (ChannelInheritanceException e) {
		}
	}

	/**
	 * Parent folder must not be excluded for include to succeed.
	 * @throws NodeException
	 */
	@Test
	public void testIncludeWithParentfolderExcluded() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		testFolder = Creator.createFolder(testFolder, "x"+name, "/");
		testFolder.changeMultichannellingRestrictions(true, Collections.<Node> emptySet(), false);
		testFolder = t.getObject(Folder.class, testFolder.getId());
		createObject(m);
		t.commit(false);
		assertExistsObject(false, m);
		try {
			reincludeObject();
			fail ("Shouldn't be able to include an object with its parent folder excluded");
		} catch (ChannelInheritanceException e) {
		}
	}

	/**
	 * Parent folder must not disinherit a channel for reinheriting it to succeed.
	 * @throws NodeException
	 */
	@Test
	public void testReinheritWithParentfolderDisinherited() throws NodeException {
		testFolder = Creator.createFolder(testFolder, "x"+name, "/");
		testFolder.changeMultichannellingRestrictions(false, new HashSet<>(Arrays.asList(ml)), false);
		createObject(m);
		assertExistsObject(false, m, mr, mrr, mrl);
		try {
			disinheritObject();
			fail ("Shouldn't be able to reinherit an object with a disinherited parent folder");
		} catch (ChannelInheritanceException e) {
		}
	}

	/**
	 * Inheriting must be possible, even if folder is localized in the channel
	 * @throws Exception
	 */
	@Test
	public void testReinheritWithParentfolderLocalized() throws Exception {
		testFolder = Creator.createFolder(testFolder, "x" + name, "/");
		createObject(m);
		assertExistsObject(true);
		disinheritObject(mlll);
		assertExistsObject(true, mlll);
		ContentNodeTestDataUtils.localize(testFolder, mlll);
		disinheritObject();
		assertExistsObject(true);
	}

	/**
	 * Reincluding should not be possible with the parentfolder disinherited.
	 * @throws NodeException
	 */
	@Test
	public void testIncludeWithParentfolderDisinherited() throws NodeException {
		testFolder = Creator.createFolder(testFolder, "x"+name, "/");
		testFolder.changeMultichannellingRestrictions(false, new HashSet<>(Arrays.asList(ml)), false);
		createObject(m);
		excludeObject();
		assertExistsObject(false, m);
		try {
			reincludeObject();
			fail ("Shouldn't be able to reinclude an object with a disinherited parent folder");
		} catch (ChannelInheritanceException e) {
		}
	}

	/**
	 * Reincluding must be possible, even if folder is localized
	 * @throws Exception
	 */
	@Test
	public void testIncludeWithParentfolderLocalized() throws Exception {
		testFolder = Creator.createFolder(testFolder, "x" + name, "/");
		createObject(m);
		assertExistsObject(true);
		excludeObject();
		assertExistsObject(false, m);
		ContentNodeTestDataUtils.localize(testFolder, mlll);
		reincludeObject();
		assertExistsObject(true);
	}

	/**
	 * It should not be possible to localize an excluded object
	 * @throws NodeException
	 */
	@Test
	public void testLocalizeExcludedObject() throws NodeException {
		createObject(m);
		excludeObject();
		try {
			localizeObject(ml);
			fail("Shouldn't be able to localize excluded object");
		} catch (ChannelInheritanceException e) {
		}
	}

	/**
	 * It should not be possible to localize an object in a disinherited channel.
	 * @throws NodeException
	 */
	@Test
	public void testLocalizeDisinheritedObject() throws NodeException {
		createObject(ml);
		disinheritObject(mll);
		try {
			localizeObject(mllr);
			fail("Shouldn't be able to localize excluded object");
		} catch (ChannelInheritanceException e) {
		}
	}

	/**
	 * It should be possible to localize a disinheriting object in a non-disinherited channel
	 * @throws NodeException
	 */
	@Test
	public void testAllowedLocalizeDisinheritedObject() throws NodeException {
		createObject(m);
		disinheritObject(mr);
		localizeObject(mll);
	}
	/**
	 * Objects in folders should be disinherited recursively
	 * @throws NodeException
	 */
	@Test
	public void testDisinheritRecursive() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		testFolder = Creator.createFolder(testFolder, "x" + name, "/asdf");

		createObject(m);

		// reload test folder
		testFolder = t.getObject(Folder.class, testFolder.getId());

		// shouldn't be possible due to localization in ml
		testFolder.changeMultichannellingRestrictions(false, new HashSet<Node>(Arrays.asList(ml)), false);

		// reload object
		masterObject = (Disinheritable<?>) t.getObject(masterObject.getObjectInfo().getObjectClass(), masterObject.getId());

		assertTrue("Object must be disinherited recursively", masterObject.getDisinheritedChannels().contains(ml));
	}

	/**
	 * Objects in folders should be excluded recursively
	 * @throws NodeException
	 */
	@Test
	public void testExcludeRecursive() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		testFolder = Creator.createFolder(testFolder, "x" + name, "/asdf");

		createObject(m);

		// reload test folder
		testFolder = t.getObject(Folder.class, testFolder.getId());

		// shouldn't be possible due to localization in ml
		testFolder.changeMultichannellingRestrictions(true, Collections.<Node> emptySet(), false);

		// reload object
		masterObject = (Disinheritable<?>) t.getObject(masterObject.getObjectInfo().getObjectClass(), masterObject.getId());

		assertTrue("Object must be excluded recursively", masterObject.isExcluded());
	}

}
