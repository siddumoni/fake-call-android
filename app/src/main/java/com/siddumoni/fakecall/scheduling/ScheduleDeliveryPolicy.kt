package com.siddumoni.fakecall.scheduling

enum class ScheduleAlarmResult {
    SCHEDULED,
    EXACT_ALARM_PERMISSION_REQUIRED,
}

/** Pure policy gate shared by the UI and AlarmManager adapter. */
object ScheduleDeliveryPolicy {
    fun resultFor(
        deliveryMode: ScheduledCallDeliveryMode,
        exactAlarmAccessGranted: Boolean,
    ): ScheduleAlarmResult =
        if (deliveryMode == ScheduledCallDeliveryMode.EXACT && !exactAlarmAccessGranted) {
            ScheduleAlarmResult.EXACT_ALARM_PERMISSION_REQUIRED
        } else {
            ScheduleAlarmResult.SCHEDULED
        }
}
