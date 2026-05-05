import React, { useMemo, useState } from 'react';
import { Platform, View, Pressable } from 'react-native';
import { buttonStyles } from '../../styles/common/common-style';
import { buttonStyles as customButtonStyles } from '../../styles/custom/custom-style';
import { FormState, IFormFlowButtonDefinition, UIState } from '../../types/interfaces';
import { TFunction } from 'i18next';
import AndpText from '@andp/core/src/components/AndpText';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';
import { useReactToPrint } from 'react-to-print';
import { getI18nTextStr } from '../../utils/Utils';
import { useButtons } from '../../screens/essentials/ButtonProvider';

interface PrintPdfButtonProps {
    formState: FormState;
    uiState: UIState;
    restService: any;
    updateState: (state: any) => void;
    mockForm?: boolean;
    t: TFunction;
    screenType: string;
    translateKey: string;
    buttonDefinition: IFormFlowButtonDefinition;
    printRef: React.RefObject<HTMLElement>;
}

const PrintPdfButtonScreenPart: React.FC<PrintPdfButtonProps> = ({
    formState,
    uiState,
    t,
    translateKey,
    buttonDefinition,
    printRef,
}) => {
    if (Platform.OS !== 'web' && buttonDefinition.mobileDisabled) return null;
    const { getButtonTranslationKey } = useButtons();

    const [hover, setHover] = useState(false);
    const breakPoints = useBreakpoints();
    const styles = useMemo(() => mergeStyles(buttonStyles, customButtonStyles), []);

    const pageHeader = getI18nTextStr(formState.culture, formState.form.formname);

    const pageStyle = `
    @media print {
    html, body { margin:0; padding:0 }

    #print-root {
        width: 100%;
    }
    }`;

    const handlePrint = useReactToPrint({
        contentRef: printRef,
        documentTitle: pageHeader,
        pageStyle: pageStyle,
    });

    return (
        <View
            style={[
                styles.btnWrapper,
                breakPoints.maximumSm && styles.btnWrapperScreenMaxSmall,
            ]}
        >
            <Pressable
                id={"sticky-bar-print-btn"}
                disabled={uiState.loading}
                onPress={() => handlePrint()}
                onHoverIn={() => setHover(true)}
                onHoverOut={() => setHover(false)}
                style={[
                    styles.btn,
                    styles.btnSecondary,
                    breakPoints.maximumXl && styles.smallBtn,
                    hover && styles.btnSecondaryHover,
                ]}
            >
                <AndpText
                    selectable={false}
                    numberOfLines={2}
                    ellipsizeMode="tail"
                    style={[
                        styles.btnText,
                        styles.btnSecondaryText,
                        breakPoints.maximumMd && styles.smallText,
                        hover && styles.btnSecondaryTextHover,
                    ]}
                >
                    {t(getButtonTranslationKey(translateKey))}
                </AndpText>
            </Pressable>
        </View>
    );
};

export default PrintPdfButtonScreenPart;