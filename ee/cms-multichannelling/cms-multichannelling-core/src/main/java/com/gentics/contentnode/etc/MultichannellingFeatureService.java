package com.gentics.contentnode.etc;

/**
 * Service that provides {@link Feature#MULTICHANNELLING} and {@link Feature#MCCR}
 */
public class MultichannellingFeatureService implements FeatureService {
	@Override
	public boolean isProvided(Feature feature) {
		return switch (feature) {
			case MULTICHANNELLING, PARTIAL_MULTICHANNELLING, MCCR -> true;
			default -> false;
		};
	}
}
