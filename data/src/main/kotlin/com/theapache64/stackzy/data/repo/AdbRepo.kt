package com.theapache64.stackzy.data.repo

import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.interactor.StartAdbInteractor
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.device.AsyncDeviceMonitorRequest
import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.device.DeviceState
import com.malinskiy.adam.request.device.ListDevicesRequest
import com.malinskiy.adam.request.framebuffer.RawImageScreenCaptureAdapter
import com.malinskiy.adam.request.framebuffer.ScreenCaptureRequest
import com.malinskiy.adam.request.pkg.InstallRemotePackageRequest
import com.malinskiy.adam.request.pkg.Package
import com.malinskiy.adam.request.prop.GetPropRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.sync.v1.PullFileRequest
import com.malinskiy.adam.request.sync.v2.PushFileRequest
import com.theapache64.stackzy.data.local.AndroidApp
import com.theapache64.stackzy.data.local.AndroidDevice
import com.theapache64.stackzy.util.OSType
import com.theapache64.stackzy.util.OsCheck
import com.toxicbakery.logging.Arbor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.zip.ZipInputStream
import javax.imageio.ImageIO
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.floor
import kotlin.math.roundToInt

@Singleton
class AdbRepo @Inject constructor(

) {

    init {
        Arbor.d("Creating new adbRepo instance")
    }

    companion object {
        const val PATH_PACKAGE_PREFIX = "package:"
        private const val DETAIL_UNKNOWN = "Unknown"
        private const val ADB_ZIP_ENTRY_NAME = "platform-tools/adb"
        private const val ADB_ZIP_ENTRY_NAME_WINDOWS = "platform-tools/adb.exe"
        private const val ADB_ZIP_ENTRY_NAME_WINDOWS_API_DLL = "platform-tools/AdbWinApi.dll"
        private const val ADB_ZIP_ENTRY_NAME_WINDOWS_API_USB_DLL = "platform-tools/AdbWinUsbApi.dll"

        private val ADB_ROOT_DIR = "${System.getProperty("user.home")}${File.separator}.stackzy"

        // platform-tools url map
        private val platformToolsUrlMap by lazy {
            mapOf(
                OSType.Linux to "https://dl.google.com/android/repository/platform-tools-latest-linux.zip",
                OSType.Windows to "https://dl.google.com/android/repository/platform-tools-latest-windows.zip",
                OSType.MacOS to "https://dl.google.com/android/repository/platform-tools-latest-darwin.zip",
            )
        }
    }

    //Screen Shot 2022-09-06 at 14.49.05
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd 'at' HH.mm.ss")

    private var deviceEventsChannel: ReceiveChannel<List<Device>>? = null

    private val startAdbInteractor by lazy {
        StartAdbInteractor()
    }

    private val adb by lazy {
        AndroidDebugBridgeClientFactory()
            .build()
    }

    fun watchConnectedDevice(): Flow<List<AndroidDevice>> {
        return flow {
            val isStarted = isAdbStarted()

            if (isStarted) {
                val deviceEventsChannel = adb.execute(
                    request = AsyncDeviceMonitorRequest(),
                    scope = GlobalScope
                ).also {
                    this@AdbRepo.deviceEventsChannel = it
                }

                adb.execute(request = ListDevicesRequest())

                for (currentDeviceList in deviceEventsChannel) {
                    val deviceList = currentDeviceList
                        .filter { it.state == DeviceState.DEVICE }
                        .map { device ->
                            val props = adb.execute(
                                request = GetPropRequest(),
                                serial = device.serial
                            )

                            val deviceProductName = props["ro.product.name"]?.singleLine() ?: DETAIL_UNKNOWN
                            val deviceProductModel = props["ro.product.model"]?.singleLine() ?: DETAIL_UNKNOWN

                            AndroidDevice(
                                deviceProductName,
                                deviceProductModel,
                                device
                            )
                        }

                    // Finally emitting result
                    emit(deviceList)
                }
            } else {
                throw IOException("Failed to start adb")
            }
        }
    }

    suspend fun isAdbStarted() = if (adbFile.exists()) {
        startAdbInteractor.execute(adbFile)
    } else {
        startAdbInteractor.execute()
    }

    fun cancelWatchConnectedDevice() {
        deviceEventsChannel?.cancel()
    }

    /**
     * To get installed app from given device
     */
    suspend fun getInstalledApps(device: Device): List<AndroidApp> = arrayOf("-3", "-s").flatMap { flag ->
        val installedPackages = getInstalledPackagesByFlag(device, flag)
        val isSystemApp = flag == "-s"

        installedPackages
            .split("\n") // parse line by line
            .filter { packageName -> packageName.isNotBlank() }
            .map { it.replace("package:", "").trim() } // filter package name
            .map { packageName -> AndroidApp(Package(packageName), isSystemApp = isSystemApp) }
    }

    private suspend fun getInstalledPackagesByFlag(device: Device, flag: String): String = adb.execute(
        request = ShellCommandRequest("pm list packages $flag"),
        serial = device.serial
    ).output

    private fun String.singleLine(): String = replace("\n", "")

    /**
     * To get APK path for given app from given device
     */
    suspend fun getApkPath(
        androidDevice: AndroidDevice,
        androidApp: AndroidApp
    ): String? {
        val cmd = "pm path ${androidApp.appPackage.name}"
        val response = adb.execute(
            request = ShellCommandRequest(
                cmd
            ),
            serial = androidDevice.device.serial
        )

        return response.output
            .split("\n")[0] // first file only
            .takeIf { it.contains(PATH_PACKAGE_PREFIX) }
            ?.replace(PATH_PACKAGE_PREFIX, "")?.trim()
    }

    suspend fun pullFile(
        androidDevice: AndroidDevice,
        apkRemotePath: String,
        destinationFile: File
    ): Flow<Int> {
        return flow {
            val channel = adb.execute(
                serial = androidDevice.device.serial,
                request = PullFileRequest(
                    apkRemotePath,
                    destinationFile
                ),
                scope = GlobalScope,
            )

            var percentage: Int? = null
            for (percentageDouble in channel) {
                percentage = floor(percentageDouble * 100).roundToInt()
                emit(percentage)
            }

            if (percentage == null || percentage < 100) {
                throw IOException("TSH : Percentage should be 100 but found $percentage")
            }
        }
    }

    suspend fun launchMarket(
        androidDevice: AndroidDevice,
        keyword: String
    ) {
        val url = "https://play.google.com/store/search?q=$keyword&c=apps"
        adb.execute(
            request = ShellCommandRequest("am start -a android.intent.action.VIEW -d \"$url\""),
            serial = androidDevice.device.serial
        )
    }

    /**
     * adb binary file name inside platform-tool.zip
     */
    private val adbZipEntryName by lazy {
        if (OsCheck.operatingSystemType == OSType.Windows) {
            ADB_ZIP_ENTRY_NAME_WINDOWS
        } else {
            ADB_ZIP_ENTRY_NAME
        }
    }

    private val adbFile by lazy {
        // only the filename (platform-tools/'adb/adb.exe')
        val fileName = adbZipEntryName.split("/").last()
        File("${ADB_ROOT_DIR}${File.separator}$fileName").also {
            it.parentFile.let { parentDir ->
                if (parentDir.exists().not()) {
                    parentDir.mkdirs()
                }
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext") // suppressing due to invalid IDE warning (bug)
    suspend fun downloadAdb() = flow {

        // Getting platform tools download url
        val pToolsUrl = platformToolsUrlMap[OsCheck.operatingSystemType]
        require(pToolsUrl != null) { "${OsCheck.operatingSystemType} doesn't have adb binary defined." }

        // Download file
        val pToolZipFile = kotlin.io.path.createTempFile().toFile()

        val urlConnection = URL(pToolsUrl).openConnection()
        val totalBytes = urlConnection.contentLengthLong

        urlConnection.getInputStream().use { input ->
            FileOutputStream(pToolZipFile).use { output ->
                val buffer = ByteArray(1024)
                var read: Int
                var counter = 0f
                emit(0) // start progress
                while (input.read(buffer).also { read = it } != -1) {
                    // Write
                    output.write(buffer, 0, read)

                    // Update progress
                    counter += read
                    val percentage = (counter / totalBytes) * 100
                    emit(percentage.toInt())
                }

                // Finish progress
                emit(100)
            }
        }

        // Unzip and create adbFile
        unzipAndSetupAdbFiles(pToolZipFile)

        // Since we've extracted adb binary from the download zip, we can delete it.
        pToolZipFile.delete()
    }

    /**
     * To unzip the given platform tools directory and write adb to adbFile (plus, dll files also - windows)
     */
    private fun unzipAndSetupAdbFiles(pToolZipFile: File) {

        var isAdbExtracted = false
        val isWindows = OsCheck.operatingSystemType == OSType.Windows
        ZipInputStream(pToolZipFile.inputStream()).use { zis ->
            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                if (zipEntry.name == adbZipEntryName) {
                    // Found adb
                    adbFile.delete()
                    FileOutputStream(adbFile).use {
                        zis.copyTo(it)
                    }
                    isAdbExtracted = true
                }

                if (isWindows) {
                    // If windows, we need dll files also.
                    if (zipEntry.name == ADB_ZIP_ENTRY_NAME_WINDOWS_API_DLL || zipEntry.name == ADB_ZIP_ENTRY_NAME_WINDOWS_API_USB_DLL) {
                        val dllFileName = zipEntry.name.split("/").last()
                        val dllFile = File("$ADB_ROOT_DIR${File.separator}$dllFileName")

                        with(dllFile) {
                            FileOutputStream(this).use { zis.copyTo(it) }

                            setExecutable(true, false)
                            setReadable(true, false)
                            setWritable(true, false)
                        }
                    }
                }

                zipEntry = zis.nextEntry
            }
        }

        if (isAdbExtracted) {
            adbFile.setExecutable(true)
            Arbor.d("Adb created '${adbFile.absolutePath}'")
        } else {
            throw IOException("Failed to find $adbZipEntryName from ${pToolZipFile.absolutePath}")
        }
    }

    suspend fun installApk(
        androidDevice: AndroidDevice,
        file: File,
        emit: (InstallResource<Double>) -> Unit
    ) {
        if (file.exists()) {
            emit(InstallResource.Loading(0.0))
            val channel = adb.execute(
                PushFileRequest(
                    file,
                    "/data/local/tmp/${file.name}",
                    supportedFeatures = listOf(Feature.SENDRECV_V2)
                ),
                GlobalScope,
                serial = androidDevice.device.serial
            )
            try {
                while (!channel.isClosedForReceive) {
                    val progress: Double = channel.receive()
                    emit(InstallResource.Loading(progress))
                }

                val output = adb.execute(
                    InstallRemotePackageRequest(
                        "/data/local/tmp/${file.name}",
                        true
                    ),
                    serial = androidDevice.device.serial
                )

                if (!output.output.startsWith("Success")) emit(InstallResource.Error("Not Installed"))
                else emit(InstallResource.Success())
            } catch (e: Exception) {
                e.printStackTrace()
                emit(InstallResource.Error(e.message ?: "Not Installed"))
            }
        } else {
            emit(InstallResource.Error("Invalid File"))
        }
    }

    suspend fun screenshot(
        androidDevice: AndroidDevice,
    ) {
        val adapter = RawImageScreenCaptureAdapter()
        val image = adb.execute(
            request = ScreenCaptureRequest(adapter),
            serial = androidDevice.device.serial
        ).toBufferedImage()

        val desktop = System.getProperty("user.home") + "/Desktop"
        val time = System.currentTimeMillis()
        if (!ImageIO.write(image, "png", File("$desktop/Screen Shot ${dateFormat.format(time)}.png"))) {
            throw IOException("Failed to find png writer")
        }
    }
}

sealed class InstallResource<T> {
    class Loading<T>(val progress: T) : InstallResource<T>()

    class Success<T> : InstallResource<T>()

    data class Error<T>(val errorData: String) : InstallResource<T>()
}


