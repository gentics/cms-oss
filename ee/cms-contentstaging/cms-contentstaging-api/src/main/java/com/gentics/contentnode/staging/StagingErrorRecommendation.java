package com.gentics.contentnode.staging;

import com.gentics.contentnode.factory.object.FileSizeException;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;

public enum StagingErrorRecommendation {
	NOT_FOUND,
	MISMATCH,
	DUPLICATION,
	OTHER;

	StagingErrorRecommendation() {
	}

	public static StagingErrorRecommendation fromThrowable(Throwable throwable) {
		if (throwable instanceof EntityNotFoundException
				|| throwable instanceof FileSizeException
				|| throwable.getCause() instanceof FileSizeException) {
			return StagingErrorRecommendation.NOT_FOUND;
		}

		return StagingErrorRecommendation.OTHER;
	}

	@Override
	public String toString() {
		final var prefix = "content.staging.recommendation.";
		return I18NHelper.get(prefix + this.name().toLowerCase());
	}
}
