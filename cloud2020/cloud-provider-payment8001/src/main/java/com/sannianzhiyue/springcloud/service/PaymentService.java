package com.sannianzhiyue.springcloud.service;

import com.sannianzhiyue.springcloud.entities.Payment;

public interface PaymentService {
    int create(Payment payment);

    Payment getPaymentById(Long id);
}
