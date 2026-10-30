package edu.srmist.camunda.mock.faults;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Global fault switches per mock service, used to demonstrate incidents and recovery.
 *
 * <ul>
 *   <li>UP – normal behaviour
 *   <li>DOWN – every call returns HTTP 503 Service Unavailable
 *   <li>SLOW – every call waits {@value #SLOW_DELAY_MS} ms before answering (longer than the
 *       connector read timeout, so the caller sees a timeout)
 * </ul>
 */
@Component
public class FaultRegistry {

  public static final long SLOW_DELAY_MS = 30_000;

  public enum Mode {
    UP,
    DOWN,
    SLOW
  }

  public static final String CREDIT = "credit";
  public static final String PAYMENT = "payment";
  public static final String SCHEDULING = "scheduling";

  private final Map<String, Mode> modes = new ConcurrentHashMap<>();

  public FaultRegistry() {
    modes.put(CREDIT, Mode.UP);
    modes.put(PAYMENT, Mode.UP);
    modes.put(SCHEDULING, Mode.UP);
  }

  public Map<String, Mode> all() {
    return new TreeMap<>(modes);
  }

  public void set(String service, Mode mode) {
    if (!modes.containsKey(service)) {
      throw new IllegalArgumentException(
          "Unknown service '" + service + "'. Use one of " + modes.keySet());
    }
    modes.put(service, mode);
  }

  /** Call at the start of every mock endpoint. */
  public void apply(String service) {
    Mode mode = modes.getOrDefault(service, Mode.UP);
    if (mode == Mode.DOWN) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Simulated outage: '" + service + "' service is DOWN (fault injection)");
    }
    if (mode == Mode.SLOW) {
      sleep(SLOW_DELAY_MS);
    }
  }

  public static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
