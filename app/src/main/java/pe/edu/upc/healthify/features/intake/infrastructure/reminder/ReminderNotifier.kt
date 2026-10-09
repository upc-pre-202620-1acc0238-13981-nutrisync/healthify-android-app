package pe.edu.upc.healthify.features.intake.infrastructure.reminder

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
import pe.edu.upc.healthify.features.intake.domain.entity.ReminderKind
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Avisos locales de PT22 con tono neutro, nunca de presión. No llevan datos clínicos (ni peso, ni comidas, ni metas):
 * solo invitan a abrir la app.
 */
@Singleton
class ReminderNotifier @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    fun show(kind: ReminderKind) {
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
        val (title, text) = when (kind) {
            ReminderKind.SELF_WEIGH_IN -> R.string.reminder_weigh_in_title to R.string.reminder_weigh_in_text
            ReminderKind.MEALS -> R.string.reminder_meals_title to R.string.reminder_meals_text
        }
        val openApp = PendingIntent.getActivity(
            context,
            kind.ordinal,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(title))
            .setContentText(context.getString(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BASE + kind.ordinal, notification)
        } catch (_: SecurityException) {
            // El permiso se retiró entre la verificación y el aviso: no se avisa (no es un error para el paciente).
            return
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "reminders"
        const val NOTIFICATION_ID_BASE = 2200
    }
}
