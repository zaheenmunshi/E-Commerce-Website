// Cart and wishlist, kept in localStorage. Reads are defensive: corrupt or old data is dropped, never thrown.
import { SIZES, type Size } from './types.js';

export type ListName = 'cart' | 'wishlist';

export interface CartItem {
  productId: string;
  name: string;
  shortDescription: string;
  image: string;
  price: number;
  size: Size | null;
  quantity: number;
}

export const MAX_QUANTITY = 9;

function isSize(value: unknown): value is Size {
  return typeof value === 'string' && (SIZES as readonly string[]).includes(value);
}

function isCartItem(value: unknown): value is CartItem {
  if (typeof value !== 'object' || value === null) return false;
  const item = value as Record<string, unknown>;
  return (
    typeof item['productId'] === 'string' &&
    typeof item['name'] === 'string' &&
    typeof item['shortDescription'] === 'string' &&
    typeof item['image'] === 'string' &&
    typeof item['price'] === 'number' &&
    Number.isFinite(item['price']) &&
    (item['size'] === null || isSize(item['size'])) &&
    Number.isInteger(item['quantity']) &&
    (item['quantity'] as number) >= 1 &&
    (item['quantity'] as number) <= MAX_QUANTITY
  );
}

export function readList(list: ListName): CartItem[] {
  try {
    const parsed: unknown = JSON.parse(localStorage.getItem(list) ?? '[]');
    return Array.isArray(parsed) ? parsed.filter(isCartItem) : [];
  } catch {
    return [];
  }
}

function writeList(list: ListName, items: CartItem[]): void {
  try {
    localStorage.setItem(list, JSON.stringify(items));
  } catch {
    // storage full or disabled: the list just isn't persisted
  }
}

/** Adds one piece; the same product in the same size is merged into one line. */
export function addItem(list: ListName, item: Omit<CartItem, 'quantity'>): void {
  const items = readList(list);
  const existing = items.find((line) => line.productId === item.productId && line.size === item.size);
  if (existing) {
    existing.quantity = Math.min(MAX_QUANTITY, existing.quantity + 1);
  } else {
    items.push({ ...item, quantity: 1 });
  }
  writeList(list, items);
}

export function setQuantity(list: ListName, index: number, quantity: number): void {
  const items = readList(list);
  const item = items[index];
  if (item) {
    item.quantity = Math.min(MAX_QUANTITY, Math.max(1, quantity));
    writeList(list, items);
  }
}

export function removeItem(list: ListName, index: number): void {
  writeList(list, readList(list).filter((_item, i) => i !== index));
}

export function clearList(list: ListName): void {
  writeList(list, []);
}

export function totalPrice(items: CartItem[]): number {
  return Math.round(items.reduce((sum, item) => sum + item.price * item.quantity, 0) * 100) / 100;
}
