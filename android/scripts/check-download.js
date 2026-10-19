(async () => {
  location.hash = '#/inventory';
  await new Promise(resolve => setTimeout(resolve, 1200));
  const button = [...document.querySelectorAll('main button')].find(button => button.textContent.trim() === 'CSV Template');
  if (!button) throw new Error('Inventory template action is missing.');
  button.click();
  return {templateClicked: true};
})()
