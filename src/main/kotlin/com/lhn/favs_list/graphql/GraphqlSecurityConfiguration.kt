package com.lhn.favs_list.graphql

import com.lhn.favs_list.shared.config.CorsAllowedOriginsProperties
import com.lhn.favs_list.shared.config.GraphqlSecurityProperties
import graphql.analysis.MaxQueryComplexityInstrumentation
import graphql.analysis.MaxQueryDepthInstrumentation
import graphql.execution.instrumentation.Instrumentation
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import org.springframework.boot.graphql.autoconfigure.GraphQlProperties
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.GraphQlSource
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class GraphqlSecurityConfiguration {

    @Bean
    fun graphqlAbuseProtectionInstrumentationFactory(
        graphqlSecurityProperties: GraphqlSecurityProperties,
    ) = GraphqlAbuseProtectionInstrumentationFactory(graphqlSecurityProperties)

    @Bean
    fun graphqlAbuseProtectionCustomizer(
        instrumentationFactory: GraphqlAbuseProtectionInstrumentationFactory,
    ): GraphQlSourceBuilderCustomizer =
        GraphQlSourceBuilderCustomizer { builder: GraphQlSource.SchemaResourceBuilder ->
            builder.instrumentation(instrumentationFactory.create())
        }

    @Bean
    fun graphqlHttpRequestHardeningFilter(
        graphqlProperties: GraphQlProperties,
        graphqlSecurityProperties: GraphqlSecurityProperties,
    ) = GraphqlHttpRequestHardeningFilter(
        graphqlPath = graphqlProperties.http.path,
        graphqlSecurityProperties = graphqlSecurityProperties,
    )

    @Bean
    fun graphqlCorsConfigurer(
        graphqlProperties: GraphQlProperties,
        corsAllowedOriginsProperties: CorsAllowedOriginsProperties,
    ): WebMvcConfigurer =
        object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping(graphqlProperties.http.path)
                    .allowedOrigins(*corsAllowedOriginsProperties.allowedOrigins.toTypedArray())
                    .allowedMethods("GET", "POST", "OPTIONS")
            }
        }
}

class GraphqlAbuseProtectionInstrumentationFactory(
    private val graphqlSecurityProperties: GraphqlSecurityProperties,
) {

    fun create(): List<Instrumentation> =
        listOf(
            MaxQueryDepthInstrumentation(graphqlSecurityProperties.maxQueryDepth),
            MaxQueryComplexityInstrumentation(graphqlSecurityProperties.maxQueryComplexity),
        )
}

class GraphqlHttpRequestHardeningFilter(
    private val graphqlPath: String,
    private val graphqlSecurityProperties: GraphqlSecurityProperties,
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.method != "POST" || requestPath(request) != graphqlPath

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestBody = readRequestBody(request)
        if (requestBody.size.toLong() > graphqlSecurityProperties.maxRequestBodyBytes) {
            writeGraphqlError(
                response = response,
                status = HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                code = "PAYLOAD_TOO_LARGE",
                message = "GraphQL request body exceeds the configured limit",
            )
            return
        }
        if (!graphqlSecurityProperties.allowBatchRequests && isBatchRequest(requestBody)) {
            writeGraphqlError(
                response = response,
                status = HttpServletResponse.SC_BAD_REQUEST,
                code = "BATCH_REQUESTS_DISABLED",
                message = "GraphQL batch requests are disabled",
            )
            return
        }

        filterChain.doFilter(CachedBodyHttpServletRequest(request, requestBody), response)
    }

    private fun readRequestBody(request: HttpServletRequest): ByteArray {
        val maxBytesToRead = graphqlSecurityProperties.maxRequestBodyBytes + 1
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        request.inputStream.use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read == -1) {
                    return output.toByteArray()
                }

                output.write(buffer, 0, read)
                if (output.size().toLong() > maxBytesToRead) {
                    return output.toByteArray()
                }
            }
        }
    }

    private fun isBatchRequest(requestBody: ByteArray): Boolean =
        requestBody.toString(Charsets.UTF_8).trimStart().startsWith("[")

    private fun writeGraphqlError(
        response: HttpServletResponse,
        status: Int,
        code: String,
        message: String,
    ) {
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(
            """{"errors":[{"message":"$message","extensions":{"code":"$code"}}]}""",
        )
    }

    private fun requestPath(request: HttpServletRequest): String =
        request.requestURI.removePrefix(request.contextPath)
}

private class CachedBodyHttpServletRequest(
    request: HttpServletRequest,
    private val requestBody: ByteArray,
) : HttpServletRequestWrapper(request) {

    override fun getContentLength(): Int = requestBody.size

    override fun getContentLengthLong(): Long = requestBody.size.toLong()

    override fun getInputStream(): ServletInputStream =
        CachedBodyServletInputStream(requestBody)

    override fun getReader(): BufferedReader =
        BufferedReader(InputStreamReader(getInputStream(), characterEncodingOrDefault()))

    private fun characterEncodingOrDefault(): String =
        characterEncoding ?: Charsets.UTF_8.name()
}

private class CachedBodyServletInputStream(
    requestBody: ByteArray,
) : ServletInputStream() {
    private val delegate = ByteArrayInputStream(requestBody)

    override fun read(): Int = delegate.read()

    override fun isFinished(): Boolean = delegate.available() == 0

    override fun isReady(): Boolean = true

    override fun setReadListener(readListener: ReadListener?) {
        // Blocking IO is sufficient for the MVC GraphQL endpoint in this service.
    }
}
