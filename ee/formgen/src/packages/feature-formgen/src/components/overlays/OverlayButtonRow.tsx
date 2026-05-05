import React, { useMemo } from 'react';
import { View } from 'react-native';
import { overlayButtonRowStyles } from '../../styles/common/common-style';
import { overlayButtonRowStyles as custOmoverlayButtonRowStyles } from '../../styles/custom/custom-style';
import { OverlayButtonRowProps } from '../../types/interfaces';
import AndpButton from '@andp/core/src/components/AndpButton';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';

const OverlayButtonRow: React.FC<OverlayButtonRowProps> = ({ buttons, buttonObjects }) => {
  const breakPoints = useBreakpoints();
  const styles = useMemo(() => mergeStyles(overlayButtonRowStyles, custOmoverlayButtonRowStyles), []);
  return (
    <>
      {buttons && buttons.length > 0 && 
        <View style={styles.buttonRow}>
          {buttons.map((button, index) => {
            return (
              <AndpButton
                id={button.key}
                key={index}
                title={button.title}
                onPress={button.onPress}
                loading={button.loading != null && button.loading == true ? button.loading : false}
                wrapperStyle={[
                  styles.buttonWrapper,
                  breakPoints.maximumSm && styles.buttonWrapperScreenMaxSmall,
                  button.wrapperStyle,
                ]}
                buttonStyle={[
                  button.buttonStyle
                ]}
                textStyle={[
                  button.textStyle
                ]}
              />
            );
          })}
        </View>
      }
      {buttonObjects && buttonObjects.length > 0 && 
        <View style={styles.buttonRow}>
          {buttonObjects.map((ButtonObj, index) => { return ButtonObj; })}
        </View>
      }
    </>
  );
};

export default OverlayButtonRow;
