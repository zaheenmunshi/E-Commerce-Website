// Typed calls to the REST API, one function per endpoint.
import { acceptAuthResponse, authFetch } from './auth.js';
import { jsonInit, readJson, requireJson } from './http.js';
import type { AuthResponse, Order, OrderRequest, Product, ProductRequest, SellerApplication, User } from './types.js';

const encode = encodeURIComponent;

// ---- storefront (public) ----

export async function productsByTag(tag: string): Promise<Product[]> {
  return requireJson<Product[]>(await fetch(`/api/products?tag=${encode(tag)}`));
}

export async function searchProducts(query: string): Promise<Product[]> {
  return requireJson<Product[]>(await fetch(`/api/products/search?q=${encode(query)}`));
}

export async function getProduct(id: string): Promise<Product> {
  return requireJson<Product>(await fetch(`/api/products/${encode(id)}`));
}

export async function similarProducts(id: string): Promise<Product[]> {
  return requireJson<Product[]>(await fetch(`/api/products/${encode(id)}/similar`));
}

// ---- seller dashboard (bearer token, ROLE_SELLER) ----

/** Becoming a seller returns a new access token that carries the SELLER role. */
export async function applyAsSeller(application: SellerApplication): Promise<User> {
  const auth = await requireJson<AuthResponse>(await authFetch('/api/seller/apply', jsonInit('POST', application)));
  return acceptAuthResponse(auth);
}

export async function myProducts(): Promise<Product[]> {
  return requireJson<Product[]>(await authFetch('/api/seller/products'));
}

export async function myProduct(id: string): Promise<Product> {
  return requireJson<Product>(await authFetch(`/api/seller/products/${encode(id)}`));
}

export async function saveProduct(product: ProductRequest, id: string | null): Promise<Product> {
  const init = jsonInit(id ? 'PUT' : 'POST', product);
  return requireJson<Product>(await authFetch(id ? `/api/seller/products/${encode(id)}` : '/api/seller/products', init));
}

export async function deleteProduct(id: string): Promise<void> {
  await readJson<null>(await authFetch(`/api/seller/products/${encode(id)}`, { method: 'DELETE' }));
}

/** Uploads one product image; returns its public path, e.g. /uploads/<uuid>.jpg. */
export async function uploadImage(file: File): Promise<string> {
  const form = new FormData();
  form.append('image', file);
  const { url } = await requireJson<{ url: string }>(await authFetch('/api/seller/uploads', { method: 'POST', body: form }));
  return url;
}

// ---- orders (bearer token) ----

export async function placeOrder(order: OrderRequest): Promise<Order> {
  return requireJson<Order>(await authFetch('/api/orders', jsonInit('POST', order)));
}
