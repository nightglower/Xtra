package com.github.exact7.xtra.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.exact7.xtra.model.kraken.user.Emote

@Dao
interface EmotesDao {

    @Query("SELECT * FROM emotes ORDER BY code")
    fun getAll(): LiveData<List<Emote>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(emotes: List<Emote>)

    @Query("DELETE FROM emotes")
    fun deleteAll()
}