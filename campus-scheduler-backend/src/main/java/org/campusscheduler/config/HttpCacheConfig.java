package org.campusscheduler.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import java.io.IOException;

/**
 * Lightweight HTTP caching for the API.
 *
 * <p>We intentionally use conditional requests (ETag + revalidation) rather than
 * long-lived cache TTLs, so the UI can feel instant when data hasn't changed
 * without risking stale reads.</p>
 *
 * <p>Note: Shallow ETags are computed from the response body, so they primarily
 * save bandwidth + client-side JSON parsing on repeat navigations.</p>
 */
@Configuration
public class HttpCacheConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/api/*");
        registration.setName("shallowEtagHeaderFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<Filter> apiCacheControlFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiCacheControlFilter());
        registration.addUrlPatterns("/api/*");
        registration.setName("apiCacheControlFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    /**
     * Add headers that allow the browser to store API responses and revalidate
     * them using ETags. This improves perceived performance on refresh and when
     * switching roles without making data stale by default.
     */
    static final class ApiCacheControlFilter extends OncePerRequestFilter {
        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String method = request.getMethod();
            if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
                return true;
            }
            String uri = request.getRequestURI();
            return uri == null || !uri.startsWith("/api/");
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            // Only set defaults if the controller didn't set caching headers.
            if (!response.containsHeader(HttpHeaders.CACHE_CONTROL)) {
                response.setHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=0, must-revalidate");
            }
            if (!response.containsHeader(HttpHeaders.PRAGMA)) {
                response.setHeader(HttpHeaders.PRAGMA, "no-cache");
            }
            if (!response.containsHeader(HttpHeaders.EXPIRES)) {
                response.setDateHeader(HttpHeaders.EXPIRES, 0);
            }

            filterChain.doFilter(request, response);
        }
    }
}

