package com.lhn.favs_list.graphql

import com.lhn.favs_list.shared.config.GraphqlSecurityProperties
import graphql.GraphQL
import graphql.Scalars
import graphql.execution.instrumentation.ChainedInstrumentation
import graphql.schema.DataFetcher
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLCodeRegistry
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLTypeReference
import kotlin.test.Test
import kotlin.test.assertTrue

class GraphqlAbuseProtectionInstrumentationFactoryTests {

    @Test
    fun `aborts execution when the configured depth limit is exceeded`() {
        val graphQl = graphQl(
            GraphqlSecurityProperties(
                maxQueryDepth = 2,
                maxQueryComplexity = 100,
                maxRequestBodyBytes = 1024,
                allowBatchRequests = false,
            ),
        )

        val result = graphQl.execute("{ node { child { child { value } } } }")

        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `aborts execution when the configured complexity limit is exceeded`() {
        val graphQl = graphQl(
            GraphqlSecurityProperties(
                maxQueryDepth = 8,
                maxQueryComplexity = 1,
                maxRequestBodyBytes = 1024,
                allowBatchRequests = false,
            ),
        )

        val result = graphQl.execute("{ node { value child { value } } }")

        assertTrue(result.errors.isNotEmpty())
    }

    private fun graphQl(graphqlSecurityProperties: GraphqlSecurityProperties): GraphQL {
        val nodeType = GraphQLObjectType.newObject()
            .name("Node")
            .field { field -> field.name("value").type(Scalars.GraphQLString) }
            .field { field -> field.name("child").type(GraphQLTypeReference("Node")) }
            .build()
        val queryType = GraphQLObjectType.newObject()
            .name("Query")
            .field { field -> field.name("node").type(nodeType) }
            .build()
        val schema = GraphQLSchema.newSchema()
            .query(queryType)
            .additionalType(nodeType)
            .codeRegistry(
                GraphQLCodeRegistry.newCodeRegistry()
                    .dataFetcher(
                        FieldCoordinates.coordinates("Query", "node"),
                        DataFetcher { nestedNode(depth = 3) },
                    )
                    .build(),
            )
            .build()
        val instrumentations = GraphqlAbuseProtectionInstrumentationFactory(graphqlSecurityProperties).create()

        return GraphQL.newGraphQL(schema)
            .instrumentation(ChainedInstrumentation(instrumentations))
            .build()
    }

    private fun nestedNode(depth: Int): Map<String, Any?> =
        if (depth <= 0) {
            mapOf("value" to "leaf", "child" to null)
        } else {
            mapOf("value" to "level-$depth", "child" to nestedNode(depth - 1))
        }
}
