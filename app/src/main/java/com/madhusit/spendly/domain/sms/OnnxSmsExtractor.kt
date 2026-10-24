package com.madhusit.spendly.domain.sms

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.nio.LongBuffer

/**
 * On-device SMS understanding using the quantized MobileBERT intent + BIO-slot
 * model exported by tools/sms-ml/extraction/train_export.py.
 *
 * ADVISORY ONLY. It extracts a predicted message type and field spans. It never
 * writes to the ledger, resolves accounts, or deduplicates — those remain the
 * deterministic repository's responsibility. If anything fails to load, callers
 * fall back to the regex parsers.
 */
class OnnxSmsExtractor private constructor(
    private val env: OrtEnvironment,
    private val session: OrtSession,
    private val tokenizer: WordPieceTokenizer,
    private val intents: List<String>,
    private val tags: List<String>,
    val modelVersion: String,
) {
    data class Result(val intent: String, val slots: Map<String, String>)

    fun extract(text: String): Result? {
        return try {
            val enc = tokenizer.encode(text)
            val shape = longArrayOf(1, enc.inputIds.size.toLong())
            val ids = OnnxTensor.createTensor(env, LongBuffer.wrap(enc.inputIds), shape)
            val mask = OnnxTensor.createTensor(env, LongBuffer.wrap(enc.attentionMask), shape)
            val out = session.run(mapOf("input_ids" to ids, "attention_mask" to mask))
            try {
                @Suppress("UNCHECKED_CAST")
                val intentLogits = (out[0].value as Array<FloatArray>)[0]
                @Suppress("UNCHECKED_CAST")
                val tokenLogits = (out[1].value as Array<Array<FloatArray>>)[0]
                val intent = intents[argmax(intentLogits)]
                val tagIds = IntArray(tokenLogits.size) { argmax(tokenLogits[it]) }
                val spans = decodeSpans(tagIds, enc.offsets, text)
                val allowed = allowedFieldsFor(intent)
                val slots = spans.filterKeys { it in allowed }
                Result(intent, slots)
            } finally {
                out.close(); ids.close(); mask.close()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "ONNX extraction failed: ${t.message}")
            null
        }
    }

    private fun argmax(a: FloatArray): Int {
        var bi = 0; var bv = a[0]
        for (i in 1 until a.size) if (a[i] > bv) { bv = a[i]; bi = i }
        return bi
    }

    /** Port of train_export.predicted_spans: merge B-/I- runs into exact substrings. */
    private fun decodeSpans(tagIds: IntArray, offsets: Array<IntArray>, text: String): Map<String, String> {
        val spans = LinkedHashMap<String, String>()
        var field: String? = null; var start = -1; var end = -1
        fun flush() {
            if (field != null && start in 0..end && end <= text.length) {
                // first occurrence wins; keep existing if already present
                spans.putIfAbsent(field!!, text.substring(start, end))
            }
        }
        for (i in tagIds.indices) {
            val (s, e) = offsets[i][0] to offsets[i][1]
            if (s == e) continue // special / pad
            val tag = tags[tagIds[i]]
            if (tag.startsWith("B-")) {
                flush(); field = tag.substring(2); start = s; end = e
            } else if (tag.startsWith("I-") && field == tag.substring(2) &&
                (end == s || (end < s && text.substring(end, s).isBlank()))
            ) {
                end = e
            } else {
                flush(); field = null; start = -1; end = -1
            }
        }
        flush()
        return spans
    }

    /** Port of train_export.allowed_fields_for_intent. */
    private fun allowedFieldsFor(intent: String): Set<String> = when (intent) {
        "TRANSACTION" -> ALL_FIELDS
        "BALANCE" -> setOf("AMOUNT", "ACCOUNT_LAST4", "DATE")
        "FAILED" -> setOf("AMOUNT", "DIRECTION")
        "MARKETING" -> setOf("AMOUNT")
        "REMINDER" -> setOf("AMOUNT", "DATE")
        "SECURITY_ALERT" -> setOf("AMOUNT")
        "AMBIGUOUS" -> setOf("AMOUNT")
        else -> emptySet() // OTP, OTHER
    }

    companion object {
        private const val TAG = "Spendly.Onnx"
        private const val ASSET_DIR = "sms-extractor"
        private val ALL_FIELDS = setOf("AMOUNT", "DIRECTION", "ACCOUNT_LAST4", "MERCHANT", "DATE", "REFERENCE")

        /** Build from app assets; returns null if the model/vocab/labels are missing or invalid. */
        fun fromAssets(context: Context): OnnxSmsExtractor? = try {
            val am = context.assets
            val modelBytes = am.open("$ASSET_DIR/model-int8.onnx").use { it.readBytes() }
            val vocab = HashMap<String, Int>()
            am.open("$ASSET_DIR/vocab.txt").bufferedReader().useLines { lines ->
                var i = 0
                for (line in lines) { vocab[line.trim()] = i; i++ }
            }
            val labelsJson = am.open("$ASSET_DIR/labels.json").bufferedReader().use { it.readText() }
            val obj = JSONObject(labelsJson)
            val intents = obj.getJSONArray("intents").let { a -> List(a.length()) { a.getString(it) } }
            val tags = obj.getJSONArray("tags").let { a -> List(a.length()) { a.getString(it) } }
            val version = obj.optString("base_model", "mobilebert-int8")

            val env = OrtEnvironment.getEnvironment()
            val opts = OrtSession.SessionOptions().apply { setIntraOpNumThreads(1) }
            val session = env.createSession(modelBytes, opts)
            val tokenizer = WordPieceTokenizer(vocab, maxLength = 96)
            OnnxSmsExtractor(env, session, tokenizer, intents, tags, "mobilebert-int8:$version")
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to load ONNX SMS extractor: ${t.message}")
            null
        }
    }
}
