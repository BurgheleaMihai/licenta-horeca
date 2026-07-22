package com.licenta.horeca.config;

import com.licenta.horeca.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter
            jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter =
                jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(
                        AbstractHttpConfigurer::disable
                )

                .cors(
                        Customizer.withDefaults()
                )

                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
                )

                .formLogin(
                        AbstractHttpConfigurer::disable
                )

                .httpBasic(
                        AbstractHttpConfigurer::disable
                )

                .exceptionHandling(
                        exception ->
                                exception

                                        .authenticationEntryPoint(
                                                (
                                                        request,
                                                        response,
                                                        authenticationException
                                                ) -> {

                                                    response.setStatus(
                                                            HttpServletResponse
                                                                    .SC_UNAUTHORIZED
                                                    );

                                                    response.setContentType(
                                                            "application/json"
                                                    );

                                                    response.setCharacterEncoding(
                                                            "UTF-8"
                                                    );

                                                    response.getWriter().write(
                                                            """
                                                            {
                                                              "status": 401,
                                                              "message": "Autentificare necesara."
                                                            }
                                                            """
                                                    );
                                                }
                                        )

                                        .accessDeniedHandler(
                                                (
                                                        request,
                                                        response,
                                                        accessDeniedException
                                                ) -> {

                                                    response.setStatus(
                                                            HttpServletResponse
                                                                    .SC_FORBIDDEN
                                                    );

                                                    response.setContentType(
                                                            "application/json"
                                                    );

                                                    response.setCharacterEncoding(
                                                            "UTF-8"
                                                    );

                                                    response.getWriter().write(
                                                            """
                                                            {
                                                              "status": 403,
                                                              "message": "Nu aveti permisiunea necesara."
                                                            }
                                                            """
                                                    );
                                                }
                                        )
                )

                .authorizeHttpRequests(
                        authorization ->
                                authorization

                                        /*
                                         * Permite procesarea interna a erorilor.
                                         *
                                         * Fara aceasta regula, o exceptie produsa
                                         * de un endpoint poate ajunge pe /error si
                                         * poate fi raportata incorect drept 401.
                                         */
                                        .dispatcherTypeMatchers(
                                                DispatcherType.ERROR
                                        )
                                        .permitAll()

                                        .requestMatchers(
                                                "/error"
                                        )
                                        .permitAll()

                                        /*
                                         * ENDPOINTURI PUBLICE
                                         */

                                        // Login angajați.
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/auth/login"
                                        )
                                        .permitAll()

                                        // Meniul clientului.
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/products",
                                                "/api/products/**"
                                        )
                                        .permitAll()

                                        // Trimiterea feedbackului.
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/feedback"
                                        )
                                        .permitAll()

                                        /*
                                         * COMENZI
                                         */

                                        // Crearea unei comenzi.
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/orders"
                                        )
                                        .hasAnyRole(
                                                "WAITER",
                                                "ADMIN"
                                        )

                                        // Lista tuturor comenzilor.
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/orders"
                                        )
                                        .hasAnyRole(
                                                "MANAGER",
                                                "ADMIN"
                                        )

                                        // Comenzile active.
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/orders/active"
                                        )
                                        .hasAnyRole(
                                                "WAITER",
                                                "MANAGER",
                                                "ADMIN"
                                        )

                                        // Statisticile comenzilor.
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/orders/statistics",
                                                "/api/orders/statistics/today"
                                        )
                                        .hasAnyRole(
                                                "MANAGER",
                                                "ADMIN"
                                        )

                                        // Comenzile pentru bucătărie.
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/orders/kitchen"
                                        )
                                        .hasAnyRole(
                                                "KITCHEN",
                                                "ADMIN"
                                        )

                                        // Comenzile pentru bar.
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/orders/bar"
                                        )
                                        .hasAnyRole(
                                                "BAR",
                                                "ADMIN"
                                        )

                                        // Actualizarea stării unui produs.
                                        .requestMatchers(
                                                HttpMethod.PUT,
                                                "/api/orders/items/*/status"
                                        )
                                        .hasAnyRole(
                                                "KITCHEN",
                                                "BAR",
                                                "ADMIN"
                                        )

                                        // Actualizarea stării generale a comenzii.
                                        .requestMatchers(
                                                HttpMethod.PUT,
                                                "/api/orders/*/status"
                                        )
                                        .hasAnyRole(
                                                "WAITER",
                                                "ADMIN"
                                        )

                                        /*
                                         * MESE
                                         */

                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/tables",
                                                "/api/tables/**"
                                        )
                                        .hasAnyRole(
                                                "WAITER",
                                                "MANAGER",
                                                "ADMIN"
                                        )

                                        /*
                                         * SESIUNI MESE
                                         */

                                        // Vizualizarea sesiunilor active.
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/table-sessions/active"
                                        )
                                        .hasAnyRole(
                                                "WAITER",
                                                "MANAGER",
                                                "ADMIN"
                                        )

                                        // Crearea sesiunii unei mese.
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/table-sessions/table/*"
                                        )
                                        .hasAnyRole(
                                                "WAITER",
                                                "ADMIN"
                                        )

                                        // Închiderea sesiunii unei mese.
                                        .requestMatchers(
                                                HttpMethod.PUT,
                                                "/api/table-sessions/*/close"
                                        )
                                        .hasAnyRole(
                                                "WAITER",
                                                "ADMIN"
                                        )

                                        /*
                                         * FEEDBACK
                                         */

                                        // Citirea tuturor recenziilor.
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/feedback"
                                        )
                                        .hasAnyRole(
                                                "MANAGER",
                                                "ADMIN"
                                        )

                                        /*
                                         * STOC AUXILIAR
                                         */

                                        .requestMatchers(
                                                "/api/auxiliary-supplies",
                                                "/api/auxiliary-supplies/**"
                                        )
                                        .hasAnyRole(
                                                "MANAGER",
                                                "ADMIN"
                                        )

                                        /*
                                         * TRAFIC
                                         */

                                        .requestMatchers(
                                                "/api/traffic",
                                                "/api/traffic/**"
                                        )
                                        .hasAnyRole(
                                                "MANAGER",
                                                "ADMIN"
                                        )

                                        /*
                                         * SISTEM DECIZIONAL AI
                                         */

                                        .requestMatchers(
                                                "/api/decision",
                                                "/api/decision/**"
                                        )
                                        .hasRole(
                                                "ADMIN"
                                        )

                                        /*
                                         * GESTIONAREA TURELOR ANGAJATILOR
                                         */

                                        .requestMatchers(
                                                "/api/employee-shifts",
                                                "/api/employee-shifts/**"
                                        )
                                        .hasAnyRole(
                                                "MANAGER",
                                                "ADMIN"
                                        )

                                        /*
                                         * ADMINISTRARE ANGAJATI
                                         */

                                        .requestMatchers(
                                                "/api/employees",
                                                "/api/employees/**"
                                        )
                                        .hasRole(
                                                "ADMIN"
                                        )

                                        /*
                                         * RESTUL ENDPOINTURILOR
                                         */

                                        .anyRequest()
                                        .authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter>
    jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter
    ) {
        FilterRegistrationBean<JwtAuthenticationFilter>
                registration =
                new FilterRegistrationBean<>(filter);

        registration.setEnabled(false);

        return registration;
    }
}