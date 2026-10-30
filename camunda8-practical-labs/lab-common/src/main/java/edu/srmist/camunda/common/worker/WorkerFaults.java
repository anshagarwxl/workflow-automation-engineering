package edu.srmist.camunda.common.worker;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fault-injection switches for dependencies used <em>inside</em> a job worker (for example the
 * simulated inventory service in Lab 2). Used only to demonstrate incidents and recovery.
 *
 * <pre>
 *   PUT /admin/worker-faults/inventory?mode=DOWN   -> worker fails, retries, then raises an incident
 *   PUT /admin/worker-faults/inventory?mode=UP     -> dependency restored, retry the incident in Operate
 * </pre>
 */
@Component
public class WorkerFaults {

  public enum Mode {
    UP,
    DOWN
  }

  private final Map<String, Mode> modes = new ConcurrentHashMap<>();

  public boolean isDown(String dependency) {
    return modes.getOrDefault(dependency, Mode.UP) == Mode.DOWN;
  }

  public void set(String dependency, Mode mode) {
    modes.put(dependency, mode);
  }

  public Map<String, Mode> all() {
    return new TreeMap<>(modes);
  }

  @RestController
  @RequestMapping("/admin/worker-faults")
  static class Api {
    private final WorkerFaults faults;

    Api(WorkerFaults faults) {
      this.faults = faults;
    }

    @GetMapping
    Map<String, Mode> list() {
      return faults.all();
    }

    @PutMapping("/{dependency}")
    ResponseEntity<Map<String, Object>> set(
        @PathVariable("dependency") String dependency, @RequestParam("mode") Mode mode) {
      faults.set(dependency, mode);
      return ResponseEntity.ok(Map.of("dependency", dependency, "mode", mode));
    }
  }
}
