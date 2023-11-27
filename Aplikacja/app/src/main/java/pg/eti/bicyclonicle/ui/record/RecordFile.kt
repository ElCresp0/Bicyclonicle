package pg.eti.bicyclonicle.ui.record

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color

class RecordFile {
    //private var _binding: RecordFileBinding? = null

    //private val binding get() = _binding!!

    var icons:Bitmap ? = defaultBitmap()
    var name:String ? = null;
    var time:String ? = "00:00";
    var path:String ? = null;
    var sdSaved: Boolean ? = null;
    var telSaved: Boolean ? = null;

    constructor(icons: Bitmap?, name: String?, time: String?, sdSaved: Boolean?, telSaved: Boolean?, path:String ?) {
        this.icons = icons
        this.name = name
        this.time = time
        this.sdSaved = sdSaved
        this.telSaved = telSaved
        this.path = path
    }

    private fun defaultBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.GRAY)
        return bitmap
    }

    public fun retVideoPath(): String? {
        return path
    }

    public fun setVisibilitySD(){
        sdSaved = true
    }

    public fun setVisibilityTele(){
        telSaved = true
    }

}