package org.campusscheduler.api.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void handleUnhandledReturnsEmptyBodyForWebSocketTransportErrors() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ws/123/abc/xhr_streaming");

        ResponseEntity<?> response = handler.handleUnhandled(new RuntimeException("transport closed"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void handleUnhandledReturnsApiErrorForApiPaths() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/solver/analytics");

        ResponseEntity<?> response = handler.handleUnhandled(new RuntimeException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(ApiErrorResponse.class);
        ApiErrorResponse body = (ApiErrorResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.path()).isEqualTo("/api/solver/analytics");
    }
}
