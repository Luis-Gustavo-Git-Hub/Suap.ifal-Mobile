package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import org.json.JSONArray
import java.util.Calendar

class ClassReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            scheduleNextClassAlarm(context)
            return
        }

        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("class_notification_enabled", false)

        val subjectName = intent.getStringExtra("subject_name")
        val startTime = intent.getStringExtra("start_time")
        val endTime = intent.getStringExtra("end_time")
        val teacher = intent.getStringExtra("teacher") ?: ""

        if (isEnabled && !subjectName.isNullOrEmpty() && !startTime.isNullOrEmpty() && !endTime.isNullOrEmpty()) {
            showClassNotification(
                context = context,
                subjectName = subjectName,
                startTime = startTime,
                endTime = endTime,
                teacher = teacher
            )
        }

        // Schedule next class alarm
        scheduleNextClassAlarm(context)
    }

    companion object {
        private const val CHANNEL_ID = "class_reminder_channel"
        private const val NOTIFICATION_ID = 2002

        fun showClassNotification(
            context: Context,
            subjectName: String,
            startTime: String,
            endTime: String,
            teacher: String = ""
        ) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Lembrete de Aulas",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificações do início das aulas"
                    enableLights(true)
                    lightColor = Color.BLUE
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val iconRes = try {
                R.drawable.ic_launcher_foreground
            } catch (e: Exception) {
                android.R.drawable.ic_popup_reminder
            }

            val bigTextContent = buildString {
                append("📚 Disciplina: ").append(subjectName).append("\n")
                append("⏰ Início: ").append(startTime).append(" | Final: ").append(endTime)
                if (teacher.isNotEmpty()) {
                    append("\n👨‍🏫 Professor: ").append(teacher)
                }
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(iconRes)
                .setContentTitle("📚 Aula: $subjectName")
                .setContentText("Início: $startTime | Final: $endTime")
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigTextContent))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setColor(Color.parseColor("#1976D2"))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            try {
                notificationManager.notify(NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun showTestNotification(context: Context) {
            showClassNotification(
                context = context,
                subjectName = "TECNOLOGIA DE GRÃOS, RAÍZES E TUBÉRCULOS",
                startTime = "07:30",
                endTime = "08:20",
                teacher = "Agda Barros"
            )
        }

        fun scheduleNextClassAlarm(context: Context) {
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("class_notification_enabled", false)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, ClassReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                2001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (!isEnabled) {
                alarmManager.cancel(pendingIntent)
                return
            }

            val classList = loadScheduleItems(context)
            if (classList.isEmpty()) return

            val now = Calendar.getInstance()
            var targetCal: Calendar? = null
            var selectedClass: ClassInfo? = null

            // Check next 7 days starting today
            for (dayOffset in 0..6) {
                val testCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, dayOffset)
                }
                val testDayOfWeek = testCal.get(Calendar.DAY_OF_WEEK)

                // Find matching classes for this day
                val dayClasses = classList.filter { matchesDayOfWeek(it.dayOfWeekStr, testDayOfWeek) }

                for (cls in dayClasses) {
                    val classTimeCal = Calendar.getInstance().apply {
                        timeInMillis = testCal.timeInMillis
                        set(Calendar.HOUR_OF_DAY, cls.startHour)
                        set(Calendar.MINUTE, cls.startMinute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    if (classTimeCal.after(now)) {
                        if (targetCal == null || classTimeCal.before(targetCal)) {
                            targetCal = classTimeCal
                            selectedClass = cls
                        }
                    }
                }

                if (targetCal != null) break
            }

            if (targetCal != null && selectedClass != null) {
                val alarmIntent = Intent(context, ClassReminderReceiver::class.java).apply {
                    putExtra("subject_name", selectedClass.subjectName)
                    putExtra("start_time", selectedClass.startTimeStr)
                    putExtra("end_time", selectedClass.endTimeStr)
                    putExtra("teacher", selectedClass.teacher)
                }

                val classPendingIntent = PendingIntent.getBroadcast(
                    context,
                    2001,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP,
                            targetCal.timeInMillis,
                            classPendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            android.app.AlarmManager.RTC_WAKEUP,
                            targetCal.timeInMillis,
                            classPendingIntent
                        )
                    }
                } catch (e: Exception) {
                    try {
                        alarmManager.set(
                            android.app.AlarmManager.RTC_WAKEUP,
                            targetCal.timeInMillis,
                            classPendingIntent
                        )
                    } catch (ignored: Exception) {}
                }
            }
        }

        fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, ClassReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                2001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }

        private fun matchesDayOfWeek(dayStr: String, calendarDayOfWeek: Int): Boolean {
            val d = dayStr.lowercase()
            return when (calendarDayOfWeek) {
                Calendar.MONDAY -> d.contains("seg") || d.contains("2")
                Calendar.TUESDAY -> d.contains("ter") || d.contains("3")
                Calendar.WEDNESDAY -> d.contains("qua") || d.contains("4")
                Calendar.THURSDAY -> d.contains("qui") || d.contains("5")
                Calendar.FRIDAY -> d.contains("sex") || d.contains("6")
                Calendar.SATURDAY -> d.contains("sáb") || d.contains("sab") || d.contains("7")
                Calendar.SUNDAY -> d.contains("dom") || d.contains("1")
                else -> false
            }
        }

        private fun loadScheduleItems(context: Context): List<ClassInfo> {
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val json = prefs.getString("offline_schedule_json", "") ?: ""
            val list = mutableListOf<ClassInfo>()

            if (json.isNotEmpty()) {
                try {
                    val arr = JSONArray(json)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val dayOfWeek = obj.optString("dayOfWeek")
                        val timeSlot = obj.optString("timeSlot")
                        val subjectName = obj.optString("subjectName")
                        val teacher = obj.optString("teacher")

                        val times = parseTimeSlot(timeSlot)
                        if (times != null && subjectName.isNotEmpty()) {
                            list.add(
                                ClassInfo(
                                    dayOfWeekStr = dayOfWeek,
                                    startTimeStr = times.first,
                                    endTimeStr = times.second,
                                    startHour = times.third,
                                    startMinute = times.fourth,
                                    subjectName = subjectName,
                                    teacher = teacher
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Fallback demo schedule if no saved schedule items found
            if (list.isEmpty()) {
                val demoSchedule = listOf(
                    Triple("Segunda", "07:30 - 08:20", "TECNOLOGIA DE GRÃOS, RAÍZES E TUBÉRCULOS"),
                    Triple("Segunda", "10:20 - 11:10", "TECNOLOGIA DA CANA DE AÇÚCAR E SEUS DERIVADOS"),
                    Triple("Terça", "07:30 - 08:20", "LÍNGUA PORTUGUESA"),
                    Triple("Terça", "09:10 - 10:00", "TECNOLOGIA DE LEITE E DERIVADOS"),
                    Triple("Terça", "13:30 - 14:20", "GESTÃO DE RESÍDUOS AGROALIMENTARES"),
                    Triple("Quarta", "07:30 - 08:20", "MATEMÁTICA"),
                    Triple("Quarta", "09:10 - 10:00", "FILOSOFIA"),
                    Triple("Quarta", "13:30 - 14:20", "EDUCAÇÃO FÍSICA"),
                    Triple("Quinta", "07:30 - 08:20", "MATEMÁTICA"),
                    Triple("Quinta", "08:20 - 09:10", "QUÍMICA"),
                    Triple("Sexta", "08:20 - 09:10", "HIGIENE E CONTROLE DA QUALIDADE NA AGROINDÚSTRIA")
                )

                for ((day, slot, name) in demoSchedule) {
                    val times = parseTimeSlot(slot)
                    if (times != null) {
                        list.add(
                            ClassInfo(
                                dayOfWeekStr = day,
                                startTimeStr = times.first,
                                endTimeStr = times.second,
                                startHour = times.third,
                                startMinute = times.fourth,
                                subjectName = name,
                                teacher = ""
                            )
                        )
                    }
                }
            }

            return list
        }

        private fun parseTimeSlot(timeSlot: String): Quad<String, String, Int, Int>? {
            try {
                val parts = timeSlot.split("-").map { it.trim() }
                if (parts.size >= 2) {
                    val startStr = parts[0]
                    val endStr = parts[1]
                    val startHM = startStr.split(":").map { it.trim().toInt() }
                    if (startHM.size >= 2) {
                        return Quad(startStr, endStr, startHM[0], startHM[1])
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private data class ClassInfo(
        val dayOfWeekStr: String,
        val startTimeStr: String,
        val endTimeStr: String,
        val startHour: Int,
        val startMinute: Int,
        val subjectName: String,
        val teacher: String
    )
}
