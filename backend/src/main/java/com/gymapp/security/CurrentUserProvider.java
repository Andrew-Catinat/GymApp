package com.gymapp.security;

import com.gymapp.common.exception.BusinessException;
import com.gymapp.common.exception.ErrorCode;
import com.gymapp.user.Role;

/**
 * Fournit l'utilisateur courant. L'implémentation définitive lira le JWT
 * (étape authentification) ; les contrôleurs n'auront pas à changer.
 */
public interface CurrentUserProvider {

    CurrentUser get();

    /** Identifiant du client courant ; 403 si l'utilisateur n'est pas un CLIENT. */
    default Long requireClientId() {
        CurrentUser user = get();
        if (user.role() != Role.CLIENT) {
            throw new BusinessException(ErrorCode.CLIENT_ROLE_REQUIRED,
                    "Cette action est réservée aux clients.");
        }
        return user.id();
    }
}
