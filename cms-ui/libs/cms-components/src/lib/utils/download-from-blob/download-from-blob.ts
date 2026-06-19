export function downloadFromBlob(blob: Blob, filename: string): Promise<void> {
    return new Promise((resolve, reject) => {
        try {
            // IE11 Download
            if ((window.navigator as any).msSaveOrOpenBlob) {
                (window.navigator as any).msSaveOrOpenBlob(blob, filename);
                resolve();
                return;
            }

            // Download for all other Browsers
            const url = (window.URL || (window as any).webkitURL).createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = filename;
            link.style.display = 'none';
            // Have to append child for Firefox
            document.body.appendChild(link);
            link.click();

            // Delayed cleanup, as firefox would clean it up before the download happens
            setTimeout(() => {
                try {
                    document.body.removeChild(link);
                    URL.revokeObjectURL(url);
                    resolve();
                } catch (error) {
                    reject(error);
                }
            }, 100);
        } catch (error) {
            reject(error);
        }
    });
}
