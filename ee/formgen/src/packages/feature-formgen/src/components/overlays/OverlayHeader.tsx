import { View, TouchableOpacity } from 'react-native';
import { overlayHeaderStyles } from '../../styles/common/common-style';
import { overlayHeaderStyles as customOverlayHeaderStyles } from '../../styles/custom/custom-style';
import * as overlayUtils from '../../utils/OverlayUtils';
import { IOverlayHeaderProps } from '../../types/interfaces';
import AndpText from '@andp/core/src/components/AndpText';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import { useMemo } from 'react';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { overlaysTheme } from '@andp/core/src/styles/theme';

const OverlayHeader: React.FC<IOverlayHeaderProps> = (
  props: IOverlayHeaderProps,
) => {
  const closeable = props.closeAble;        
  const onClose = props.onClose;
  const title = props.title;
  const styles = useMemo(() => mergeStyles(overlayHeaderStyles, customOverlayHeaderStyles), []);
  return (
    <>
      {closeable &&
        <TouchableOpacity
          id={'overlay-close-btn'}
          onPress={onClose ? onClose : () => overlayUtils.hideOverlay(props.uiState, props.updateState)}
          style={styles.closeButton}>
          <AndpIcon name='close' 
            size={overlaysTheme.closeButton.iconSize}
          />
        </TouchableOpacity>
      }
      <View style={styles.headerContent}>
        <AndpIcon 
          name='info' 
          size={overlaysTheme.header.iconSize}
          color={overlaysTheme.header.iconColor}
          />
        <AndpText style={styles.headerTitle}>{title}</AndpText>
      </View>
    </>
  );
};

export default OverlayHeader;
