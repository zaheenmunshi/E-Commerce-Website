package com.zaheenmunshi.ecommerce.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zaheenmunshi.ecommerce.auth.service.AuthService;
import com.zaheenmunshi.ecommerce.user.domain.UserRepository;

/** Password login lookup. Accounts created through Google have no password and are skipped. */
@Service
public class AppUserDetailsService implements UserDetailsService {

	private final UserRepository users;

	public AppUserDetailsService(UserRepository users) {
		this.users = users;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) {
		return users.findByEmail(AuthService.normalizeEmail(email))
			.filter(user -> user.getPasswordHash() != null)
			.map(AppUserPrincipal::from)
			.orElseThrow(() -> new UsernameNotFoundException("no password account for this email"));
	}

}
