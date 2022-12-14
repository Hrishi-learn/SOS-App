package com.hrishi.sosapplication

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactsDao{
    @Insert
    suspend fun insert(contactEntity: ContactEntity)
    @Delete
    suspend fun delete(contactEntity: ContactEntity)
    @Update
    suspend fun update(contactEntity: ContactEntity)
    @Query("SELECT*FROM `contacts-table`")
    fun fetchAllContacts():Flow<List<ContactEntity>>
}