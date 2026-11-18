// Storefront product cards: a grid (search results) or a horizontal slider (home, similar products).
import { escapeHtml, qs, safeImageUrl } from '../lib/dom.js';
import type { Product } from '../lib/types.js';
import { formatPrice } from '../lib/ui.js';

function cardHtml(product: Product): string {
  const href = `/products/${encodeURIComponent(product.id)}`;
  const discount = product.discountPercent ? `<span class="discount-tag">${escapeHtml(product.discountPercent)}% off</span>` : '';
  return `
    <div class="product-card">
      <div class="product-image">
        ${discount}
        <img src="${escapeHtml(safeImageUrl(product.images[0]))}" class="product-thumb" alt="${escapeHtml(product.name)}">
      </div>
      <div class="product-info" data-href="${escapeHtml(href)}" role="link" tabindex="0">
        <h2 class="product-brand">${escapeHtml(product.name)}</h2>
        <p class="product-short-des">${escapeHtml(product.shortDescription)}</p>
        <span class="price">${escapeHtml(formatPrice(product.sellingPrice))}</span>
        <span class="actual-price">${escapeHtml(formatPrice(product.actualPrice))}</span>
      </div>
    </div>`;
}

/** Cards are plain divs (keeps the original styling); clicks and Enter open the product page. */
function wireCardLinks(root: HTMLElement): void {
  const open = (event: Event) => {
    const target = event.target instanceof Element ? event.target.closest<HTMLElement>('[data-href]') : null;
    if (target?.dataset['href']) {
      location.href = target.dataset['href'];
    }
  };
  root.addEventListener('click', open);
  root.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') open(event);
  });
}

function containerHtml(products: Product[]): string {
  return `<div class="product-container">${products.map(cardHtml).join('')}</div>`;
}

export function renderProductGrid(container: HTMLElement, products: Product[]): void {
  container.innerHTML = products.length ? containerHtml(products) : '<p class="product-short-des">no products found</p>';
  wireCardLinks(container);
}

export function renderProductSlider(container: HTMLElement, title: string, products: Product[]): void {
  if (!products.length) return;
  const section = document.createElement('section');
  section.className = 'product';
  section.innerHTML = `
    <h2 class="product-category">${escapeHtml(title)}</h2>
    <button class="pre-btn" aria-label="previous"><img src="/img/arrow.png" alt=""></button>
    <button class="nxt-btn" aria-label="next"><img src="/img/arrow.png" alt=""></button>
    ${containerHtml(products)}`;
  container.append(section);
  const track = qs('.product-container', section);
  qs('.nxt-btn', section).addEventListener('click', () => {
    track.scrollLeft += track.getBoundingClientRect().width;
  });
  qs('.pre-btn', section).addEventListener('click', () => {
    track.scrollLeft -= track.getBoundingClientRect().width;
  });
  wireCardLinks(section);
}
