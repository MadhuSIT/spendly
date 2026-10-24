package com.madhusit.spendly.domain.sms

/**
 * Minimal, offset-preserving WordPiece tokenizer compatible with the
 * `google/mobilebert-uncased` BERT tokenizer used to train the on-device SMS
 * extraction model. Pure Kotlin, no native deps.
 *
 * Replicates HuggingFace BertTokenizer (do_lower_case=true):
 *  1. Basic tokenization: whitespace split, then split punctuation into its own
 *     tokens, lower-casing for vocab lookup while keeping original char offsets.
 *  2. WordPiece: greedy longest-match sub-wording with "##" continuation.
 *
 * Char offsets map every produced sub-token back to its span in the ORIGINAL
 * text so BIO slot predictions can be turned back into exact substrings. SMS are
 * effectively ASCII; accent stripping is intentionally omitted (would misalign
 * offsets and is irrelevant for Indian bank SMS).
 */
class WordPieceTokenizer(
    private val vocab: Map<String, Int>,
    private val maxLength: Int = 96,
    private val unkToken: String = "[UNK]",
    private val clsToken: String = "[CLS]",
    private val sepToken: String = "[SEP]",
    private val padToken: String = "[PAD]",
    private val maxInputCharsPerWord: Int = 100,
) {
    data class Encoding(
        val inputIds: LongArray,
        val attentionMask: LongArray,
        /** (start,end) char span in the original text for each position; (0,0) for specials/pad. */
        val offsets: Array<IntArray>,
    )

    private val unkId = vocab.getValue(unkToken)
    private val clsId = vocab.getValue(clsToken)
    private val sepId = vocab.getValue(sepToken)
    private val padId = vocab[padToken] ?: 0

    /** Split text into (token, start, end) pieces: whitespace + punctuation boundaries. */
    private fun basicTokenize(text: String): List<Triple<String, Int, Int>> {
        val pieces = ArrayList<Triple<String, Int, Int>>()
        var i = 0
        val n = text.length
        while (i < n) {
            val c = text[i]
            if (c.isWhitespace()) { i++; continue }
            if (!c.isLetterOrDigit()) {
                // punctuation / symbol → its own token
                pieces.add(Triple(c.lowercaseChar().toString(), i, i + 1))
                i++
                continue
            }
            // run of letters/digits
            val start = i
            while (i < n && text[i].isLetterOrDigit()) i++
            pieces.add(Triple(text.substring(start, i).lowercase(), start, i))
        }
        return pieces
    }

    /** Greedy WordPiece over a single basic token, preserving char offsets. */
    private fun wordpiece(token: String, start: Int, out: MutableList<Triple<Int, Int, Int>>) {
        if (token.length > maxInputCharsPerWord) {
            out.add(Triple(unkId, start, start + token.length)); return
        }
        var begin = 0
        val subTokens = ArrayList<Triple<Int, Int, Int>>()
        while (begin < token.length) {
            var end = token.length
            var curId: Int? = null
            var curEnd = begin
            while (begin < end) {
                val sub = (if (begin > 0) "##" else "") + token.substring(begin, end)
                val id = vocab[sub]
                if (id != null) { curId = id; curEnd = end; break }
                end--
            }
            if (curId == null) { // unmatched → whole word is UNK
                out.add(Triple(unkId, start, start + token.length)); return
            }
            subTokens.add(Triple(curId, start + begin, start + curEnd))
            begin = curEnd
        }
        out.addAll(subTokens)
    }

    fun encode(text: String): Encoding {
        val wordSubTokens = ArrayList<Triple<Int, Int, Int>>() // id, charStart, charEnd
        for ((tok, s, _) in basicTokenize(text)) {
            wordpiece(tok, s, wordSubTokens)
            if (wordSubTokens.size >= maxLength - 2) break
        }
        val body = if (wordSubTokens.size > maxLength - 2) wordSubTokens.subList(0, maxLength - 2) else wordSubTokens

        val ids = LongArray(maxLength) { padId.toLong() }
        val mask = LongArray(maxLength) { 0L }
        val offsets = Array(maxLength) { intArrayOf(0, 0) }

        var p = 0
        ids[p] = clsId.toLong(); mask[p] = 1L; offsets[p] = intArrayOf(0, 0); p++
        for ((id, cs, ce) in body) {
            ids[p] = id.toLong(); mask[p] = 1L; offsets[p] = intArrayOf(cs, ce); p++
        }
        ids[p] = sepId.toLong(); mask[p] = 1L; offsets[p] = intArrayOf(0, 0); p++
        return Encoding(ids, mask, offsets)
    }
}
