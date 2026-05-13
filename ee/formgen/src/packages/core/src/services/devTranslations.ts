import localDeTranslationPortal from '../../../../translations/de/portal.json';
import localDeTranslationForm from '../../../../translations/de/form.json';
import localDeTranslationProject from '../../../../translations/de/project.json';

import localEnTranslationPortal from '../../../../translations/en/portal.json';
import localEnTranslationForm from '../../../../translations/en/form.json';

export const mergedDe = {
  ...localDeTranslationPortal,
  ...localDeTranslationForm,
  ...localDeTranslationProject,
};

export const mergedEn = {
  ...localEnTranslationPortal,
  ...localEnTranslationForm,
};
