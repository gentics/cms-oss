import React, { useEffect, useMemo } from 'react';
import { Platform } from 'react-native';
import SuccessScreen, {
  SuccessScreenProps,
  resolveSuccessUrl,
} from './SuccessScreen';

/**
 * Screen used as the final step of CMS form flows.
 *
 * If the form defines a `successUrl`, the user is redirected there automatically
 * on load (no UI is shown). If no `successUrl` is defined, the regular
 * SuccessScreen is rendered instead.
 */
const OptionalRedirectScreen: React.FC<SuccessScreenProps> = (props) => {
  const successUrl = useMemo(
    () => resolveSuccessUrl(props.formState),
    [props.formState?.successUrl, props.formState?.culture],
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
