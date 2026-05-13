import React from 'react';
import { TouchableOpacity, View, Platform } from 'react-native';
import { fileStyles } from '../styles/common/common-style';
import RNFetchBlob from 'react-native-blob-util';
import FileViewer from 'react-native-file-viewer';
import { IRequestResult as IResponse } from '../types/interfaces'

import AndpText from './AndpText';
import theme from '../styles/theme';
import AndpIcon from './AndpIcon';
import { useToastApi } from '../sdk/MicroAppSDK';

interface FileListComponentProps {
  documents: any[];
  baseUrl: string;
  // downloadDocument: (doc: any) => void;
  /** Optional: Funktion, die zusätzliche Hover-Props zurückgibt (z. B. für Web) */
  getDocHoverProps?: (index: number) => object;
  /** Optional: Index des aktuell gehoverten Dokuments */
  hoveredDocIndex?: number;
}

/**
 * Bestimmt anhand des Dateinamens (Endung) den passenden Icon.
 */
const getFileIcon = (name: string): JSX.Element => {
  const fill = theme.colors.textOnGray
  const iconStyle = { marginLeft: theme.space.regular }
  const iconSize = 'lg'
  const fileExt = name.split('.').pop()?.replace(/['"]/g, "") || 'pdf';
  const extensionIcon: Record<string, JSX.Element> = {
    'pdf': <AndpIcon name='pdfFile' color={fill} size={iconSize} style={iconStyle} />,
    'doc': <AndpIcon name='wordFile' color={fill} size={iconSize} style={iconStyle} />,
    'docx': <AndpIcon name='wordFile' color={fill} size={iconSize} style={iconStyle} />,
    'xls': <AndpIcon name='excelFile' color={fill} size={iconSize} style={iconStyle} />,
    'xlsx': <AndpIcon name='excelFile' color={fill} size={iconSize} style={iconStyle} />,
    'ppt': <AndpIcon name='powerPointFile' color={fill} size={iconSize} style={iconStyle} />,
    'pptx': <AndpIcon name='powerPointFile' color={fill} size={iconSize} style={iconStyle} />,
    'jpg': <AndpIcon name='imageFile' color={fill} size={iconSize} style={iconStyle} />,
    'png': <AndpIcon name='imageFile' color={fill} size={iconSize} style={iconStyle} />,
    'txt': <AndpIcon name='textFile' color={fill} size={iconSize} style={iconStyle} />,
  };

  return extensionIcon[fileExt] || <AndpIcon name='file' color={fill} size={iconSize} style={iconStyle} />;
};



const FileListComponent: React.FC<FileListComponentProps> = ({
  documents,
  baseUrl,
  getDocHoverProps,
  hoveredDocIndex,
}) => {
  // TODO - just a temp solution - either add toast for web or add loading indicator for web 
  const toastApi = Platform.OS !== 'web' ? useToastApi() : { show: () => {}, hide: () => {} };
  const downloadAndOpenFile = async (contentObjectID: string) => {
    try {
      const encodedUri = encodeURIComponent(contentObjectID);

      toastApi.show({
        type: 'info',
        text1: 'Wird geladen...',
        text2: 'Dokument wird vorbereitet.',
        autoHide: false
      });
      const response = await downloadFile(encodedUri);
      toastApi.hide();

      if (response.success) {
        const exists = await RNFetchBlob.fs.exists(response.data);
        if (exists) {
          await FileViewer.open(response.data);
        } else {
          console.error('File does not exist at:', response.data);
          toastApi.show({
            type: 'error',
            text1: 'File Error',
            text2: 'Could not open the file.',
          });
        }
      } else {
        console.error('Download Failed', 'Could not download the file.');
        toastApi.show({
          type: 'error',
          text1: 'Download failed',
          text2: 'Could not download the file.',
        });
      }
    } catch (error) {
      console.error('Error', error);
      toastApi.show({
        type: 'error',
        text1: 'Error',
        text2: 'Could not open the file.',
      });
    }
  };

  const downloadFile = async (encodedURIContentIDObject: string): Promise<IResponse> => {
    try {
      let downloadPath = ''
      if (Platform.OS === 'ios')
        downloadPath = `${RNFetchBlob.fs.dirs.DocumentDir}/downloadedFile`;
      if (Platform.OS === 'android')
        downloadPath = `${RNFetchBlob.fs.dirs.DownloadDir}/downloadedFile`;

      const response = await RNFetchBlob
        .config({ path: downloadPath })
        .fetch('GET', `${baseUrl}/api/acta-nova/binary/${encodedURIContentIDObject}`);

      if (response.respInfo.status !== 200) return { success: false, data: null };

      const contentDisposition = response.respInfo.headers['Content-Disposition'] || response.respInfo.headers['content-disposition']
      const fileExt = contentDisposition.split('.').pop().replace(/['"]/g, "") || 'pdf';
      const filePath = `${downloadPath}.${fileExt}`

      const fileExists = await RNFetchBlob.fs.exists(filePath);
      if (fileExists) {
        await RNFetchBlob.fs.unlink(filePath);
      }
      await RNFetchBlob.fs.mv(downloadPath, filePath);
      if (Platform.OS === 'android') {
        await RNFetchBlob.fs.scanFile([{ path: filePath, mime: '*/*' }]);
      }

      const data = filePath;
      return { success: true, data: data };
    } catch (error) {
      console.error(error);
      return { success: false, error: error };
    }
  }

  const downloadDocument = (doc: FileData) => {
    if (Platform.OS !== 'web') downloadAndOpenFile(doc.contentObjectID)
    else {
      const url = baseUrl + '/api/acta-nova/binary/' + encodeURIComponent(doc.contentObjectID);
      window.open(url, '_blank');
    }
  };

  return (
    <>
      {documents && documents.map((doc, idx) => (
        <TouchableOpacity
          style={[fileStyles.attachmentItem,
          {
            opacity: hoveredDocIndex === idx ? 0.85 : 1
          }
          ]}
          key={idx}
          onPress={() => downloadDocument(doc)}
          {...(getDocHoverProps ? getDocHoverProps(idx) : {})}
        >
          <View style={{ flex: 1 }}>
            <AndpText
              weight={theme.fontWeight.bold}
              color={theme.colors.textOnGray}
              numberOfLines={1}
              ellipsizeMode="tail"
            >
              {doc.name}
            </AndpText>
            <AndpText
              weight={theme.fontWeight.light}
              color={theme.colors.textOnGray}
            >
              {doc.createdAt}
            </AndpText>
          </View>
          {getFileIcon(doc.name) || (
            <AndpIcon name='file' size='lg' style={{ marginLeft: theme.space.regular }} />
          )}
        </TouchableOpacity>
      ))}
    </>
  );
};

export default FileListComponent;
