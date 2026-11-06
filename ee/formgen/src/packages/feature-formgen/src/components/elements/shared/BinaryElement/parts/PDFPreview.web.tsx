import { Platform, View } from 'react-native';
import React, { useMemo } from 'react';
/* @metro-ignore */
import { Document, Page, pdfjs } /* @metro-ignore */ from 'react-pdf';
import { IBinaryFile } from '../../../../../types/interfaces';
import { Loading } from '@andp/core/src/components/LoadingComponents';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { pdfPreviewStyles } from '../../../../../styles/common/common-style.web';
import { customPdfPreviewStyles } from '../../../../../styles/custom/custom-style.web';

if (Platform.OS === 'web') {
  // In dev we serve the worker from the public folder root,
  // in the built app it lives under the exported static path.
  const workerSrc =
    typeof __DEV__ !== 'undefined' && __DEV__
      ? '/pdf.worker.min.mjs'
      : '/static/cpa/formgen/_expo/static/js/web/pdf.worker.min.mjs';

  /* @metro-ignore */
  pdfjs.GlobalWorkerOptions.workerSrc = workerSrc;
}


export const PDFPreview = ({
  file,
  uri,
  width,
  onError,
}: {
  file: IBinaryFile;
  uri: any;
  width: number;
  onError?: () => void;
}) => {

  const styles = useMemo(
    () => mergeStyles(pdfPreviewStyles, customPdfPreviewStyles),
    [],
  );
  return (
    <View style={styles.pdfPreview}>
      <Document
        file={uri}
        onLoadError={onError}
        onSourceError={onError}
        loading={<Loading></Loading>}
      >
        <Page
          width={width}
          pageNumber={1}
          renderAnnotationLayer={false}
          renderTextLayer={false}
        />
      </Document>
    </View>
  );

}