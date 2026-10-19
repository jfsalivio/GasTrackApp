// Android only exposes this bridge to the bundled, trusted application origin.
export const isAndroidApp = () => Boolean(window.GasTrackAndroid);
const downloadBlobs = new Map();

async function saveAndroidDownload(anchor) {
  try {
    let blob = downloadBlobs.get(anchor.href);
    if (!blob) {
      const response = await fetch(anchor.href);
      if (!response.ok) throw new Error('Could not download this file.');
      blob = await response.blob();
    }
    if (blob.size > 24 * 1024 * 1024) throw new Error('Choose an export smaller than 24 MB.');
    const reader = new FileReader();
    reader.onerror = () => window.alert('Could not read the file for saving.');
    reader.onload = () => window.GasTrackAndroid.saveFile(anchor.download || 'gastrack-download', blob.type || 'application/octet-stream', String(reader.result).split(',')[1]);
    reader.readAsDataURL(blob);
  } catch (error) { window.alert(error.message); }
}

export function installAndroidPlatform() {
  if (!isAndroidApp()) return;
  // Keep the original blob while download helpers synchronously revoke their URL.
  // Reading it directly also avoids WebView connect-src rules for blob fetches.
  const createObjectURL = URL.createObjectURL.bind(URL);
  const revokeObjectURL = URL.revokeObjectURL.bind(URL);
  URL.createObjectURL = blob => {
    const url = createObjectURL(blob);
    downloadBlobs.set(url, blob);
    return url;
  };
  URL.revokeObjectURL = url => { downloadBlobs.delete(url); revokeObjectURL(url); };
  const originalClick = HTMLAnchorElement.prototype.click;
  HTMLAnchorElement.prototype.click = function (...args) {
    if (this.hasAttribute('download')) { void saveAndroidDownload(this); return; }
    return originalClick.apply(this, args);
  };
  document.addEventListener('click', event => {
    const anchor = event.target.closest?.('a[download]');
    if (!anchor) return;
    event.preventDefault();
    void saveAndroidDownload(anchor);
  });
}
