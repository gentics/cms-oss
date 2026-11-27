import React, { useEffect, useMemo } from 'react';
import { Platform } from 'react-native';
import SuccessScreen, {
  SuccessScreenProps,
  resolveSuccessUrl,
} from './SuccessScreen';

/**
 * Screen used as the final step of CMS form flows.
 *
 * If the form has a `returnUrl` or defines a `successUrl` (`returnUrl` wins), the user is redirected there
 * automatically on load (no UI is shown). Otherwise the regular SuccessScreen is rendered instead.
 */
const OptionalRedirectScreen: React.FC<SuccessScreenProps> = (props) => {
  const successUrl = useMemo(
    () => props.formState?.returnUrl ?? resolveSuccessUrl(props.formState),
    [props.formState?.returnUrl, props.formState?.successUrl, props.formState?.culture],
  );

  const shouldRedirect = Platform.OS === 'web' && !!successUrl;

  useEffect(() => {
    if (shouldRedirect) {
      window.open(successUrl, '_self')?.focus();
    }
  }, [shouldRedirect, successUrl]);

  if (!shouldRedirect) {
    return <SuccessScreen {...props} />;
  }

  return null;
};

export default OptionalRedirectScreen;
