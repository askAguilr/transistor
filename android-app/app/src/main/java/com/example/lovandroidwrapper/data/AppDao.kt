package com.example.lovandroidwrapper.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM transistor_apps ORDER BY lastAccessed DESC")
    fun getAllApps(): Flow<List<TransistorApp>>

    @Query("SELECT * FROM transistor_apps WHERE url = :url LIMIT 1")
    suspend fun getAppByUrl(url: String): TransistorApp?

    @Query("SELECT * FROM transistor_apps WHERE url = :url LIMIT 1")
    fun getAppByUrlFlow(url: String): Flow<TransistorApp?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: TransistorApp)

    @Update
    suspend fun updateApp(app: TransistorApp)

    @Delete
    suspend fun deleteApp(app: TransistorApp)
}
