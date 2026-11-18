// Site footer with the category link lists.
import { escapeHtml } from '../lib/dom.js';

const CATEGORY_LINKS = [
  't-shirts', 'sweatshirts', 'shirts', 'jeans', 'trousers', 'shoes', 'casuals', 'formals', 'sports', 'watch',
];

function categoryList(title: string): string {
  const items = CATEGORY_LINKS.map(
    (item) =>
      `<li><a href="/search?q=${encodeURIComponent(item)}" class="footer-link">${escapeHtml(item)}</a></li>`,
  ).join('');
  return `<ul class="category"><li class="category-title">${escapeHtml(title)}</li>${items}</ul>`;
}

export function renderFooter(): void {
  const footer = document.querySelector('footer');
  if (!footer) return;
  footer.innerHTML = `
    <div class="footer-content">
      <img src="/img/light-logo.png" class="logo" alt="Clothing">
      <div class="footer-ul-container">${categoryList('men')}${categoryList('women')}</div>
    </div>
    <p class="footer-title">about company</p>
    <p class="info">Clothing brings you the best apparels from independent sellers: shirts, t-shirts, dresses,
      shoes and accessories, delivered to your door.</p>
    <p class="info">support emails - help@clothing.com, customersupport@clothing.com</p>
    <p class="info">telephone - 180 00 00 001, 180 00 00 002</p>
    <div class="footer-social-container">
      <div>
        <a href="#" class="social-link">terms & services</a>
        <a href="#" class="social-link">privacy page</a>
      </div>
      <div>
        <a href="#" class="social-link">instagram</a>
        <a href="#" class="social-link">facebook</a>
        <a href="#" class="social-link">twitter</a>
      </div>
    </div>
    <p class="footer-credit">Clothing, Best apparels online store</p>`;
}
