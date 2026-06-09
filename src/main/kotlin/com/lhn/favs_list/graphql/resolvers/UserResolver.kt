package com.lhn.favs_list.graphql.resolvers

import com.lhn.favs_list.graphql.GRAPHQL_REQUEST_CONTEXT_KEY
import com.lhn.favs_list.graphql.GraphqlRequestContext
import com.lhn.favs_list.graphql.auth.GraphqlAuthGuard
import com.lhn.favs_list.users.UpdateCurrentUserCommand
import com.lhn.favs_list.users.UserProfile
import com.lhn.favs_list.users.UserService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.ContextValue
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class UserResolver(
    private val userService: UserService,
    private val authGuard: GraphqlAuthGuard,
) {

    @QueryMapping
    fun me(
        @ContextValue(name = GRAPHQL_REQUEST_CONTEXT_KEY, required = false)
        requestContext: GraphqlRequestContext?,
    ): UserProfile {
        val authenticatedSession = authGuard.requireAuthenticated(requestContext)

        return userService.getCurrentUser(authenticatedSession.userId)
    }

    @MutationMapping
    fun updateMe(
        @Argument input: UpdateMeInput,
        @ContextValue(name = GRAPHQL_REQUEST_CONTEXT_KEY, required = false)
        requestContext: GraphqlRequestContext?,
    ): UserProfile {
        val authenticatedSession = authGuard.requireAuthenticated(requestContext)

        return userService.updateCurrentUser(
            userId = authenticatedSession.userId,
            command = UpdateCurrentUserCommand(
                name = input.name,
                nickname = input.nickname,
            ),
        )
    }

    @MutationMapping
    fun deleteMe(
        @ContextValue(name = GRAPHQL_REQUEST_CONTEXT_KEY, required = false)
        requestContext: GraphqlRequestContext?,
    ): DeleteMePayload {
        val authenticatedSession = authGuard.requireAuthenticated(requestContext)

        return DeleteMePayload(
            success = userService.deleteCurrentUser(authenticatedSession.userId),
        )
    }
}
