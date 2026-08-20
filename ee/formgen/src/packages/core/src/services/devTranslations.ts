import localDeTranslationPortal from '../../../../translations/de/portal.json';
import localDeTranslationProject from '../../../../translations/de/project.json';

import localEnTranslationPortal from '../../../../translations/en/portal.json';

export const mergedDe = {
  ...localDeTranslationPortal,
  ...localDeTranslationProject,
};

export const mergedEn = {
  ...localEnTranslationPortal
};
