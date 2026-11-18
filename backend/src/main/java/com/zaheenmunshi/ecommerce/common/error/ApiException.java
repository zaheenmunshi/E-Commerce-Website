package com.zaheenmunshi.ecommerce.common.error;

import org.springframework.http.HttpStatus;

/**
 * An error whose message is safe to show to the shopper; rendered as an RFC 9457 problem
 * response by {@link ApiExceptionHandler}.
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;

	private ApiException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	public static ApiException badRequest(String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, message);
	}

	public static ApiException unauthorized(String message) {
		return new ApiException(HttpStatus.UNAUTHORIZED, message);
	}

	public static ApiException forbidden(String message) {
		return new ApiException(HttpStatus.FORBIDDEN, message);
	}

	public static ApiException notFound(String message) {
		return new ApiException(HttpStatus.NOT_FOUND, message);
	}

	public static ApiException conflict(String message) {
		return new ApiException(HttpStatus.CONFLICT, message);
	}

	public static ApiException tooManyRequests(String message) {
		return new ApiException(HttpStatus.TOO_MANY_REQUESTS, message);
	}

	public HttpStatus getStatus() {
		return status;
	}

}
