package noro.me.sawtti

import android.content.Context
import android.media.AudioFormat
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.util.fft.FFT
import be.tarsos.dsp.util.fft.HannWindow

import io.reactivex.Observable
import io.reactivex.Observable.*
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.realm.Realm
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.net.URI
import java.net.URL
import kotlin.math.absoluteValue




enum class ProcessingError(val value: String) {
    BufferCreationFailed("BufferCreationFailed"),
    BufferUnloaded("BufferCreationFailed")
}

class ViewModel {

    private val bufferSize = 1024
    private val sampleSize = 8
    private val samplingFrequency = 44100
    private var silenceThreashold: Float = 10.0f
    private val maxNeighbor = 7
    private var sampleCount = 0
    //private var applicationContext: Context?;
    private val realmService = RealmService()
    private var audioEngine: AudioDispatcher? = null
    private var micAudioEngine: AudioDispatcher? = null
    private var audioEvents:ArrayList<AudioEvent> = arrayListOf()

    private var peaks: ArrayList<FPModel> = arrayListOf()
    private val freqPins: Array<Int> = arrayOf(25, 50, 92, 184, 276) //8Kb
    var startNextFingerPrinting = PublishSubject.create<Boolean>()
    var detectionResult = PublishSubject.create<String>()
    var averageMagnitude = PublishSubject.create<Float>()

    /*constructor(context: Context) {
        applicationContext = context
    }
*/
    fun fingerPrint(songUrl: Uri, songName: String, artist: String, cover: String?) {

        silenceThreashold = 100.0f

        try {
            realmService.createSongRecord(songName, artist, cover)
        } catch (error: Throwable) {
            Log.d("SWATTI", "Could not create database record ${error.localizedMessage}")
            return
        }

        try {
            processAudioFile(songUrl)
        } catch (error: Throwable) {

            Log.d("SWATTI", "Could not process file ${error.localizedMessage}")
        }
    }


    private fun processAudioFile(url: Uri) {
        sampleCount = 0

        //launch {
            if (audioEngine != null) {
                audioEngine = null
            }


            audioEngine = AudioDispatcherFactory.fromPipe(url.path, samplingFrequency, bufferSize, 0)

            audioEngine!!.addAudioProcessor(FTTProcess())
        /*
            audioEngine?.addAudioProcessor(object: AudioProcessor{
                var window = HannWindow()
                var fft = FFT(bufferSize, window)
                val fftSize = bufferSize / 2
                var amplitudes = FloatArray(fftSize)
                var time: Double = 0.0
                var audioEvents: ArrayList<AudioEvent> = arrayListOf()

                fun extractPeak() {
                    //fun extractPeaks() {
                     for (audioEvent in audioEvents) {
                         var magz = FloatArray(fftSize)
                         val audioBuffer = audioEvent.floatBuffer
                         fft.forwardTransform(audioBuffer)
                         fft.modulus(audioBuffer, magz)
                         time = audioEvent.timeStamp
                         var magnitudes: Array<Float> = arrayOf(0.0f, 0.0f, 0.0f, 0.0f)
                         var frequencies: Array<Int> = arrayOf(0, 0, 0, 0)
                         val max = freqPins.last() - 1
                         for (i in 0..max) {
                             val mag = magz[i]
                             val freq = i * samplingFrequency / (bufferSize * sampleSize)

                             when (i) {
                                 in freqPins[0] until freqPins[1] ->
                                     if (magnitudes[0] < mag) {
                                         frequencies[0] = freq
                                         magnitudes[0] = mag
                                     }
                                 in freqPins[1] until freqPins[2] ->
                                     if (magnitudes[1] < mag) {
                                         frequencies[1] = freq
                                         magnitudes[1] = mag
                                     }
                                 in freqPins[2] until freqPins[3] ->
                                     if (magnitudes[2] < mag) {
                                         frequencies[2] = freq
                                         magnitudes[2] = mag
                                     }
                                 in freqPins[3] until freqPins[4] ->
                                     if (magnitudes[3] < mag) {
                                         frequencies[3] = freq
                                         magnitudes[3] = mag
                                     }

                             }

                         }

                         val highestMagnitude = magnitudes.sorted().last() ?: 0.0f

                         //if (highestMagnitude > silenceThreashold) {

                         for (x in 0 until freqPins.count()) {
                             val fp = FPModel(sampleCount, frequencies[x], magnitudes[x], time, x + 1)
                             peaks.add(fp)
                         }


                         //}

                     }




                }

                override fun process(audioEvent: AudioEvent): Boolean {
                    Log.d("SWATTI", "process")
                    audioEvents.add(audioEvent)
                    //val audioBuffer = audioEvent.floatBuffer
                    //fft.forwardTransform(audioBuffer)
                    //fft.modulus(audioBuffer, amplitudes)
                    //time = audioEvent.timeStamp
                    //extractPeaks(amplitudes, audioEvent.timeStamp,  amplitudes.size)
                    // for (i in 0 until amplitudes.length) {
                    // Log.d(TAG, String.format("Amplitude at %3d Hz: %8.3f", fft.binToHz(i, sampleRate) as Int, amplitudes[i]))
                    //}

                    return true
                }

                override fun processingFinished() {
                    Log.d("SWATTI", "process done")
                    for (audioEvent in audioEvents) {
                        var magz = FloatArray(fftSize)
                        val audioBuffer = audioEvent.floatBuffer
                        fft.forwardTransform(audioBuffer)
                        fft.modulus(audioBuffer, magz)
                        time = audioEvent.timeStamp
                        var magnitudes: Array<Float> = arrayOf(0.0f, 0.0f, 0.0f, 0.0f)
                        var frequencies: Array<Int> = arrayOf(0, 0, 0, 0)
                        val max = freqPins.last() - 1
                        for (i in 0..max) {
                            val mag = magz[i]
                            val freq = i * samplingFrequency / (bufferSize * sampleSize)

                            when (i) {
                                in freqPins[0] until freqPins[1] ->
                                    if (magnitudes[0] < mag) {
                                        frequencies[0] = freq
                                        magnitudes[0] = mag
                                    }
                                in freqPins[1] until freqPins[2] ->
                                    if (magnitudes[1] < mag) {
                                        frequencies[1] = freq
                                        magnitudes[1] = mag
                                    }
                                in freqPins[2] until freqPins[3] ->
                                    if (magnitudes[2] < mag) {
                                        frequencies[2] = freq
                                        magnitudes[2] = mag
                                    }
                                in freqPins[3] until freqPins[4] ->
                                    if (magnitudes[3] < mag) {
                                        frequencies[3] = freq
                                        magnitudes[3] = mag
                                    }

                            }

                        }

                        val highestMagnitude = magnitudes.sorted().last() ?: 0.0f

                        //if (highestMagnitude > silenceThreashold) {

                        for (x in 0 until freqPins.count()) {
                            val fp = FPModel(sampleCount, frequencies[x], magnitudes[x], time, x + 1)
                            peaks.add(fp)
                        }


                        //}

                    }

                    Log.d("SWATTI", "STOP")
                    //extractPeaks(amplitudes, time,  amplitudes.size)
                    //peakMap()
                    //micAudioEngine?.stop()
                    //audioEngine?.stop()
                    print("done")
                }
            })
            */
            audioEngine!!.run()
        //}

    }


    fun listenToMic() {
        sampleCount = 0
        silenceThreashold = 10.0f


        launch {

            if (micAudioEngine == null) {
                micAudioEngine = AudioDispatcherFactory.fromDefaultMicrophone(samplingFrequency, bufferSize, 0)


            }
            val audioFormat = TarsosDSPAudioFormat(samplingFrequency.toFloat(), sampleSize, 1, true, false )

            //micAudioEngine?.addAudioProcessor(fftAudioProcess)
            micAudioEngine?.run()

        }

    }


    fun extractPeaks(magz: FloatArray, time: Double, size: Int) {
    //fun extractPeaks() {

            var magnitudes: Array<Float> = arrayOf(0.0f, 0.0f, 0.0f, 0.0f)
            var frequencies: Array<Int> = arrayOf(0, 0, 0, 0)
            val max = freqPins.last() - 1
            for (i in 0..max) {
                val mag = magz[i]
                val freq = i * samplingFrequency / (bufferSize * sampleSize)

                when (i) {
                    in freqPins[0] until freqPins[1] ->
                        if (magnitudes[0] < mag) {
                            frequencies[0] = freq
                            magnitudes[0] = mag
                        }
                    in freqPins[1] until freqPins[2] ->
                        if (magnitudes[1] < mag) {
                            frequencies[1] = freq
                            magnitudes[1] = mag
                        }
                    in freqPins[2] until freqPins[3] ->
                        if (magnitudes[2] < mag) {
                            frequencies[2] = freq
                            magnitudes[2] = mag
                        }
                    in freqPins[3] until freqPins[4] ->
                        if (magnitudes[3] < mag) {
                            frequencies[3] = freq
                            magnitudes[3] = mag
                        }

                }

            }

            val highestMagnitude = magnitudes.sorted().last() ?: 0.0f

            //if (highestMagnitude > silenceThreashold) {

            for (x in 0 until freqPins.count()) {
                val fp = FPModel(sampleCount, frequencies[x], magnitudes[x], time, x + 1)
                peaks.add(fp)
            }


            //}

            if (sampleCount % 10 == 0) {
                val totalMagnitude = magz.reduce { acc, fl -> acc + fl }
                val average = totalMagnitude / (magz.count()).toFloat()
                print("average $average")
                //averageMagnitude.onNext(average)
            }



        peakMap()


    }

    fun generateHashes(duplicates: Boolean): ArrayList<Int> {

        var hashes: ArrayList<Int> = arrayListOf()

        if (peaks.count() > maxNeighbor) {
            val max = peaks.count() - maxNeighbor
            for (n in 0 until max) {
                for (x in 1 until maxNeighbor) {
                    val nextId = n + x
                    if (peaks[n].band != peaks[nextId].band && peaks[n].position != peaks[nextId].position) {
                        if (peaks[n].frequency != 0 && peaks[nextId].frequency != 0) {
                            val hashValue = hash(peaks[n].frequency, peaks[n].time, peaks[nextId].frequency, peaks[nextId].time)
                            hashes.add(hashValue)
                        }
                    }
                }
            }
        }

        //print("size before = \(hashes.count)")
        if (!duplicates) {
            return hashes.toSet().toList() as ArrayList<Int>
        }
        return hashes
    }


    fun hash(peak: Int, peakTime: Double, nextPeak: Int, nextPeakTime: Double): Int {
        val timeDiff = (nextPeakTime - peakTime).toInt()
        val freqDiff = Math.abs(nextPeak - peak)
        var pins = arrayOf<Int>(0, 0, 0, 0)
        pins[0] = (Math.abs(((peak + nextPeak).shl(3)) + timeDiff)).toInt()
        pins[1] = (Math.abs(((nextPeak).shl(3) + peak - freqDiff))).toInt()
        val peakHash = ((pins[0] * 1000000) + pins[1]).toInt()
        return peakHash

    }

    fun saveToDatabase() {

        try {
            realmService.addToDatabase(generateHashes(false))
        } catch (error: Throwable) {
            print("database error $error")
        }

        peaks.clear()
        startNextFingerPrinting.onNext(true)


    }

    fun filter(results: ArrayList<FingerPrints>): LinkedHashMap<String, Int> {

        var filteredResults = linkedMapOf<String, Int>()

        if (!results.isEmpty()) {
            for (fingerPrint in results) {
                for (songId in fingerPrint.songs) {
                    val currentValue = filteredResults[songId] ?: 0
                    filteredResults[songId] = currentValue + 1
                }
            }

        }

        return filteredResults
    }


    fun detectSong(): Observable<String> {

        return Observable.create {
            var songName: String = ""
            val qResults = realmService.searchDatabase(generateHashes(true))
            val filteredResults = filter(qResults)

            var reverseResults: LinkedHashMap<Int, String> = linkedMapOf()

            for (fp in filteredResults) {
                reverseResults[fp.value] = fp.key
            }

            val sortedResults  = reverseResults.toSortedMap(compareBy { it.absoluteValue })
            print(sortedResults.keys)

            if (sortedResults.count() > 0) {
                //let correctSong:Results<Song>? = nil
                val songId = sortedResults[sortedResults.firstKey()]

                if (songId != null) {
                val correctSong  = realmService.getSong(songId)


                if (correctSong?.first()?.name == null) {
                    it.onNext("Song not found in the Database?")
                    it.onComplete()
                } else {
                    songName = (correctSong.first())!!.name
                }


                } else {
                    print("song id error")
                }

                if (sortedResults.count() > 1) {
                    var songFirstCount = -1
                    var songSecondCount = -1

                    for (song in sortedResults) {
                        if (songFirstCount == -1) {
                            songFirstCount = song.key
                        }

                        if (songSecondCount == -1) {
                            songSecondCount = song.key
                        }

                        if (songFirstCount != -1 && songSecondCount != -1) {
                            break
                        }
                    }

                    if ((songFirstCount - songSecondCount) < 10) {
                        it.onNext("I guess $songName")
                } else {
                    it.onNext(songName)
                }
                    //val nextSong = realmService.getSong (sortedResults[1]!)
                    //print("next song = \(String(describing: nextSong?.first?.name))")
                } else {
                    it.onNext(songName)
                    print("song found $songName")
                }
            } else {
                it.onNext("No single match in Database?")

            }

            // for testing only ---- check order or detection
            var x = 1
            for (res in sortedResults) {
                print("$x - ${res.value}")
                x += 1
            }
            //---------------------
            it.onComplete()
        }


    }


    fun stop() {
        micAudioEngine?.stop()
        audioEngine?.stop()

        // peakMap()

        detectSong().subscribeOn(Schedulers.computation()).subscribe({
            detectionResult.onNext(it)
        }, {
            print("error")
        }, {
            peaks.clear()
        })



    }







        /*for Visualization peak map only*/
        fun peakMap() {
            var freq: ArrayList<Int> = arrayListOf()
            var time:  ArrayList<Int> = arrayListOf()

            for (fp in peaks) {
                freq.add(fp.frequency)
                time.add(fp.position)

                Log.d("SAWTTI", "freq = ${fp.frequency} , time = ${fp.position};")
            }

            //Log.d("SAWTTI", "freq = $freq;")
            //Log.d("SAWTTI", "freq = $time;")

        }

    inner class FTTProcess:AudioProcessor {

            var window = HannWindow()
            var fft = FFT(bufferSize, window)
            val fftSize = bufferSize / 2
            var amplitudes = FloatArray(fftSize)
            var time: Double = 0.0
            var audioEvents: ArrayList<AudioEvent> = arrayListOf()

        protected fun finalize() {
           Log.d("SAWTTI","goodbye")
        }

            private fun extractPeak() {
                //fun extractPeaks() {
                for (audioEvent in audioEvents) {
                    var magz = FloatArray(fftSize)
                    val audioBuffer = audioEvent.floatBuffer
                    fft.forwardTransform(audioBuffer)
                    fft.modulus(audioBuffer, magz)
                    time = audioEvent.timeStamp
                    var magnitudes: Array<Float> = arrayOf(0.0f, 0.0f, 0.0f, 0.0f)
                    var frequencies: Array<Int> = arrayOf(0, 0, 0, 0)
                    val max = freqPins.last() - 1
                    for (i in 0..max) {
                        val mag = magz[i]
                        val freq = i * samplingFrequency / (bufferSize * sampleSize)

                        when (freq) {
                            in freqPins[0] until freqPins[1] ->
                                if (magnitudes[0] < mag) {
                                    frequencies[0] = freq
                                    magnitudes[0] = mag
                                }
                            in freqPins[1] until freqPins[2] ->
                                if (magnitudes[1] < mag) {
                                    frequencies[1] = freq
                                    magnitudes[1] = mag
                                }
                            in freqPins[2] until freqPins[3] ->
                                if (magnitudes[2] < mag) {
                                    frequencies[2] = freq
                                    magnitudes[2] = mag
                                }
                            in freqPins[3] until freqPins[4] ->
                                if (magnitudes[3] < mag) {
                                    frequencies[3] = freq
                                    magnitudes[3] = mag
                                }

                        }

                    }

                    val highestMagnitude = magnitudes.sorted().last() ?: 0.0f

                    //if (highestMagnitude > silenceThreashold) {

                    for (x in 0 until freqPins.size-1) {
                        Log.d("SAWTTI", "${frequencies[x]}")
                        val fp = FPModel(sampleCount, frequencies[x], magnitudes[x], time, x + 1)
                        peaks.add(fp)
                    }
                    Log.d("SAWTTI", "GOOOOOOO")

                    //}

                }

                peakMap()




            }


            override fun process(audioEvent: AudioEvent): Boolean {
                Log.d("SWATTI", "process")
                audioEvents.add(audioEvent)
                val audioBuffer = audioEvent.floatBuffer
                fft.forwardTransform(audioBuffer)
                fft.modulus(audioBuffer, amplitudes)
                //time = audioEvent.timeStamp
                //extractPeaks(amplitudes, audioEvent.timeStamp,  amplitudes.size)
                 for (i in 0 until amplitudes.size) {
                 //Log.d("SAWTTI", String.format("Amplitude at %3d Hz: %8.3f", fft.binToHz(i, sampleSize.toFloat()) as Int, amplitudes[i]))
                }

                return true
            }

            override fun processingFinished() {
                Log.d("SWATTI", "process done")
                extractPeak()
                Log.d("SWATTI", "STOP")

            }
        }


    }


