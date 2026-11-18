// Small product rows for the cart, wishlist and checkout pages, with +/- counters and remove buttons.
import { MAX_QUANTITY, readList, removeItem, setQuantity, totalPrice, type CartItem, type ListName } from '../lib/cart-store.js';
import { escapeHtml, safeImageUrl } from '../lib/dom.js';
import { formatPrice } from '../lib/ui.js';

function rowHtml(item: CartItem, index: number): string {
  const size = item.size ? ` · size ${item.size.toUpperCase()}` : '';
  return `
    <div class="sm-product" data-index="${index}">
      <img src="${escapeHtml(safeImageUrl(item.image))}" class="sm-product-img" alt="${escapeHtml(item.name)}">
      <div class="sm-text">
        <p class="sm-product-name">${escapeHtml(item.name)}</p>
        <p class="sm-des">${escapeHtml(item.shortDescription + size)}</p>
      </div>
      <div class="item-counter">
        <button class="counter-btn decrement" data-action="decrement" aria-label="one less">-</button>
        <p class="item-count">${item.quantity}</p>
        <button class="counter-btn increment" data-action="increment" aria-label="one more">+</button>
      </div>
      <p class="sm-price">${escapeHtml(formatPrice(item.price * item.quantity))}</p>
      <button class="sm-delete-btn" data-action="remove" aria-label="remove"><img src="/img/close.png" alt=""></button>
    </div>`;
}

/** Re-renders the list on every change and reports the new items (e.g. to update the bill). */
export function renderCartList(container: HTMLElement, list: ListName, onChange?: (items: CartItem[]) => void): void {
  const items = readList(list);
  container.innerHTML = items.length
    ? items.map(rowHtml).join('')
    : `<img src="/img/no-products.png" class="empty-img" alt="your ${list} is empty">`;
  onChange?.(items);

  // property handler (not addEventListener) so re-rendering never stacks listeners
  container.onclick = (event) => {
    const button = event.target instanceof Element ? event.target.closest<HTMLElement>('[data-action]') : null;
    const row = button?.closest<HTMLElement>('[data-index]');
    const index = Number(row?.dataset['index']);
    const item = items[index];
    if (!button || !item) return;
    const action = button.dataset['action'];
    if (action === 'increment' && item.quantity < MAX_QUANTITY) setQuantity(list, index, item.quantity + 1);
    else if (action === 'decrement' && item.quantity > 1) setQuantity(list, index, item.quantity - 1);
    else if (action === 'remove') removeItem(list, index);
    else return;
    renderCartList(container, list, onChange);
  };
}

export function showBill(items: CartItem[]): void {
  const bill = document.querySelector('.bill');
  if (bill) {
    bill.textContent = formatPrice(totalPrice(items));
  }
}
