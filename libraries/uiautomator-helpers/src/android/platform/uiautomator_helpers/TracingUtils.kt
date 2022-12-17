package android.platform.uiautomator_helpers

import android.os.Trace

/** Tracing utils until androidx tracing library is updated in the tree. */
internal object TracingUtils {
    inline fun <T> trace(sectionName: String, block: () -> T): T {
        Trace.beginSection(sectionName)
        try {
            return block()
        } finally {
            Trace.endSection()
        }
    }
}
