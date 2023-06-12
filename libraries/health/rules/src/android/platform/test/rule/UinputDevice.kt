package android.platform.test.rule

/**
 * Interface representing a Uinput device to be used in [InputDeviceRule].
 */
interface UinputDevice {
    val vendorId: Int
    val productId: Int
    val name: String
    fun getRegisterCommand(): String
}

/**
 * Class to represent a keyboard as a [UinputDevice]. This can then be used to register a keyboard
 * using [InputDeviceRule].
 *
 * @param supportedKeys should be keycodes obtained from the linux event code set.
 * See https://source.corp.google.com/kernel-upstream/include/uapi/linux/input-event-codes.h
 */
class UinputKeyboard @JvmOverloads constructor(
        override val productId: Int = ARBITRARY_PRODUCT_ID,
        override val vendorId: Int = ARBITRARY_VENDOR_ID,
        override val name: String = "Test Keyboard",
        private val supportedKeys: Array<Int> = SUPPORTED_KEYS_QWEABC,
): UinputDevice {
    override fun getRegisterCommand(): String {
        return """{
            "id": 1,
            "type": "uinput",
            "command": "register",
            "name": "$name",
            "vid": $vendorId,
            "pid": $productId,
            "bus": "usb",
            "configuration":[
                {"type": 100, "data": [1]},
                {"type": 101, "data": ${supportedKeys.contentToString()}}
            ]
        }""".trimIndent().replace("\n", "")
    }

    companion object {
        val SUPPORTED_KEYS_QWEABC = arrayOf(16, 17, 18, 30, 48, 46)

        const val ARBITRARY_PRODUCT_ID = 0xabcd
        const val ARBITRARY_VENDOR_ID = 0x18d1
    }
}