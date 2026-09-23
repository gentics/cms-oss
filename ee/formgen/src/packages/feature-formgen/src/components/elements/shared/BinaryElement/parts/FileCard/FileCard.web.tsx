import React, { useEffect, useMemo, useState } from "react";
import { View, Text, Pressable } from "react-native";
import { IBinaryFile } from "../../../../../../types/interfaces";
import { binaryFileCardStyles } from '../../../../../../styles/common/common-style';
import { binaryFileCardStyles as customBinaryFileCardStyles } from '../../../../../../styles/custom/custom-style';
import { mergeStyles, useBreakpoints } from "@andp/core/src/AndpUtils";
import AndpIcon from '@andp/core/src/components/AndpIcon';
import { PDFPreview } from "../PDFPreview.web";
import Animated, { useSharedValue, useAnimatedStyle, withTiming, Easing } from 'react-native-reanimated';
import { isTouchOnlyDevice } from "../../../../../../utils/Utils";
import { TFunction } from "i18next";


export interface IFileCardProps {
    file: IBinaryFile;
    onRemove: () => void;
    onLink: () => void;
    showDownload: boolean;
    showDelete: boolean;
    t: TFunction;
    parentJsonPath: string;
    index:number;
    maxFileSizeToPreviewMB?: number;
}


type IconName = React.ComponentProps<typeof AndpIcon>['name'];

const DEFAULT_MAX_FILE_SIZE_TO_PREVIEW = 2500000; // 2,5MB
const fileTypesToPreview = ['pdf', 'png', 'jpg', 'jpeg'];
const extensionToIconName: Record<string, IconName> = {
    pdf: 'pdfFile',
    xls: 'excelFile',
    xlsx: 'excelFile',
    csv: 'excelFile',
    odt: 'wordFile',
    docx: 'wordFile',
    ppt: 'powerPointFile',
    pptx: 'powerPointFile',
    jpg: 'imageFile',
    jpeg: 'imageFile',
    png: 'imageFile',
    gif: 'imageFile',
    svg: 'imageFile',
    heic: 'imageFile',
    txt: 'textFile',
};

function FileCard({
    file,
    onRemove,
    onLink,
    showDownload,
    showDelete,
    t,
    parentJsonPath,
    index,
    maxFileSizeToPreviewMB,
}: IFileCardProps) {
    const DEVICE_IS_TOUCH_ONLY = isTouchOnlyDevice();
    const breakpoints = useBreakpoints();
    const [hovered, setHovered] = useState(DEVICE_IS_TOUCH_ONLY ? true : false);
    const opacity = useSharedValue(DEVICE_IS_TOUCH_ONLY ? 1 : 0);
    const translateY = useSharedValue(DEVICE_IS_TOUCH_ONLY ? 0 : -10);

    const styles = useMemo(
        () => mergeStyles(binaryFileCardStyles, customBinaryFileCardStyles),
        [],
    );

    function getIconNameForExt(ext: string): IconName {
        return extensionToIconName[ext] || 'file';
    }

    const iconName = getIconNameForExt(file.fileExt);
    const maxFileSizeToPreview = maxFileSizeToPreviewMB != null
        ? maxFileSizeToPreviewMB * 1000 * 1000
        : DEFAULT_MAX_FILE_SIZE_TO_PREVIEW;
    const isPreviewAble = fileTypesToPreview.includes(file.fileExt) && ((file.binary && file.binary.size && file.binary.size <= maxFileSizeToPreview) || (file.file && file.file.size <= maxFileSizeToPreview));
    const isImage = fileTypesToPreview.filter(x => x != "pdf").includes(file.fileExt)

    const [previewUrl, setPreviewUrl] = useState<string | null>(null);
    const [previewFailed, setPreviewFailed] = useState(false);

    useEffect(() => {
        let url: string | null = null;
        setPreviewFailed(false);

        if (!isPreviewAble) {
            setPreviewUrl(null);
            return; // nothing to preview
        }

        if (file.source === "local" && file.file) {
            // Browser File/Blob -> blob: URL
            try {
                url = URL.createObjectURL(file.file as Blob);
            } catch (e) {
                url = null;
            }
        } else if (file.binary?.uri) {
            // Remote/previously uploaded URI
            url = file.binary.uri as string;
        }

        setPreviewUrl(url);

        return () => {
            // Revoke blob URLs to avoid memory leaks
            if (url && url.startsWith("blob:")) {
                URL.revokeObjectURL(url);
            }
        };
    }, [file, isPreviewAble]);

    const shouldShowPreview = isPreviewAble && !!previewUrl && !previewFailed;

    const animatedButtonStyle = useAnimatedStyle(() => {
        return {
            opacity: opacity.value,
            transform: [{ translateY: translateY.value }],
        };
    });

    const handleHoverIn = () => {
        setHovered(true);
        opacity.value = withTiming(1, {
            duration: 200,
            easing: Easing.out(Easing.quad),
        });
        translateY.value = withTiming(0, {
            duration: 200,
            easing: Easing.out(Easing.quad),
        });
    };

    const handleHoverOut = () => {
        setHovered(false);
        opacity.value = withTiming(0, {
            duration: 200,
            easing: Easing.out(Easing.quad),
        });
        translateY.value = withTiming(-10, {
            duration: 200,
            easing: Easing.out(Easing.quad),
        });
    };

    const [containerWidth, setContainerWidth] = useState<number>(0);

    return (
        <View
            // @ts-ignore
            onMouseEnter={DEVICE_IS_TOUCH_ONLY ? undefined : handleHoverIn}
            onMouseLeave={DEVICE_IS_TOUCH_ONLY ? undefined : handleHoverOut}
            accessibilityLabel="File preview card"
            style={[
                styles.card,
                breakpoints.minimumSm && styles.cardSizeSmScreen,
                breakpoints.minimumLg && styles.cardSizeMdScreen,
                breakpoints.minimumXxl && styles.cardSizeXlScreen,
                styles.cardShadow,
                hovered && styles.cardShadowHover,
                breakpoints.maximumSm && styles.cardmobile
            ]}
            onLayout={(event) => {
                const { width } = event.nativeEvent.layout;
                setContainerWidth(width);
            }}
        >
            <Animated.View
                style={[styles.header, animatedButtonStyle]}
            >
                <View style={[styles.headerActions]}
                    pointerEvents="box-none"
                >
                    {showDownload &&
                        <Pressable style={[styles.iconButton]} onPress={() => onLink()} onHoverIn={handleHoverIn}
                            id={`${parentJsonPath}-binary-file-download-btn-${index}`}
                            //@ts-ignore
                            dataSet={{ class: 'binary-file-download-btn' }}
                        >
                            <AndpIcon name="download" style={styles.downloadIcon}></AndpIcon>
                        </Pressable>
                    }
                    {showDelete &&
                        <Pressable style={styles.iconButton} onPress={() => onRemove()} onHoverIn={handleHoverIn}
                            id={`${parentJsonPath}-binary-file-delete-btn-${index}`}
                            //@ts-ignore
                            dataSet={{ class: 'binary-file-delete-btn' }}
                        >
                            <AndpIcon name="trashCan" style={styles.deleteIcon}></AndpIcon>
                        </Pressable>
                    }
                </View>
            </Animated.View>

            {shouldShowPreview ? (
                <View
                    style={styles.bodyThumbnailContainer}
                >
                    {isImage ? (
                        <>
                            <img
                                src={previewUrl ?? undefined}
                                alt={file.binary.fileName}
                                loading="eager"
                                style={{
                                    width: '100%',
                                    height: '100%',
                                    objectFit: 'cover',
                                    display: previewUrl ? 'block' : 'none',
                                }}
                                onError={() => setPreviewFailed(true)}
                            />
                        </>
                    ) : (
                        <>
                            <PDFPreview
                                width={containerWidth}
                                file={file}
                                uri={previewUrl}
                                onError={() => setPreviewFailed(true)}
                            />
                        </>
                    )}
                </View>
            ) : (
                <View style={styles.bodyIconContainer}>
                    <AndpIcon name={iconName} style={styles.bodyIcon} />
                </View>
            )}


            {/* Footer — filename (ellipsis) + filetype */}
            <View style={[styles.footer, breakpoints.maximumSm && styles.footerMobile]}>
                <Text style={[styles.fileName, breakpoints.maximumSm && styles.fileNameMobile]} numberOfLines={1}>
                    {file.binary.fileName}
                </Text>
                <Text style={[styles.fileType, breakpoints.maximumSm && styles.fileTypeMobile]}>{file.fileExt.toUpperCase()}</Text>
            </View>
        </View>
    );
}

export default FileCard;