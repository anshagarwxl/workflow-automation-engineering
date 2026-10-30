package edu.srmist.camunda.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.srmist.camunda.mock.credit.CreditBureau;
import edu.srmist.camunda.mock.payment.PaymentGateway;
import edu.srmist.camunda.mock.scheduling.SchedulingCalendar;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MockServicesLogicTest {

  @Test
  void creditBureauReturnsSyntheticRecordOrNoHistory() {
    CreditBureau bureau = new CreditBureau();
    var known =
        bureau.assess(
            new CreditBureau.AssessmentRequest(
                "LA-1", "SYN-1001", new BigDecimal("120000"), new BigDecimal("5000"), null));
    assertEquals(780, known.creditScore());
    assertEquals("A", known.riskGrade());
    assertEquals("FOUND", known.bureauStatus());

    var unknown =
        bureau.assess(new CreditBureau.AssessmentRequest("LA-2", "SYN-7777", null, null, null));
    assertEquals("NO_HISTORY", unknown.bureauStatus());
    assertEquals(CreditBureau.NO_HISTORY_SCORE, unknown.creditScore());
  }

  @Test
  void paymentGatewayAuthorisesDeclinesAndIsIdempotent() {
    PaymentGateway gw = new PaymentGateway();
    var ok =
        gw.charge(
            new PaymentGateway.ChargeRequest(
                "ORD-1", new BigDecimal("100"), "INR", "tok_test_visa_success", "C1"));
    assertEquals("AUTHORISED", ok.status());
    assertNull(ok.declineReason());

    var again =
        gw.charge(
            new PaymentGateway.ChargeRequest(
                "ORD-1", new BigDecimal("100"), "INR", "tok_test_visa_success", "C1"));
    assertEquals(ok.chargeId(), again.chargeId());
    assertTrue(again.idempotentReplay());

    var declined =
        gw.charge(
            new PaymentGateway.ChargeRequest(
                "ORD-2", new BigDecimal("100"), "INR", "tok_test_card_declined", "C1"));
    assertEquals("DECLINED", declined.status());
    assertEquals("CARD_DECLINED", declined.declineReason());
  }

  @Test
  void schedulingCalendarHasSlotsExceptDermatologyAndRemovesBookedSlots() {
    SchedulingCalendar cal = new SchedulingCalendar();
    LocalDate date = LocalDate.of(2030, 1, 15);
    var morning = cal.availableSlots("GENERAL_MEDICINE", date, "MORNING");
    assertEquals(6, morning.size());
    assertEquals(10, cal.availableSlots("GENERAL_MEDICINE", date, "ANY").size());
    assertTrue(cal.availableSlots("DERMATOLOGY", date, "ANY").isEmpty());

    String slotId = morning.get(0).slotId();
    var booking =
        cal.book(new SchedulingCalendar.BookingRequest(slotId, "SYN-PAT-3001", "ROUTINE", "test"));
    assertEquals("CONFIRMED", booking.status());
    assertEquals(5, cal.availableSlots("GENERAL_MEDICINE", date, "MORNING").size());
    assertThrows(
        SchedulingCalendar.SlotNotAvailableException.class,
        () -> cal.book(new SchedulingCalendar.BookingRequest(slotId, "SYN-PAT-3002", "ROUTINE", "t")));
  }
}
