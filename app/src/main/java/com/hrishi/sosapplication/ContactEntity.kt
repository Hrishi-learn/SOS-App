package com.hrishi.sosapplication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="contacts-table")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    var id:Int=0,
    var name:String="",
    @ColumnInfo(name="phone")
    var phone:String=""
)
