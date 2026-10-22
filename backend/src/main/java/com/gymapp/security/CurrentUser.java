package com.gymapp.security;

import com.gymapp.user.Role;

/** Utilisateur authentifié à l'origine de la requête. */
public record CurrentUser(Long id, Role role) {
}
