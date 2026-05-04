import { ICommonOverlayProps, IOverlayExtraCommonProps } from '../../../types/interfaces';
import DefaultOverlay from '../DefaultOverlay';
import * as overlayUtils from '../../../utils/OverlayUtils';

const UpdateFormOverlay: React.FC<ICommonOverlayProps> = (
  props: ICommonOverlayProps,
) => {
  const additionalProps: IOverlayExtraCommonProps = props.uiState.overlayState
    .additionalProps as IOverlayExtraCommonProps;
  const t = props.formState.t;
  return (
    <DefaultOverlay
      {...props}
      title={t('form_error_overlay_deletedDataVersion_title')}
      contentMessages={[t('form_error_overlay_deletedDataVersion_content')]}
      buttonList={[
        {
          key: 'update-form-overlay-close-btn',
          title: t('form_error_overlay_close_button'),
          onPress: () =>
            overlayUtils.hideOverlay(props.uiState, props.updateState),
        },
      ]}
      footerMessages={[
        t('form_save_overlay__first_line'),
        t('form_save_overlay__second_line'),
      ]}
      debugInfo={additionalProps ? additionalProps.response : undefined}
    />
  );
};

export default UpdateFormOverlay;
