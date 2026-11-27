package com.example.mykku

import com.example.mykku.config.TestEmailConfig
import com.example.mykku.config.TestEmailSenderConfig
import com.example.mykku.config.TestImageUploadConfig
import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.domain.vo.ContestStatusType
import com.example.mykku.role.adapter.output.persistence.entity.RoleJpaEntity
import com.example.mykku.util.ClassLevelDatabaseCleaner
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@Import(TestEmailConfig::class, TestEmailSenderConfig::class, TestImageUploadConfig::class)
@ExtendWith(ClassLevelDatabaseCleaner::class)
@Transactional
abstract class BaseRepositoryTest : BaseFixtureTest() {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var contestFixtureRepository: ContestJpaRepository

    protected fun createAndSaveRole(
        name: String = "테스트역할",
        description: String = "테스트 역할 설명"
    ): RoleJpaEntity {
        val role = RoleJpaEntity(
            name = name,
            description = description
        )
        return roleJpaRepository.save(role)
    }

    protected fun createAndSaveContest(
        title: String = "테스트 콘테스트",
        description: String? = "테스트 설명",
        startedAt: LocalDateTime = LocalDateTime.now().minusDays(1),
        expiredAt: LocalDateTime = LocalDateTime.now().plusDays(7),
        status: ContestStatusType = ContestStatusType.ACTIVE
    ): ContestJpaEntity {
        val contest = ContestJpaEntity(
            title = title,
            description = description,
            startedAt = startedAt,
            expiredAt = expiredAt,
            status = status,
            thumbnailUrl = "https://example.com/thumbnail.jpg"
        )
        return contestFixtureRepository.save(contest)
    }

    protected fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }
}
