package com.lhn.favs_list.graphql.resolvers

import com.lhn.favs_list.auth.AuthService
import com.lhn.favs_list.auth.LoginCommand
import com.lhn.favs_list.auth.RegisterUserCommand
import com.lhn.favs_list.graphql.GRAPHQL_REQUEST_CONTEXT_KEY
import com.lhn.favs_list.graphql.GraphqlRequestContext
import com.lhn.favs_list.graphql.auth.GraphqlAuthGuard
import com.lhn.favs_list.graphql.auth.toRequestMetadata
import java.lang.Math.toIntExact
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.ContextValue
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller

@Controller
class AuthResolver(
    private val authService: AuthService,
    private val authGuard: GraphqlAuthGuard,
) {

    @MutationMapping
    fun registerUser(
        @Argument input: RegisterUserInput,
    ): RegisterUserPayload =
        RegisterUserPayload(
            user = authService.registerUser(
                RegisterUserCommand(
                    email = input.email,
                    name = input.name,
                    nickname = input.nickname,
                    password = input.password,
                ),
            ),
        )

    @MutationMapping
    fun login(
        @Argument input: LoginInput,
        @ContextValue(name = GRAPHQL_REQUEST_CONTEXT_KEY, required = false)
        requestContext: GraphqlRequestContext?,
    ): LoginPayload {
        val loginResult = authService.login(
            command = LoginCommand(
                email = input.email,
                password = input.password,
            ),
            requestMetadata = requestContext?.toRequestMetadata()
                ?: com.lhn.favs_list.auth.RequestMetadata(),
        )

        return LoginPayload(
            accessToken = loginResult.accessToken,
            tokenType = loginResult.tokenType,
            expiresIn = toIntExact(loginResult.expiresIn),
            user = loginResult.user,
        )
    }

    @MutationMapping
    fun logout(
        @ContextValue(name = GRAPHQL_REQUEST_CONTEXT_KEY, required = false)
        requestContext: GraphqlRequestContext?,
    ): LogoutPayload {
        val authenticatedSession = authGuard.requireAuthenticated(requestContext)

        return LogoutPayload(
            success = authService.logout(authenticatedSession.sessionJti),
        )
    }
}
