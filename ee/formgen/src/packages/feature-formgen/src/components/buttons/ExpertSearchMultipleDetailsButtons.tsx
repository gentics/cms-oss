import React, { useEffect, useMemo, useState } from 'react';
import {
    TouchableOpacity,
    View,
    ActivityIndicator,
    Platform,
    GestureResponderEvent,
    ScrollView,
} from 'react-native';
import { buttonStyles } from '../../styles/common/common-style';
import { buttonStyles as customButtonStyles } from '../../styles/custom/custom-style';
import { FormState, UIState } from '../../types/interfaces';
import AndpText from '@andp/core/src/components/AndpText';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';
import theme from '@andp/core/src/styles/theme';

interface IExpertSearchMultipleButtonsProps {
    uiState: UIState;
    formState: FormState;
    t: (key: string) => string;
    onPress: (buttonID: string) => void;
    disabled?: boolean;
    isMobile: boolean;
}

interface IMultiButtonProps {
    id: string;
    name: string;
    uiState: UIState;
    onPress: (id: string) => void;
    disabled?: boolean;
}

/**
 * Einzelner Multi-Select-Button mit eigenem Hover-State.
 */
const MultiSelectButton: React.FC<IMultiButtonProps> = ({
    id,
    name,
    uiState,
    onPress,
    disabled,
}) => {
    const breakPoints = useBreakpoints();
    const [hover, setHover] = useState(false);
    const styles = useMemo(() => mergeStyles(buttonStyles, customButtonStyles), []);

    const handlePress = (_: GestureResponderEvent) => {
        if (!disabled) {
            onPress(id);
        }
    };

  return (
    <TouchableOpacity
      id={'sticky-bar-expert-search-details-btn'}
      key={id}
      onPress={handlePress}
      activeOpacity={0.7}
      disabled={disabled}
      style={[
        styles.btn,
        styles.btnSecondary,
        hover && styles.btnSecondaryHover,
        breakPoints.maximumXl && styles.smallBtn,
        { minWidth: 60, width: '23%', maxWidth: 400 },
      ]}
      {...(Platform.OS === 'web'
        ? {
            onMouseEnter: () => setHover(true),
            onMouseLeave: () => setHover(false),
          }
        : {})}
    >
      {uiState.loading ? (
        <ActivityIndicator
          size="small"
          color={
            hover
              ? styles.btnSecondaryTextHover.color
              : styles.btnSecondaryText.color
          }
          style={[
            styles.btnText,
            { justifyContent: 'center', alignItems: 'center' },
          ]}
        />
      ) : (
        <AndpText
          selectable={false}
          numberOfLines={1}
          lineBreakMode="head"
          ellipsizeMode="tail"
          style={[
            styles.btnText,
            styles.btnSecondaryText,
            hover && styles.btnSecondaryTextHover,
            breakPoints.maximumMd && styles.smallText,
          ]}
        >
          {name}
        </AndpText>
      )}
    </TouchableOpacity>
  );
};

/**
 * Rendert für jede headerColumn mit type="button" und isMultiSelect=true
 * einen eigenen Button im Footer (Sticky-Layer).
 */
const ExpertSearchMultipleButtons: React.FC<IExpertSearchMultipleButtonsProps> = ({
    uiState,
    formState,
    t,
    onPress,
    disabled,
    isMobile
}) => {
    const [buttons, setButtons] = useState<Array<{ id: string; name: string }>>([]);
    const styles = useMemo(() => mergeStyles(buttonStyles, customButtonStyles), [])
    useEffect(() => {
        const cols = formState?.data?.reportResult?.headerColumns;
        if (Array.isArray(cols)) {
            const multi = cols
                .filter((c: any) => c.type === 'button' && c.isMultiSelect)
                .map((c: any) => ({
                    id: c.id as string,
                    name: c.name as string,
                }));
            setButtons(multi);
        }
    }, [formState.data?.reportResult]);

    if (buttons.length === 0) {
        return null;
    }

    return (
        <View style={[, {
            width: "100%",
            flexDirection: 'column',
            padding: theme.space.small,
        }]}>
            {/* Zeige Anzahl */}
            <AndpText style={{ marginBottom: theme.space.small }}>
                {formState.expertSearchCheckMarkCount && formState.expertSearchCheckMarkCount > 0
                    && `${formState.expertSearchCheckMarkCount} ${formState.expertSearchCheckMarkCount == 1 ? t("form_expertSearch_singleCheckMark") : t("form_expertSearch_multipleCheckMark")}`}
            </AndpText>

            {/* Buttons horizontal scrollbar */}
            {formState.expertSearchCheckMarkCount && formState.expertSearchCheckMarkCount > 0 ? (
                <ScrollView
                    horizontal
                    showsHorizontalScrollIndicator={true}
                    contentContainerStyle={{
                        flexDirection: 'row',
                        alignItems: 'center',
                        gap: theme.space.small,
                        width: "100%",
                    }}
                >
                    {buttons.map(btn => (
                        <MultiSelectButton
                            key={btn.id}
                            id={btn.id}
                            name={btn.name}
                            uiState={uiState}
                            onPress={onPress}
                            disabled={disabled}
                        />
                    ))}
                </ScrollView>
            ) : null}
        </View>
    );
};

export default ExpertSearchMultipleButtons;