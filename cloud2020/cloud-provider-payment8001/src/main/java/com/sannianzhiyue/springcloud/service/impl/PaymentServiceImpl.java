package com.sannianzhiyue.springcloud.service.impl;

import com.sannianzhiyue.springcloud.dao.PaymentDao;
import com.sannianzhiyue.springcloud.entities.Payment;
import com.sannianzhiyue.springcloud.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Resource
    private PaymentDao paymentDao;

    @Override
    public int create(Payment payment) {
        return paymentDao.create(payment);
    }

    @Override
    public Payment getPaymentById(Long id) {
        return paymentDao.getPaymentById(id);
    }
}
