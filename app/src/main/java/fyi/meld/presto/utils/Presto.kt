package fyi.meld.presto.utils

import android.app.Application
import com.androidisland.vita.startVita

class Presto: Application()
{
    override fun onCreate() {
        super.onCreate()
        startVita()
    }
}
