import React, { useMemo, useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
} from 'react-native';
import * as Clipboard from 'expo-clipboard';
import { debugModeSectionStyles } from '../../styles/common/common-style';
import { debugModeSectionStyles as customDebugModeSectionStyles } from '../../styles/custom/custom-style';
import AndpText from '@andp/core/src/components/AndpText';
import { mergeStyles } from '@andp/core/src/AndpUtils';

export interface IDebugSectionProps {
  label: string;
  content: string;
  isMultiline: boolean;
}

export const DebugSection: React.FC<IDebugSectionProps> = ({
  label,
  content,
  isMultiline,
}) => {
  const [copied, setCopied] = useState(false);
  const handleCopy = async () => {
    try {
      // --unsafely-treat-insecure-origin-as-secure="http://portal.localhost.gentics.com:8081"
      // Die Copy functioon läuft nur wenn die URL mit https beginnt oder wenn man den Command --unsafely-treat-insecure-origin-as-secure="http://portal.localhost.gentics.com:8081" in chrome gesettzt hat. 
      await Clipboard.setStringAsync(content);
      setCopied(true);
      setTimeout(() => setCopied(false), 1000);
    } catch (error) {
      console.error("Fehler beim Kopieren in die Zwischenablage:", error);
    }
  };
  const styles = useMemo(() => mergeStyles(debugModeSectionStyles, customDebugModeSectionStyles), []);
  
  if (content == '') {
    return;
  }

  return (
    <View style={styles.section}>
      <View style={styles.headerRow}>
        <AndpText style={styles.sectionLabel}>{label}</AndpText>
        <TouchableOpacity
          onPress={handleCopy}
          style={styles.copyButton}
          disabled={copied}
        >
          <AndpText style={styles.copyButtonText}>
            {/* TODO v8.2 Translation */}
            {copied ? 'Copied' : 'Copy'}
          </AndpText>
        </TouchableOpacity>
      </View>

      {isMultiline ? (
        <View style={styles.scrollableArea}>
          <TextInput
            style={styles.textarea}
            value={content}
            editable={false}
            multiline={isMultiline}
          />
        </View>
      ) : (
        <AndpText style={styles.contentText}>{content}</AndpText>
      )}
    </View>
  );
};
