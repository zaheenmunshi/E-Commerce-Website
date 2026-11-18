package com.zaheenmunshi.ecommerce.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.context.TestPropertySource;

import com.zaheenmunshi.ecommerce.common.error.ApiException;
import com.zaheenmunshi.ecommerce.support.IntegrationTest;
import com.zaheenmunshi.ecommerce.user.domain.User;

@TestPropertySource(properties = {
		"spring.security.oauth2.client.registration.google.client-id=test-client-id",
		"spring.security.oauth2.client.registration.google.client-secret=test-client-secret" })
class GoogleLoginIT extends IntegrationTest {

	@Autowired
	private OAuthAccountService accounts;

	@Test
	void googleIsOfferedOnceConfigured() throws Exception {
		mvc.perform(get("/api/auth/providers")).andExpect(jsonPath("$[0]").value("google"));
	}

	@Test
	void authorizationRequestUsesTheCodeFlowWithPkceAndState() throws Exception {
		String location = mvc.perform(get("/oauth2/authorization/google"))
			.andExpect(status().is3xxRedirection())
			.andReturn()
			.getResponse()
			.getRedirectedUrl();

		assertThat(location).startsWith("https://accounts.google.com/")
			.contains("response_type=code")
			.contains("code_challenge_method=S256")
			.contains("code_challenge=")
			.contains("state=")
			.contains("client_id=test-client-id");
	}

	@Test
	void verifiedGoogleAccountsAreCreatedOnceThenRecognised() {
		String email = uniqueEmail();
		String subject = UUID.randomUUID().toString();

		User first = accounts.findOrCreate(googleLogin(subject, email, true));
		User again = accounts.findOrCreate(googleLogin(subject, email, true));

		assertThat(again.getId()).isEqualTo(first.getId());
		assertThat(first.getPasswordHash()).isNull();
	}

	@Test
	void unverifiedEmailsAreRefused() {
		assertThatThrownBy(() -> accounts.findOrCreate(googleLogin(UUID.randomUUID().toString(), uniqueEmail(), false)))
			.isInstanceOf(ApiException.class)
			.hasMessageContaining("not verified");
	}

	private static OAuth2AuthenticationToken googleLogin(String subject, String email, boolean verified) {
		OidcIdToken idToken = OidcIdToken.withTokenValue("id-token")
			.subject(subject)
			.claim("email", email)
			.claim("email_verified", verified)
			.claim("name", "Google Shopper")
			.build();
		var authorities = List.of(new SimpleGrantedAuthority("OIDC_USER"));
		return new OAuth2AuthenticationToken(new DefaultOidcUser(authorities, idToken), authorities, "google");
	}

}
