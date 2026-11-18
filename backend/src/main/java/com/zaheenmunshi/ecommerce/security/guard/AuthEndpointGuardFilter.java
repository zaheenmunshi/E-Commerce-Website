package com.zaheenmunshi.ecommerce.security.guard;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Guards the cookie-using auth endpoints (POST /api/auth/*):
 * <ol>
 * <li>CSRF defence in depth on top of the SameSite=Strict cookie: the request must be a
 * same-origin script call (custom {@code X-Requested-With} header, which cross-site pages
 * cannot send without a CORS preflight we never allow, plus Fetch Metadata).</li>
 * <li>Per-IP rate limit for login and signup, against credential stuffing and spam.</li>
 * </ol>
 */
public class AuthEndpointGuardFilter extends OncePerRequestFilter {

	public static final String REQUESTED_WITH = "X-Requested-With";

	private static final Set<String> RATE_LIMITED = Set.of("/api/auth/login", "/api/auth/signup");

	private final Map<String, Window> windows = new ConcurrentHashMap<>();

	private final int requestsPerMinute;

	private final Clock clock;

	public AuthEndpointGuardFilter(int requestsPerMinute, Clock clock) {
		this.requestsPerMinute = requestsPerMinute;
		this.clock = clock;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !("POST".equals(request.getMethod()) && request.getRequestURI().startsWith("/api/auth/"));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String fetchSite = request.getHeader("Sec-Fetch-Site");
		if (request.getHeader(REQUESTED_WITH) == null || (fetchSite != null && !fetchSite.equals("same-origin"))) {
			reject(response, HttpStatus.FORBIDDEN, "cross-site request refused");
			return;
		}
		if (RATE_LIMITED.contains(request.getRequestURI()) && !allow(request.getRemoteAddr())) {
			reject(response, HttpStatus.TOO_MANY_REQUESTS, "too many attempts, please wait a minute");
			return;
		}
		chain.doFilter(request, response);
	}

	/** Fixed one-minute window per client IP. */
	private boolean allow(String clientIp) {
		long minute = clock.millis() / 60_000;
		if (windows.size() > 10_000) {
			windows.values().removeIf(window -> window.minute() != minute);
		}
		Window window = windows.compute(clientIp,
				(ip, old) -> old == null || old.minute() != minute ? new Window(minute, new AtomicInteger()) : old);
		return window.count().incrementAndGet() <= requestsPerMinute;
	}

	private static void reject(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.getWriter()
			.write("{\"status\":" + status.value() + ",\"title\":\"" + status.getReasonPhrase() + "\",\"detail\":\""
					+ detail + "\"}");
	}

	private record Window(long minute, AtomicInteger count) {
	}

}
