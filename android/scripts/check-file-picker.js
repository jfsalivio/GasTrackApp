(async () => {
  location.hash = '#/inventory';
  await new Promise(resolve => setTimeout(resolve, 1500));
  const input = document.querySelector('input[type="file"]');
  if (!input) throw new Error('Inventory file input is missing.');
  input.click();
  return {filePickerRequested: true, accepts: input.accept};
})()
