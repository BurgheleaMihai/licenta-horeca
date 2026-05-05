package com.licenta.horeca;

import com.licenta.horeca.entity.Product;
import com.licenta.horeca.repository.ProductRepository;
import com.licenta.horeca.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getAvailableProducts_shouldReturnOnlyAvailableProducts() {
        Product pizza = new Product();
        pizza.setName("Pizza Margherita");
        pizza.setDescription("Pizza cu sos de rosii, mozzarella si busuioc");
        pizza.setPrice(BigDecimal.valueOf(32.00));
        pizza.setAvailable(true);

        Product tiramisu = new Product();
        tiramisu.setName("Tiramisu");
        tiramisu.setDescription("Desert italian cu mascarpone si cafea");
        tiramisu.setPrice(BigDecimal.valueOf(22.00));
        tiramisu.setAvailable(true);

        when(productRepository.findByAvailableTrue())
                .thenReturn(List.of(pizza, tiramisu));

        List<Product> result = productService.getAvailableProducts();

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(Product::isAvailable));
    }
}