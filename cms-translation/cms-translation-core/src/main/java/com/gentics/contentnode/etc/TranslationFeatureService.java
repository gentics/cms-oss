package com.gentics.contentnode.etc;

import com.gentics.contentnode.LicenseService;
import com.gentics.contentnode.LicensedFeature;
import com.gentics.contentnode.api.translation.TranslationService;
import com.gentics.contentnode.translation.TranslationUtil;
import com.gentics.lib.log.NodeLogger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/**
 * Implementation of {@link FeatureService} which provides the
 * {@link Feature#AUTOMATIC_TRANSLATION}
 */
public class TranslationFeatureService implements FeatureService {

	private static final NodeLogger logger = NodeLogger.getNodeLogger(
			TranslationFeatureService.class);

	@Override
	public boolean isProvided(Feature feature) {
		var isActivatedAndLicensed =
				feature == Feature.AUTOMATIC_TRANSLATION && LicenseService.isLicenseSufficient(
						LicensedFeature.TRANSL);
		if (!isActivatedAndLicensed) {
			return false;
		}
		var isCreated = TranslationUtil.createServiceDefinitionFileIfNeeded();
		if(!isCreated) {
			return false;
		}
		TranslationUtil.reloadService();
		return true;
	}

}
