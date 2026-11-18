import { renderCartList, showBill } from '../components/cart-list.js';
import { renderFooter } from '../components/footer.js';
import { renderNav } from '../components/nav.js';
import { qs } from '../lib/dom.js';

renderNav();
renderFooter();

renderCartList(qs('.cart'), 'cart', showBill);
renderCartList(qs('.wishlist'), 'wishlist');
