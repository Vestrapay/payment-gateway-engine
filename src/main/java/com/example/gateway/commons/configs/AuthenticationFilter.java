//package com.example.gateway.config;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.core.env.Environment;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//@Service
//public class AuthenticationFilter extends OncePerRequestFilter {
//    private List<String> excludeUrlPatterns = new ArrayList<>(Arrays.asList(
//            "/swagger-ui/swagger-initializer.js",
//            "/v3/api-docs",
//            "/v3/api-docs/swagger-config",
//            "/swagger-ui.html",
//            "/swagger-uui.html",
//            "/webjars/springfox-swagger-ui/springfox.css",
//            "/webjars/springfox-swagger-ui/swagger-ui-bundle.js",
//            "/webjars/springfox-swagger-ui/swagger-ui.css",
//            "/webjars/springfox-swagger-ui/swagger-ui-standalone-preset.js",
//            "/webjars/springfox-swagger-ui/springfox.js",
//            "/swagger-resources/configuration/ui",
//            "/webjars/springfox-swagger-ui/favicon-32x32.png",
//            "/swagger-resources/configuration/security",
//            "/swagger-resources",
//            "/v2/api-docs","/error",
//            "/favicon.ico","/swagger-ui/index.html",
//            "/webjars/springfox-swagger-ui/fonts/titillium-web-v6-latin-700.woff2",
//            "/webjars/springfox-swagger-ui/fonts/open-sans-v15-latin-regular.woff2",
//            "/webjars/springfox-swagger-ui/fonts/open-sans-v15-latin-700.woff2",
//            "/webjars/springfox-swagger-ui/favicon-16x16.png"));
//
//    private static final String HEADER_NAME = "Authorization";
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        String headerValue = request.getHeader(HEADER_NAME);
//        logger.info(request.getServletPath());
//        getEnvironment();
//        if (excludeUrlPatterns.contains(request.getContextPath())){
//            filterChain.doFilter(request, response);
//        }
//        else if (headerValue.isEmpty()) {
//            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or missing header");
//        } else {
//            filterChain.doFilter(request, response);
//        }
//    }
//    @Override
//    protected boolean shouldNotFilterAsyncDispatch() {
//        return false;
//    }
//
//    @Override
//    protected boolean shouldNotFilterErrorDispatch() {
//        return false;
//    }
//
//    @Override
//    public Environment getEnvironment() {
//        Environment environment = super.getEnvironment();
//        logger.info(environment.toString());
//        return environment;
//    }
//
//    @Override
//    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
//        String path = request.getRequestURI();
//        if (excludeUrlPatterns.contains(path)) {
//            return true;
//        } else {
//            return false;
//        }
//    }
//}
