package com.example.mykku.auth.application.usecase

import com.example.mykku.auth.application.dto.RefreshTokenCommand
import com.example.mykku.auth.application.dto.RefreshTokenResult
import com.example.mykku.auth.application.port.input.RefreshTokenUseCase
import com.example.mykku.auth.application.port.output.MemberAuthPort
import com.example.mykku.auth.application.port.output.TokenProvider
import com.example.mykku.auth.exception.AuthException
import com.example.mykku.member.exception.MemberException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RefreshTokenUseCaseImpl(
    private val tokenProvider: TokenProvider,
    private val memberAuthPort: MemberAuthPort
) : RefreshTokenUseCase {

    override fun refresh(command: RefreshTokenCommand): RefreshTokenResult {
        validateRefreshToken(command.refreshToken)

        val memberId = tokenProvider.getMemberIdFromToken(command.refreshToken)
        val member = memberAuthPort.findById(memberId)
            ?: throw MemberException.memberNotFound()

        return RefreshTokenResult(
            accessToken = tokenProvider.generateAccessToken(member.id.value, member.email),
            refreshToken = tokenProvider.generateRefreshToken(member.id.value),
            expiresIn = tokenProvider.getAccessTokenExpiration(),
            refreshTokenExpiresIn = tokenProvider.getRefreshTokenExpiration()
        )
    }

    private fun validateRefreshToken(refreshToken: String) {
        if (!tokenProvider.validateToken(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
            throw AuthException.invalidToken()
        }
    }
}
