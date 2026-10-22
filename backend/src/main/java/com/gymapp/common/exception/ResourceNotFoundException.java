package com.gymapp.common.exception;

/** Ressource introuvable (ou appartenant à un autre utilisateur) : HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    private final String resource;
    private final Object resourceId;

    public ResourceNotFoundException(String resource, Object resourceId) {
        super("%s %s introuvable".formatted(resource, resourceId));
        this.resource = resource;
        this.resourceId = resourceId;
    }

    public String getResource() {
        return resource;
    }

    public Object getResourceId() {
        return resourceId;
    }
}
