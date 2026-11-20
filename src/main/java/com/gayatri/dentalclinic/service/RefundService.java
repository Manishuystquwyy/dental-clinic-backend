package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.entity.Appointment;
import java.util.Map;

public interface RefundService {
    /** Joins the cancellation transaction; never contacts the payment provider. */
    void queueCancellation(Appointment appointment);
    /** Called only after Razorpay's signature has been verified. */
    void handleWebhook(String event, Map<?, ?> refundEntity);
    void processDueRefunds();
}
