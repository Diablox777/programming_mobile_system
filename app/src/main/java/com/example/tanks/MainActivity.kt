package com.example.tanks

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.CopyOnWriteArrayList

class MainActivity : AppCompatActivity() {

    // Переменные для элементов пользовательского интерфейса и управления игрой
    private lateinit var gameContainer: CustomFrameLayout
    private lateinit var scoreTextView: TextView
    private lateinit var hpTextView: TextView
    private val beetles = CopyOnWriteArrayList<Beetle>() // Хранит экземпляры жуков на экране
    private val handler = Handler(Looper.getMainLooper()) // Обработчик для управления потоками
    private var hp = 5 // Значение здоровья игрока
    private var score = 0 // Текущий счет игрока
    private var record = 0 // Рекорд игрока
    private lateinit var sharedPreferences: SharedPreferences // Хранилище настроек (нужно для записи рекорда)
    private val soundMiss = listOf(R.raw.yaaa, R.raw.yaaayaaa) //Звуки промаха
    private lateinit var backgroundMusicPlayer: MediaPlayer
    private var delayMillis: Long = 500 //Задержка перед созданием нового жука (будет уменьшаться)

    // Метод, вызываемый при создании активности
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация музыкального плеера для фоновой музыки
        backgroundMusicPlayer = MediaPlayer.create(this, R.raw.rock2)
        backgroundMusicPlayer.isLooping = true // Зацикливание воспроизведения

        // Воспроизведение фоновой музыки
        backgroundMusicPlayer.start()

        // Инициализация пользовательского интерфейса и начальных значений
        sharedPreferences = getSharedPreferences("my_prefs", Context.MODE_PRIVATE) //Загрузка рекорда из файла "my_prefs"
        record = sharedPreferences.getInt("record", 0)
        gameContainer = findViewById(R.id.game_container) //Контейнер с игровым полем (берется из xml файла)
        scoreTextView = findViewById(R.id.score_text_view)
        hpTextView = findViewById(R.id.hp_text_view)

        //Обработчик события - реагирует на нажатия (т.е. код внутри него будет выполняться при каждом нажатии)
        gameContainer.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && hp > 0) { //Проверка на нажатие. В теории не нужна, но на практике без нее все ломается :(
                val x = event.x
                val y = event.y

                if (!checkBeetleTouched(x, y)) { // Проверка попадания по жуку
                    if (hp > 0){
                        val randomIndex = (0 until soundMiss.size).random() //Случайный звук промаха
                        val soundResId = soundMiss[randomIndex]
                        playSound(soundResId)

                    }
                    decreaseHP()


                }
                gameContainer.performClick()
            }
            true
        }
        startGame()
    }

    private fun startGame() {
        // Сброс значений скорости до начальных
        BeetleConfig.minBeetleSpeed = 8
        BeetleConfig.maxBeetleSpeed = 14

        val thread = Thread { //Создание нового потока (для задержки)
            while (true) {
                try {
                    Thread.sleep(delayMillis) // Задержка
                    handler.post { createBeetle() } //Создание нового жука в главном потоке
                } catch (e: InterruptedException) {
                    e.printStackTrace()  //Вывод исключений в логи
                }
            }
        }
        thread.start() //Запуск потока
    }

    private fun updateDelay() {
        // Обновление задержки в зависимости от счета
        when (score) {
            in 20 until 40 -> delayMillis = 400
            in 40 until 60 -> delayMillis = 300
            in 60 until 80 -> delayMillis = 200
            in 80 until Long.MAX_VALUE -> delayMillis = 100
        }
    }

    private fun createBeetle() {
        if (beetles.size >= 7) return //Максимум жуков на экране
        val beetle = Beetle(this) //Экземпляр жука
        beetles.add(beetle) //Добавление жука в список
        gameContainer.addView(beetle) //Добавление жука на экран
        beetle.start()
        updateDelay()
    }

    // Метод проверки нажатия на жука
    private fun checkBeetleTouched(x: Float, y: Float): Boolean {
        val touchedBeetles = mutableListOf<Beetle>() //Список для жуков, которых коснулись за текущий клик
        for (beetle in beetles) { //Цикл по всем жукам
            if (beetle.isTouched(x, y)) { //Если было касание
                touchedBeetles.add(beetle) //Добавляем жука в список
                score++
                updateScore(score) //Обновление счета UI
                playSound(R.raw.kill) //
                if (score > record) {
                    record = score
                    saveRecord()
                }
                // Перемещение жука на передний план
                beetle.bringToFront()
            }
        }

        if (touchedBeetles.size == 2){  //Дополнительные звуки для убийства сразу нескольких жуков
            playSound(R.raw.doublekill)
        }
        else if (touchedBeetles.size == 3){
            playSound(R.raw.tripple)
        }
        else if (touchedBeetles.size > 3){
            playSound(R.raw.ultra)
        }
        if (score % 100 == 0 && score != 0 && touchedBeetles.isNotEmpty()){
            playSound(R.raw.sss)
        }

        // Удаление найденных жуков из основного списка
        beetles.removeAll(touchedBeetles)

        // Уничтожение найденных жуков
        for (beetle in touchedBeetles) {
            beetle.destroy()
        }
        return touchedBeetles.isNotEmpty() //Вернет true если было попадение
    }

    // Обновление счета UI
    private fun updateScore(score: Int) {
        scoreTextView.text = getString(R.string.score_label, score)

        // Каждые 5 очков, увеличиваем скорость жуков
        if (score % 5 == 0) {
            increaseBeetleSpeed()
        }
    }

    // Увеличение скорости
    private fun increaseBeetleSpeed() {
        BeetleConfig.minBeetleSpeed += 1
        BeetleConfig.maxBeetleSpeed += 1
    }

    // Уменьшение здоровья
    private fun decreaseHP() {
        hp--
        hpTextView.text = getString(R.string.hp_label, hp)
        if (hp <= 0) {
            gameOver()
        }
    }

    private fun gameOver() {
        if (score > 49 && score < 100){
            playSound(R.raw.dismall)
        }
        else if (score > 99 && score < 150){
            playSound(R.raw.crazy)
        }
        else if (score > 149 && score < 200){
            playSound(R.raw.sick_skills)
        }
        else if (score > 199 && score < 250){
            playSound(R.raw.savage)
        }
        else if (score > 249 && score < 300){
            playSound(R.raw.sadistic)
        }

        val builder = AlertDialog.Builder(this) //Создание диалогового окна
        builder.setTitle("Game Over")
        builder.setMessage("The enemy's army won.\n\nYour score: $score\nYour record: $record")
        builder.setNegativeButton("Try again") { dialog, _ ->
            dialog.dismiss()
            restartGame()
        }
        builder.setPositiveButton("Exit") { dialog, _ ->
            dialog.dismiss() //Закрытие окна
            finish()
        }
        builder.setOnCancelListener {  //Если диалоговое окно закрыто без кнопки ОК
            finish()
        }
        builder.show() //Отображение диалогового окна
    }

    private fun restartGame() {
        // Сброс значений счета и здоровья
        score = 0
        hp = 5
        delayMillis = 500
        updateScore(score)
        hpTextView.text = getString(R.string.hp_label, hp)

        // Удаление всех жуков с игрового поля
        for (beetle in beetles) {
            beetle.destroy()
        }
        beetles.clear() // Очистка списка жуков

        // Остановка текущих потоков и обработчиков
        handler.removeCallbacksAndMessages(null) // Удаляем все сообщения и задачи из очереди обработчика
        delayMillis = 500 // Сбрасываем задержку для создания новых жуков
        startGame() // Запуск игры заново
    }


    // Сохранение рекорда
    private fun saveRecord() {
        val editor = sharedPreferences.edit()
        editor.putInt("record", record) //Перезаписываем рекорд
        editor.apply()
    }

    // Метод проигрывания звука
    private fun playSound(soundResId: Int) {
        val mediaPlayer = MediaPlayer.create(this, soundResId) //Создание экземпляра плеера с текущим звуком
        mediaPlayer.setOnCompletionListener { player -> player.release() } //Слушатель завершения воспроизведения, освобождает ресурсы плеера
        mediaPlayer.start()
    }

    //Эти методы вызываются автоматически при сворачивании и возврате в приложение
    override fun onPause() {
        super.onPause()
        // При сворачивании приложения останавливаем воспроизведение музыки
        if (backgroundMusicPlayer.isPlaying) {
            backgroundMusicPlayer.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        // При возобновлении приложения продолжаем воспроизведение музыки, если оно было запущено
        if (!backgroundMusicPlayer.isPlaying) {
            backgroundMusicPlayer.start()
        }
    }
}
