package com.gentics.contentnode.tests.multichannelling;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.ChannelTreeSegment;
import com.gentics.contentnode.object.Node;

public class ChannelTreeSegmentSandboxTest extends AbstractMultichannellingRestrictionSandboxTest {

	@Parameterized.Parameters(name = "{index} Object: {0} useRootFolder: {1}")
	public static Collection<Object[]> parameters() {
		ArrayList<Object[]> result = new ArrayList<>();
		result.add(new Object[] { DisinheritableObjTypes.FILE, false });
		return result;
	}

	public ChannelTreeSegmentSandboxTest(DisinheritableObjTypes objType, boolean useRootFolder) {
		super(objType, useRootFolder);
	}

	/**
	 * Asserts that a channel tree segment only contains exactly the specified nodes and nothing else.
	 * @param seg the segment to check
	 * @param nodes the nodes to look for
	 * @throws NodeException
	 */
	protected static void assertContainedNodes(ChannelTreeSegment seg, Node... nodes) throws NodeException {
		Node[] expected = nodes.clone();
		Node[] actual = seg.getAllNodes().toArray(new Node[] {});
		Comparator<Node> nodeComparator = new Comparator<Node>() {

			@Override
			public int compare(Node arg0, Node arg1) {
				int s = arg0.getId();
				int o = arg1.getId();
				if (s < o) {
					return -1;
				}
				if (s > o) {
					return 1;
				}
				return 0;
			}
		};
		Arrays.sort(expected, nodeComparator);
		Arrays.sort(actual, nodeComparator);
		assertArrayEquals("Incorrect contained nodes", expected, actual);
	}

	@Test
	public void testSegmentDisinheriting() throws NodeException {
		createObject(m);
		disinheritObject(mlll, mlr);
		localizeObject(mll);

		ChannelTreeSegment seg = getSegment(ml);
		assertContainedNodes(seg, m, ml, mr, mrl, mrr);

		ChannelTreeSegment seg2 = new ChannelTreeSegment(masterObject, true);
		assertContainedNodes(seg2, m, ml, mll, mllr, mr, mrl, mrr);
	}

	@Test
	public void testSegmentExclusion() throws NodeException {
		createObject(m);
		excludeObject();

		ChannelTreeSegment seg = getSegment(m);
		assertContainedNodes(seg, m);
	}
}
