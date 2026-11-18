import { renderFooter } from '../components/footer.js';
import { renderNav } from '../components/nav.js';
import { renderProductSlider } from '../components/product-cards.js';
import { getProduct, similarProducts } from '../lib/api.js';
import { addItem, type ListName } from '../lib/cart-store.js';
import { qs, qsa, safeImageUrl } from '../lib/dom.js';
import type { Product, Size } from '../lib/types.js';
import { formatPrice, showAlert } from '../lib/ui.js';

renderNav();
renderFooter();

let selectedSize: Size | null = null;

function showImages(product: Product): void {
  const slider = qs('.image-slider');
  const thumbs = qsa<HTMLImageElement>('.product-images img');
  const select = (thumb: HTMLImageElement) => {
    thumbs.forEach((other) => other.classList.toggle('active', other === thumb));
    slider.style.backgroundImage = `url("${thumb.src}")`;
  };
  thumbs.forEach((thumb, i) => {
    const image = product.images[i];
    if (image) {
      thumb.src = safeImageUrl(image);
      thumb.alt = product.name;
      thumb.addEventListener('click', () => select(thumb));
    } else {
      thumb.hidden = true;
    }
  });
  const first = thumbs[0];
  if (first && product.images.length) {
    select(first);
  } else {
    slider.style.backgroundImage = 'url("/img/no-image.png")';
  }
}

function showSizes(product: Product): void {
  const labels = qsa<HTMLLabelElement>('.size-radio-btn');
  labels.forEach((label) => {
    const input = document.getElementById(label.htmlFor);
    const size = input instanceof HTMLInputElement ? (input.value as Size) : null;
    if (!size || !product.sizes.includes(size)) {
      label.hidden = true;
      return;
    }
    label.addEventListener('click', () => {
      labels.forEach((other) => other.classList.toggle('check', other === label));
      selectedSize = size;
    });
  });
  if (!product.sizes.length) {
    qs('.product-sub-heading').hidden = true;
  }
}

function showDetails(product: Product): void {
  document.title = `Product - ${product.name}`;
  qs('.product-brand').textContent = product.name;
  qs('.product-short-des').textContent = product.shortDescription ?? '';
  qs('.des').textContent = product.description ?? '';
  qs('.product-price').textContent = formatPrice(product.sellingPrice);
  qs('.product-actual-price').textContent = formatPrice(product.actualPrice);
  qs('.product-discount').textContent = product.discountPercent ? `( ${product.discountPercent}% off )` : '';
}

function wireButtons(product: Product): void {
  const add = (list: ListName, button: HTMLButtonElement) => {
    if (product.sizes.length && !selectedSize) {
      showAlert('select a size first');
      return;
    }
    addItem(list, {
      productId: product.id,
      name: product.name,
      shortDescription: product.shortDescription ?? '',
      image: safeImageUrl(product.images[0]),
      price: product.sellingPrice ?? 0,
      size: selectedSize,
    });
    button.textContent = 'added';
  };
  const cartButton = qs<HTMLButtonElement>('.cart-btn');
  const wishlistButton = qs<HTMLButtonElement>('.wishlist-btn');
  cartButton.addEventListener('click', () => add('cart', cartButton));
  wishlistButton.addEventListener('click', () => add('wishlist', wishlistButton));
}

async function load(): Promise<void> {
  const id = decodeURIComponent(location.pathname.split('/').pop() ?? '');
  let product: Product;
  try {
    product = await getProduct(id);
  } catch {
    location.replace('/404');
    return;
  }
  showDetails(product);
  showImages(product);
  showSizes(product);
  wireButtons(product);
  const similar = await similarProducts(product.id).catch(() => []);
  renderProductSlider(qs('.container-for-card-slider'), 'similar products', similar);
}

void load();
