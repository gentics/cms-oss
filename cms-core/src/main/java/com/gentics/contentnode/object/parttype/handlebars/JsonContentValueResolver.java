package com.gentics.contentnode.object.parttype.handlebars;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections4.keyvalue.UnmodifiableMapEntry;

import com.github.jknack.handlebars.ValueResolver;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * A singleton of {@link ValueResolver} of {@link JsonObject} or {@link JsonArray}.
 */
public enum JsonContentValueResolver implements ValueResolver {

	INSTANCE;

	@Override
	public Object resolve(Object context, String name) {
		if (context instanceof JsonArray ja) {
			try {
				int index = Integer.parseInt(name);
				return ja.getValue(index);
			} catch (NumberFormatException e) {
			}
		} else if (context instanceof JsonObject jo) {
			return jo.getValue(name);
		}
		return UNRESOLVED;
	}

	@Override
	public Object resolve(Object context) {
		if (context instanceof JsonObject || context instanceof JsonArray) {
			return context;
		}
		return UNRESOLVED;
	}

	@Override
	public Set<Entry<String, Object>> propertySet(Object context) {
		if (context instanceof JsonArray ja) {
			return IntStream.range(0, ja.size()).mapToObj(i -> new UnmodifiableMapEntry<>(Integer.toString(i), ja.getValue(i))).collect(Collectors.toSet());
		} else if (context instanceof JsonObject jo) {
			return jo.getMap().entrySet();
		}
		return Collections.emptySet();
	}
}
