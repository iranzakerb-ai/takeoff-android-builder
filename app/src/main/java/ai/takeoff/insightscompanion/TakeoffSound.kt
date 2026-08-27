package ai.takeoff.insightscompanion

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object TakeoffSound {
    enum class Cue { CAPTURE, SUCCESS, REMINDER, WARNING, ERROR, SYNC }
    fun play(cue: Cue) { thread(name = "takeoff-sonic-$cue", isDaemon = true) { runCatching { render(cue) } } }
    private fun render(cue: Cue) { val sampleRate = 44_100; val duration = when (cue) { Cue.CAPTURE -> .18; Cue.SUCCESS -> .42; Cue.REMINDER -> .55; Cue.WARNING -> .38; Cue.ERROR -> .34; Cue.SYNC -> .46 }; val total = (sampleRate * duration).toInt(); val pcm = ShortArray(total); val notes = when (cue) { Cue.CAPTURE -> listOf(880.0 to 0.00, 1320.0 to .055); Cue.SUCCESS -> listOf(659.25 to 0.00, 987.77 to .085, 1318.51 to .17); Cue.REMINDER -> listOf(523.25 to 0.00, 783.99 to .12, 1046.50 to .28); Cue.WARNING -> listOf(587.33 to 0.00, 493.88 to .12); Cue.ERROR -> listOf(440.00 to 0.00, 349.23 to .09, 293.66 to .18); Cue.SYNC -> listOf(698.46 to 0.00, 1046.50 to .09, 1396.91 to .21) }; for (i in 0 until total) { val t = i.toDouble() / sampleRate; var v = 0.0; for ((freq, start) in notes) { if (t < start) continue; val local = t - start; val attack = (local / .012).coerceIn(0.0, 1.0); val decay = exp(-local * 9.5); val fundamental = sin(2.0 * PI * freq * local); val air = .16 * sin(2.0 * PI * freq * 2.01 * local); v += (fundamental + air) * attack * decay }; if (t < .028) v += .10 * sin(2.0 * PI * 2400.0 * t) * exp(-t * 90.0); pcm[i] = (v.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * .42).toInt().toShort() }; val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(); val format = AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(); val track = AudioTrack.Builder().setAudioAttributes(attrs).setAudioFormat(format).setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(pcm.size * 2).build(); try { track.write(pcm, 0, pcm.size); track.play(); Thread.sleep((duration * 1000).toLong() + 80) } finally { runCatching { track.stop() }; track.release() } }
}
