package com.lhn.favs_list.graphql

import com.lhn.favs_list.shared.config.GraphqlSecurityProperties
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class GraphqlHttpRequestHardeningFilterTests {

    @Test
    fun `rejects request bodies that exceed the configured size`() {
        val filter = GraphqlHttpRequestHardeningFilter(
            graphqlPath = "/graphql",
            graphqlSecurityProperties = GraphqlSecurityProperties(
                maxQueryDepth = 8,
                maxQueryComplexity = 100,
                maxRequestBodyBytes = 10,
                allowBatchRequests = false,
            ),
        )
        val request = MockHttpServletRequest("POST", "/graphql").apply {
            contentType = "application/json"
            setContent("""{"query":"1234567890"}""".toByteArray())
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        assertEquals(413, response.status)
        assertTrue(response.contentAsString.contains("PAYLOAD_TOO_LARGE"))
    }

    @Test
    fun `rejects batch requests when they are disabled`() {
        val filter = GraphqlHttpRequestHardeningFilter(
            graphqlPath = "/graphql",
            graphqlSecurityProperties = GraphqlSecurityProperties(
                maxQueryDepth = 8,
                maxQueryComplexity = 100,
                maxRequestBodyBytes = 1024,
                allowBatchRequests = false,
            ),
        )
        val request = MockHttpServletRequest("POST", "/graphql").apply {
            contentType = "application/json"
            setContent("""[{"query":"{ me { id } }"}]""".toByteArray())
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        assertEquals(400, response.status)
        assertTrue(response.contentAsString.contains("BATCH_REQUESTS_DISABLED"))
    }

    @Test
    fun `passes through non batch requests within the configured size limit`() {
        val filter = GraphqlHttpRequestHardeningFilter(
            graphqlPath = "/graphql",
            graphqlSecurityProperties = GraphqlSecurityProperties(
                maxQueryDepth = 8,
                maxQueryComplexity = 100,
                maxRequestBodyBytes = 1024,
                allowBatchRequests = false,
            ),
        )
        val request = MockHttpServletRequest("POST", "/graphql").apply {
            contentType = "application/json"
            setContent("""{"query":"{ me { id } }"}""".toByteArray())
        }
        val response = MockHttpServletResponse()
        val chain = MockFilterChain(
            object : HttpServlet() {
                override fun service(
                    req: HttpServletRequest,
                    resp: HttpServletResponse,
                ) {
                    resp.writer.write(req.reader.readText())
                }
            },
        )

        filter.doFilter(request, response, chain)

        assertEquals(200, response.status)
        assertEquals("""{"query":"{ me { id } }"}""", response.contentAsString)
    }
}
