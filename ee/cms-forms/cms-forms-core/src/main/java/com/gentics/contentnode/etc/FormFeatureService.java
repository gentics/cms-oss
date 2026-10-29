package com.gentics.contentnode.etc;

/**
 * Implementation of {@link FeatureService} that provides the {@link Feature#FORMS}
 */
public class FormFeatureService implements FeatureService {
	@Override
	public boolean isProvided(Feature feature) {
		switch (feature) {
		case FORMS:
			return FormConfigProvider.isConfigValid();
		default:
			return false;
		}
	}
}
