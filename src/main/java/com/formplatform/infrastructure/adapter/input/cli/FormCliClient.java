package com.formplatform.infrastructure.adapter.input.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * CLI Client for Form Platform REST API
 * Provides command-line interface to interact with the form submission API
 */
@Command(
    name = "formcli",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "CLI client for Form Platform REST API"
)
public class FormCliClient implements Callable<Integer> {

    @Option(names = {"-u", "--url"}, description = "Base URL of the API (default: http://localhost:8080)")
    private String baseUrl = "http://localhost:8080";

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private boolean verbose = false;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new FormCliClient())
            .addSubcommand("submit", new SubmitCommand())
            .addSubcommand("health", new HealthCommand())
            .execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        System.out.println("Use --help to see available commands");
        return 0;
    }

    /**
     * Command to submit a form
     */
    @Command(name = "submit", description = "Submit a form with data")
    static class SubmitCommand implements Callable<Integer> {

        @Option(names = {"-u", "--url"}, description = "Base URL of the API (default: http://localhost:8080)")
        private String baseUrl = "http://localhost:8080";

        @Option(names = {"-d", "--data"}, description = "Form data in JSON format")
        private String jsonData;

        @Option(names = {"-f", "--field"}, description = "Add a field (format: key=value). Can be used multiple times")
        private Map<String, String> fields = new HashMap<>();

        @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
        private boolean verbose = false;

        @Override
        public Integer call() {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> formData;

                // Parse JSON data or use fields
                if (jsonData != null && !jsonData.isEmpty()) {
                    formData = objectMapper.readValue(jsonData, Map.class);
                } else if (!fields.isEmpty()) {
                    formData = new HashMap<>(fields);
                } else {
                    System.err.println("Error: Either --data or --field must be provided");
                    return 1;
                }

                if (verbose) {
                    System.out.println("Submitting form to: " + baseUrl + "/api/forms");
                    System.out.println("Data: " + objectMapper.writeValueAsString(formData));
                }

                // Create HTTP client and request
                HttpClient client = HttpClient.newHttpClient();
                String requestBody = objectMapper.writeValueAsString(formData);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/forms"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

                // Send request
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (verbose) {
                    System.out.println("Response status: " + response.statusCode());
                }

                // Handle response
                if (response.statusCode() == 201) {
                    Map<String, Object> responseData = objectMapper.readValue(response.body(), Map.class);
                    System.out.println("✓ Form submitted successfully!");
                    System.out.println("Form ID: " + responseData.get("id"));
                    System.out.println("Message: " + responseData.get("message"));
                    return 0;
                } else {
                    System.err.println("✗ Error submitting form (HTTP " + response.statusCode() + ")");
                    System.err.println("Response: " + response.body());
                    return 1;
                }

            } catch (Exception e) {
                System.err.println("✗ Error: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 1;
            }
        }
    }

    /**
     * Command to check API health
     */
    @Command(name = "health", description = "Check API health status")
    static class HealthCommand implements Callable<Integer> {

        @Option(names = {"-u", "--url"}, description = "Base URL of the API (default: http://localhost:8080)")
        private String baseUrl = "http://localhost:8080";

        @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
        private boolean verbose = false;

        @Override
        public Integer call() {
            try {
                if (verbose) {
                    System.out.println("Checking health at: " + baseUrl + "/api/forms/health");
                }

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/forms/health"))
                    .GET()
                    .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (verbose) {
                    System.out.println("Response status: " + response.statusCode());
                }

                if (response.statusCode() == 200) {
                    System.out.println("✓ API is healthy");
                    System.out.println("Response: " + response.body());
                    return 0;
                } else {
                    System.err.println("✗ API health check failed (HTTP " + response.statusCode() + ")");
                    System.err.println("Response: " + response.body());
                    return 1;
                }

            } catch (Exception e) {
                System.err.println("✗ Error connecting to API: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 1;
            }
        }
    }
}
