package edu.srmist.camunda.hospital.worker;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Calls the mock hospital scheduling API to book a slot. */
@Component
public class SchedulingClient {

  /** Result of a booking attempt. */
  public record BookingResult(boolean confirmed, Map<String, Object> body, String problem) {}

  /** A technical failure that may succeed on retry (service down, timeout, HTTP 5xx). */
  public static class TransientSchedulingException extends RuntimeException {
    public TransientSchedulingException(String message) {
      super(message);
    }
  }

  private static final ParameterizedTypeReference<Map<String, Object>> MAP =
      new ParameterizedTypeReference<>() {};

  private final RestClient restClient = RestClient.create();
  private final String defaultBaseUrl;

  public SchedulingClient(@Value("${lab3.scheduling-base-url:http://localhost:8090}") String defaultBaseUrl) {
    this.defaultBaseUrl = defaultBaseUrl;
  }

  public BookingResult book(String baseUrl, Map<String, Object> request) {
    String url = (baseUrl == null || baseUrl.isBlank() ? defaultBaseUrl : baseUrl) + "/scheduling/api/v1/bookings";
    try {
      Map<String, Object> body =
          restClient
              .post()
              .uri(url)
              .contentType(MediaType.APPLICATION_JSON)
              .body(request)
              .retrieve()
              .body(MAP);
      return new BookingResult(true, body, null);
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() == 409) {
        return new BookingResult(false, Map.of(), "SLOT_TAKEN: " + e.getResponseBodyAsString());
      }
      if (e.getStatusCode().is5xxServerError()) {
        throw new TransientSchedulingException(
            "Scheduling API returned HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
      }
      return new BookingResult(false, Map.of(), "REJECTED: HTTP " + e.getStatusCode().value() + " " + e.getResponseBodyAsString());
    } catch (ResourceAccessException e) {
      throw new TransientSchedulingException("Scheduling API not reachable: " + e.getMessage());
    }
  }
}
