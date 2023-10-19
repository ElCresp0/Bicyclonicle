package pg.eti.bicyclonicle

//import android.content.Context
//import android.content.pm.PackageManager
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import pg.eti.bicyclonicle.Manifest

//class PermissionsManager(private val context: Context) {
//
//    fun checkNeededPermissions(){
//        // Check if the app has location permission
//        if (ContextCompat.checkSelfPermission(
//                context,
//                Manifest.permission.
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            // Request location permission from the user
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                LOCATION_PERMISSION_REQUEST_CODE
//            )
//        } else {
//            // Permission already granted, proceed with your location-related logic
//        }
//    }
//    }
//}

//if (ActivityCompat.checkSelfPermission(
//context,
//Manifest.permission.BLUETOOTH_CONNECT
//) != PackageManager.PERMISSION_GRANTED
//) {
//    // TODO: Consider calling
//    //    ActivityCompat#requestPermissions
//    // here to request the missing permissions, and then overriding
//    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//    //                                          int[] grantResults)
//    // to handle the case where the user grants the permission. See the documentation
//    // for ActivityCompat#requestPermissions for more details.
//    return true
//}