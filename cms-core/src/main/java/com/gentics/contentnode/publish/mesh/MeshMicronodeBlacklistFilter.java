package com.gentics.contentnode.publish.mesh;

/**
 * Blacklist filter. Any keyword matches, if not contained in the blacklist
 */
public class MeshMicronodeBlacklistFilter extends AbstractMeshMicronodeFilter {
	/**
	 * Create an instance for the given keywords
	 * @param keywords keywords
	 */
	public MeshMicronodeBlacklistFilter(String[] keywords) {
		super(keywords);
	}

	@Override
	protected boolean matches(String keyword) {
		return !blackList.contains(keyword);
	}
}
