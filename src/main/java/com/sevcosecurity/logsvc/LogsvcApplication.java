package com.sevcosecurity.logsvc;

import com.sevcosecurity.logsvc.controllers.LogsController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LogsvcApplication {

    @Autowired
    LogsController logsController;

    public static void main(String[] args) {
        SpringApplication.run(LogsvcApplication.class, args);
    }

}
