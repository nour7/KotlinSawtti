package noro.me.sawtti

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*


open class Song: RealmObject()  {
    @PrimaryKey
    var id:String = UUID.randomUUID().toString()
    var name:String = ""
    var artist:String = ""
    var cover:String? = null
}

open class FingerPrints: RealmObject() {
    @PrimaryKey
    var id:Int = 0
    var songs = RealmList<String>()
}
