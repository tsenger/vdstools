package vdstools.idb


enum class IdbMessageType(val value: Byte) {
    VISA(0x01.toByte()),
    EMERGENCY_TRAVEL_DOCUMENT(0x02.toByte()),
    PROOF_OF_TESTING(0x03.toByte()),
    PROOF_OF_VACCINATION(0x04.toByte()),
    PROOF_OF_RECOVERY(0x05.toByte()),
    DIGITALTRAVEL_AUTHORIZATION(0x06.toByte()),
    MRZ_TD1(0x07.toByte()),
    MRZ_TD3(0x08.toByte()),
    CAN(0x09.toByte()),
    EF_CARDACCESS(0x0A.toByte());

    companion object {
        private val map = HashMap<Byte, IdbMessageType>()

        init {
            for (messageType in entries) {
                map[messageType.value] = messageType
            }
        }

        fun valueOf(messageTag: Byte): IdbMessageType? {
            return map[messageTag]
        }
    }
}
