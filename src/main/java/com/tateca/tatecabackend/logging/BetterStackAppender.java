package com.tateca.tatecabackend.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Logback Appender for sending logs to Better Stack via HTTP.
 * <p>
 * This appender sends logs asynchronously to avoid blocking the main application thread.
 * Logs are queued and sent to Better Stack's HTTP endpoint.
 */
public class BetterStackAppender extends AppenderBase<ILoggingEvent> {

    private static final int DEFAULT_QUEUE_SIZE = 1000;
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(10);

    @Setter @Getter private String sourceToken;
    @Setter @Getter private String endpoint = "https://in.logs.betterstack.com";
    @Setter @Getter private Encoder<ILoggingEvent> encoder;
    @Setter @Getter private int queueSize = DEFAULT_QUEUE_SIZE;

    private BlockingQueue<ILoggingEvent> eventQueue;
    private ExecutorService executorService;
    private HttpClient httpClient;
    private volatile boolean running = false;

    @Override
    public void start() {
        if (sourceToken == null || sourceToken.trim().isEmpty()) {
            addWarn("Better Stack source token not configured. Appender will not start.");
            return;
        }

        if (encoder == null) {
            addError("No encoder set for the appender named [" + name + "].");
            return;
        }

        encoder.start();
        eventQueue = new ArrayBlockingQueue<>(queueSize);
        httpClient = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .build();
        running = true;

        executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "BetterStackAppender-Worker");
            thread.setDaemon(true);
            return thread;
        });
        executorService.submit(this::processQueue);

        super.start();
        addInfo("Better Stack Appender started successfully");
    }

    @Override
    public void stop() {
        running = false;

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT.getSeconds(), TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (encoder != null) {
            encoder.stop();
        }

        super.stop();
        addInfo("Better Stack Appender stopped");
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (!running) {
            return;
        }

        if (!eventQueue.offer(eventObject)) {
            addWarn("Better Stack event queue is full. Dropping log event.");
        }
    }

    private void processQueue() {
        while (running || !eventQueue.isEmpty()) {
            try {
                ILoggingEvent event = eventQueue.poll(1, TimeUnit.SECONDS);
                if (event != null) {
                    sendToBetterStack(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                addError("Error processing log event", e);
            }
        }
    }

    private void sendToBetterStack(ILoggingEvent event) {
        try {
            byte[] encodedEvent = encoder.encode(event);
            String jsonPayload = new String(encodedEvent, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + sourceToken)
                    .header("Content-Type", "application/json")
                    .timeout(HTTP_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 400) {
                            addWarn("Better Stack returned HTTP " + response.statusCode() + ": " + response.body());
                        }
                    })
                    .exceptionally(throwable -> {
                        addError("Failed to send log to Better Stack", throwable);
                        return null;
                    });

        } catch (Exception e) {
            addError("Error encoding or sending log event", e);
        }
    }
}
