import { View } from 'react-native';
import { overlayFooterStyles } from '../../styles/common/common-style';
import { overlayFooterStyles as customOverlayFooterStyles } from '../../styles/custom/custom-style';
import { IOverlayFooterProps } from '../../types/interfaces';
import AndpText from '@andp/core/src/components/AndpText';
import { useMemo } from 'react';
import { mergeStyles } from '@andp/core/src/AndpUtils';

const OverlayFooter: React.FC<IOverlayFooterProps> = (
  props: IOverlayFooterProps,
) => {
  const footerMessages = props.footerMessages;
  const styles = useMemo(() => mergeStyles(overlayFooterStyles, customOverlayFooterStyles), []);

  if (!footerMessages || footerMessages.length === 0) {
    return null;
  }

  return (
    <View style={styles.footer}>
      {footerMessages.map((line, index) => (
        <AndpText 
          key={`overlayFooterText-${index}`} 
          style={styles.footerText}>
          {line}
        </AndpText>
      ))}
    </View>
  );
};
export default OverlayFooter;
