package org.mjdev.libs.barcodescanner.base

import android.os.Process
import com.google.zxing.*
import java.util.*

/**
 * Decoder for zxing
 * It decode any picture from camera on the fly
 */
class Decoder(
    private var stateListener: StateListener,
    formats: List<BarcodeFormat?>,
    private var callback: CodeScanner.DecodeCallback?
) {
    private var reader: MultiFormatReader = MultiFormatReader()
    private var decoderThread: DecoderThread
    private var hints: Map<DecodeHintType, Any>
    private var taskLock = Any()
    private var task: DecodeTask? = null
    private var state: State

    init {
        decoderThread = DecoderThread()
        hints = EnumMap(DecodeHintType::class.java)
        (hints as EnumMap<DecodeHintType, Any>)[DecodeHintType.POSSIBLE_FORMATS] = formats
        reader.setHints(hints)
        state = State.INITIALIZED
    }

    /**
     * Formats to lookup for
     * You can choose one or more formats to scan at once
     * @see: BarcodeFormat
     */
    fun setFormats(formats: List<BarcodeFormat?>) {
        (hints as EnumMap<DecodeHintType, Any>)[DecodeHintType.POSSIBLE_FORMATS] = formats
        reader.setHints(hints)
    }

    /**
     * Set decoder callback
     * callback to handle decoder results
     */
    fun setCallback(callback: CodeScanner.DecodeCallback?) {
        this.callback = callback
    }

    /**
     * Main decode routine
     */
    fun decode(task: DecodeTask) {
        synchronized(taskLock) {
            if (state != State.STOPPED) {
                this.task = task
                taskLock.notify()
            }
        }
    }

    /**
     * Start decode
     */
    fun start() {
        check(state == State.INITIALIZED) { "Illegal decoder state" }
        decoderThread.start()
    }

    /**
     * stop decoding
     */
    fun shutdown() {
        decoderThread.interrupt()
        task = null
    }

    /**
     * Return current decoder state
     */
    fun getState(): State {
        return state
    }

    /**
     * Set current decoder state
     * @param state a state to set to decoder
     * @return true if success
     */
    fun setState(state: State): Boolean {
        this.state = state
        return stateListener.onStateChanged(state)
    }

    /**
     * Complicated thread to go through all task needed
     * Rewrote from java, should be optimised
     */
    inner class DecoderThread : Thread("cs-decoder") {
        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            synchronized(taskLock) {
                mainLoop@ while (true) {
                    setState(Decoder.State.IDLE)
                    var result: Result? = null
                    try {
                        var pTask: DecodeTask
                        loop2@ while (true) {
                            val t: DecodeTask? = task
                            if (t != null) {
                                task = null
                                pTask = t
                                break@loop2
                            }
                            try {
                                taskLock.wait()
                            } catch (e: InterruptedException) {
                                setState(Decoder.State.STOPPED)
                                break@mainLoop
                            }
                        }
                        setState(Decoder.State.DECODING)
                        result = pTask.decode(reader)
                    } catch (ignored: ReaderException) {
                    } finally {
                        if (result != null) {
                            task = null
                            if (setState(Decoder.State.DECODED)) {
                                callback?.onDecoded(result)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Decoder state listener
     */
    interface StateListener {
        fun onStateChanged(state: State): Boolean
    }

    /**
     * Decoder states
     */
    enum class State {
        INITIALIZED,
        IDLE,
        DECODING,
        DECODED,
        STOPPED
    }

    companion object {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "NOTHING_TO_INLINE")
        internal inline fun Any.wait() = (this as Object).wait()

        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "NOTHING_TO_INLINE")
        internal inline fun Any.notify() = (this as Object).notify()
    }
}
