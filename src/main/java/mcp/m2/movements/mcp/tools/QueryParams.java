package mcp.m2.movements.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class QueryParams {

	private QueryParams() {
	}

	static Map<String, List<String>> of(Object... keyValues) {
		var params = new LinkedHashMap<String, List<String>>();
		for (int i = 0; i < keyValues.length; i += 2) {
			var value = keyValues[i + 1];
			if (value instanceof List<?> list) {
				if (!list.isEmpty()) {
					params.put((String) keyValues[i], list.stream().map(String::valueOf).toList());
				}
			} else if (value != null) {
				params.put((String) keyValues[i], List.of(String.valueOf(value)));
			}
		}
		return params;
	}
}
