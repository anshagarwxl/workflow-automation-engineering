package edu.srmist.camunda.order.worker;

import edu.srmist.camunda.common.worker.DataContractException;
import edu.srmist.camunda.common.worker.JobFailures;
import edu.srmist.camunda.common.worker.WorkerFaults;
import edu.srmist.camunda.order.logic.Inventory;
import edu.srmist.camunda.order.logic.OrderOutcomes;
import edu.srmist.camunda.order.logic.OrderValidator;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** External job workers for the order processing process. */
@Component
public class OrderWorkers {

  private static final Logger LOG = LoggerFactory.getLogger(OrderWorkers.class);
  public static final String INVENTORY = "inventory";
  private static final Duration BACKOFF = Duration.ofSeconds(10);

  private final Inventory inventory = new Inventory();
  private final WorkerFaults faults;

  public OrderWorkers(WorkerFaults faults) {
    this.faults = faults;
  }

  /** Service task "Validate order, check stock and price". */
  @JobWorker(type = "order.validate-and-price", fetchAllVariables = true)
  public Map<String, Object> validateAndPrice(ActivatedJob job) {
    Map<String, Object> vars = job.getVariablesAsMap();
    LOG.info("[validate] instance={} order={} retries={}", job.getProcessInstanceKey(), vars.get("orderId"), job.getRetries());
    if (faults.isDown(INVENTORY)) {
      // Transient technical failure: retry with back-off; Zeebe raises an incident at 0 retries.
      throw JobFailures.retryable(job, "Inventory service unavailable (simulated outage)", BACKOFF);
    }
    try {
      Map<String, Object> result = OrderValidator.validateAndPrice(vars, inventory);
      LOG.info("[validate] instance={} result={}", job.getProcessInstanceKey(), result);
      return result;
    } catch (DataContractException e) {
      throw JobFailures.nonRetryable("Order data invalid: " + e.getMessage() + ". Fix the variable in Operate and retry.");
    }
  }

  /** Service task "Fulfil order". */
  @JobWorker(type = "order.fulfil", fetchAllVariables = true)
  public Map<String, Object> fulfil(ActivatedJob job) {
    if (faults.isDown(INVENTORY)) {
      throw JobFailures.retryable(job, "Inventory service unavailable (simulated outage)", BACKOFF);
    }
    Map<String, Object> result = OrderOutcomes.fulfil(job.getVariablesAsMap(), inventory, Instant.now());
    if (result == null) {
      throw JobFailures.nonRetryable(
          "Stock was sold out between validation and fulfilment. Restock or cancel the instance in Operate.");
    }
    LOG.info("[fulfil] instance={} {}", job.getProcessInstanceKey(), result);
    return result;
  }

  /** All "Notify ..." service tasks (the notification type comes from the BPMN input mapping). */
  @JobWorker(type = "order.notify-customer", fetchAllVariables = true)
  public Map<String, Object> notifyCustomer(ActivatedJob job) {
    Map<String, Object> result = OrderOutcomes.notification(job.getVariablesAsMap());
    LOG.info("[notify] instance={} {}", job.getProcessInstanceKey(), result);
    return result;
  }
}
