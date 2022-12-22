package com.hrishi.sosapplication

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Location
import android.os.*
import android.telephony.SmsManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.hrishi.sosapplication.ShakeDetector.OnShakeListener
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class SensorService: Service(){
    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
    private  var mSensorManager:SensorManager?=null
    private  var mAccelerometer:Sensor?=null
    private  var shakeDetector:ShakeDetector?=null
    private lateinit var fusedLocationProvider:FusedLocationProviderClient
    override fun onCreate() {
        super.onCreate()
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            startMyOwnForeGround()
        }else{
            startForeground(1, Notification())
        }

        mSensorManager=getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer=mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        shakeDetector= ShakeDetector()
        fusedLocationProvider=LocationServices.getFusedLocationProviderClient(applicationContext)
        shakeDetector?.setOnShakeListener(object : OnShakeListener {
            @SuppressLint("MissingPermission")
            override fun onShake(count: Int) {
                if(count>2){
                    vibrate()
                    val contactsDao=(application as ContactsApp).db.ContactsDao()
                    fusedLocationProvider.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY,null)
                        .addOnSuccessListener {
                        val location:Location?=it
                        GlobalScope.launch {
                            contactsDao.fetchAllContacts().collect{entity->
                                val arr=ArrayList(entity)
                                sendMessages(arr,location)
                            }
                        }
                    }.addOnFailureListener {
                        Log.e("Location fetch error","$it")
                        GlobalScope.launch {
                            contactsDao.fetchAllContacts().collect{entity->
                                val arr=ArrayList(entity)
                                sendMessageWithoutLocation(arr)
                            }
                        }
                    }
                }
            }
        })
        mSensorManager?.registerListener(shakeDetector,mAccelerometer,SensorManager.SENSOR_DELAY_UI)
    }
    private fun vibrate(){
        if(Build.VERSION.SDK_INT>=31){
            val vibratorManager=getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator=vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createOneShot(500,VibrationEffect.DEFAULT_AMPLITUDE))
        }else{
            val vibrator=getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if(Build.VERSION.SDK_INT>=26){
                vibrator.vibrate(VibrationEffect.createOneShot(500,VibrationEffect.DEFAULT_AMPLITUDE))
            }else{
                vibrator.vibrate(500)
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startMyOwnForeGround(){
        val NOTIFICATION_CHANNEL_ID="example.permanence"
        val channelName="Background Service"
        val notificationChannel=
            NotificationChannel(NOTIFICATION_CHANNEL_ID,channelName,NotificationManager.IMPORTANCE_MIN)
        val notificationManager:NotificationManager=getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)

        val notificationBuilder=Notification.Builder(this,NOTIFICATION_CHANNEL_ID)
        val notification=notificationBuilder.setOngoing(true).setContentTitle("Message is Sent")
            .setContentText("Don't Worry the information to provided").setPriority(Notification.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE).build()
        startForeground(2,notification)
    }
    private fun sendMessageWithoutLocation(arr:ArrayList<ContactEntity>){
        val smsManager = SmsManager.getDefault()
        for(c in arr){
            val msg="Urgent help required,Please Call the Police"
            smsManager.sendTextMessage(c.phone,null,msg,null,null)
        }
    }
    private fun sendMessages(arr:ArrayList<ContactEntity>,location: Location?){
        val smsManager = SmsManager.getDefault()
        if(location==null){
            for(c in arr){
                val msg="Urgent help required,Please Call the Police"
                smsManager.sendTextMessage(c.phone,null,msg,null,null)
            }
        }else{
            for(c in arr){
                val msg="Urgent help required,here is my location"+"http://maps.google.com/?q=" + location.latitude.toString() + ","+location.longitude.toString()
                smsManager.sendTextMessage(c.phone,null,msg,null,null)
            }
        }
        Log.e("hola","$location")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }
    override fun onDestroy() {
        val broadcastIntent = Intent()
        broadcastIntent.action = "restartable"
        broadcastIntent.setClass(this, ReactivateService::class.java)
        this.sendBroadcast(broadcastIntent)
        super.onDestroy()
    }
}