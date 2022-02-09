package com.sevcosecurity.logsvc.configs;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearch {
    RestClient restClient;
    ElasticsearchTransport transport;
    ElasticsearchClient client;

    void init() {
        if (restClient == null) {
            restClient = RestClient.builder(new HttpHost("elasticsearch", 9200, "http")).build();
//            restClient = RestClient.builder(new HttpHost("localhost", 9200)).build();
        }
        if (transport == null) {
            transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        }
        if (client == null) {
            client = new ElasticsearchClient(transport);
        }
    }

    @Bean
    RestClient restClient() {
        init();
        return restClient;
    }

    @Bean
    ElasticsearchClient client () {
        init();
        return client;
    }
}
