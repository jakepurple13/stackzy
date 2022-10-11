package com.theapache64.stackzy.data.repo

import com.github.theapache64.expekt.should
import com.malinskiy.adam.request.pkg.Package
import com.theapache64.stackzy.data.local.AndroidApp
import com.theapache64.stackzy.data.local.toResult
import com.theapache64.stackzy.data.remote.Result
import com.theapache64.stackzy.data.util.calladapter.flow.Resource
import com.theapache64.stackzy.test.MyDaggerMockRule
import com.theapache64.stackzy.test.runBlockingUnitTest
import com.theapache64.stackzy.util.loadLibs
import com.toxicbakery.logging.Arbor
import it.cosenonjaviste.daggermock.InjectFromComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.BeforeAll
import kotlin.io.path.createTempDirectory

class ResultsRepoTest {

    companion object {
        private const val TEST_PACKAGE_NAME = "com.theapache64.test.app"
        private const val TEST_VERSION_CODE = 11111
        private const val DUMMY_IMAGE_URL =
            "https://play-lh.googleusercontent.com/6iyA2zVz5PyyMjK5SIxdUhrb7oh9cYVXJ93q6DZkmx07Er1o90PXYeo6mzL4VC2Gj9s"
    }

    @get:Rule
    val daggerMockRule = MyDaggerMockRule()

    @InjectFromComponent
    private lateinit var resultsRepo: ResultsRepo

    @InjectFromComponent
    private lateinit var adbRepo: AdbRepo

    @InjectFromComponent
    private lateinit var apkAnalyzerRepo: ApkAnalyzerRepo

    @InjectFromComponent
    private lateinit var librariesRepo: LibrariesRepo

    @InjectFromComponent
    private lateinit var apkToolRepo: ApkToolRepo

    @Test
    @BeforeAll
    fun `Add result`() = runBlockingUnitTest {
        val result = Result(
            appName = "Test App",
            packageName = TEST_PACKAGE_NAME,
            libPackages = "okhttp3, retrofit2",
            versionCode = TEST_VERSION_CODE,
            versionName = "v1.2.3-alpha04",
            platform = "NativeKotlin",
            apkSizeInMb = 5.6f,
            gradleInfoJson = "{}",
            permissions = "android.permission.INTERNET",
            stackzyLibVersion = 1,
            logoImageUrl = DUMMY_IMAGE_URL,
            fullManifest = ""
        )

        resultsRepo.add(result).collect {
            when (it) {
                is Resource.Loading -> {
                    Arbor.d("Adding...")
                }
                is Resource.Success -> {
                    it.data.should.equal(result)
                }
                is Resource.Error -> {
                    assert(false) {
                        it.errorData
                    }
                }
            }
        }
    }

    @Test
    fun `Pull, Decompile and Add (complex)`() = runBlocking {

        val androidDevice = adbRepo.watchConnectedDevice().first().first()
        val apkFile = kotlin.io.path.createTempFile(suffix = ".apk").toFile()
        val packageName = "com.netflix.mediaclient"
        adbRepo.pullFile(
            androidDevice,
            adbRepo.getApkPath(androidDevice, AndroidApp(Package(packageName), false))!!,
            apkFile
        ).collect { pullPercentage ->
            if (pullPercentage == 100) {
                // pulled

                val decompiledDir = createTempDirectory().toFile()
                apkToolRepo.decompile(apkFile, decompiledDir)
                librariesRepo.loadLibs { allLibs ->
                    val report = apkAnalyzerRepo.analyze(packageName, apkFile, decompiledDir, allLibs)
                    val result = report.toResult(resultsRepo, null, DUMMY_IMAGE_URL)
                    resultsRepo.add(result).collect {
                        if (it is Resource.Error) {
                            assert(false) { it.errorData }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `Find result with valid package name`() = runBlockingUnitTest {
        resultsRepo
            .findResult(
                packageName = TEST_PACKAGE_NAME,
                versionCode = TEST_VERSION_CODE,
                libVersionCode = 1
            )
            .collect {
                when (it) {
                    is Resource.Loading -> {
                        Arbor.d("Finding...")
                    }

                    is Resource.Success -> {
                        it.data.packageName.should.equal(TEST_PACKAGE_NAME)
                    }

                    is Resource.Error -> {
                        assert(false) {
                            it.errorData
                        }
                    }
                }
            }
    }

    @Test
    fun `Find result with invalid package name`() = runBlockingUnitTest {
        resultsRepo
            .findResult(
                packageName = "com.theapache64.this.app.does.not.exist",
                versionCode = 11111,
                libVersionCode = 1
            )
            .collect {
                when (it) {
                    is Resource.Loading -> {
                        Arbor.d("Finding...")
                    }

                    is Resource.Success -> {
                        assert(false) {
                            "This app shouldn't exist but returned $it"
                        }
                    }

                    is Resource.Error -> {
                        Arbor.d(it.errorData)
                        assert(true)
                    }
                }
            }
    }

    @Test
    fun `Get all lib packages`() = runBlockingUnitTest {
        resultsRepo.getAllLibPackages().collect {
            when (it) {
                is Resource.Loading -> {

                }

                is Resource.Success -> {
                    it.data.should.not.empty
                }

                is Resource.Error -> {
                    assert(false)
                }
            }
        }
    }


    @Test
    fun `Get apps that are using a specific library`() = runBlockingUnitTest {
        resultsRepo.getResults("com.squareup.moshi").collect {
            when (it) {
                is Resource.Loading -> {
                    // Do nothing
                }
                is Resource.Success -> {
                    it.data.size.should.above(0)
                }
                is Resource.Error -> {
                    assert(false)
                }
            }
        }
    }
}