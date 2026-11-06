import React, { useEffect, useMemo, useRef, useState } from 'react';
import { TouchableOpacity, View } from 'react-native';
import { buttonStyles } from '../../styles/common/common-style';
import { buttonStyles as customButtonStyles } from '../../styles/custom/custom-style';
import AndpText from '@andp/core/src/components/AndpText';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';
import { useButtons } from '../../screens/essentials/ButtonProvider';

interface IExpertSearchButtonProps {
  t: (key: string) => string;
  onPress: () => void;
  disabled?: boolean;
}

const ExpertSearchButton: React.FC<IExpertSearchButtonProps> = ({
  t,
  onPress,
  disabled,
}) => {
  const { getButtonTranslationKey } = useButtons();
  const styles = useMemo(() => mergeStyles(buttonStyles, customButtonStyles), [])
  const breakPoints = useBreakpoints();
  const [hover, setHover] = useState<boolean>(false);
  return (
    <View style={[styles.btnWrapper,
          breakPoints.maximumSm && styles.btnWrapperScreenMaxSmall,
        ]}>
      <TouchableOpacity
        id={"sticky-bar-expert-search-btn"}
        onPress={onPress}
        style={[styles.btn,
        styles.btnPrimary,
        hover && styles.btnPrimaryHover,
        breakPoints.maximumXl && styles.smallBtn,
        ]}
        disabled={disabled}
      >
        <AndpText
          selectable={false}
          numberOfLines={2}
          ellipsizeMode='tail'
          style={[styles.btnText,
          styles.btnPrimaryText,
          hover && styles.btnPrimaryTextHover,
          breakPoints.maximumMd && styles.smallText
          ]}
        >
          {!disabled ?
            getButtonTranslationKey(t('form_expertSearchButton'))
            :
            t('form_loading')
          }
        </AndpText>
      </TouchableOpacity>
    </View>
  );
};

export default ExpertSearchButton;
