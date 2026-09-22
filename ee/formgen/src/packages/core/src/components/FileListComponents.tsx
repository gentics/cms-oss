import React, { useMemo, useState } from 'react';
import { ActivityIndicator, TouchableOpacity, View, Platform } from 'react-native';
import { fileStyles } from '../styles/common/common-style';
import { fileStyles as customFileStyles } from '../styles/custom/custom-style';
import { mergeStyles } from '../AndpUtils';
import RNFetchBlob from 'react-native-blob-util';
import FileViewer from 'react-native-file-viewer';
import { IRequestResult as IResponse } from '../types/interfaces'

import AndpText from './AndpText';
import AndpIcon from './AndpIcon';
import { useToastApi } from '../sdk/MicroAppSDK';

type DocStatus = 'loading' | 'error' | undefined;

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
const getFileIcon = (name: string, iconStyle: any): JSX.Element => {
  const iconSize = 'lg'
  const fileExt = name.split('.').pop()?.replace(/['"]/g, "") || 'pdf';
  const extensionIcon: Record<string, JSX.Element> = {
    'pdf': <AndpIcon name='pdfFile' size={iconSize} style={iconStyle} />,
    'doc': <AndpIcon name='wordFile' size={iconSize} style={iconStyle} />,
    'docx': <AndpIcon name='wordFile' size={iconSize} style={iconStyle} />,
    'xls': <AndpIcon name='excelFile' size={iconSize} style={iconStyle} />,
    'xlsx': <AndpIcon name='excelFile' size={iconSize} style={iconStyle} />,
    'ppt': <AndpIcon name='powerPointFile' size={iconSize} style={iconStyle} />,
    'pptx': <AndpIcon name='powerPointFile' size={iconSize} style={iconStyle} />,
    'jpg': <AndpIcon name='imageFile' size={iconSize} style={iconStyle} />,
    'png': <AndpIcon name='imageFile' size={iconSize} style={iconStyle} />,
    'txt': <AndpIcon name='textFile' size={iconSize} style={iconStyle} />,
  };

  return extensionIcon[fileExt] || <AndpIcon name='file' size={iconSize} style={iconStyle} />;
};



const FileListComponent: React.FC<FileListComponentProps> = ({
  documents,
  baseUrl,
  getDocHoverProps,
  hoveredDocIndex,
}) => {
  // TODO - just a temp solution - either add toast for web or add loading indicator for web 
  const toastApi = Platform.OS !== 'web' ? useToastApi() : { show: () => {}, hide: () => {} };
  const [docStatus, setDocStatus] = useState<Record<string, DocStatus>>({});
  const styles = useMemo(() => mergeStyles(fileStyles, customFileStyles), []);
  const downloadAndOpenFile = async (contentObjectID: string): Promise<boolean> => {
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
          return true;
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
    return false;
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

  /**
   * Web-Download ohne neuen Tab und ohne Navigation: Datei als Blob holen und
   * ueber ein verstecktes <a download> speichern (gleiches Muster wie der
   * Excel-Export in RestService). window.open() hat hier kurz ein leeres
   * Fenster geoeffnet und wieder geschlossen.
   */
  const downloadDocumentWeb = async (doc: FileData): Promise<boolean> => {
    const url = baseUrl + '/api/acta-nova/binary/' + encodeURIComponent(doc.contentObjectID);
    let response: Response;
    try {
      response = await fetch(url, { method: 'GET', credentials: 'include' });
    } catch (error) {
      // Der Request kam nicht durch (Netzwerk/CORS). Fallback auf das alte
      // Verhalten, damit der Download z. B. im Dev-Modus weiter funktioniert.
      console.error('Download request failed, falling back to window.open', error);
      return window.open(url, '_blank') !== null;
    }

    // Antwort kam an, ist aber ein Fehler (401/404/...). Ein neuer Tab wuerde
    // hier nur dieselbe Fehlerseite zeigen, also direkt als Fehler markieren.
    if (!response.ok) {
      console.error('Download failed with status', response.status);
      return false;
    }

    try {
      const blob = await response.blob();
      const blobUrl = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = blobUrl;
      a.download = doc.name;
      // Element muss im DOM haengen, damit Firefox den Klick annimmt
      document.body.appendChild(a);
      a.click();
      a.remove();
      // Erst nach dem Klick freigeben, sonst bricht der Download ab
      setTimeout(() => window.URL.revokeObjectURL(blobUrl), 0);
      return true;
    } catch (error) {
      console.error('Error', error);
      return false;
    }
  };

  const downloadDocument = async (doc: FileData) => {
    // Doppelklicks waehrend eines laufenden Downloads ignorieren
    if (docStatus[doc.contentObjectID] === 'loading') return;

    setDocStatus((prev) => ({ ...prev, [doc.contentObjectID]: 'loading' }));
    const success = Platform.OS !== 'web'
      ? await downloadAndOpenFile(doc.contentObjectID)
      : await downloadDocumentWeb(doc);
    setDocStatus((prev) => ({
      ...prev,
      [doc.contentObjectID]: success ? undefined : 'error',
    }));
  };

  /**
   * Rechtes Icon der Zeile: Spinner waehrend des Downloads, Warnhinweis bei
   * Fehler, sonst das Datei-Icon.
   */
  const renderTrailingIcon = (doc: FileData) => {
    if (docStatus[doc.contentObjectID] === 'loading') {
      return (
        <View style={styles.attachmentLoaderContainer}>
          <ActivityIndicator size='small' color={styles.attachmentLoader?.color} />
        </View>
      );
    }
    if (docStatus[doc.contentObjectID] === 'error') {
      return <AndpIcon name='circleWarning' size='lg' style={styles.attachmentErrorIcon} />;
    }
    return getFileIcon(doc.name, styles.attachmentIcon);
  };

  return (
    <>
      {documents && documents.map((doc, idx) => (
        <TouchableOpacity
          style={[styles.attachmentItem,
          {
            opacity: hoveredDocIndex === idx ? 0.85 : 1
          }
          ]}
          key={idx}
          onPress={() => downloadDocument(doc)}
          disabled={docStatus[doc.contentObjectID] === 'loading'}
          {...(getDocHoverProps ? getDocHoverProps(idx) : {})}
        >
          <View style={{ flex: 1 }}>
            <AndpText
              style={[
                styles.attachmentName,
                docStatus[doc.contentObjectID] === 'error' && styles.attachmentNameError,
              ]}
              numberOfLines={1}
              ellipsizeMode="tail"
            >
              {doc.name}
            </AndpText>
            <AndpText
              style={[
                styles.attachmentDate,
                docStatus[doc.contentObjectID] === 'error' && styles.attachmentDateError,
              ]}
            >
              {doc.createdAt}
            </AndpText>
          </View>
          {renderTrailingIcon(doc)}
        </TouchableOpacity>
      ))}
    </>
  );
};

export default FileListComponent;
