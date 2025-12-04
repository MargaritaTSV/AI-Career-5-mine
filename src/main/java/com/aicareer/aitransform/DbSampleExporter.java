package com.aicareer.aitransform;

import com.aicareer.hh.infrastructure.db.DbConnectionProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DbSampleExporter {
    public static void main(String[] args) throws Exception {
        Path output = args.length > 0
                ? Paths.get(args[0])
                : Paths.get("target/samples/skills-extraction-db-sample.json");
        int limit = args.length > 1 ? Integer.parseInt(args[1]) : 50;
        export(output, limit);
    }

    public static void export(Path output, int limit) throws Exception {
        DbConnectionProvider provider = new DbConnectionProvider();
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        ArrayNode array = mapper.createArrayNode();

        try (Connection connection = provider.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT title, description FROM vacancy ORDER BY published_at DESC NULLS LAST LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String title = rs.getString("title");
                    String description = rs.getString("description");
                    if (title == null && description == null) {
                        continue;
                    }
                    ObjectNode node = mapper.createObjectNode();
                    if (title != null) {
                        node.put("title", title);
                    }
                    if (description != null) {
                        node.put("description", description);
                    }
                    array.add(node);
                }
            }
        }

        Files.createDirectories(output.toAbsolutePath().getParent());
        mapper.writeValue(output.toFile(), array);
        System.out.println("Saved " + array.size() + " rows to " + output.toAbsolutePath());
    }
}
