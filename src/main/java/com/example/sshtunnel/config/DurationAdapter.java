package com.example.sshtunnel.config;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.Duration;

public class DurationAdapter implements JsonSerializer<Duration>, JsonDeserializer<Duration> {
    @Override
    public Duration deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        String s = jsonElement.getAsJsonPrimitive().getAsString();
        return Duration.parse(s);
    }

    @Override
    public JsonElement serialize(Duration duration, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(duration.toString());
    }
}
