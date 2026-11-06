package com.gentics.contentnode.tests.perm;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;

/**
 * Test channel specific permission checks in a non-root folder of the node/channel
 */
public class ChannelNonRootFolderPermissionTest extends AbstractChannelPermissionTest {
	/**
	 * Create a test instance
	 * @param clazz tested class
	 * @param perm tested permission
	 * @param folderSetting folder setting
	 * @param objectSetting object setting
	 */
	public ChannelNonRootFolderPermissionTest(Class<? extends NodeObject> clazz, ObjectPermission perm, ChannelSetting folderSetting,
			ChannelSetting objectSetting) {
		super(clazz, perm, folderSetting, objectSetting);
	}

	/**
	 * Get test parameters
	 * @return test parameters
	 */
	@Parameters(name = "{index}: class {0}, permission: {1}, folder: {2}, object: {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (Class<? extends NodeObject> clazz : Arrays.asList(File.class, Page.class, Template.class, Folder.class)) {
			for (ObjectPermission perm : ObjectPermission.values()) {
				for (ChannelSetting folderSetting : ChannelSetting.values()) {
					for (ChannelSetting objectSetting : ChannelSetting.values()) {
						if (allowCombination(clazz, perm, folderSetting, objectSetting)) {
							data.add(new Object[] {clazz, perm, folderSetting, objectSetting});
						}
					}
				}
			}
		}
		return data;
	}

	/**
	 * Check whether the given parameter combination is allowed
	 * @param clazz tested class
	 * @param perm tested permission
	 * @param folderSetting folder setting
	 * @param objectSetting object setting
	 * @return
	 */
	protected static boolean allowCombination(Class<? extends NodeObject> clazz, ObjectPermission perm, ChannelSetting folderSetting, ChannelSetting objectSetting) {
		if (folderSetting == ChannelSetting.LOCAL && objectSetting != ChannelSetting.LOCAL) {
			return false;
		}
		switch (perm) {
		case publish:
			return clazz.equals(Page.class);
		default:
			return true;
		}
	}

	/**
	 * Test permission check
	 * @throws Exception
	 */
	@Test
	public void testPermissionCheck() throws Exception {
		doPermissionCheck(supply(nodeUser, this::createTestFolder));
	}
}
