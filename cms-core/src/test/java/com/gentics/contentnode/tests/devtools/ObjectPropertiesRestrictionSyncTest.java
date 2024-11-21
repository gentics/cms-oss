package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.operate;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Operator;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.NodeObjectHandler;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for importing object property definitions with different restriction settings
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.DEVTOOLS })
public class ObjectPropertiesRestrictionSyncTest extends AbstractObjectPropertiesSyncTest {
	protected static Node otherNode;

	protected Node deletedNode;

	@BeforeClass
	public static void additionalSetupOnce() throws NodeException {
		otherNode = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
	}

	/**
	 * Get test parameters
	 * @return test parameters
	 */
	@Parameters(name = "{index}: type {0}, new {1}, restricted in package {2}, to node {3}, to deleted node {4}, restricted in CMS {5}, to node {6}, to other node {7}, assign package to node {8}, assign package to other node {9}, assign package before import {10}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (int type : Arrays.asList(Folder.TYPE_FOLDER, Page.TYPE_PAGE, Template.TYPE_TEMPLATE, ImageFile.TYPE_IMAGE,
				File.TYPE_FILE)) {
			for (boolean asNew : Arrays.asList(true, false)) {
				for (boolean restrictedInPackage : Arrays.asList(true, false)) {
					for (boolean toNode : Arrays.asList(true, false)) {
						if (!restrictedInPackage && toNode) {
							continue;
						}
						for (boolean toDeletedNode : Arrays.asList(true, false)) {
							if (!restrictedInPackage && toDeletedNode) {
								continue;
							}
							for (boolean restrictedInCMS : Arrays.asList(true, false)) {
								if (restrictedInCMS && asNew) {
									continue;
								}
								for (boolean toNodeInCMS : Arrays.asList(true, false)) {
									if (!restrictedInCMS && toNodeInCMS) {
										continue;
									}
									for (boolean toOtherNode : Arrays.asList(true, false)) {
										if (!restrictedInCMS && toOtherNode) {
											continue;
										}
										for (boolean assignPackageToNode : Arrays.asList(true, false)) {
											for (boolean assignPackageToOtherNode : Arrays.asList(true, false)) {
												for (boolean assignPackageBeforeImport : Arrays.asList(true, false)) {
													if (assignPackageBeforeImport && !assignPackageToNode && !assignPackageToOtherNode) {
														continue;
													}
													data.add(new Object[] { type, asNew, restrictedInPackage, toNode,
															toDeletedNode, restrictedInCMS, toNodeInCMS, toOtherNode,
															assignPackageToNode, assignPackageToOtherNode, assignPackageBeforeImport });
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
//		data.add(new Object[] {Folder.TYPE_FOLDER, true, true, true, true, false, false, false, true, true, false});
		return data;
	}

	/**
	 * Tested object type
	 */
	@Parameter(0)
	public int type;

	/**
	 * Sync as new object
	 */
	@Parameter(1)
	public boolean asNew;

	/**
	 * Restricted in package
	 */
	@Parameter(2)
	public boolean restrictedInPackage;

	/**
	 * Whether the object property definition is restricted to the node (in the package)
	 */
	@Parameter(3)
	public boolean toNode;

	/**
	 * Whether the object property definition is restricted to the deleted node (in the package)
	 */
	@Parameter(4)
	public boolean toDeletedNode;

	/**
	 * Restricted in CMS
	 */
	@Parameter(5)
	public boolean restrictedInCMS;

	/**
	 * Whether the object property definition is restricted to the node in the CMS
	 */
	@Parameter(6)
	public boolean toNodeInCMS;

	/**
	 * Whether the object property definition is restricted to the other node in the CMS
	 */
	@Parameter(7)
	public boolean toOtherNode;

	/**
	 * Whether the package is assigned to the node
	 */
	@Parameter(8)
	public boolean assignPackageToNode;

	/**
	 * Whether the package is assigned to the other node
	 */
	@Parameter(9)
	public boolean assignPackageToOtherNode;

	/**
	 * Whether to assign the package to the node(s) before the import
	 */
	@Parameter(10)
	public boolean assignPackageBeforeImport;

	@Before
	public void setup() throws NodeException {
		super.setup();

		if (toDeletedNode) {
			deletedNode = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		}

		if (assignPackageToNode && assignPackageBeforeImport) {
			operate(() -> Synchronizer.addPackage(node, PACKAGE_NAME));
		} else {
			operate(() -> Synchronizer.removePackage(node, PACKAGE_NAME));
		}
		if (assignPackageToOtherNode && assignPackageBeforeImport) {
			operate(() -> Synchronizer.addPackage(otherNode, PACKAGE_NAME));
		} else {
			operate(() -> Synchronizer.removePackage(otherNode, PACKAGE_NAME));
		}
	}

	@After
	public void teardown() throws NodeException {
		super.teardown();

		if (deletedNode != null) {
			consume(Node::delete, deletedNode);
			deletedNode = null;
		}
	}

	@Test
	public void test() throws NodeException {
		NodeObjectHandler<ObjectTagDefinition> prepare = prop -> {
			prop.setRestricted(restrictedInPackage);
			if (toNode) {
				prop.getNodes().add(node);
			}
			if (toDeletedNode) {
				prop.getNodes().add(deletedNode);
			}
		};

		NodeObjectHandler<ObjectTagDefinition> change = prop -> {
			prop.setRestricted(restrictedInCMS);
			if (toNodeInCMS) {
				prop.getNodes().add(node);
			}
			if (toOtherNode) {
				prop.getNodes().add(otherNode);
			}
		};

		Operator beforeSync = () -> {
			if (deletedNode != null) {
				consume(Node::delete, deletedNode);
				deletedNode = null;
			}
		};

		Operator afterSync = () -> {
			if (assignPackageToNode && !assignPackageBeforeImport) {
				operate(() -> Synchronizer.addPackage(node, PACKAGE_NAME));
			}
			if (assignPackageToOtherNode && !assignPackageBeforeImport) {
				operate(() -> Synchronizer.addPackage(otherNode, PACKAGE_NAME));
			}
		};

		Operator asserter = () -> {
			Trx.operate(() -> {
				// we assert that the object property definition is restricted, if it is restricted in the package
				assertThat(objectProperty).as("Synchronized object property").isNotNull()
						.hasFieldOrPropertyWithValue("restricted", restrictedInPackage);
				if (restrictedInPackage) {
					Set<Node> expectedNodes = new HashSet<>();
					// object property definition is expected to be restricted to the node, if
					// the restriction is stored in the package
					// or the package is assigned to the node
					// or the property definition was already assigned to the node
					if (toNode || assignPackageToNode || (restrictedInCMS && toNodeInCMS)) {
						expectedNodes.add(node);
					}
					// object property definition is expected to be restricted to the other node, if
					// the package is assigned to the other node
					// or the property definition was already assigned to the other node
					if (toOtherNode || assignPackageToOtherNode) {
						expectedNodes.add(otherNode);
					}

					assertThat(objectProperty.getNodes()).as("Restricted nodes").containsOnlyElementsOf(expectedNodes);
				} else {
					assertThat(objectProperty.getNodes()).as("Restricted nodes").isEmpty();
				}
			});
		};

		syncTest(type, prepare, asNew, change, beforeSync, afterSync, asserter);
	}
}
