import React, { useState } from 'react';
import { View } from 'react-native';
import {
  ICommonOverlayProps,
  IOverlayCustomOptionExtraProps,
  OverlayButtonDefinition,
} from '../../../types/interfaces';
import DefaultOverlay from '../DefaultOverlay';
import AndpInputElement from '@andp/core/src/components/AndpInputElement';
import * as overlayUtils from '../../../utils/OverlayUtils';

/**
 * Overlay used by the catalog dropdown element when `customOption` is enabled.
 *
 * It shows a single text input. On confirm the entered value is handed back to
 * the caller through `additionalProps.onConfirm(...)`, which adds and selects the
 * value in the dropdown. Cancelling invokes `additionalProps.onCancel(...)` so the
 * special option gets deselected again. The overlay keeps only the local input
 * text; all persistence happens in the calling element.
 */
const CustomOptionOverlay: React.FC<ICommonOverlayProps> = (
  props: ICommonOverlayProps,
) => {
  const t = props.formState.t;
  const additionalProps = props.uiState.overlayState
    .additionalProps as IOverlayCustomOptionExtraProps;

  const [value, setValue] = useState('');

  const close = () => overlayUtils.hideOverlay(props.uiState, props.updateState);

  const buttonList: OverlayButtonDefinition[] = [
    {
      key: 'custom-option-overlay-cancel-btn',
      title: t('form_cancel'),
      onPress: () => {
        additionalProps?.onCancel && additionalProps.onCancel();
        close();
      },
    },
    {
      key: 'custom-option-overlay-confirm-btn',
      title: t('form_catalog_customOption_confirm'),
      disabled: value.trim().length === 0,
      onPress: () => {
        const trimmed = value.trim();
        if (trimmed.length === 0) {
          return;
        }
        additionalProps?.onConfirm && additionalProps.onConfirm(trimmed);
        close();
      },
    },
  ];

  return (
    <DefaultOverlay
      {...props}
      closeAble={true}
      onClose={() => {
        additionalProps?.onCancel && additionalProps.onCancel();
        close();
      }}
      title={
        additionalProps?.title || t('form_catalog_customOption_overlay_title')
      }
      buttonList={buttonList}
      contentElementNodes={
        <View style={{ marginVertical: 12 }}>
          <AndpInputElement
            value={value}
            onChangeText={setValue}
            placeholder={
              additionalProps?.placeholder ||
              t('form_catalog_customOption_placeholder')
            }
            autoFocus={true}
          />
        </View>
      }
    />
  );
};

export default CustomOptionOverlay;
