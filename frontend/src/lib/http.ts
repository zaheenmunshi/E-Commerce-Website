// fetch wrapper: JSON in/out, problem+json errors turned into ApiError with a readable message.
import type { Problem } from './types.js';

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

const FALLBACK_MESSAGES: Record<number, string> = {
  401: 'please log in again',
  403: 'you are not allowed to do that',
  404: 'not found',
  413: 'that file is too large (max 5 MB)',
  429: 'too many attempts, please wait a minute',
};

export async function toApiError(response: Response): Promise<ApiError> {
  let detail: string | undefined;
  try {
    const problem = (await response.json()) as Problem;
    detail = typeof problem.detail === 'string' ? problem.detail : undefined;
  } catch {
    // not JSON (e.g. a proxy error page)
  }
  const message = detail ?? FALLBACK_MESSAGES[response.status] ?? 'something went wrong, please try again';
  return new ApiError(response.status, message);
}

/** Reads a successful response body; 204 / empty bodies become null. */
export async function readJson<T>(response: Response): Promise<T | null> {
  if (!response.ok) {
    throw await toApiError(response);
  }
  if (response.status === 204) {
    return null;
  }
  const text = await response.text();
  return text ? (JSON.parse(text) as T) : null;
}

export function jsonInit(method: string, body?: unknown): RequestInit {
  return body === undefined
    ? { method }
    : { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) };
}

/** Like readJson but for endpoints that always return a body. */
export async function requireJson<T>(response: Response): Promise<T> {
  const value = await readJson<T>(response);
  if (value === null) {
    throw new ApiError(response.status, 'unexpected empty response');
  }
  return value;
}
