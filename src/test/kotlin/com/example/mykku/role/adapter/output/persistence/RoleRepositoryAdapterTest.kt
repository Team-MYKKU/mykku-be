package com.example.mykku.role.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.role.application.port.output.RoleRepository
import com.example.mykku.role.domain.entity.Role
import com.example.mykku.role.domain.vo.RoleId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("RoleRepository 통합 테스트")
class RoleRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var roleRepository: RoleRepository

    private fun createRole(
        name: String = "ADMIN",
        description: String? = "관리자 역할"
    ): Role {
        return Role.create(name = name, description = description)
    }

    @Nested
    @DisplayName("save 메서드")
    inner class Save {

        @Test
        @DisplayName("새로운 역할을 저장하면 ID가 생성된다")
        fun saveNewRole() {
            val role = createRole()

            val savedRole = roleRepository.save(role)

            assertThat(savedRole.id.value).isGreaterThan(0)
            assertThat(savedRole.name).isEqualTo(role.name)
            assertThat(savedRole.description).isEqualTo(role.description)
        }

        @Test
        @DisplayName("기존 역할을 수정하여 저장하면 변경사항이 반영된다")
        fun updateExistingRole() {
            val savedRole = roleRepository.save(createRole())
            val updatedRole = savedRole.updateInfo(
                name = "SUPER_ADMIN",
                description = "슈퍼 관리자"
            )

            val result = roleRepository.save(updatedRole)
            flushAndClear()

            assertThat(result.id).isEqualTo(savedRole.id)
            assertThat(result.name).isEqualTo("SUPER_ADMIN")
            assertThat(result.description).isEqualTo("슈퍼 관리자")
            val foundRole = roleRepository.findById(savedRole.id)!!
            assertThat(foundRole.name).isEqualTo("SUPER_ADMIN")
            assertThat(foundRole.description).isEqualTo("슈퍼 관리자")
        }
    }

    @Nested
    @DisplayName("findById 메서드")
    inner class FindById {

        @Test
        @DisplayName("존재하는 ID로 조회하면 역할을 반환한다")
        fun findByExistingId() {
            val savedRole = roleRepository.save(createRole())

            val foundRole = roleRepository.findById(savedRole.id)

            assertThat(foundRole).isNotNull
            assertThat(foundRole!!.id).isEqualTo(savedRole.id)
            assertThat(foundRole.name).isEqualTo(savedRole.name)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 null을 반환한다")
        fun findByNonExistingId() {
            val foundRole = roleRepository.findById(RoleId(999999L))

            assertThat(foundRole).isNull()
        }
    }

    @Nested
    @DisplayName("findByName 메서드")
    inner class FindByName {

        @Test
        @DisplayName("존재하는 이름으로 조회하면 역할을 반환한다")
        fun findByExistingName() {
            val savedRole = roleRepository.save(createRole(name = "MANAGER"))

            val foundRole = roleRepository.findByName("MANAGER")

            assertThat(foundRole).isNotNull
            assertThat(foundRole!!.id).isEqualTo(savedRole.id)
            assertThat(foundRole.name).isEqualTo("MANAGER")
        }

        @Test
        @DisplayName("존재하지 않는 이름으로 조회하면 null을 반환한다")
        fun findByNonExistingName() {
            val foundRole = roleRepository.findByName("NON_EXISTING_ROLE")

            assertThat(foundRole).isNull()
        }
    }

    @Nested
    @DisplayName("findAll 메서드")
    inner class FindAll {

        @Test
        @DisplayName("저장된 모든 역할을 반환한다")
        fun findAllRoles() {
            val savedAdmin = roleRepository.save(createRole(name = "ADMIN"))
            val savedUser = roleRepository.save(createRole(name = "USER"))
            val savedModerator = roleRepository.save(createRole(name = "MODERATOR"))

            val roles = roleRepository.findAll()

            assertThat(roles.map { it.id }).containsExactlyInAnyOrder(savedAdmin.id, savedUser.id, savedModerator.id)
            assertThat(roles.map { it.name }).containsExactlyInAnyOrder("ADMIN", "USER", "MODERATOR")
        }

        @Test
        @DisplayName("저장된 역할이 없으면 빈 리스트를 반환한다")
        fun findAllWhenEmpty() {
            val roles = roleRepository.findAll()

            assertThat(roles).isEmpty()
        }
    }

    @Nested
    @DisplayName("existsByName 메서드")
    inner class ExistsByName {

        @Test
        @DisplayName("존재하는 이름이면 true를 반환한다")
        fun existsByExistingName() {
            roleRepository.save(createRole(name = "ADMIN"))

            val exists = roleRepository.existsByName("ADMIN")

            assertThat(exists).isTrue()
        }

        @Test
        @DisplayName("존재하지 않는 이름이면 false를 반환한다")
        fun existsByNonExistingName() {
            val exists = roleRepository.existsByName("NON_EXISTING_ROLE")

            assertThat(exists).isFalse()
        }
    }

    @Nested
    @DisplayName("delete 메서드")
    inner class Delete {

        @Test
        @DisplayName("역할을 삭제하면 조회되지 않는다")
        fun deleteRole() {
            val savedRole = roleRepository.save(createRole())

            roleRepository.delete(savedRole)

            val foundRole = roleRepository.findById(savedRole.id)
            assertThat(foundRole).isNull()
            flushAndClear()
            assertThat(roleRepository.findById(savedRole.id)).isNull()
        }
    }
}
