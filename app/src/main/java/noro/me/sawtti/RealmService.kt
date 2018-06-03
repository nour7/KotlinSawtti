package noro.me.sawtti

import android.os.AsyncTask.execute
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import io.realm.kotlin.createObject
import java.io.IOError
import java.util.UUID.randomUUID



enum class DbErrors(val value: String) {
    RealmInstanceIsNill("RealmInstanceIsNill"),
    SongRecrodIsNotCreated("SongRecrodIsNotCreated"),
    FingerPrintWriteOperationFailed("FingerPrintWriteOperationFailed")
}


class RealmService  {
    var songId: String? = null
    var duplicates = 0
    val config = RealmConfiguration.Builder()

    init {
        config.assetFile("fingerprints.realm")
        config.readOnly()

    }

    fun createSongRecord(name: String, artist:String, cover: String?)
    {
        //let realm = try? Realm(configuration: config)

        //val realm = Realm.getInstance(config.build())
        val realm = Realm.getDefaultInstance()

        var song = Song()
        song.name = name
        song.artist = artist
        song.cover = cover


        realm.executeTransaction {
            realm.insert(song)
            songId = song.id
            Realm.Transaction.OnSuccess{
                print("success")
            }
            Realm.Transaction.OnError {
                throw Exception(DbErrors.SongRecrodIsNotCreated.value)
            }
        }


    }





    fun searchDatabase(hashes: ArrayList<Int>): ArrayList<FingerPrints> {


        val realm = Realm.getInstance(config.build())
        var results: ArrayList<FingerPrints> = arrayListOf<FingerPrints>()

            for (hash in hashes) {
                val fp = realm.where(FingerPrints::class.java).equalTo("id", hash).findFirst()

                fp?.let { results.add(it) }

            }

            return results
        }



    fun addToDatabase(hashes: ArrayList<Int>) {

        val realm = Realm.getDefaultInstance()
        if (songId == null) throw Exception(DbErrors.SongRecrodIsNotCreated.value)

            for (hash in hashes) {
                val fingerPrint = realm.where(FingerPrints::class.java).equalTo("id",hash).findFirst()
                if (fingerPrint == null) {
                    var fp = FingerPrints()
                    fp.id = hash
                    val currentSongId = songId ?: ""
                    fp.songs.add(currentSongId)

                    realm.executeTransaction {
                        realm.insert(fp)
                    }

                    }else {
                        duplicates += 1
                        realm.beginTransaction()
                        fingerPrint.songs.add(songId ?: "")
                        realm.commitTransaction()
                    }
                }

            print("duplicates = $duplicates")
            duplicates = 0
        }

    fun getSong(id: String): RealmResults<Song>? {
        val realm = Realm.getInstance(config.build())
        return realm.where(Song::class.java).equalTo("id", id).findAll()

    }

}
