(async () => {
  const pause = milliseconds => new Promise(resolve => setTimeout(resolve, milliseconds));
  const click = (selector, text) => {
    const button = [...document.querySelectorAll(selector)].find(button => button.textContent.trim() === text);
    if (!button) throw new Error('Missing action: ' + text);
    button.click();
  };
  location.hash = '#/products';
  await pause(1300);
  document.querySelector('.mobile-app-header button').click();
  await pause(200);
  const menuOpened = !!document.querySelector('.mobile-menu-open');
  click('.sidebar-nav-item', 'Products');
  await pause(300);
  const menuClosed = !document.querySelector('.mobile-menu-open');
  click('main button', 'Add Product');
  await pause(700);
  const productFields = [...document.querySelectorAll('.add-product-modal-card label')].map(label => label.textContent.trim());
  const lookupManagers = [...document.querySelectorAll('.lookup-settings')].map(button => button.getAttribute('aria-label'));
  click('.add-product-modal-card button', 'Cancel');
  await pause(100);
  location.hash = '#/pos';
  await pause(1600);
  const originalHeldCarts = localStorage.getItem('gastrack_held_carts');
  const availableProduct = document.querySelector('.product-card:not(:disabled)');
  if (!availableProduct) throw new Error('No available product for a reversible cart check.');
  availableProduct.click();
  await pause(100);
  const cartAfterAdd = document.querySelectorAll('.cart-item').length;
  click('.cart-action-btn', 'Hold');
  await pause(200);
  const heldCount = JSON.parse(localStorage.getItem('gastrack_held_carts') || '[]').length;
  const cartAfterHold = document.querySelectorAll('.cart-item').length;
  const restore = [...document.querySelectorAll('.cart-action-btn')].find(button => button.textContent.trim().startsWith('Restore'));
  restore.click();
  await pause(200);
  const restoreButton = [...document.querySelectorAll('button')].find(button => button.textContent.trim() === 'Restore' && !button.matches('.cart-action-btn'));
  if (!restoreButton) throw new Error('Held-cart restore action is missing.');
  restoreButton.click();
  await pause(200);
  const cartAfterRestore = document.querySelectorAll('.cart-item').length;
  // Restore only the storage modified by this check. No checkout is submitted.
  if (originalHeldCarts == null) localStorage.removeItem('gastrack_held_carts');
  else localStorage.setItem('gastrack_held_carts', originalHeldCarts);
  location.hash = '#/inventory';
  await pause(500);
  return {menuOpened, menuClosed, productFields, lookupManagers, cartAfterAdd, heldCount, cartAfterHold, cartAfterRestore};
})()
