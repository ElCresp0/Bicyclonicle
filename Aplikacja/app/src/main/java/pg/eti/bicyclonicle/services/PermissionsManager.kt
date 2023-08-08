package pg.eti.bicyclonicle.services

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import pg.eti.bicyclonicle.Manifest

class PermissionsManager(private val context: Context) {

    fun checkNeededPermissions(){
        // Check if the app has location permission
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permission from the user
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission already granted, proceed with your location-related logic
        }
    }
    }
}