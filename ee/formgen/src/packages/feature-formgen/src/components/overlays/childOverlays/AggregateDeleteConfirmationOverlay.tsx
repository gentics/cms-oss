import { ICommonOverlayProps, OverlayButtonDefinition } from '../../../types/interfaces';
import DefaultOverlay from '../DefaultOverlay';
import * as overlayUtils from '../../../utils/OverlayUtils';
import * as utils from '../../../utils/Utils';
import { IOverlayAggregateDeleteConfirmationExtraProps } from '../../../types/interfaces';

const AggregateDeleteConfirmationOverlay: React.FC<ICommonOverlayProps> = (
  props: ICommonOverlayProps,
) => {
  const t = props.formState.t;
  const additionalProps: IOverlayAggregateDeleteConfirmationExtraProps = props
    .uiState.overlayState
    .additionalProps as IOverlayAggregateDeleteConfirmationExtraProps;

  let buttonList: OverlayButtonDefinition[] | undefined = [
    {
      key: 'aggregate-delete-confirmation-overlay-cancel-btn',
      title: t('form_cancel'),
      onPress: () => {
        console.log('closing delete confirmation with no action');
        overlayUtils.hideOverlay(props.uiState, props.updateState);
      },
    },
    {
      key: 'aggregate-delete-confirmation-overlay-delete-entry-btn',
      title: t('form_delete_entry'),
      onPress: () => {
        console.log('confirm delete and close');
        utils.listDeleteElement(
          props.formState,
          additionalProps.parentJsonPath,
          additionalProps.index,
          props.updateState,
        );
        overlayUtils.hideOverlay(props.uiState, props.updateState);

      },
    },
  ];

  return (
    <DefaultOverlay
      {...props}
      title={t('form_delete_entry_confirmation_overlay_title')}
      contentMessages={[t('form_delete_entry_confirmation_overlay_content')]}
      buttonList={buttonList}
      debugInfo={additionalProps ? additionalProps.response : undefined}
    />
  );
};

export default AggregateDeleteConfirmationOverlay;
