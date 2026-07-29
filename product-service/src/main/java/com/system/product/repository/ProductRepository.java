//package com.system.product.repository;
//import com.system.product.model.Product;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//@Repository
//public interface ProductRepository extends JpaRepository<Product, Long> {
//}

package com.system.product.repository;

import com.system.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    // Spring Data JPA automatically writes the SQL query for this based on the method name!
    // It translates to: SELECT * FROM products WHERE is_active = true;
    List<Product> findAllByIsActiveTrue();
}