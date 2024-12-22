package org.sheet.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.TextFormat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class LeetCodeClient {
    private static HttpClient httpClient = null;
    public static List<List<Object>> fetchLeetcodeStatuses(List<String> titleSlugs, String username) throws JsonProcessingException {
        if (httpClient == null) {
            httpClient = HttpClient.newHttpClient();
        }

        // Prepare an ExecutorService to fetch the status asynchronously
        ExecutorService executorService = Executors.newFixedThreadPool(25);
        List<Callable<String>> tasks = new ArrayList<>();

        // Iterate over titleSlugs and create tasks
        for (String titleSlug : titleSlugs) {
            tasks.add(() -> fetchStatusForTitleSlug(titleSlug, username));
        }

        // Invoke the tasks asynchronously and collect the results
        try {
            List<Future<String>> futures = executorService.invokeAll(tasks);
            List<List<Object>> statuses = new ArrayList<>();

            // Collect results from futures
            for (Future<String> future : futures) {
                statuses.add(List.of(future.get())); // blocking call, adding status to the list
            }
            executorService.shutdown();
            return statuses;

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error while fetching statuses", e);
        }
    }

    private static String fetchStatusForTitleSlug(String titleSlug, String username) {
        // Create a JSON payload with the GraphQL query
        String query = String.format(
                "{\"query\":\"query userQuestionStatus($titleSlug: String!) { "
                        + "question(titleSlug: $titleSlug) { "
                        + "status "
                        + "} "
                        + "}\",\"variables\":{\"titleSlug\":\"%s\"}}",
                titleSlug);

        // Create the HTTP request body
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://leetcode.com/graphql/"))
                .header("Content-Type", "application/json")
                .header("referer", "https://leetcode.com/" + username + "/")
                .header("Cookie", "LEETCODE_SESSION=<get-from-browser-cookie")
                .POST(HttpRequest.BodyPublishers.ofString(query, StandardCharsets.UTF_8))  // Set the request body
                .build();

        try {
            // Send the request and get the response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Check if the response is successful
            if (response.statusCode() == 200) {
                var responseNode = new ObjectMapper().readTree(response.body());
                var status = responseNode.path("data").path("question").path("status");
                return processStatus(status);
            } else {
                throw new RuntimeException("Error while fetching status of problem " + titleSlug + " with status code: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            // Handle exceptions
            throw new RuntimeException("Error while fetching status of problem " + titleSlug, e);
        }
    }

    private static String processStatus(JsonNode status) {
        if (status.asText().equals("ac")) {
            return "Solved";
        } else if (status.asText().equals("notac")) {
            return "Attempted";
        } else {
            return "Pending";
        }
    }

}
