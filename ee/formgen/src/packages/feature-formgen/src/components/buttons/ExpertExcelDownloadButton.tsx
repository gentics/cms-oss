import React, { useMemo, useState } from 'react';
import { TouchableOpacity, View, ActivityIndicator, Platform } from 'react-native';
import { buttonStyles } from '../../styles/common/common-style';
import { buttonStyles as customButtonStyles } from '../../styles/custom/custom-style';
import { UIState } from '../../types/interfaces';
import AndpText from '@andp/core/src/components/AndpText';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';
import { useButtons } from '../../screens/essentials/ButtonProvider';

interface IExpertExcelDownloadButtonProps {
  uiState: UIState;
  t: (key: string) => string;
  onPress: () => void;
  disabled?: boolean;
}

const ExpertExcelDownloadButton: React.FC<IExpertExcelDownloadButtonProps> = ({
  uiState,
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
        id={"sticky-bar-expert-excel-download-btn"}
        onPress={onPress}
        style={[styles.btn,
        styles.btnSecondary,
        hover && styles.btnSecondaryHover,
        breakPoints.maximumXl && styles.smallBtn,
        ]}
        activeOpacity={0.7}
        {...(Platform.OS === "web" && {
          onMouseEnter: () => setHover(true),
          onMouseLeave: () => setHover(false),
        })}
      >
        {uiState.loading ? (
          <ActivityIndicator
            size="small"
            color={
              hover
                ? styles.btnSecondaryTextHover.color
                : styles.btnSecondaryText.color
            }

            style={[styles.btnText, { justifyContent: 'center', alignItems: 'center' },]}
          ></ActivityIndicator>
        ) : (
          <AndpText
            selectable={false}
            numberOfLines={1}
            lineBreakMode='head'
            ellipsizeMode='tail'
            style={[styles.btnText,
            styles.btnSecondaryText,
            hover && styles.btnSecondaryTextHover,
            breakPoints.maximumMd && styles.smallText
            ]}
          >
            {t(getButtonTranslationKey('form_expertDownloadAsExcel'))}
          </AndpText>
        )}
      </TouchableOpacity>
    </View>

  );
};

export default ExpertExcelDownloadButton;
