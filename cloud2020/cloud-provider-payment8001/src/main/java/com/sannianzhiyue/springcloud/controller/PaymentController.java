package com.sannianzhiyue.springcloud.controller;

import com.sannianzhiyue.springcloud.entities.CommonResult;
import com.sannianzhiyue.springcloud.entities.Payment;
import com.sannianzhiyue.springcloud.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@Slf4j
public class PaymentController {
    @Resource
    private PaymentService paymentService;

    @PostMapping("/payment/create")
    public CommonResult create(@RequestBody Payment payment) {
        int result = paymentService.create(payment);
        log.info("*********插入结果：" + result);
        if (result > 0) {
            return new CommonResult(200, "插入成功", result);
        } else {
            return new CommonResult(444, "插入失败");
        }
    }

    @GetMapping("/payment/get/{id}")
    public CommonResult getPaymentById(@PathVariable("id") Long id) {
        Payment payment = paymentService.getPaymentById(id);
        log.info("*********查询结果：" + payment);
        if (payment == null) {
            return new CommonResult(200, "没有查询的数据");
        } else {
            return new CommonResult(200, "查询成功", payment);
        }
    }
}
