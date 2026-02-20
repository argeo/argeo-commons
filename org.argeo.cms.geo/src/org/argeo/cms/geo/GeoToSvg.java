package org.argeo.cms.geo;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Converts a geographical feature to an SVG. */
public class GeoToSvg {
	public void convertGeoJsonToSvg(Path source, Path target) {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = Files.newInputStream(source);
				Writer out = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
			JsonNode tree = objectMapper.readTree(in);
			JsonNode coord = tree.get("features").get(0).get("geometry").get("coordinates");
			double ratio = 100;
			double minX = Double.POSITIVE_INFINITY;
			double maxX = Double.NEGATIVE_INFINITY;
			double minY = Double.POSITIVE_INFINITY;
			double maxY = Double.NEGATIVE_INFINITY;
			List<String> shapes = new ArrayList<>();
			for (JsonNode shape : coord) {
				StringBuffer sb = new StringBuffer();
				sb.append("<polyline style=\"stroke-width:0.00000003;stroke:#000000;\" points=\"");
				for (JsonNode latlng : shape) {
					double lat = latlng.get(0).asDouble();
					double y = lat * ratio;
					if (y < minY)
						minY = y;
					if (y > maxY)
						maxY = y;
					double lng = latlng.get(1).asDouble();
					double x = lng * ratio;
					if (x < minX)
						minX = x;
					if (x > maxX)
						maxX = x;
					sb.append(y + "," + x + " ");
				}
				sb.append("\">");
				sb.append("</polyline>\n");
				shapes.add(sb.toString());
			}

			double width = maxX - minX;
			double height = maxY - minY;
			out.write("<svg xmlns=\"http://www.w3.org/2000/svg\"\n");
			out.write(" width=\"" + (int) (width * 1000) + "\"\n");
			out.write(" height=\"" + (int) (height * 1000) + "\"\n");
			out.write(" viewBox=\"" + minX + "," + minY + "," + width + "," + height + "\"\n");
			out.write(">\n");
			for (String shape : shapes) {
				out.write(shape);
				out.write("\n");
			}
			out.write("</svg>");
		} catch (IOException e) {
			throw new RuntimeException("Cannot convert " + source + " to " + target, e);
		}
	}

}
