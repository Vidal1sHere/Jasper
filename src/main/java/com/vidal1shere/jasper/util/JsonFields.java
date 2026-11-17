package com.vidal1shere.jasper.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class JsonFields {
    public static void check(JsonObject json, Set<String> allowed, String field) {
        for (String key : json.keySet()) {
            if (!allowed.contains(key)) {
                throw new JsonParseException((field.isEmpty() ? "" : field + ".") + key + ": unknown field, expected one of " + allowed);
            }
        }
    }
}
