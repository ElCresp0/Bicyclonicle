package pg.eti.bicyclonicle.ui.record

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import pg.eti.bicyclonicle.R

class RecordFileAdapter(var context: Context, var arrayList: ArrayList<RecordFile>) : BaseAdapter() {
    override fun getCount(): Int {
        return arrayList.size
    }

    override fun getItem(p0: Int): Any {
        return arrayList.get(p0)
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        var view:View = View.inflate(context, R.layout.record_file , null)
        var icons:ImageView  = view.findViewById(R.id.imageViewFile);
        var name:TextView  = view.findViewById(R.id.recordNameFile);
        var time:TextView  = view.findViewById(R.id.recordTimeFile);
        var recordFile: RecordFile = arrayList.get(p0)
        icons.setImageBitmap(recordFile.icons !!)
        name.text = recordFile.name
        time.text = recordFile.time
        return view
    }
}