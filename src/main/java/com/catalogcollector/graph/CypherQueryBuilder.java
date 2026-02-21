package com.catalogcollector.graph;

/**
 * Builds SQL-wrapped Cypher queries for Apache AGE.
 * <p>
 * AGE exposes graph queries through a {@code cypher()} SQL function.
 * This builder creates properly formatted SQL strings that can be
 * executed via standard JDBC / JPA native queries.
 * <p>
 * Example:
 * <pre>{@code
 * String sql = CypherQueryBuilder.forGraph("catalog_graph")
 *     .match("(f:Franchise)-[:HAS_SERIES]->(s:Series)")
 *     .returning("f.name, s.name")
 *     .columns("franchise agtype", "series agtype")
 *     .build();
 * }</pre>
 * Produces:
 * <pre>
 * SELECT * FROM cypher('catalog_graph', $$ MATCH (f:Franchise)-[:HAS_SERIES]->(s:Series) RETURN f.name, s.name $$) AS (franchise agtype, series agtype);
 * </pre>
 */
public class CypherQueryBuilder {

    private final String graphName;
    private String matchClause;
    private String createClause;
    private String whereClause;
    private String returnClause;
    private String columnDefinitions;

    private CypherQueryBuilder(String graphName) {
        if (graphName == null || graphName.isBlank()) {
            throw new IllegalArgumentException("Graph name must not be null or blank");
        }
        this.graphName = graphName;
    }

    public static CypherQueryBuilder forGraph(String graphName) {
        return new CypherQueryBuilder(graphName);
    }

    public CypherQueryBuilder match(String pattern) {
        this.matchClause = pattern;
        return this;
    }

    public CypherQueryBuilder create(String pattern) {
        this.createClause = pattern;
        return this;
    }

    public CypherQueryBuilder where(String condition) {
        this.whereClause = condition;
        return this;
    }

    public CypherQueryBuilder returning(String expression) {
        this.returnClause = expression;
        return this;
    }

    public CypherQueryBuilder columns(String definitions) {
        this.columnDefinitions = definitions;
        return this;
    }

    public String build() {
        if (returnClause == null || returnClause.isBlank()) {
            throw new IllegalStateException("RETURN clause is required");
        }
        if (columnDefinitions == null || columnDefinitions.isBlank()) {
            throw new IllegalStateException("Output columns definition is required");
        }

        var cypher = new StringBuilder();

        if (matchClause != null) {
            cypher.append("MATCH ").append(matchClause).append(' ');
        }
        if (createClause != null) {
            cypher.append("CREATE ").append(createClause).append(' ');
        }
        if (whereClause != null) {
            cypher.append("WHERE ").append(whereClause).append(' ');
        }
        cypher.append("RETURN ").append(returnClause);

        return "SELECT * FROM cypher('%s', $$ %s $$) AS (%s);".formatted(
                graphName, cypher.toString().trim(), columnDefinitions);
    }
}
