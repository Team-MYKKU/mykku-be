package com.example.mykku

import com.example.mykku.board.adapter.output.persistence.BoardJpaRepository
import com.example.mykku.board.adapter.output.persistence.entity.BoardJpaEntity
import com.example.mykku.member.adapter.output.persistence.MemberJpaRepository
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.member.domain.vo.SocialProvider
import com.example.mykku.role.adapter.output.persistence.entity.RoleJpaEntity
import com.example.mykku.role.adapter.output.persistence.repository.RoleJpaRepository
import org.springframework.beans.factory.annotation.Autowired

abstract class BaseFixtureTest {

    @Autowired
    protected lateinit var memberJpaRepository: MemberJpaRepository

    @Autowired
    protected lateinit var boardJpaRepository: BoardJpaRepository

    @Autowired
    protected lateinit var roleJpaRepository: RoleJpaRepository

    protected fun createAndSaveMember(
        memberId: String? = "testmember",
        nickname: String = "테스트유저",
        email: String = "test@example.com",
        socialId: String = "12345",
        provider: SocialProvider = SocialProvider.GOOGLE,
        role: RoleJpaEntity? = null,
        profileImage: String = ""
    ): MemberJpaEntity {
        val member = MemberJpaEntity(
            memberId = memberId,
            nickname = nickname,
            email = email,
            socialId = socialId,
            provider = provider,
            role = role,
            profileImage = profileImage
        )
        return memberJpaRepository.save(member)
    }

    protected fun createAndSaveEmailMember(
        email: String,
        password: String,
        memberId: String? = null,
        nickname: String? = null,
        role: RoleJpaEntity? = null
    ): MemberJpaEntity {
        val member = MemberJpaEntity(
            memberId = memberId,
            nickname = nickname,
            role = role,
            profileImage = "",
            provider = SocialProvider.EMAIL,
            socialId = null,
            email = email,
            password = password
        )
        return memberJpaRepository.save(member)
    }

    protected fun createAndSaveBoard(
        title: String = "테스트 게시판",
        logo: String = "test_logo.png"
    ): BoardJpaEntity {
        val board = BoardJpaEntity(
            title = title,
            logo = logo
        )
        return boardJpaRepository.save(board)
    }
}
