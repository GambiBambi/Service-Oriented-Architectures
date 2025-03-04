package org.bp.microbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "org.bp.microbooking")
public class MicroBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroBookingApplication.class, args);
    }

}
