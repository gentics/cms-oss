import { Linking, Platform, ScrollView, TouchableOpacity, useWindowDimensions, View } from "react-native";
import AndpText from "./AndpText";
import AndpIcon from "./AndpIcon";
import theme from "../styles/theme";
import { useEffect, useMemo, useRef, useState } from "react";
import { Message } from "../../models/Message";
import FileListComponent from './FileListComponents';
import { messagesStyle } from "../styles/common/common-style";
import { useReactToPrint } from 'react-to-print';
import CustomRenderHTML from "./CustomRenderHTML";
import AndpButton from "./AndpButton";


interface MessageContentComponentProps {
  t: any;
  message?: Message;
  closeHandler: () => void;
  showCloseButton: boolean;
  endpoint: string
}

const MessageContentComponent: React.FC<MessageContentComponentProps> = 
({
  t,
  message,
  closeHandler,
  showCloseButton,
  endpoint
}) => {
  const htmlSource = useMemo(() => ({
    html: message?.content.replace(/&nbsp;/g, '\u00A0')
  }), [message?.content]);
  const printRef = useRef(null);
  const { width } = useWindowDimensions();

  const smallScreen = useMemo(() => width < 500, [width < 500]);
  const mediumScreen = useMemo(() => width < 1007, [width < 1007]);

  const closeMessage = () => {
    closeHandler();
  };

  const handlePrint = useReactToPrint({
    contentRef: printRef,
  });

  const CloseAndPrintContainer = (
    <>
      <TouchableOpacity
        style={[messagesStyle.messagePrintButton, 
          !mediumScreen && {marginLeft: theme.space.regular}
        ]}
        onPress={() => handlePrint()}
      >
        <AndpIcon name='printer' size='md'/>
        <AndpText color={theme.colors.textOnGray}>{t('formdetail__print')}</AndpText>
      </TouchableOpacity>
      <TouchableOpacity onPress={() => closeMessage()}>
        <AndpIcon name='close' size='lg' style={{marginLeft: theme.space.regular, marginRight: theme.space.extraSmall}}/>
      </TouchableOpacity>
    </>
  );

  const checkISODateString = (dateString: string) => {
    return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?Z?$/.test(dateString);
  }

  return (
    <ScrollView 
      style={[
        messagesStyle.messageDetailScrollView,
        smallScreen && messagesStyle.messageDetailScrollViewSmallScreen
      ]}
    >
      <View
        style={[
          { paddingHorizontal: theme.space.large, paddingBottom: theme.space.large },
          Platform.OS === 'web' && mediumScreen && { paddingTop: theme.space.large },
        ]}
        ref={printRef}
      >
        {/* Header */}
        {Platform.OS === 'web' && mediumScreen && (
          <View
            style={{ flexDirection: 'row', justifyContent: 'space-between' }}
          >
            {CloseAndPrintContainer}
          </View>
        )}

        <View style={[{ flexDirection: 'row'}, Platform.OS == 'web' && {alignItems: 'center'}]}>
          <AndpText style={[theme.screenTitle, { flex: 1 }, Platform.OS !== 'web' && { marginTop: 0 }]}>
            {message?.subject}
          </AndpText>
          <View style={{ alignItems: 'flex-end' }}>
            {showCloseButton && <TouchableOpacity onPress={() => closeMessage()}>
              <AndpIcon name='close' size='lg' style={{marginLeft: theme.space.regular, marginBottom: theme.space.regular}}/>
            </TouchableOpacity>}
            <AndpText style={messagesStyle.messageListItemDate}>
              {
                message?.date 
                ? (typeof message.date === "string" && checkISODateString(message.date) 
                    ? new Date(message.date).toLocaleDateString() 
                    : message.date instanceof Date 
                      ? message.date.toLocaleDateString() 
                      : message?.date.toString().split(' ')[0])
                : "No Date"
              }
            </AndpText>
            <AndpText style={messagesStyle.messageListItemDate}>
              {
                message?.date 
                ? (typeof message.date === "string" && checkISODateString(message.date) 
                    ? new Date(message.date).toLocaleDateString() 
                    : message.date instanceof Date 
                      ? message?.date.getHours() + ":" + message?.date.getMinutes()
                      : message?.date.toString().split(' ')[1])
                : "No Date"
              }
            </AndpText>
          </View>
          {Platform.OS === 'web' && !mediumScreen && CloseAndPrintContainer}
        </View>

        {/* Metadata Summary */}
        <View style={messagesStyle.messageMetadataSummaryContainer}>
          <AndpText
            weight={theme.fontWeight.bold}
            style={messagesStyle.messageSenderText}
          >
            {t('messages_from')}:{' '}{message?.senderTenant}
          </AndpText>
          <AndpText style={messagesStyle.messageMetadataText}>
            {t('messages_business_partner')}: {message?.businessPartnerName}
          </AndpText>
          <AndpText style={messagesStyle.messageMetadataText}>
            {t('messages_businessCaseType')}: {message?.businessCaseType}
          </AndpText>
          <AndpText style={messagesStyle.messageMetadataText}>
            {t('messages_businessCaseTitle')}: {message?.businessCaseTitle}
          </AndpText>
          <AndpText style={messagesStyle.messageMetadataText}>
            {t('messages_businessCaseNumber')}: {message?.businessCaseNumber}
          </AndpText>
          <AndpText style={messagesStyle.messageMetadataText}>
            {t('messages_document_identification')}: {message?.referenceNumber}
          </AndpText>
        </View>
        {/* Message Content */}
        <View
          style={messagesStyle.messageDividerLine}
        >
          <CustomRenderHTML 
            basePath={endpoint} 
            source={htmlSource}
          />
        </View>
         {/* GF-Bearbeiten / Response Button */}
        {message && message.metaInfo?.responseButton?.formID && (
            <View
             style={messagesStyle.messageDividerLine}
             >
              <AndpButton
                title={message.metaInfo?.responseButton?.buttonText}
                onPress={
                  () => {
                    let url = '/api/andp/form/' + encodeURI(message.metaInfo?.responseButton?.formID!)
                    if(message.metaInfo?.responseButton?.businessObjectID){
                      url = url + '/' + encodeURIComponent(message.metaInfo?.responseButton?.businessObjectID)
                    }
                    return Linking.openURL(url);
                  }
                }
                />
            </View>
        )}     
        {/* Attachments */}
        {message?.files.length > 0 ? (
          <View>
            <AndpText
              style={messagesStyle.messageAttachmentTitle}
            >
              {t('messages_attachment')}
            </AndpText>             

            <FileListComponent
              documents={message?.files}
              baseUrl={endpoint}
            />
          </View>
        ) : null}
      </View>
    </ScrollView>
  );
}



export default MessageContentComponent;