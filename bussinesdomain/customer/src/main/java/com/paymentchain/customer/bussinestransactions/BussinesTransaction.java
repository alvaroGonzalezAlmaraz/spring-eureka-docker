package com.paymentchain.customer.bussinestransactions;

import com.fasterxml.jackson.databind.JsonNode;
import com.paymentchain.customer.entities.Customer;
import com.paymentchain.customer.entities.CustomerProduct;
import com.paymentchain.customer.exceptions.BussinesRuleException;
import com.paymentchain.customer.repository.CustomerRepository;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class BussinesTransaction {

    @Autowired
    CustomerRepository customerRepository;

    private final WebClient.Builder webclientBuider;

    public BussinesTransaction(WebClient.Builder webclientBuider) {
        this.webclientBuider = webclientBuider;
    }

    HttpClient client = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(EpollChannelOption.TCP_KEEPCNT, 300)
            .option(EpollChannelOption.TCP_KEEPINTVL, 60)
            .responseTimeout(Duration.ofSeconds(10))
            .doOnConnected(connection->{
                connection.addHandlerLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS));
                connection.addHandlerLast(new WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS));
            });


    public Customer get(@RequestParam String code) {
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

    private String getProductName(long id){
        WebClient build = webclientBuider.clientConnector(new ReactorClientHttpConnector(client))
                .baseUrl("http://businessdomain-productos/product")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultUriVariables(Collections.singletonMap("url", "http://businessdomain-productos/product"))
                .build();

        JsonNode block = build.method(HttpMethod.GET)
                .uri("/" + id)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        assert block != null;
        return block.get("name").asText();

    }

    private List<?> getTransactions(String iban){
        WebClient build = webclientBuider.clientConnector(new ReactorClientHttpConnector(client))
                .baseUrl("http://businessdomain-transactions/transaction")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        return build.method(HttpMethod.GET)
                .uri(uriBuilder ->
                        uriBuilder.path("/customer/transaction")
                                .queryParam("ibanAccount", iban)
                                .build())
                .retrieve().bodyToFlux(Object.class)
                .collectList()
                .block();
    }

    public Customer save(Customer customer) throws BussinesRuleException{
        for (CustomerProduct customerProduct : customer.getProducts()) {
            String productName = customerProduct.getProductName();
            if (productName.isBlank()) {
                BussinesRuleException exception = new BussinesRuleException("1025", "Error de validacion, producto no existe", HttpStatus.PRECONDITION_FAILED);
                throw exception;
            } else {
                customerProduct.setCustomer(customer);
            }
        }

        return customerRepository.save(customer);
    }
}
