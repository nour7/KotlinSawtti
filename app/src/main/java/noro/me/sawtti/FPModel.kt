package noro.me.sawtti

data class FPModel( val position: Int,
                    val frequency: Int,
                    val magnitude: Float,
                    val time: Double,
                    val band: Int)

data class HashModel( val peak: Int, val peakTime: Double, val nextPeak: Int, val nextPeakTime: Double)