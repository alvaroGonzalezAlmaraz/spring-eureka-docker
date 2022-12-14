package com.paymentchain.customer.controller;


import com.fasterxml.jackson.databind.JsonNode;
import com.paymentchain.customer.entities.Customer;
import com.paymentchain.customer.entities.CustomerProduct;
import com.paymentchain.customer.repository.CustomerRepository;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.bind.annotation.*;


import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/customer")
public class CustomerRestController {

    @Autowired
    CustomerRepository customerRepository;

    private final WebClient.Builder webclientBuider;

    public CustomerRestController(WebClient.Builder webclientBuider) {
        this.webclientBuider = webclientBuider;
    }

    @GetMapping()
    public List<Customer> findAll(){
        return customerRepository.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Customer> get(@PathVariable long id){
        return customerRepository.findById(id);
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
    public ResponseEntity<?> post(@RequestBody Customer customer){
        customer.getProducts().forEach(customerProduct->
                customerProduct.setCustomer(customer));
        Customer cliente = customerRepository.save(customer);
        return ResponseEntity.ok(cliente);
    }

    @GetMapping("/full")
    public Customer getCustomerByCode(@RequestParam String code) {
        Customer customer = customerRepository.findByCode(code);
        List<CustomerProduct> customerProducts = customer.getProducts();
        customerProducts.forEach(customerProduct -> {
            String productName = getProductName(customerProduct.getProductId());
            customerProduct.setProductName(productName);
        });
        List<?> transactions = getTransactions(customer.getIban());
        customer.setTransactions(transactions);
        return customer;
    }

    HttpClient client = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(EpollChannelOption.TCP_KEEPCNT, 300)
            .option(EpollChannelOption.TCP_KEEPINTVL, 60)
            .responseTimeout(Duration.ofSeconds(1))
            .doOnConnected(connection->{
                connection.addHandlerLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS));
                connection.addHandlerLast(new WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS));
            });

    private String getProductName(long id){
        WebClient build = webclientBuider.clientConnector(new ReactorClientHttpConnector(client))
                .baseUrl("http://bussinesdomain-productos/product")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultUriVariables(Collections.singletonMap("url", "http://bussinesdomain-productos/product"))
                .build();

        try{
            JsonNode block = build.method(HttpMethod.GET)
                    .uri("/" + id)
                    .retrieve()
                    .bodyToMono(JsonNode.class).block();
            assert block != null;
            return block.get("name").asText();
        } catch (Exception e){
            System.out.printf("Error: " + e.getMessage());
        }
        
        return null;
    }

    private List<?> getTransactions(String iban){
        WebClient build = webclientBuider.clientConnector(new ReactorClientHttpConnector(client))
                .baseUrl("http://bussinesdomain-transaction/transaction")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        List<?> transactions = build.method(HttpMethod.GET)
                .uri(uriBuilder ->
                        uriBuilder.path("/customer/transaction")
                .queryParam("ibanAccount", iban)
                                .build())
                .retrieve().bodyToFlux(Object.class)
                .collectList()
                .block();
        return transactions;
    }

}
