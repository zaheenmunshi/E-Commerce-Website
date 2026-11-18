// Shapes of the backend's JSON (see backend/.../dto). Nullable fields belong to drafts.

export interface User {
  id: string;
  name: string;
  email: string;
  seller: boolean;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresAt: string;
  user: User;
}

export const SIZES = ['xs', 's', 'm', 'l', 'xl', 'xxl', 'xxxl'] as const;
export type Size = (typeof SIZES)[number];

export interface Product {
  id: string;
  name: string;
  shortDescription: string | null;
  description: string | null;
  images: string[];
  sizes: Size[];
  actualPrice: number | null;
  discountPercent: number | null;
  sellingPrice: number | null;
  stock: number | null;
  tags: string[];
  draft: boolean;
}

export interface ProductRequest {
  name: string;
  shortDescription: string;
  description: string;
  images: string[];
  sizes: Size[];
  actualPrice: number | null;
  discountPercent: number | null;
  stock: number | null;
  tags: string[];
  termsAccepted: boolean;
  draft: boolean;
}

export interface SellerApplication {
  businessName: string;
  address: string;
  about: string;
  phone: string;
  termsAccepted: boolean;
  informationConfirmed: boolean;
}

export interface SignupRequest {
  name: string;
  email: string;
  password: string;
  phone: string;
  termsAccepted: boolean;
  marketingOptIn: boolean;
}

export interface Address {
  address: string;
  street: string;
  city: string;
  state: string;
  pincode: string;
  landmark: string;
}

export interface OrderRequest {
  items: { productId: string; size: Size | null; quantity: number }[];
  address: Address;
}

export interface Order {
  id: string;
  status: string;
  totalAmount: number;
  createdAt: string;
  items: { productId: string; productName: string; size: string | null; quantity: number; unitPrice: number }[];
}

/** RFC 9457 problem details, as returned by every API error. */
export interface Problem {
  status?: number;
  title?: string;
  detail?: string;
  errors?: string[];
}
