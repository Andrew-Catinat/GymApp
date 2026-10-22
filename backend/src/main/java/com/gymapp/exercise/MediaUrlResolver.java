package com.gymapp.exercise;

import org.springframework.stereotype.Component;

/** Transforme les chemins relatifs du dataset en URL absolues. */
@Component
public class MediaUrlResolver {

    private final String baseUrl;

    public MediaUrlResolver(MediaProperties properties) {
        String url = properties.baseUrl() == null ? "" : properties.baseUrl();
        this.baseUrl = url.isEmpty() || url.endsWith("/") ? url : url + "/";
    }

    public String resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String path = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        return baseUrl + path;
    }
}
