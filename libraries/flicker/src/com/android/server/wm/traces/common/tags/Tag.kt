package com.android.server.wm.traces.common.tags

/**
 * Tag Class relating to a particular transition event in a WindowManager
 * or SurfaceFlinger trace state.
 * @param id The id to match the end and start tags
 * @param transition Transition the tag represents the transition
 * @param isStartTag Tag represents the start or end moment in transition
 * @param layerId The Layer the tag is associated with (or -1 if no taskId associated with it)
 * @param windowToken The Window the tag is associated
 * with (or empty string if no taskId associated with it)
 * @param taskId The Task the tag is associated with (or -1 if no taskId associated with it)
 */
data class Tag(
    val id: Int,
    val transition: Transition,
    val isStartTag: Boolean,
    val layerId: Int = -1,
    val windowToken: String = "",
    val taskId: Int = -1
) {
    override fun toString(): String {
        if (isStartTag) {
            return "Start Of $transition"
        }
        return "End Of $transition"
    }
}