package com.example.mykku.config

import com.example.mykku.image.ImageUploadService
import com.example.mykku.image.NoOpImageUploadService
import com.example.mykku.image.S3ImageUploadService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.assertj.AssertableApplicationContext
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

@DisplayName("S3 배선 테스트")
class S3ConfigTest {

    companion object {
        private val ABSENT_AWS_SHARED_FILES = arrayOf(
            "aws.configFile=build/s3-config-test/absent-config",
            "aws.sharedCredentialsFile=build/s3-config-test/absent-credentials"
        )
        private val DUMMY_S3_PROPERTIES = arrayOf(
            "aws.s3.access-key=dummy-access-key",
            "aws.s3.secret-key=dummy-secret-key",
            "aws.s3.region=us-west-2",
            "aws.s3.bucket-name=dummy-bucket",
            "aws.s3.cloudfront-domain=dummy.cloudfront.test"
        )
    }

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(
            S3Config::class.java,
            S3ImageUploadService::class.java,
            NoOpImageUploadService::class.java
        )
        .withSystemProperties(*ABSENT_AWS_SHARED_FILES)
        .withPropertyValues(*DUMMY_S3_PROPERTIES)

    @Test
    @DisplayName("aws.s3.enabled=true면 S3Config·S3Client·S3ImageUploadService가 올라오고 업로드 서비스는 S3다")
    fun registersS3BeansWhenEnabled() {
        contextRunner.withPropertyValues("aws.s3.enabled=true").run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context).hasSingleBean(S3Config::class.java)
            assertThat(context).hasSingleBean(S3Client::class.java)
            assertThat(context).hasSingleBean(S3ImageUploadService::class.java)
            assertThat(context).doesNotHaveBean(NoOpImageUploadService::class.java)
            assertThat(context).getBean(ImageUploadService::class.java).isInstanceOf(S3ImageUploadService::class.java)
        }
    }

    @Test
    @DisplayName("aws.s3.enabled=true면 더미 자격·region·bucket·CloudFront 값이 S3Properties에 바인딩되고 region이 S3Client에 쓰인다")
    fun bindsDummyS3Properties() {
        contextRunner.withPropertyValues("aws.s3.enabled=true").run { context ->
            val properties = context.getBean(S3Properties::class.java)
            assertThat(properties.accessKey).isEqualTo("dummy-access-key")
            assertThat(properties.secretKey).isEqualTo("dummy-secret-key")
            assertThat(properties.region).isEqualTo("us-west-2")
            assertThat(properties.bucketName).isEqualTo("dummy-bucket")
            assertThat(properties.cloudfrontDomain).isEqualTo("dummy.cloudfront.test")
            assertThat(context.getBean(S3Client::class.java).serviceClientConfiguration().region())
                .isEqualTo(Region.US_WEST_2)
        }
    }

    @Test
    @DisplayName("aws.s3.enabled=false면 S3Config·S3Client·S3ImageUploadService가 없고 업로드 서비스는 NoOp이다")
    fun skipsS3BeansWhenDisabled() {
        contextRunner.withPropertyValues("aws.s3.enabled=false").run { assertS3BeansSkipped(it) }
    }

    @Test
    @DisplayName("aws.s3.enabled가 없으면 S3Config·S3Client·S3ImageUploadService가 없고 업로드 서비스는 NoOp이다")
    fun skipsS3BeansWhenMissing() {
        contextRunner.run { assertS3BeansSkipped(it) }
    }

    private fun assertS3BeansSkipped(context: AssertableApplicationContext) {
        assertThat(context).hasNotFailed()
        assertThat(context).doesNotHaveBean(S3Config::class.java)
        assertThat(context).doesNotHaveBean(S3Client::class.java)
        assertThat(context).doesNotHaveBean(S3ImageUploadService::class.java)
        assertThat(context).getBean(ImageUploadService::class.java).isInstanceOf(NoOpImageUploadService::class.java)
    }
}
