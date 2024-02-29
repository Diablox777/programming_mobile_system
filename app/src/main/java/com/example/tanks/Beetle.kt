package com.example.tanks

// Импорт необходимых классов и методов из Android SDK
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.atan2
import kotlin.random.Random
import android.view.ViewGroup
import android.animation.ObjectAnimator


// Объект для хранения конфигурации жуков
object BeetleConfig {
    var minBeetleSpeed = 10
    var maxBeetleSpeed = 14
}

// Открытый класс, представляющий жука
open class Beetle : AppCompatImageView {

    private val handler = Handler(Looper.getMainLooper()) //Обработчик для взаимодействия с главным потоком
    private val beetleSize = 320

    //Случайное направление жука
    private var horizontalDirection = Random.nextFloat() * 2 - 1
    private var verticalDirection = Random.nextFloat() * 2 - 1

    private var destroyed = false //Флаг уничтожения жука

    constructor(context: Context) : super(context) {
        init(context)
    }

    // Инициализация жука
    private fun init(context: Context) {
        //Установка изображения жука
        val random = Random.nextInt(5)
        val drawableResId: Int = when (random) {
            0 -> R.drawable.bug1
            1 -> R.drawable.bug3
            2 -> R.drawable.bug4
            3 -> R.drawable.bug5
            else -> R.drawable.bug6
        }
        val drawable: Drawable? = AppCompatResources.getDrawable(context, drawableResId)
        setImageDrawable(drawable)

        // Выбор случайного угла для спавна жука
        val corner = Random.nextInt(4)
        val params = FrameLayout.LayoutParams(beetleSize, beetleSize)
        when (corner) {
            0 -> params.gravity = Gravity.BOTTOM or Gravity.START
            1 -> params.gravity = Gravity.BOTTOM or Gravity.END
            2 -> params.gravity = Gravity.TOP or Gravity.START
            3 -> params.gravity = Gravity.TOP or Gravity.END
        }
        layoutParams = params //Запись местоположения жука в родительский контейнер (игровое поле)
    }

    // Запуск жука в своем собственном потоке
    internal fun start() {

        // Перемещение жука на передний план
        post {
            bringToFront()
        }

        val thread = Thread { // Создание отдельного потока для нового жука
            while (!destroyed) {
                try {
                    Thread.sleep(50) // Грубо говоря, пауза между кадрами. Каждые 50мс жук будет сдвигаться
                    move()
                } catch (e: InterruptedException) {
                    e.printStackTrace() //Вывод исключений в логи
                }
            }
        }
        thread.start() // Запуск потока
    }

    private fun move() {
        // Генерация случайных изменений направлений
        val randomHorizontalChange = (Random.nextFloat() - 0.5f) * 0.1f
        val randomVerticalChange = (Random.nextFloat() - 0.5f) * 0.1f

        //Случайная скорость в диапазоне минимальной и максимальной скорости
        val beetleSpeed =
            (BeetleConfig.minBeetleSpeed..BeetleConfig.maxBeetleSpeed).random() + Random.nextInt(
                BeetleConfig.maxBeetleSpeed
            )

        // Если жук уничтожен, отбой
        if (destroyed) return

        // Изменение направления движения
        horizontalDirection += randomHorizontalChange
        verticalDirection += randomVerticalChange

        // Ограничение диапазона значений направлений (Если значение выйдет за пределы [-1;1], оно станет равным -1 или 1
        // Без ограничения жуки будут резко поворачивать
        horizontalDirection = horizontalDirection.coerceIn(-1f, 1f)
        verticalDirection = verticalDirection.coerceIn(-1f, 1f)

        // Вычисление новой позиции жука
        var newX = x + beetleSpeed * horizontalDirection //Вычисление новой позиции по X
        newX = newX.coerceIn(0f, (parent as FrameLayout).width.toFloat() - width) //Ограничение, чтобы жук оставался в пределах родительского контейнера по X

        var newY = y + beetleSpeed * verticalDirection //Аналогично
        newY = newY.coerceIn(0f, (parent as FrameLayout).height.toFloat() - height)

        // Изменение направления при достижении границ экрана
        if (newX == 0f || newX == (parent as FrameLayout).width.toFloat() - width) {
            horizontalDirection *= -1
        }
        //При достижении края родительского контейнера отражается направление
        if (newY == 0f || newY == (parent as FrameLayout).height.toFloat() - height) {
            verticalDirection *= -1
        }

        // Установка новой позиции и угла поворота жука в главном потоке через Handler
        // Эти значения взаимодействуют с UI, поэтому они обязаны находится в главном потоке
        handler.post {
            x = newX
            y = newY
            val angle = atan2(verticalDirection.toDouble(), horizontalDirection.toDouble()) // Вычисление угла между направлениями по X и Y (в радианах)
            rotation = Math.toDegrees(angle).toFloat() + 90 //Поворот картинки жука на полученный угол (преобразованный в градусы)
        }
    }


    // Метод уничтожения жука
    fun destroy() {
        if (!destroyed) {
            destroyed = true
            try {
                // Картинки крови
                val bloodImages = listOf(
                    R.drawable.blood1,
                    R.drawable.blood2,
                    R.drawable.blood3,
                    R.drawable.blood4,
                    R.drawable.blood5
                )
                val randomBloodImageResId = bloodImages.random()
                val bloodDrawable: Drawable? =
                    AppCompatResources.getDrawable(context, randomBloodImageResId)
                setImageDrawable(bloodDrawable) //Замена картинки жука на случайную картинку крови

                // Удаление жука из текущего положения
                val parentContainer = parent as? ViewGroup
                parentContainer?.removeView(this)
                // Добавление жука на нижний слой
                parentContainer?.addView(this, 0)

                // Анимация для плавного исчезновения крови
                val animation = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f)
                animation.duration = 1000 // Продолжительность анимации
                animation.start()

                // Удаление жука из контейнера через секунду
                postDelayed({
                    try {
                        parentContainer?.removeView(this)
                    } catch (e: Exception) {
                        e.printStackTrace() //Вывод исключений в логи
                    }
                }, 1000)
            } catch (e: Exception) {
                e.printStackTrace() //Вывод исключений в логи
            }
        }
    }

    // Проверка попадания
    fun isTouched(x: Float, y: Float): Boolean {
        val touched = x >= this.x && x <= this.x + width &&
                y >= this.y && y <= this.y + height //True, если X и Y попадают в прямоугольник, заданный жуком
        //this.x и this.y - координаты верхнего левого угла прямоугольника жука, а
        //width и height - его ширина и высота
        if (touched) {
            destroy()
        }
        return touched
    }
}
