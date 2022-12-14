package com.hrishi.sosapplication

import android.app.Application

class ContactsApp:Application(){
    val db by lazy{
        ContactsDB.getInstance(this)
    }
}