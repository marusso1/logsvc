package com.sevcosecurity.logsvc.controllers;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sevcosecurity.logsvc.models.LogMessage;
import com.sevcosecurity.logsvc.models.LogMessageList;
import com.sevcosecurity.logsvc.models.ParsedLogMessage;
import com.sevcosecurity.logsvc.models.SearchParams;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Controller for the "logs" REST API.
 */
@RestController
@RequestMapping("/logs")
public class LogsController {
    Logger logger = LoggerFactory.getLogger(LogsController.class);

    private final ElasticsearchClient esClient;
    private final RestClient restClient;
    final String IDX_NAME = "log_idx";
    ObjectMapper mapper = new ObjectMapper();
    private boolean initComplete = false;
    public LogsController(ElasticsearchClient esClient, RestClient restClient) {
        this.esClient = esClient;
        this.restClient = restClient;
    }

    @PostConstruct
    void postConstruct() throws IOException {
        logger.info("API controller constructed");

        // NOTE: initializing the index here results in ConnectionRefused. When running
        // this app in Docker alongside Elasticsearch, ES is not yet ready then
        // the PostConstruct event happens.
        // idxInit();
    }

    @GetMapping(value = "/about")
    public String getRootInfo() throws IOException {
        idxInit();
        return "Hastily created log service from Mark Russo";
    }

    /**
     * Creates the index in ES if it doesn't already exist.
     *
     * @throws IOException
     */
    void idxInit() throws IOException {
        if (!initComplete)
            return;

        if (!esClient.indices().exists(r -> r.index(IDX_NAME)).value()) {
            Request request = new Request("PUT", "/" + IDX_NAME);
            String mapping = "{\n" +
                    "    \"mappings\": {\n" +
                    "      \"properties\": {\n" +
                    "        \"dt\":    { \"type\": \"date\" },  \n" +
                    "        \"level\":  { \"type\": \"keyword\"  }, \n" +
                    "        \"module\":  { \"type\": \"keyword\"  }, \n" +
                    "        \"msg\":   { \"type\": \"text\"  }     \n" +
                    "      }\n" +
                    "    }\n" +
                    "}";
            request.setJsonEntity(mapping);
            restClient.performRequest(request);
        }

        initComplete = true;
    }

    /**
     * Inserts log messages into Elasticsearch.
     *
     * @param payload
     * @return
     * @throws IOException
     */
    @PostMapping()
    public ResponseEntity<String> post(@RequestBody LogMessageList payload) throws IOException {
        idxInit();
        logger.info("Insert: " + payload);

        String body = mapper.writeValueAsString(payload);

        // Create the bulk-insert records
        List<LogMessage> msgList = payload.getItems();
        List<String> bulkDataList = new ArrayList<>(msgList.size() * 2);
        for (LogMessage msg : msgList) {
            System.out.println("msg = '" + msg);
            bulkDataList.add("{\"index\": {}}");
            bulkDataList.add(mapper.writeValueAsString(new ParsedLogMessage(msg.getMessage())));
        }
        String postBody = String.join("\n", bulkDataList) + "\n";

        // Insert into ES
        Request request = new Request("PUT", "/" + IDX_NAME + "/_bulk");
        logger.debug(postBody);
        request.setJsonEntity(postBody);
        restClient.performRequest(request);
        return ResponseEntity.ok("");
    }

    /**
     * Searches for matching log messages.
     *
     * @param params
     * @return
     * @throws IOException
     */
    @PostMapping(value = "/search")
    public ResponseEntity<LogMessageList> search(@RequestBody SearchParams params) throws IOException {
        idxInit();
        logger.info("Search: " + params);

        // Execute the search against ES
        JSONObject query = buildQuery(params);
        String body = query.toString();
        Request request = new Request("GET", "/" + IDX_NAME + "/_search");
        request.setJsonEntity(body);
        logger.debug(body);
        final Response response = restClient.performRequest(request);

        // Process the ES response to collect the log messages
        String esJson = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
        logger.debug("esJson: " + esJson);
        Map<String, Object> esMap = mapper.readValue(esJson, Map.class);
        Map<String, Object> hits = (Map<String, Object>) esMap.get("hits");
        logger.debug("hits: " + hits);
        String value;
        List<LogMessage> msgList = new ArrayList<>();
        LogMessageList logMessageList = new LogMessageList(msgList);
        if (!hits.isEmpty()) {
            logger.debug("hits is not empty");
            List<Map<String, Object>> hitsList = (List<Map<String, Object>>) hits.get("hits");
            logger.debug("hitsList: " + hitsList);
            if (hitsList != null) {
                logger.debug("hitsList is not null");
                for (Map<String, Object> hit : hitsList) {
                    logger.debug("single hit: " + hit);
                    Map<String, Object> source = (Map<String, Object>) hit.get("_source");
                    if (source != null) {
                        value = String.format("%s %s %s - %s",
                                source.get("dt"),
                                source.get("level"),
                                source.get("module"),
                                source.get("msg"));
                        msgList.add(new LogMessage(value));
                    }
                }
            }
        }

        return ResponseEntity.ok(logMessageList);
    }

    /**
     * Deletes matching log messages from Elasticsearch.
     *
     * @param params
     * @return
     * @throws IOException
     */
    @DeleteMapping(value = "/search")
    public ResponseEntity<String> delete(@RequestBody SearchParams params) throws IOException {
        idxInit();
        logger.info("Delete: " + params);

        //
        // Search ES for matching documents
        //
        JSONObject query = buildQuery(params);
        String postBody = query.toString();
        Request request = new Request("GET", "/" + IDX_NAME + "/_search");
        request.setJsonEntity(postBody);
        logger.debug(postBody);
        Response response = restClient.performRequest(request);

        //
        // Collect the ids of the matching documents
        //
        String esJson = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
        logger.debug("esJson: " + esJson);
        Map<String, Object> esMap = mapper.readValue(esJson, Map.class);
        Map<String, Object> hits = (Map<String, Object>) esMap.get("hits");
        logger.debug("hits: " + hits);
        List<String> idList = new ArrayList<>();
        if (!hits.isEmpty()) {
            logger.debug("hits is not empty");
            List<Map<String, Object>> hitsList = (List<Map<String, Object>>) hits.get("hits");
            logger.debug("hitsList: " + hitsList);
            if (hitsList != null) {
                logger.debug("hitsList is not null");
                for (Map<String, Object> hit : hitsList) {
                    logger.debug("single hit: " + hit);
                    String id = (String) hit.get("_id");
                    if (StringUtils.isNotEmpty(id)) {
                        idList.add(id);
                    }
                }
            }
        }

        if (idList.isEmpty()) {
            return ResponseEntity.ok("{\"deleted\": 0}");
        }

        //
        // Delete the matching documents
        //
        int delCount = 0;
        request = new Request("POST", "/" + IDX_NAME + "/_bulk");
        List<String> bulkDataList = new ArrayList<>(idList.size());

        for (String id : idList) {
            bulkDataList.add(String.format("{ \"delete\" : { \"_id\" : \"%s\" } }", id));
        }

        postBody = String.join("\n", bulkDataList) + "\n";
        logger.debug("delete postBody: " + postBody);
        request.setJsonEntity(postBody);
        response = restClient.performRequest(request);
        esJson = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);

        //
        // Read the ES response to get the count of deleted documents
        //
        esMap = mapper.readValue(esJson, Map.class);
        List<Map<String, Object>> itemList = (List<Map<String, Object>>) esMap.get("items");
        for (Map<String, Object> item : itemList) {
            Map<String, Object> delResult = (Map<String, Object>) item.get("delete");
            if (delResult != null) {
                String result = (String) delResult.get("result");
                if (StringUtils.equals(result, "deleted")) {
                    ++delCount;
                }
            }
        }

        return ResponseEntity.ok(String.format("{\"deleted\": %d}", delCount));
    }

    /**
     * Renders a {@link SearchParams} object as an Elasticsearch query.
     *
     * @param params
     * @return A JSON object to execute against Elasticsearch
     */
    static JSONObject buildQuery(SearchParams params) {
        JSONObject query = new JSONObject();
        JSONArray filter = new JSONArray();
        JSONObject dtValues = new JSONObject();

        String value = params.getStart();
        if (StringUtils.isNotEmpty(value))
            dtValues.put("gte", value);

        value = params.getEnd();
        if (StringUtils.isNotEmpty(value))
            dtValues.put("lte", value);

        if (!dtValues.isEmpty()) {
            filter.put(new JSONObject().put("term", new JSONObject().put("range", new JSONObject().put("dt", dtValues))));
        }

        addTermToFilter(filter, "level.keyword", params.getLevel());
        addTermToFilter(filter, "module.keyword", params.getModule());
        addTermToFilter(filter, "msg.keyword", params.getMessage());

        if (!filter.isEmpty()) {
            query.put("query", new JSONObject().put("bool", new JSONObject().put("filter", filter)));
        }

        return query;
    }

    static void addTermToFilter(JSONArray filter, String field, String value) {
        if (StringUtils.isNotEmpty(value)) {
            filter.put(new JSONObject().put("term", new JSONObject().put(field, value)));
        }
    }

    public static void main(String[] args) {
        SearchParams params = new SearchParams();
        params.setStart("2020-01-01");
        params.setEnd("2022-12-31");
        params.setLevel("INFO");
        params.setModule("api");
        params.setMessage("From 2022: This is an INFO level log message!");

        JSONObject query = buildQuery(params);
        System.out.println(query);
    }
}