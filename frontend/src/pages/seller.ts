// Seller dashboard: apply to become a seller, or manage your own product listings.
import { applyAsSeller, deleteProduct, myProducts } from '../lib/api.js';
import { requireUser } from '../lib/auth.js';
import { escapeHtml, inputValue, isChecked, qs, safeImageUrl } from '../lib/dom.js';
import type { Product, SellerApplication } from '../lib/types.js';
import { formatPrice, showAlert, withLoader } from '../lib/ui.js';

const becomeSeller = qs('.become-seller');
const applyForm = qs('.apply-form');
const listing = qs('.product-listing');
const productContainer = qs('.product-container', listing);
const deletePopup = qs('.delete-alert');
let productToDelete: string | null = null;

function cardHtml(product: Product): string {
  const id = encodeURIComponent(product.id);
  return `
    <div class="product-card">
      <div class="product-image">
        ${product.draft ? '<span class="tag">Draft</span>' : ''}
        <img src="${escapeHtml(safeImageUrl(product.images[0]))}" class="product-thumb" alt="${escapeHtml(product.name)}">
        <button class="card-action-btn edit-btn" data-action="edit" data-id="${escapeHtml(id)}" aria-label="edit"><img src="/img/edit.png" alt=""></button>
        <button class="card-action-btn open-btn" data-action="open" data-id="${escapeHtml(id)}" aria-label="open"><img src="/img/open.png" alt=""></button>
        <button class="card-action-btn delete-popup-btn" data-action="delete" data-id="${escapeHtml(product.id)}" aria-label="delete"><img src="/img/delete.png" alt=""></button>
      </div>
      <div class="product-info">
        <h2 class="product-brand">${escapeHtml(product.name)}</h2>
        <p class="product-short-des">${escapeHtml(product.shortDescription)}</p>
        <span class="price">${escapeHtml(formatPrice(product.sellingPrice))}</span>
        <span class="actual-price">${escapeHtml(formatPrice(product.actualPrice))}</span>
      </div>
    </div>`;
}

async function showProducts(): Promise<void> {
  const products = await withLoader(myProducts);
  if (!products) return;
  listing.classList.remove('hide');
  qs('.no-product-image').classList.toggle('hide', products.length > 0);
  productContainer.innerHTML = products.map(cardHtml).join('');
}

productContainer.addEventListener('click', (event) => {
  const button = event.target instanceof Element ? event.target.closest<HTMLElement>('[data-action]') : null;
  const id = button?.dataset['id'];
  if (!button || !id) return;
  switch (button.dataset['action']) {
    case 'edit':
      location.href = `/add-product/${id}`;
      break;
    case 'open':
      location.href = `/products/${id}`;
      break;
    case 'delete':
      productToDelete = id;
      deletePopup.style.display = 'flex';
      break;
  }
});

qs('.close-btn', deletePopup).addEventListener('click', () => {
  productToDelete = null;
  deletePopup.style.display = '';
});

qs('.delete-btn', deletePopup).addEventListener('click', async () => {
  const id = productToDelete;
  deletePopup.style.display = '';
  productToDelete = null;
  if (id && (await withLoader(() => deleteProduct(id).then(() => true)))) {
    await showProducts();
  }
});

qs('#add-product-btn').addEventListener('click', () => {
  location.href = '/add-product';
});

qs('#apply-btn').addEventListener('click', () => {
  becomeSeller.classList.add('hide');
  applyForm.classList.remove('hide');
});

function readApplication(): SellerApplication | null {
  const application: SellerApplication = {
    businessName: inputValue('#business-name'),
    address: inputValue('#business-add'),
    about: inputValue('#about'),
    phone: inputValue('#number'),
    termsAccepted: isChecked('#terms-and-cond'),
    informationConfirmed: isChecked('#legitInfo'),
  };
  if (!application.businessName || !application.address || !application.about || !application.phone) {
    showAlert('fill all the inputs');
    return null;
  }
  if (!/^\d{10,15}$/.test(application.phone)) {
    showAlert('invalid number, please enter valid one');
    return null;
  }
  if (!application.termsAccepted || !application.informationConfirmed) {
    showAlert('you must agree to our terms and conditions');
    return null;
  }
  return application;
}

qs('#apply-form-btn').addEventListener('click', async () => {
  const application = readApplication();
  if (application && (await withLoader(() => applyAsSeller(application)))) {
    applyForm.classList.add('hide');
    await showProducts();
  }
});

void requireUser().then((user) => {
  if (user.seller) {
    void showProducts();
  } else {
    becomeSeller.classList.remove('hide');
  }
});
