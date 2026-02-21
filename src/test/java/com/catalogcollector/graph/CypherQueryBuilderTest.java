package com.catalogcollector.graph;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CypherQueryBuilderTest {

    @Test
    void match_shouldBuildBasicMatchQuery() {
        String sql = CypherQueryBuilder.forGraph("catalog_graph")
                .match("(f:Franchise)-[:HAS_SERIES]->(s:Series)")
                .returning("f.name, s.name")
                .columns("franchise agtype, series agtype")
                .build();

        assertThat(sql).contains("SELECT * FROM cypher('catalog_graph'");
        assertThat(sql).contains("MATCH (f:Franchise)-[:HAS_SERIES]->(s:Series)");
        assertThat(sql).contains("RETURN f.name, s.name");
        assertThat(sql).contains(") AS (franchise agtype, series agtype)");
    }

    @Test
    void match_shouldBuildQueryWithWhereClause() {
        String sql = CypherQueryBuilder.forGraph("catalog_graph")
                .match("(f:Franchise)")
                .where("f.name = 'Dragon Ball'")
                .returning("f")
                .columns("franchise agtype")
                .build();

        assertThat(sql).contains("WHERE f.name = 'Dragon Ball'");
    }

    @Test
    void create_shouldBuildCreateNodeQuery() {
        String sql = CypherQueryBuilder.forGraph("catalog_graph")
                .create("(f:Franchise {name: 'Gundam', category: 'Mecha'})")
                .returning("f")
                .columns("franchise agtype")
                .build();

        assertThat(sql).contains("CREATE (f:Franchise {name: 'Gundam', category: 'Mecha'})");
    }

    @Test
    void build_shouldRequireGraphName() {
        assertThatThrownBy(() ->
                CypherQueryBuilder.forGraph(null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void build_shouldRequireReturnClause() {
        assertThatThrownBy(() ->
                CypherQueryBuilder.forGraph("g")
                        .match("(n)")
                        .columns("n agtype")
                        .build()
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RETURN");
    }

    @Test
    void build_shouldRequireColumns() {
        assertThatThrownBy(() ->
                CypherQueryBuilder.forGraph("g")
                        .match("(n)")
                        .returning("n")
                        .build()
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("columns");
    }
}
