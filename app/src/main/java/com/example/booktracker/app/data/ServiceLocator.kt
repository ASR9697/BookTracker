package com.example.booktracker.app.data

import android.content.Context
import androidx.room.Room
import com.example.booktracker.app.data.local.AppDatabase

/**
 * Minimal manual DI. Both the UI (via ViewModel) and WearSyncService — which
 * runs without the Activity — resolve the repository through here, so they
 * share one database instance.
 */
object ServiceLocator {
    @Volatile
    private var database: AppDatabase? = null

    private val _timerControlEvents = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 5)
    val timerControlEvents: kotlinx.coroutines.flow.SharedFlow<String> = _timerControlEvents

    fun emitTimerControlEvent(action: String) {
        _timerControlEvents.tryEmit(action)
    }

    fun repository(context: Context): BookRepository =
        db(context).let { RoomBookRepository(it.bookDao(), it.sessionDao(), it.marginNoteDao()) }

    fun settings(context: Context): SettingsRepository =
        SettingsRepository(context.applicationContext)

    @Volatile
    private var dndManager: DndManager? = null

    fun dndManager(context: Context): DndManager =
        dndManager ?: synchronized(this) {
            dndManager ?: DndManager(
                context.applicationContext,
                db(context).sessionDao(),
                settings(context)
            ).also { dndManager = it }
        }

    private fun db(context: Context): AppDatabase =
        database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "booktracker.db"
            )
                .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
                .build().also { database = it }
        }
}
