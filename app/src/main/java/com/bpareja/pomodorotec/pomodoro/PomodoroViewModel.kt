package com.bpareja.pomodorotec.pomodoro

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.CountDownTimer
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bpareja.pomodorotec.MainActivity
import com.bpareja.pomodorotec.PomodoroReceiver
import com.bpareja.pomodorotec.R

enum class Phase {
    FOCUS, BREAK
}

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {
    init {
        instance = this
    }

    companion object {
        private var instance: PomodoroViewModel? = null
        fun skipBreak() {
            instance?.startFocusSession()
        }
    }
    private val context = getApplication<Application>().applicationContext

    private val _timeLeft = MutableLiveData("25:00")
    val timeLeft: LiveData<String> = _timeLeft

    private val _isRunning = MutableLiveData(false)
    val isRunning: LiveData<Boolean> = _isRunning

    private val _currentPhase = MutableLiveData(Phase.FOCUS)
    val currentPhase: LiveData<Phase> = _currentPhase

    private var countDownTimer: CountDownTimer? = null
    private var timeRemainingInMillis: Long = 25 * 60 * 1000L // Tiempo inicial para FOCUS

    // Función para iniciar la sesión de concentración
    fun startFocusSession() {
        countDownTimer?.cancel() // Cancela cualquier temporizador en ejecución
        _currentPhase.value = Phase.FOCUS
        timeRemainingInMillis = 1 * 15 * 1000L // el tiempo esta configurado en 15 segundos
        _timeLeft.value = "00:04" // Valor inicial de texto en pantalla (solo para prueba)
        showNotification("Inicio de Concentración", "La sesión de concentración ha comenzado.")
        startTimer() // Inicia el temporizador con el tiempo de enfoque actualizado
    }

    // Función para iniciar la sesión de descanso
    private fun startBreakSession() {
        _currentPhase.value = Phase.BREAK
        timeRemainingInMillis = 5 * 60 * 1000L // 5 minutos para descanso
        _timeLeft.value = "05:00"
        showNotification("Inicio de Descanso", "La sesión de descanso ha comenzado.")
        startTimer()
    }

    // Inicia o reanuda el temporizador
    fun startTimer() {
        countDownTimer?.cancel() // Cancela cualquier temporizador en ejecución antes de iniciar uno nuevo
        _isRunning.value = true

        countDownTimer = object : CountDownTimer(timeRemainingInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingInMillis = millisUntilFinished
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                _timeLeft.value = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                _isRunning.value = false
                when (_currentPhase.value ?: Phase.FOCUS) { // Si es null, se asume FOCUS
                    Phase.FOCUS -> startBreakSession()
                    Phase.BREAK -> startFocusSession()
                }
            }
        }.start()
    }


    // Pausa el temporizador
    fun pauseTimer() {
        countDownTimer?.cancel()
        _isRunning.value = false
    }

    // Restablece el temporizador
    fun resetTimer() {
        countDownTimer?.cancel()
        _isRunning.value = false
        _currentPhase.value = Phase.FOCUS
        timeRemainingInMillis = 25 * 60 * 1000L // Restablece a 25 minutos
        _timeLeft.value = "25:00"
    }

    // Muestra la notificación
    private fun showNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para el botón "Saltar descanso"
        val skipBreakIntent = Intent(context, PomodoroReceiver::class.java).apply {
            action = "SKIP_BREAK"
        }
        val skipBreakPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context, 0, skipBreakIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
            .setSmallIcon(R.drawable.pomodoro)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Solo mostrar el botón "Saltar descanso" si está en el estado de descanso
        if (_currentPhase.value == Phase.BREAK) {
            builder.addAction(
                R.drawable.ic_skip, "Saltar descanso", skipBreakPendingIntent
            )
        }

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(MainActivity.NOTIFICATION_ID, builder.build())
        }
    }
}