(async () => {
  location.hash = '#/pos';
  await new Promise(resolve => setTimeout(resolve, 1500));
  const result = await fetch(window.__GASTRACK_API_BASE_URL__ + '/uploads/acc-lpg-hose.png').then(async response => ({status: response.status, type: response.headers.get('content-type'), bytes: (await response.blob()).size})).catch(error => ({error: error.message}));
  const image = new Image();
  const imageResult = await new Promise(resolve => {
    image.onload = () => resolve({loaded: true, width: image.naturalWidth});
    image.onerror = () => resolve({loaded: false});
    image.src = window.__GASTRACK_API_BASE_URL__ + '/uploads/acc-lpg-hose.png';
  });
  await new Promise(resolve => setTimeout(resolve, 1800));
  return {fetch: result, directHttpImage: imageResult, images: [...document.querySelectorAll('.product-image-img')].map(image => ({alt: image.alt, loaded: image.complete && image.naturalWidth > 0, placeholder: image.src.startsWith('data:'), source: image.src.slice(0, 140)}))};
})()
