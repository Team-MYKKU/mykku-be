package com.example.mykku.role.application.usecase

import com.example.mykku.BaseControllerTest
import com.example.mykku.role.adapter.output.persistence.entity.RoleJpaEntity
import com.example.mykku.role.application.dto.MemberRoleResult
import com.example.mykku.role.application.port.input.GetNewRolesUseCase
import com.example.mykku.role.application.port.output.MemberRoleRepository
import com.example.mykku.role.domain.entity.MemberRole
import com.example.mykku.role.domain.vo.RoleId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DisplayName("신규 칭호 조회 동시성 통합 테스트")
class GetNewRolesConcurrencyTest : BaseControllerTest() {

    companion object {
        private const val THREAD_COUNT = 2
        private const val TIMEOUT_SECONDS = 30L
    }

    @Autowired
    private lateinit var getNewRolesUseCase: GetNewRolesUseCase

    @Autowired
    private lateinit var memberRoleRepository: MemberRoleRepository

    @Test
    @DisplayName("동시에 조회해도 같은 신규 칭호는 한 번만 반환된다")
    fun concurrentCallsReturnEachNewRoleOnce() {
        val role = roleJpaRepository.save(RoleJpaEntity(name = "여름이었다", description = "좋아요 1회 누르기"))
        val member = createAndSaveMember(memberId = "newrolerace", nickname = "테스터", role = null)
        memberRoleRepository.save(MemberRole.create(member.id, RoleId(role.id!!)))

        val results = getNewRolesConcurrently(member.id)

        assertThat(results.flatten().map { it.id }).hasSize(1)
        assertThat(memberRoleRepository.findUncheckedByMemberIdWithRole(member.id)).isEmpty()
    }

    private fun getNewRolesConcurrently(memberPk: Long): List<List<MemberRoleResult>> {
        fillConnectionPool(THREAD_COUNT)
        val executor = Executors.newFixedThreadPool(THREAD_COUNT)
        val start = CountDownLatch(1)
        try {
            val futures = (1..THREAD_COUNT).map { executor.submit(getNewRolesTask(memberPk, start)) }
            start.countDown()
            return futures.map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun getNewRolesTask(memberPk: Long, start: CountDownLatch) =
        Callable<List<MemberRoleResult>> {
            start.await()
            getNewRolesUseCase.getNewRoles(memberPk, null)
        }
}
