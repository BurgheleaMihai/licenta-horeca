package com.licenta.horeca;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.licenta.horeca.auth.security.SecurityConfig;
import com.licenta.horeca.controller.ProductController;
import com.licenta.horeca.entity.Category;
import com.licenta.horeca.entity.Product;
import com.licenta.horeca.auth.security.CustomUserDetailsService;
import com.licenta.horeca.auth.security.JwtService;
import com.licenta.horeca.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    private static final String PRODUCTS_ENDPOINT =
            "/api/products";

    private static final String AVAILABLE_PRODUCTS_ENDPOINT =
            "/api/products/available";

    private static final String CATEGORY_PRODUCTS_ENDPOINT =
            "/api/products/category/1";

    private static final String PIZZA_NAME =
            "Pizza Margherita";

    private static final String TIRAMISU_NAME =
            "Tiramisu";

    private static final String CATEGORY_NAME =
            "Mancare";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getAllProductsShouldReturnAllProducts()
            throws Exception {

        Product pizza =
                createProduct(
                        PIZZA_NAME,
                        true
                );

        Product tiramisu =
                createProduct(
                        TIRAMISU_NAME,
                        true
                );

        when(productService.getAllProducts())
                .thenReturn(
                        List.of(
                                pizza,
                                tiramisu
                        )
                );

        mockMvc
                .perform(
                        get(PRODUCTS_ENDPOINT)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].name")
                                .value(PIZZA_NAME)
                )
                .andExpect(
                        jsonPath("$[1].name")
                                .value(TIRAMISU_NAME)
                );
    }

    @Test
    void getAvailableProductsShouldReturnAvailableProducts()
            throws Exception {

        Product pizza =
                createProduct(
                        PIZZA_NAME,
                        true
                );

        when(productService.getAvailableProducts())
                .thenReturn(
                        List.of(pizza)
                );

        mockMvc
                .perform(
                        get(AVAILABLE_PRODUCTS_ENDPOINT)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].name")
                                .value(PIZZA_NAME)
                )
                .andExpect(
                        jsonPath("$[0].available")
                                .value(true)
                );
    }

    @Test
    void getProductsByCategoryShouldReturnProductsFromCategory()
            throws Exception {

        Product pizza =
                createProduct(
                        PIZZA_NAME,
                        true
                );

        when(productService.getProductsByCategory(1L))
                .thenReturn(
                        List.of(pizza)
                );

        mockMvc
                .perform(
                        get(CATEGORY_PRODUCTS_ENDPOINT)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].name")
                                .value(PIZZA_NAME)
                )
                .andExpect(
                        jsonPath("$[0].category.name")
                                .value(CATEGORY_NAME)
                );
    }

    private Product createProduct(
            String name,
            boolean available
    ) {
        Category category =
                new Category();

        category.setName(
                CATEGORY_NAME
        );

        Product product =
                new Product();

        product.setName(name);
        product.setDescription(
                "Descriere test"
        );
        product.setPrice(
                BigDecimal.valueOf(32)
        );
        product.setAvailable(available);
        product.setCategory(category);

        return product;
    }
}
