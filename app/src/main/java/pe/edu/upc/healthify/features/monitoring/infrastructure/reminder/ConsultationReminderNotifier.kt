package pe.edu.upc.healthify.features.monitoring.infrastructure.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import pe.edu.upc.healthify.MainActivity
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.network.AndroidAppLanguageSetter
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aviso local de una consulta próxima: «Tu consulta es mañana · Con tu nutricionista a las 10:00» (paciente) o
 * «Consulta en una hora · Con Ana Flores a las 10:00» (nutricionista). En la pantalla bloqueada se ve una versión sin
 * nombres.
 */
@Singleton
class ConsultationReminderNotifier @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    fun show(followUpId: Long, scheduledFor: Instant, lead: Duration, isPractitioner: Boolean, patientFullName: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) return
        // Los textos en el idioma elegido en la app (antes de Android 13 el contexto de la app no lo sabe).
        val context = AndroidAppLanguageSetter.wrap(appContext)
        ensureChannel(context)
        val isDayBefore = lead >= Duration.ofHours(DAY_BEFORE_HOURS)
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(context.resources.configuration.locales[0])
            .format(scheduledFor.atZone(ZoneId.systemDefault()))
        val title = context.getString(
            when {
                isPractitioner && isDayBefore -> R.string.consultation_reminder_practitioner_title_day
                isPractitioner -> R.string.consultation_reminder_practitioner_title_hour
                isDayBefore -> R.string.consultation_reminder_patient_title_day
                else -> R.string.consultation_reminder_patient_title_hour
            },
        )
        val text = when {
            !isPractitioner -> context.getString(R.string.consultation_reminder_patient_text, time)
            patientFullName.isNullOrBlank() -> context.getString(R.string.consultation_reminder_practitioner_text_no_name, time)
            else -> context.getString(R.string.consultation_reminder_practitioner_text, patientFullName, time)
        }
        val openApp = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.consultation_reminder_public_text))
            .build()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        try {
            // Un aviso por consulta: el de una hora antes reemplaza al del día anterior.
            NotificationManagerCompat.from(context).notify(NOTIFICATION_TAG, followUpId.toInt(), notification)
        } catch (_: SecurityException) {
            // El permiso se retiró entre la verificación y el aviso: no se avisa.
            return
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.consultation_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.consultation_reminder_channel_description) }
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "consultations"
        const val NOTIFICATION_TAG = "consultation"
        const val REQUEST_CODE = 2500
        const val DAY_BEFORE_HOURS = 12L
    }
}
