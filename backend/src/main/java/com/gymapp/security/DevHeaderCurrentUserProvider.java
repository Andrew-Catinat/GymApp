package com.gymapp.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import com.gymapp.common.exception.BusinessException;
import com.gymapp.common.exception.ErrorCode;
import com.gymapp.user.AppUser;
import com.gymapp.user.AppUserRepository;

/**
 * TEMPORAIRE, en attendant l'authentification JWT : l'utilisateur courant est
 * lu dans l'en-tête {@code X-User-Id}. À remplacer par une implémentation
 * basée sur le SecurityContext ; ne jamais déployer en l'état.
 */
@Component
public class DevHeaderCurrentUserProvider implements CurrentUserProvider {

    static final String HEADER = "X-User-Id";

    private final HttpServletRequest request;
    private final AppUserRepository users;

    public DevHeaderCurrentUserProvider(HttpServletRequest request, AppUserRepository users) {
        this.request = request;
        this.users = users;
    }

    @Override
    public CurrentUser get() {
        String header = request.getHeader(HEADER);
        if (header == null || header.isBlank()) {
            throw unauthenticated();
        }
        long id;
        try {
            id = Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            throw unauthenticated();
        }
        AppUser user = users.findById(id)
                .filter(AppUser::isActive)
                .orElseThrow(this::unauthenticated);
        return new CurrentUser(user.getId(), user.getRole());
    }

    private BusinessException unauthenticated() {
        return new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED, "Authentification requise.");
    }
}
