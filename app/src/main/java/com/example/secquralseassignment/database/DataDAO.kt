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

    @Delete
    fun delete(dataEntity: DataEntity)

    @Query("Select * from data_table")
    fun getAllData(): LiveData<List<DataEntity>>
}