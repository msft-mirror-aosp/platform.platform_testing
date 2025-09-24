/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.tools.traces.parsers.perfetto

import android.annotation.SuppressLint
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import android.util.Log
import androidx.benchmark.Shell
import androidx.benchmark.ShellFile
import androidx.benchmark.UserFile
import androidx.benchmark.UserInfo
import androidx.benchmark.VirtualFile
import androidx.benchmark.traceprocessor.ExperimentalTraceProcessorApi
import androidx.benchmark.traceprocessor.ServerLifecycleManager
import androidx.test.platform.app.InstrumentationRegistry
import java.io.Closeable
import java.nio.charset.Charset
import kotlin.random.Random
import kotlin.random.nextUInt

@OptIn(ExperimentalTraceProcessorApi::class)
class ShellServerLifecycleManager : ServerLifecycleManager {
    companion object {
        private const val SERVER_PROCESS_NAME = "trace_processor_shell"
        private val TAG = ShellServerLifecycleManager::class.simpleName

        internal val shellPath: String by lazy { createExecutable("trace_processor_shell") }
        internal const val PORT = 9001

        fun createExecutable(tool: String): String {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val inputStream = instrumentation.context.assets.open(tool)
            return Shell.createRunnableExecutable(tool, inputStream)
        }
    }

    private var shellScript: ShellScript? = null
    private var startedShellScript: StartedShellScript? = null
    private var processId: Int? = null

    /**
     * Returns a cached instance of the shell script to run the perfetto trace shell processor as
     * http server. Note that the generated script doesn't specify the port and this must be passed
     * as parameter when running the script.
     */
    private fun getOrCreateShellScript(): ShellScript =
        shellScript
            ?: synchronized(this) {
                var instance = shellScript
                if (instance != null) {
                    return@synchronized instance
                }
                val script = "echo pid:$$ ; exec $shellPath -D --http-port \"$PORT\""
                instance = createShellScript(script)
                shellScript = instance
                instance
            }

    fun createShellScript(script: String, stdin: String? = null): ShellScript {
        // dirUsableByAppAndShell is writable, but we can't execute there (as of Q),
        // so we copy to /data/local/tmp
        val scriptName = "temporaryScript_${Random.nextUInt()}.sh"

        val (scriptContentFile, stdInFile) =
            if (UserInfo.isAdditionalUser) {
                Pair(
                    ShellFile.inTempDir(scriptName).apply { writeText(script) },
                    stdin?.let {
                        ShellFile.inTempDir("${scriptName}_stdin").apply { writeText(it) }
                    },
                )
            } else {
                Pair(
                    UserFile.inOutputsDir(scriptName).apply { writeText(script) },
                    stdin?.let { input ->
                        UserFile.inOutputsDir("${scriptName}_stdin").apply { writeText(input) }
                    },
                )
            }

        // we use a path on /data/local/tmp (as opposed to externalDir) because some shell
        // commands fail to redirect stderr to externalDir (notably, `am start`).
        // This also means we need to `cat` the file to read it, and `rm` to remove it.
        val stderrPath = "/data/local/tmp/${scriptName}_stderr"

        return try {
            ShellScript(
                stdinFile = stdInFile,
                scriptContentFile = scriptContentFile,
                stderrPath = stderrPath,
            )
        } catch (e: Exception) {
            throw Exception("Can't create shell script", e)
        }
    }

    @SuppressLint("BanThreadSleep")
    override fun start(): Int {
        getOrCreateShellScript().start().apply {
            processId =
                stdOutLineSequence().first { it.startsWith("pid:") }.split("pid:")[1].toInt()
            startedShellScript = this
            println("Started, processId $processId")
        }
        return PORT
    }

    override fun timeoutMessage(): String {

        // In the event that the instrumentation app cannot connect to the
        // trace_processor_shell server, trying to read the full stderr may make the
        // process hang. Here we check if the process is still running to determine if
        // that's the case and throw the correct exception.
        val processRunning =
            processId?.let { Shell.isProcessAlive(it, SERVER_PROCESS_NAME) } ?: false

        return if (processRunning) {
            "The instrumentation app cannot connect to the trace_processor_shell server."
        } else {
            "Perfetto trace_processor_shell did not start correctly." +
                " Stderr = ${startedShellScript?.getOutputAndClose()?.stderr}"
        }
    }

    override fun stop() {
        if (processId == null) {
            Log.w(TAG, "Tried to stop trace shell processor http server without starting it.")
            return
        }
        println("stop, processId=$processId")
        Shell.executeScriptSilent("kill -TERM $processId")
        Log.i(TAG, "Perfetto trace processor shell server stopped (pid=$processId).")
        processId = null
    }
}

class ShellScript
internal constructor(
    private val stdinFile: VirtualFile?,
    private val scriptContentFile: VirtualFile,
    private val stderrPath: String,
) {
    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    private var cleanedUp: Boolean = false

    /**
     * Starts the shell script previously created.
     *
     * @return a [StartedShellScript] that contains streams to read output streams.
     */
    fun start(): StartedShellScript {
        val stdoutDescriptor =
            executeCommandNonBlockingUnsafe(
                scriptWrapperCommand(
                    scriptContentPath = scriptContentFile.absolutePath,
                    stderrPath = stderrPath,
                    stdinPath = stdinFile?.absolutePath,
                )
            )
        val stderrDescriptorFn = stderrPath.run { { executeCommandUnsafe("cat $stderrPath") } }

        return StartedShellScript(
            stdoutDescriptor = stdoutDescriptor,
            stderrDescriptorFn = stderrDescriptorFn,
            cleanUpBlock = ::cleanUp,
        )
    }

    /** Manually clean up the shell script temporary files from the temp folder. */
    fun cleanUp() {
        if (cleanedUp) {
            return
        }

        // NOTE: while we could theoretically remove some of these files from the script, this
        // isn't
        // safe when the script is called multiple times, expecting the intermediates to remain.
        // We need a rm to clean up the stderr file anyway (b/c it's not ready until stdout is
        // complete), so we just delete everything here, all at once.
        executeCommandNonBlockingUnsafe(
            "rm -f " +
                listOfNotNull(stderrPath, scriptContentFile.absolutePath, stdinFile?.absolutePath)
                    .joinToString(" ")
        )
        cleanedUp = true
    }

    fun executeCommandUnsafe(cmd: String): String =
        executeCommandNonBlockingUnsafe(cmd).fullyReadInputStream()

    fun executeCommandNonBlockingUnsafe(cmd: String): ParcelFileDescriptor =
        uiAutomation.executeShellCommand("su root $cmd")

    companion object {
        /** Usage args: ```path/to/shellWrapper.sh <scriptFile> <stderrFile> [inputFile]``` */
        private val scriptWrapperPath =
            Shell.createRunnableExecutable(
                // use separate paths to prevent access errors after `adb unroot`
                "shellWrapper.sh",
                """
                    ### shell script which passes in stdin as needed, and captures stderr in a file
                    # $1 == script content (not executable)
                    # $2 == stderr
                    # $3 == stdin (optional)
                    if [[ $3 -eq "0" ]]; then
                        /system/bin/sh $1 2> $2
                    else
                        cat $3 | /system/bin/sh $1 2> $2
                    fi
                """
                    .trimIndent()
                    .byteInputStream(),
            )

        fun scriptWrapperCommand(
            scriptContentPath: String,
            stderrPath: String,
            stdinPath: String?,
        ): String =
            listOfNotNull(scriptWrapperPath, scriptContentPath, stderrPath, stdinPath)
                .joinToString(" ")
    }
}

class StartedShellScript
internal constructor(
    private val stdoutDescriptor: ParcelFileDescriptor,
    private val stderrDescriptorFn: (() -> (String)),
    private val cleanUpBlock: () -> Unit,
) : Closeable {

    /** Returns a [Sequence] of [String] containing the lines written by the process to stdOut. */
    fun stdOutLineSequence(): Sequence<String> =
        AutoCloseInputStream(stdoutDescriptor).bufferedReader().lineSequence()

    /** Cleans up this shell script. */
    override fun close() = cleanUpBlock()

    /** Reads the full process output and cleans up the generated script */
    fun getOutputAndClose(): Shell.Output {
        val output =
            Shell.Output(
                stdout = stdoutDescriptor.fullyReadInputStream(),
                stderr = stderrDescriptorFn.invoke(),
            )
        close()
        return output
    }
}

internal fun ParcelFileDescriptor.fullyReadInputStream(): String {
    AutoCloseInputStream(this).use { inputStream ->
        return inputStream.readBytes().toString(Charset.defaultCharset())
    }
}
