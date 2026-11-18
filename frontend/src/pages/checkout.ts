import { renderCartList, showBill } from '../components/cart-list.js';
import { renderNav } from '../components/nav.js';
import { placeOrder } from '../lib/api.js';
import { requireUser } from '../lib/auth.js';
import { clearList, readList } from '../lib/cart-store.js';
import { inputValue, qs } from '../lib/dom.js';
import type { Address } from '../lib/types.js';
import { showAlert, withLoader } from '../lib/ui.js';

renderNav();

const cart = qs('.cart');
const placeOrderButton = qs<HTMLButtonElement>('.place-order-btn');

function readAddress(): Address | null {
  const address: Address = {
    address: inputValue('#address'),
    street: inputValue('#street'),
    city: inputValue('#city'),
    state: inputValue('#state'),
    pincode: inputValue('#pincode'),
    landmark: inputValue('#landmark'),
  };
  if (Object.values(address).some((value) => !value)) {
    showAlert('fill all the inputs first');
    return null;
  }
  if (!/^\d{6}$/.test(address.pincode)) {
    showAlert('enter a valid 6 digit pin code');
    return null;
  }
  return address;
}

async function submitOrder(): Promise<void> {
  const items = readList('cart');
  if (!items.length) {
    showAlert('your cart is empty');
    return;
  }
  const address = readAddress();
  if (!address) return;
  placeOrderButton.disabled = true;
  const order = await withLoader(() =>
    placeOrder({
      items: items.map((item) => ({ productId: item.productId, size: item.size, quantity: item.quantity })),
      address,
    }),
  );
  placeOrderButton.disabled = false;
  if (order) {
    clearList('cart');
    renderCartList(cart, 'cart', showBill);
    showAlert('your order is placed', 'success');
  }
}

void requireUser().then(() => {
  renderCartList(cart, 'cart', showBill);
  placeOrderButton.addEventListener('click', () => void submitOrder());
});
