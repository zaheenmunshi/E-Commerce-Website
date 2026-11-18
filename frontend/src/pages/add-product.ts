// Add or edit a product (/add-product or /add-product/{id}); sellers only.
import { myProduct, saveProduct, uploadImage } from '../lib/api.js';
import { requireUser } from '../lib/auth.js';
import { inputValue, isChecked, qs, qsa } from '../lib/dom.js';
import { SIZES, type Product, type ProductRequest, type Size } from '../lib/types.js';
import { showAlert, withLoader } from '../lib/ui.js';

const MAX_IMAGE_BYTES = 5 * 1024 * 1024;
const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
const MIN_STOCK = 20;

const actualPrice = qs<HTMLInputElement>('#actual-price');
const discount = qs<HTMLInputElement>('#discount');
const sellingPrice = qs<HTMLInputElement>('#sell-price');
const uploadInputs = qsa<HTMLInputElement>('.fileupload');
const preview = qs('.product-image');

/** Image paths by upload slot (0-3); empty slots are skipped on save. */
const images: (string | null)[] = uploadInputs.map(() => null);
const pathParts = location.pathname.split('/').filter(Boolean);
const productId = pathParts[0] === 'add-product' && pathParts[1] ? decodeURIComponent(pathParts[1]) : null;

// ---- pricing: selling price follows the discount and vice versa ----

const toNumber = (input: HTMLInputElement) => (input.value.trim() === '' ? null : Number(input.value));

function updateSellingPrice(): void {
  const price = toNumber(actualPrice);
  let percent = toNumber(discount);
  if (percent !== null && percent > 100) {
    percent = 100;
    discount.value = '100';
  }
  if (price !== null && percent !== null) {
    sellingPrice.value = (Math.round(price * (100 - percent)) / 100).toString();
  }
}

discount.addEventListener('input', updateSellingPrice);
actualPrice.addEventListener('input', updateSellingPrice);
sellingPrice.addEventListener('input', () => {
  const price = toNumber(actualPrice);
  const sell = toNumber(sellingPrice);
  if (price && sell !== null) {
    discount.value = Math.min(100, Math.max(0, Math.round(100 - (sell / price) * 100))).toString();
  }
});

// ---- images ----

function showImage(slot: number, url: string): void {
  const input = uploadInputs[slot];
  const label = input ? document.querySelector<HTMLElement>(`label[for="${input.id}"]`) : null;
  if (label) label.style.backgroundImage = `url("${url}")`;
  preview.style.backgroundImage = `url("${url}")`;
}

uploadInputs.forEach((input, slot) => {
  input.addEventListener('change', async () => {
    const file = input.files?.[0];
    if (!file) return;
    if (!IMAGE_TYPES.includes(file.type)) {
      showAlert('upload image only (jpg, png, gif or webp)');
      return;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      showAlert('image must be smaller than 5 MB');
      return;
    }
    const url = await withLoader(() => uploadImage(file));
    if (url) {
      images[slot] = url;
      showImage(slot, url);
    }
  });
});

// ---- form ----

function selectedSizes(): Size[] {
  return qsa<HTMLInputElement>('.size-checkbox')
    .filter((box) => box.checked && (SIZES as readonly string[]).includes(box.value))
    .map((box) => box.value as Size);
}

function readForm(draft: boolean): ProductRequest {
  const percent = toNumber(discount);
  return {
    name: inputValue('#product-name'),
    shortDescription: inputValue('#short-des'),
    description: inputValue('#des'),
    images: images.filter((url): url is string => url !== null),
    sizes: selectedSizes(),
    actualPrice: toNumber(actualPrice),
    discountPercent: percent === null ? null : Math.round(percent),
    stock: toNumber(qs<HTMLInputElement>('#stock')),
    tags: inputValue('#tags').split(',').map((tag) => tag.trim()).filter(Boolean),
    termsAccepted: isChecked('#tac'),
    draft,
  };
}

/** Same rules and messages as the server's publish check (ProductService). */
function validateForPublishing(product: ProductRequest): string | null {
  if (!product.name) return 'enter product name';
  if (product.shortDescription.length < 10 || product.shortDescription.length > 100) {
    return 'short description must be between 10 to 100 letters long';
  }
  if (!product.description) return 'enter detail description about the product';
  if (!product.images.length) return 'upload at least one product image';
  if (!product.sizes.length) return 'select at least one size';
  if (product.actualPrice === null || product.discountPercent === null) return 'you must add pricings';
  if (product.stock === null || product.stock < MIN_STOCK) return 'you should have at least 20 items in stock';
  if (!product.tags.length) return 'enter few tags to help ranking your product in search';
  if (!product.termsAccepted) return 'you must agree to our terms and conditions';
  return null;
}

async function submit(draft: boolean): Promise<void> {
  const product = readForm(draft);
  const problem = draft ? (product.name ? null : 'enter product name') : validateForPublishing(product);
  if (problem) {
    showAlert(problem);
    return;
  }
  if (await withLoader(() => saveProduct(product, productId))) {
    location.href = '/seller';
  }
}

qs('#add-btn').addEventListener('click', () => void submit(false));
qs('#save-btn').addEventListener('click', () => void submit(true));

function fillForm(product: Product): void {
  qs<HTMLInputElement>('#product-name').value = product.name;
  qs<HTMLInputElement>('#short-des').value = product.shortDescription ?? '';
  qs<HTMLTextAreaElement>('#des').value = product.description ?? '';
  actualPrice.value = product.actualPrice?.toString() ?? '';
  discount.value = product.discountPercent?.toString() ?? '';
  sellingPrice.value = product.sellingPrice?.toString() ?? '';
  qs<HTMLInputElement>('#stock').value = product.stock?.toString() ?? '';
  qs<HTMLTextAreaElement>('#tags').value = product.tags.join(', ');
  product.images.slice(0, images.length).forEach((url, slot) => {
    images[slot] = url;
    showImage(slot, url);
  });
  qsa<HTMLInputElement>('.size-checkbox').forEach((box) => {
    box.checked = (product.sizes as string[]).includes(box.value);
  });
}

void requireUser().then(async (user) => {
  if (!user.seller) {
    location.replace('/seller');
    return;
  }
  if (productId) {
    const product = await withLoader(() => myProduct(productId));
    if (product) fillForm(product);
    else location.replace('/seller');
  }
});
