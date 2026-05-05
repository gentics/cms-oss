import React, { useMemo, useState } from 'react';
import { View, TouchableOpacity, Platform } from 'react-native';
import { buttonStyles } from '../../styles/common/common-style';
import { buttonStyles as customButtonStyles } from '../../styles/custom/custom-style';

import { FormState, UIState } from '../../types/interfaces';
import AndpText from '@andp/core/src/components/AndpText';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';
import { TFunction } from 'i18next';

interface IDialogFormBackButton {
    onPress: any;
    uiState: UIState;
    formState: FormState;
    updateState: any;
    t: TFunction;
}

const DialogFormBackButton: React.FC<IDialogFormBackButton> = (props) => {
    const uiState = props.uiState;
    const formState = props.formState;
    const t = props.t;
    const breakPoints = useBreakpoints();
    const [hover, setHover] = useState<boolean>(false);
    const styles = useMemo(() => mergeStyles(buttonStyles, customButtonStyles), [])

    return (
        <View style={[styles.btnDialogFormBack,
        breakPoints.maximumSm && styles.btnWrapperScreenMaxSmall,
        ]}>
            <TouchableOpacity
                onPress={() => props.onPress()}
                style={[styles.btn,
                styles.btnDialogFormBackStyle,
                hover && styles.btnDialogFormBackStyleHover,
                ]}
                activeOpacity={0.7}
                {...(Platform.OS === "web" && {
                    onMouseEnter: () => setHover(true),
                    onMouseLeave: () => setHover(false),
                })}
            >
                <AndpText
                    selectable={false}
                    numberOfLines={1}
                    lineBreakMode='head'
                    ellipsizeMode='tail'
                    style={[styles.btnText,
                    styles.btnDialogFormBackText,
                    hover && styles.btnDialogFormBackTextHover,
                    breakPoints.maximumMd && styles.smallText
                    ]}
                >
                    {props.t("form_dialog_back_to_main_form_label")}
                </AndpText>
            </TouchableOpacity>
        </View>
    );
};

export default DialogFormBackButton;
