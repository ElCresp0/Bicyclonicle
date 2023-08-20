package pg.eti.bicyclonicle.ui.record

import pg.eti.bicyclonicle.databinding.FragmentRecordingsBinding
import pg.eti.bicyclonicle.databinding.RecordFileBinding

class Record_file {
    //private var _binding: RecordFileBinding? = null

    //private val binding get() = _binding!!

    var icons:Int ? = null;
    var name:String ? = null;
    var time:String ? = null;

    constructor(icons: Int?, name: String?, time: String?) {
        this.icons = icons
        this.name = name
        this.time = time
    }


}