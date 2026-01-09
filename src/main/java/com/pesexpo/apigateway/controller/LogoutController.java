package com.pesexpo.apigateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Controller
public class LogoutController {

    @Value("${app.auth-server.url:http://localhost:9000}")
    private String authServerUrl;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @PostMapping("/logout")
    public Mono<Void> logout(ServerWebExchange exchange) {
        log.info("=== Processing logout request ===");

        // Clear local session first
        return exchange.getSession()
                .flatMap(session -> {
                    log.info("Invalidating local session: {}", session.getId());
                    return session.invalidate();
                })
                .then(Mono.defer(() -> {
                    // Clear cookies
                    clearCookies(exchange.getResponse());

                    // Check if we have an authenticated user to do OIDC logout
                    return ReactiveSecurityContextHolder.getContext()
                            .flatMap(securityContext -> {
                                if (securityContext.getAuthentication() != null &&
                                        securityContext.getAuthentication().getPrincipal() instanceof OidcUser oidcUser) {

                                    // Perform OIDC logout
                                    return performOidcLogout(oidcUser, exchange);
                                }

                                // No OIDC user, just redirect to frontend
                                return redirectToFrontend(exchange);
                            })
                            .switchIfEmpty(redirectToFrontend(exchange));
                }));
    }

    private Mono<Void> performOidcLogout(OidcUser oidcUser, ServerWebExchange exchange) {
        String idToken = oidcUser.getIdToken().getTokenValue();
        String sid = oidcUser.getIdToken().getClaim("sid");

        log.info("Performing OIDC logout for user: {}", oidcUser.getName());
        log.info("ID Token present: {}", idToken != null);
        log.info("SID from token: {}", sid);

        // Build OIDC logout URL
        String logoutUrl = buildOidcLogoutUrl(idToken, sid);
        log.info("Redirecting to OIDC logout: {}", logoutUrl);

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().setLocation(URI.create(logoutUrl));

        return response.setComplete();
    }

    private String buildOidcLogoutUrl(String idToken, String sid) {
        StringBuilder url = new StringBuilder(authServerUrl)
                .append("/connect/logout");

        boolean hasParam = false;

        // Add id_token_hint if available
        if (idToken != null) {
            url.append("?id_token_hint=").append(URLEncoder.encode(idToken, StandardCharsets.UTF_8));
            hasParam = true;
        }

        // Add sid if available
        if (sid != null && !sid.isEmpty()) {
            url.append(hasParam ? "&" : "?")
                    .append("sid=").append(URLEncoder.encode(sid, StandardCharsets.UTF_8));
            hasParam = true;
        }

        // Add post_logout_redirect_uri
        String redirectUri = "http://localhost:8888/logout-success";
        url.append(hasParam ? "&" : "?")
                .append("post_logout_redirect_uri=")
                .append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));

        // Add logout=true to skip confirmation (if your auth server supports it)
        url.append("&logout=true");

        return url.toString();
    }

    private Mono<Void> redirectToFrontend(ServerWebExchange exchange) {
        log.info("No OIDC user, redirecting directly to frontend");

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().setLocation(URI.create(frontendUrl + "?logout=success"));

        return response.setComplete();
    }

    private void clearCookies(ServerHttpResponse response) {
        response.addCookie(ResponseCookie.from("SESSION", "")
                .path("/")
                .maxAge(0)
                .build());

        response.addCookie(ResponseCookie.from("XSRF-TOKEN", "")
                .path("/")
                .maxAge(0)
                .build());

        response.addCookie(ResponseCookie.from("JSESSIONID", "")
                .path("/")
                .maxAge(0)
                .build());
    }

    @GetMapping("/logout-success")
    public Mono<Void> logoutSuccess(ServerWebExchange exchange) {
        log.info("=== OIDC Logout Successful ===");

        ServerHttpResponse response = exchange.getResponse();

        // Clear any remaining cookies
        clearCookies(response);

        // Redirect to frontend
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().set(HttpHeaders.LOCATION, frontendUrl + "?logout=success&oidc=true");

        return response.setComplete();
    }
}