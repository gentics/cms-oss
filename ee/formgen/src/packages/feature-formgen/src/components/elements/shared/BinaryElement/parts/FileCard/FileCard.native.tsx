import React, { useEffect, useMemo, useState } from "react";
import { View, Text, Pressable, Image, Alert } from "react-native";
import { IBinaryFile } from "../../../../../../types/interfaces";
import { binaryFileCardStyles } from '../../../../../../styles/common/common-style';
import { binaryFileCardStyles as customBinaryFileCardStyles } from '../../../../../../styles/custom/custom-style';
import { mergeStyles, useBreakpoints } from "@andp/core/src/AndpUtils";
import AndpIcon from '@andp/core/src/components/AndpIcon';
import { TFunction } from "i18next";

type IconName = React.ComponentProps<typeof AndpIcon>['name'];

const maxFileSizeToPreview = 2500000; // 2,5MB 
const fileTypesToPreview = ['png', 'jpg', 'jpeg'];
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
    t
}: {
    file: IBinaryFile;
    onRemove: () => void;
    onLink: () => void;
    showDownload: boolean;
    showDelete: boolean;
    t: TFunction;
}) {
    const [hovered, setHovered] = useState(false);
    const breakpoints = useBreakpoints();

    const styles = useMemo(
        () => mergeStyles(binaryFileCardStyles, customBinaryFileCardStyles),
        [],
    );

    function getIconNameForExt(ext: string): IconName {
        return extensionToIconName[ext] || 'file';
    }

    const iconName = getIconNameForExt(file.fileExt);
    const isPreviewAble =
      fileTypesToPreview.includes(file.fileExt) &&
      (
        (file.binary && (file.binary.size == null || file.binary.size <= maxFileSizeToPreview)) ||
        (file.file && (file.file.size == null || file.file.size <= maxFileSizeToPreview))
      );
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

        // On native we typically get a URI either from the binary metadata
        // or directly from the picked file (e.g. react-native-document-picker).
        if (file.binary?.uri) {
            url = file.binary.uri as string;
        } else if ((file as any).file?.uri) {
            url = (file as any).file.uri as string;
        }

        setPreviewUrl(url);
    }, [file, isPreviewAble]);

    const shouldShowPreview = isPreviewAble && !!previewUrl && !previewFailed;

    const handleRemove = () => {
        Alert.alert(
            t("form_fileCard_delete_title"),
            t("form_fileCard_delete_desc"),
            [
                {
                    text: t("form_fileCard_delete_btn_cancel"),
                    style: "cancel"
                },
                {
                    text: t("form_fileCard_delete_btn_confirm"),
                    onPress: () => onRemove(),
                    style: "destructive"
                }
            ]
        );
    };

    return (
        <Pressable
            accessibilityLabel="File preview card"
            style={styles.card}
            onPress={showDownload ? () => onLink() : undefined}
        >
            {shouldShowPreview && isImage ? (
                <>
                    <View style={styles.bodyThumbnailContainer}>
                        {previewUrl ? (
                            <Image
                                source={{ uri: previewUrl }}
                                accessibilityLabel={file.binary?.fileName ?? "File preview"}
                                style={{ width: "100%", height: "100%", objectFit: "cover" }}
                                onError={() => setPreviewFailed(true)}
                            />
                        ) : null}
                    </View>
                </>
            ) : (
                <>
                    <View style={styles.bodyIconContainer}>
                        <AndpIcon name={iconName} style={styles.bodyIcon} />
                    </View>
                </>
            )}

            {showDelete &&
                <View style={styles.headerActions}>
                    <Pressable style={styles.iconButton} onPress={handleRemove}>
                        <AndpIcon name="trashCan" style={styles.deleteIcon}></AndpIcon>
                    </Pressable>
                </View>
            }

            {/* Footer — filename (ellipsis) + filetype */}
            <View style={styles.footer}>
                <Text style={styles.fileName} numberOfLines={1}>
                    {file.binary?.fileName ?? (file as any).file?.name ?? ""}
                </Text>
                <Text style={styles.fileType}>{file.fileExt.toUpperCase()}</Text>
            </View>
        </Pressable>
    );
}

export default FileCard;