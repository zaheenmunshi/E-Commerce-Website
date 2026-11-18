// Top navigation: logo, search box, account popup (log in / log out) and cart link.
import { logout, session } from '../lib/auth.js';
import { escapeHtml, qs } from '../lib/dom.js';
import type { User } from '../lib/types.js';

const CATEGORIES = ['women', 'men', 'kids', 'accessories'];

function markup(): string {
  const links = CATEGORIES.map(
    (category) =>
      `<li class="link-item"><a href="/search?q=${encodeURIComponent(category)}" class="link">${escapeHtml(category)}</a></li>`,
  ).join('');
  return `
    <div class="nav">
      <a href="/"><img src="/img/dark-logo.png" class="brand-logo" alt="Clothing"></a>
      <div class="nav-items">
        <div class="search">
          <input type="text" class="search-box" placeholder="search brand, product" maxlength="100">
          <button class="search-btn">search</button>
        </div>
        <div class="nav-account">
          <img src="/img/user.png" id="user-img" alt="account">
          <div class="login-logout-popup hide">
            <p class="account-info">log in to place order</p>
            <button class="btn" id="user-btn">log in</button>
            <a class="popup-link" id="seller-link" href="/seller" hidden>seller dashboard</a>
          </div>
        </div>
        <a href="/cart"><img src="/img/cart.png" alt="cart"></a>
      </div>
    </div>
    <ul class="links-container">
      <li class="link-item"><a href="/" class="link">home</a></li>
      ${links}
    </ul>`;
}

function wireSearch(nav: HTMLElement): void {
  const box = qs<HTMLInputElement>('.search-box', nav);
  const search = () => {
    const query = box.value.trim();
    if (query) {
      location.href = `/search?q=${encodeURIComponent(query)}`;
    }
  };
  qs('.search-btn', nav).addEventListener('click', search);
  box.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') search();
  });
}

function showAccount(nav: HTMLElement, user: User | null): void {
  const info = qs('.account-info', nav);
  const button = qs<HTMLButtonElement>('#user-btn', nav);
  qs('#seller-link', nav).hidden = user === null;
  if (user) {
    info.textContent = `log in as, ${user.name}`;
    button.textContent = 'log out';
    button.addEventListener('click', async () => {
      await logout();
      location.reload();
    });
  } else {
    info.textContent = 'log in to place order';
    button.textContent = 'log in';
    button.addEventListener('click', () => {
      location.href = `/login?next=${encodeURIComponent(location.pathname + location.search)}`;
    });
  }
}

export function renderNav(): void {
  const nav = document.querySelector<HTMLElement>('.navbar');
  if (!nav) return;
  nav.innerHTML = markup();
  const popup = qs('.login-logout-popup', nav);
  qs('#user-img', nav).addEventListener('click', () => popup.classList.toggle('hide'));
  wireSearch(nav);
  void session().then((user) => showAccount(nav, user));
}
