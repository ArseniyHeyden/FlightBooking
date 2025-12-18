plugins {
    // Обновляем AGP до 8.3.2, что полностью совместимо с Gradle 8.5+
    id("com.android.application") version "8.3.2" apply false

    // Обновляем KGP для лучшей совместимости с новым AGP
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}