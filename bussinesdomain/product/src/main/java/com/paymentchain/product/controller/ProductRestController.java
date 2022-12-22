package com.paymentchain.product.controller;

import com.paymentchain.product.entities.Product;
import com.paymentchain.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/product")
public class ProductRestController {

    @Autowired
    ProductRepository productRepository;

    @Value("${user.role}")
    private String role;

    @GetMapping()
    public List<Product> findAll(){
        System.out.println("El role es: " + role);
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Product> get(@PathVariable long id){
        return productRepository.findById(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> put(@PathVariable long id, @RequestBody Product input) {
        Product save = productRepository.save(input);
        return ResponseEntity.ok(save);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        Optional<Product> findByID = productRepository.findById(id);
        if(findByID.get() != null){
            productRepository.delete(findByID.get());
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<?> post(@RequestBody Product product){
        Product producto = productRepository.save(product);
        return ResponseEntity.ok(producto);
    }

}
