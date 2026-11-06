import localDeTranslationForm from '../../../../local_dev_translations/de/form.json';
import localDeTranslationPortal from '../../../../local_dev_translations/de/portal.json';
import localDeTranslationProject from '../../../../local_dev_translations/de/project.json';

import localEnTranslationPortal from '../../../../local_dev_translations/en/portal.json';

export const mergedDe = {
  ...localDeTranslationForm,
  ...localDeTranslationPortal,
  ...localDeTranslationProject,
};

export const mergedEn = {
  ...localEnTranslationPortal
};
