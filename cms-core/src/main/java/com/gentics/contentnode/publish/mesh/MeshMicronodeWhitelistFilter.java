package com.gentics.contentnode.publish.mesh;

/**
 * Whitelist filter. Any keyword matches, if contained in the whitelist.
 */
public class MeshMicronodeWhitelistFilter extends AbstractMeshMicronodeFilter {
	/**
	 * Create instance
	 * @param keywords keywords
	 */
	public MeshMicronodeWhitelistFilter(String[] keywords) {
		super(keywords);
	}

	@Override
	protected boolean matches(String keyword) {
		return whiteList.contains(keyword);
	}
}
