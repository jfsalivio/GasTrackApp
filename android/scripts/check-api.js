(async () => {
  const results = [];
  for (const path of ['/health', '/auth/me', '/products', '/inventory', '/settings']) {
    results.push(await fetch(window.__GASTRACK_API_BASE_URL__ + path, {headers: {Authorization: 'Bearer ' + localStorage.getItem('token')}})
      .then(async response => { const data = await response.json(); return {path, status: response.status, count: Array.isArray(data) ? data.length : undefined, error: data.error}; })
      .catch(error => ({path, error: error.message})));
  }
  return {results, pageMessage: document.querySelector('.pos')?.innerText.slice(-600)};
})()
