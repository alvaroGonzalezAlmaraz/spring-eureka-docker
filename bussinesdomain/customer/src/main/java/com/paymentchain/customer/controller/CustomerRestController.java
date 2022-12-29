package com.paymentchain.customer.controller;


import com.fasterxml.jackson.databind.JsonNode;
import com.paymentchain.customer.bussinestransactions.BussinesTransaction;
import com.paymentchain.customer.entities.Customer;
import com.paymentchain.customer.entities.CustomerProduct;
import com.paymentchain.customer.exceptions.BussinesRuleException;
import com.paymentchain.customer.repository.CustomerRepository;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.bind.annotation.*;


import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/customer")
public class CustomerRestController {

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    BussinesTransaction bussinesTransaction;

    @GetMapping()
    public List<Customer> findAll(){
        return customerRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> get(@PathVariable long id){
        return customerRepository.findById(id)
                .map(response-> ResponseEntity.ok().body(response))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> put(@PathVariable long id, @RequestBody Customer input) {
        Optional <Customer> customer = customerRepository.findById(id);
        Customer save = customerRepository.save(input);
        return ResponseEntity.ok(save);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        Optional<Customer> findByID = customerRepository.findById(id);
        if(findByID.get() != null){
            customerRepository.delete(findByID.get());
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<?> post(@RequestBody Customer customer) throws BussinesRuleException {
        return new ResponseEntity<>(bussinesTransaction.save(customer),
                HttpStatus.CREATED);
    }

    @GetMapping("/full")
    public Customer getCustomerByCode(@RequestParam String code) {
        return bussinesTransaction.get(code);

    }

}
