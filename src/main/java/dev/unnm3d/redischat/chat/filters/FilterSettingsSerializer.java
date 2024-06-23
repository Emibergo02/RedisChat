package dev.unnm3d.redischat.chat.filters;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import de.exlll.configlib.Serializer;
import dev.unnm3d.redischat.settings.FiltersConfig;

import java.lang.reflect.Type;
import java.util.Map;

public class FilterSettingsSerializer implements Serializer<FiltersConfig.FilterSettings, Map<String, String>> {
    private static final Gson gson = new Gson();

    @Override
    public Map<String, String> serialize(FiltersConfig.FilterSettings filterSettings) {
        JsonElement jsonElement = gson.toJsonTree(filterSettings);
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();

        return gson.fromJson(jsonElement, type);
    }

    @Override
    public FiltersConfig.FilterSettings deserialize(Map<String, String> stringStringMap) {
        Gson gson = new Gson();
        JsonElement jsonElement = gson.toJsonTree(stringStringMap);
        return gson.fromJson(jsonElement, FiltersConfig.FilterSettings.class);
    }
}
