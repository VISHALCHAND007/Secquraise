package com.example.secquralseassignment.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DataDAO {
    @Insert (onConflict = OnConflictStrategy.REPLACE)
    fun insert(dataEntity: DataEntity)

    @Query ("Delete From data_table where timestamp = :timeStamp")
    fun delete(timeStamp: String)

    @Query("Select * from data_table")
    fun getAllData(): List<DataEntity>
}