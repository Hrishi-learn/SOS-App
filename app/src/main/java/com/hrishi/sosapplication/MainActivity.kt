package com.hrishi.sosapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hrishi.sosapplication.databinding.ActivityMainBinding
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.DexterError
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding?=null
    private var PICK_CONTACT=1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        val contactDao=(application as ContactsApp).db.ContactsDao()
        binding?.btnAdd?.setOnClickListener {
            val intent=Intent(Intent.ACTION_PICK,ContactsContract.Contacts.CONTENT_URI)
            startActivityForResult(intent,PICK_CONTACT)
        }
        lifecycleScope.launch{
            contactDao.fetchAllContacts().collect {
                val arr=ArrayList(it)
                showAllRecords(arr)
            }
        }
        requestPermissions()
        val sensorService=SensorService()
        val intent = Intent(this, sensorService.javaClass)
        if(!isMyServiceRunning(sensorService::class.java)){
            startService(intent)
        }
    }
    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                Log.i("Service status", "Running")
                return true
            }
        }
        Log.i("Service status", "Not running")
        return false
    }
    override fun onDestroy() {
        val broadcastIntent = Intent()
        broadcastIntent.action = "restartable"
        broadcastIntent.setClass(this, ReactivateService::class.java)
        this.sendBroadcast(broadcastIntent)
        super.onDestroy()
    }

    private fun addRecord(name:String,phone:String){
        val contactDao=(application as ContactsApp).db.ContactsDao()
        lifecycleScope.launch{
            contactDao.insert(ContactEntity(name = name, phone = phone))
        }
    }

    @SuppressLint("Range")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==PICK_CONTACT && resultCode==Activity.RESULT_OK){
            val contactData = data?.data
//            Log.e("contactdata","$contactData")
            val c: Cursor = managedQuery(contactData, null, null, null, null)
            if (c.moveToFirst()) {
                val id: String = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                val hasPhone: String =
                    c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                var phone: String? = null
                try {
                    if (hasPhone.equals("1", ignoreCase = true)) {
                        val phones: Cursor? = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                            null,
                            null
                        )
                        phones?.moveToFirst()
                        phone = phones?.getString(phones.getColumnIndex("data1"))
                    }
                    val name: String =
                        c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                    addRecord(name,phone!!)

                } catch (ex: Exception) {
                    Log.e("contactError","$ex")
                }
            }
        }
    }

    private fun requestPermissions(){
        Dexter.withActivity(this) // below line is use to request the number of permissions which are required in our app.
            .withPermissions(
                Manifest.permission.SEND_SMS,  // below is the list of permissions
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.VIBRATE,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(multiplePermissionsReport: MultiplePermissionsReport) {
                    if (multiplePermissionsReport.areAllPermissionsGranted()) {
                        Toast.makeText(
                            this@MainActivity,
                            "All the permissions are granted..",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied) {
                        showSettingsDialog()
                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    list: List<PermissionRequest?>?,
                    permissionToken: PermissionToken
                ) {
                    permissionToken.continuePermissionRequest()
                }
            }).withErrorListener { error: DexterError? ->
                Toast.makeText(
                    applicationContext,
                    "Error occurred! ",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .onSameThread().check()
    }

    private fun showAllRecords(arrayList:ArrayList<ContactEntity>){
        val adapter=ItemAdapter(arrayList) { id ->
            deleteContact(id)
        }
        binding?.rvList?.adapter=adapter
        binding?.rvList?.layoutManager=LinearLayoutManager(this)
    }
    private fun deleteContact(id:Int){
        val contactDao=(application as ContactsApp).db.ContactsDao()
        lifecycleScope.launch {
            contactDao.delete(ContactEntity(id))
        }
    }

    private fun showSettingsDialog(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)

        builder.setTitle("Need Permissions")
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.")
        builder.setPositiveButton("GO TO SETTINGS") { dialog, which ->
            dialog.cancel()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }
        builder.show()
    }
}