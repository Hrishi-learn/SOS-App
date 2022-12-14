package com.hrishi.sosapplication
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ContactEntity::class],version=1)
abstract class ContactsDB:RoomDatabase(){

    abstract fun ContactsDao(): ContactsDao

    companion object{
        @Volatile
        private var INSTANCE: ContactsDB?=null

        fun getInstance(context: Context): ContactsDB {
            synchronized(this){
                var instance= INSTANCE
                if(instance==null){
                    instance= Room.databaseBuilder(
                        context.applicationContext,
                        ContactsDB::class.java,
                        "contacts_database"
                    ).fallbackToDestructiveMigration().build()
                    INSTANCE =instance
                }
                return instance
            }
        }
    }
}