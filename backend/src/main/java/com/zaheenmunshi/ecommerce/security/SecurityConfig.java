package com.zaheenmunshi.ecommerce.security;

import java.time.Clock;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

import com.zaheenmunshi.ecommerce.config.AppProperties;
import com.zaheenmunshi.ecommerce.security.guard.AuthEndpointGuardFilter;
import com.zaheenmunshi.ecommerce.security.jwt.JwtUserConverter;
import com.zaheenmunshi.ecommerce.security.oauth.OAuth2LoginSuccessHandler;

/**
 * Stateless OAuth 2.0 security:
 * <ul>
 * <li>The API is an OAuth 2.0 <b>resource server</b>: every protected call needs a valid
 * JWT access token in {@code Authorization: Bearer}. No server session, so no CSRF on
 * the API (browsers never attach bearer tokens by themselves).</li>
 * <li>The refresh token lives in an HttpOnly SameSite=Strict cookie used only by
 * /api/auth/*, which {@link AuthEndpointGuardFilter} additionally guards.</li>
 * <li>"Continue with Google" is an OAuth 2.0 / OIDC <b>client</b> (authorization code +
 * PKCE), enabled when a client registration is configured.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

	private static final String CONTENT_SECURITY_POLICY = "default-src 'self'; img-src 'self' data: blob:; "
			+ "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

	private static final String PERMISSIONS_POLICY = "camera=(), microphone=(), geolocation=(), payment=(), usb=()";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtUserConverter jwtUserConverter,
			ObjectProvider<ClientRegistrationRepository> clientRegistrations,
			OAuth2LoginSuccessHandler oauth2LoginSuccessHandler, AppProperties properties, Clock clock)
			throws Exception {
		http.authorizeHttpRequests(auth -> auth
			.requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login", "/api/auth/refresh",
					"/api/auth/logout")
			.permitAll()
			.requestMatchers(HttpMethod.GET, "/api/auth/me", "/api/auth/providers").permitAll()
			.requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
			.requestMatchers("/api/seller/apply").hasRole("CUSTOMER")
			.requestMatchers("/api/seller/**").hasRole("SELLER")
			.requestMatchers("/api/**").authenticated()
			.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
			.requestMatchers("/actuator/**").denyAll()
			// pages, scripts, styles and images are public; the data behind them is not
			.anyRequest()
			.permitAll())
			.oauth2ResourceServer(resourceServer -> resourceServer
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtUserConverter))
				.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
				.accessDeniedHandler(new BearerTokenAccessDeniedHandler()))
			.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
				.accessDeniedHandler(new BearerTokenAccessDeniedHandler()))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.securityContext(context -> context.securityContextRepository(new RequestAttributeSecurityContextRepository()))
			.requestCache(AbstractHttpConfigurer::disable)
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.addFilterBefore(new AuthEndpointGuardFilter(properties.security().authRequestsPerMinute(), clock),
					BearerTokenAuthenticationFilter.class)
			.headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
				.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
				.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
				.addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", PERMISSIONS_POLICY))
				.addHeaderWriter(new StaticHeadersWriter("Cross-Origin-Opener-Policy", "same-origin")));

		ClientRegistrationRepository registrations = clientRegistrations.getIfAvailable();
		if (registrations != null) {
			DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
					registrations, "/oauth2/authorization");
			resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
			http.oauth2Login(login -> login.loginPage("/login")
				.authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(resolver))
				.successHandler(oauth2LoginSuccessHandler)
				.failureHandler((_, response, _) -> response.sendRedirect("/login?error=oauth")));
		}
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		// bcrypt by default, with the {id} prefix so the algorithm can be upgraded later
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

}
