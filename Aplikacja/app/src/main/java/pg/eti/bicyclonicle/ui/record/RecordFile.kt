package pg.eti.bicyclonicle.ui.record

import android.graphics.Bitmap

class RecordFile {
    //private var _binding: RecordFileBinding? = null

    //private val binding get() = _binding!!

    var icons:Bitmap ? = null;
    var name:String ? = null;
    var time:String ? = null;
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