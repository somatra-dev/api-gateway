package com.pesexpo.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Route Configuration for BFF (Backend-for-Frontend) Pattern
 *
 * Architecture:
 * 1. Browser calls Gateway for ALL requests (single entry point)
 * 2. Gateway handles OAuth2 authentication (session-based)
 * 3. Gateway routes API calls to microservices with TokenRelay
 * 4. Gateway routes page requests to NextJS for SSR
 *
 * This avoids double-routing through Gateway that happens when
 * NextJS proxies API calls back through Gateway.
 */
//@Configuration
//public class RouteGatewayConfig {
//
//    @Value("${app.frontend.url:http://localhost:3000}")
//    private String frontendUrl;
//
//    @Bean
//    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
//        return builder.routes()
//
//                // MICROSERVICES API ROUTES
//                // Browser → Gateway → Microservice (direct, with TokenRelay)
//                // Product Service
//                .route("product-service", r -> r
//                        .path("/api/v1/products/**")
//                        .filters(GatewayFilterSpec::tokenRelay)
//                        .uri("lb://PRODUCT-SERVICE"))
//
//                // Order Service
//                .route("order-service", r -> r
//                        .path("/api/v1/orders/**")
//                        .filters(GatewayFilterSpec::tokenRelay)
//                        .uri("lb://ORDER-SERVICE"))
//
//                // NEXTJS SSR ROUTES
//                // For server-side rendered pages (not API calls)
//                // Static assets from NextJS
//                .route("nextjs-static", r -> r
//                        .path("/_next/**")
//                        .uri(frontendUrl))
//
//                // NextJS pages (SSR) - catch-all for page navigation
//                // This routes page requests to NextJS for server-side rendering
//                .route("nextjs-pages", r -> r
//                        .path("/app/**", "/products/**", "/orders/**")
//                        .filters(GatewayFilterSpec::tokenRelay)
//                        .uri(frontendUrl))
//
//                // NextJS root and other pages
//                .route("nextjs-root", r -> r
//                        .path("/", "/favicon.ico")
//                        .uri(frontendUrl))
//
//                .build();
//    }
//}
