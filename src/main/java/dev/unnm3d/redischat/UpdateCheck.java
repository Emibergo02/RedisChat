package dev.unnm3d.redischat;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class UpdateCheck {

    public final Logger logger;

    public UpdateCheck(RedisChat plugin) {
        this.logger = plugin.getLogger();
    }

    public void getVersion(final Consumer<String> consumer) {
        CompletableFuture.runAsync(() -> {
            try (InputStream inputStream = new URL("https://api.spiget.org/v2/resources/111015/versions/latest")
                    .openStream(); Scanner scanner = new Scanner(inputStream)) {
                if (scanner.hasNext()) {
                    JsonElement element = JsonParser.parseString(scanner.next()).getAsJsonObject().get("name");
                    if (element != null)
                        consumer.accept(element.getAsString());
                }
            } catch (IOException exception) {
                logger.severe("Unable to check for updates: " + exception.getMessage());
            }
        }, RedisChat.getInstance().getExecutorService());
    }
}