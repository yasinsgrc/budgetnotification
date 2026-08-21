package com.bildirimbutce.parser

/**
 * Minimal JSON okuyucu.
 *
 * Neden kendi implementasyonumuz: bu modulun SIFIR bagimliligi olmasini
 * istiyoruz. Boylece parser hem Android'de hem duz JVM'de, hicbir kutuphane
 * indirmeden derlenir ve test edilir. Okudugumuz tek dosya patterns.json;
 * tam bir JSON kutuphanesine ihtiyac yok.
 *
 * Desteklenen: nesne, dizi, string (\" \\ \/ \b \f \n \r \t \uXXXX),
 * sayi, true/false/null.
 */
sealed interface JsonValue {
    data class Obj(val fields: Map<String, JsonValue>) : JsonValue
    data class Arr(val items: List<JsonValue>) : JsonValue
    data class Str(val value: String) : JsonValue
    data class Num(val value: Double) : JsonValue
    data class Bool(val value: Boolean) : JsonValue
    data object Null : JsonValue
}

class JsonParseException(message: String, val position: Int) :
    Exception("$message (konum $position)")

object Json {

    fun parse(input: String): JsonValue {
        val reader = Reader(input)
        reader.skipWhitespace()
        val value = reader.readValue()
        reader.skipWhitespace()
        if (!reader.atEnd()) reader.fail("Beklenmeyen fazladan icerik")
        return value
    }

    private class Reader(private val s: String) {
        private var i = 0

        fun atEnd(): Boolean = i >= s.length

        fun fail(message: String): Nothing = throw JsonParseException(message, i)

        fun skipWhitespace() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        fun readValue(): JsonValue {
            if (atEnd()) fail("Beklenmedik dosya sonu")
            return when (s[i]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> JsonValue.Str(readString())
                't', 'f' -> readBoolean()
                'n' -> readNull()
                else -> readNumber()
            }
        }

        private fun readObject(): JsonValue.Obj {
            expect('{')
            val fields = LinkedHashMap<String, JsonValue>()
            skipWhitespace()
            if (peek() == '}') { i++; return JsonValue.Obj(fields) }
            while (true) {
                skipWhitespace()
                if (peek() != '"') fail("Nesne anahtari string olmali")
                val key = readString()
                skipWhitespace()
                expect(':')
                skipWhitespace()
                fields[key] = readValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> i++
                    '}' -> { i++; return JsonValue.Obj(fields) }
                    else -> fail("Nesnede ',' veya '}' bekleniyordu")
                }
            }
        }

        private fun readArray(): JsonValue.Arr {
            expect('[')
            val items = ArrayList<JsonValue>()
            skipWhitespace()
            if (peek() == ']') { i++; return JsonValue.Arr(items) }
            while (true) {
                skipWhitespace()
                items += readValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> i++
                    ']' -> { i++; return JsonValue.Arr(items) }
                    else -> fail("Dizide ',' veya ']' bekleniyordu")
                }
            }
        }

        private fun readString(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                if (atEnd()) fail("Kapanmamis string")
                when (val c = s[i++]) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (atEnd()) fail("Yarim kacis dizisi")
                        when (val e = s[i++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 > s.length) fail("Eksik \\u kacisi")
                                val hex = s.substring(i, i + 4)
                                val code = hex.toIntOrNull(16) ?: fail("Gecersiz \\u kacisi: $hex")
                                sb.append(code.toChar())
                                i += 4
                            }
                            else -> fail("Bilinmeyen kacis dizisi: \\$e")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun readNumber(): JsonValue.Num {
            val start = i
            if (peek() == '-') i++
            while (!atEnd() && (s[i].isDigit() || s[i] in ".eE+-")) i++
            val text = s.substring(start, i)
            val value = text.toDoubleOrNull() ?: run { i = start; fail("Gecersiz sayi: $text") }
            return JsonValue.Num(value)
        }

        private fun readBoolean(): JsonValue.Bool = when {
            s.startsWith("true", i) -> { i += 4; JsonValue.Bool(true) }
            s.startsWith("false", i) -> { i += 5; JsonValue.Bool(false) }
            else -> fail("Gecersiz deger")
        }

        private fun readNull(): JsonValue {
            if (!s.startsWith("null", i)) fail("Gecersiz deger")
            i += 4
            return JsonValue.Null
        }

        private fun peek(): Char = if (atEnd()) fail("Beklenmedik dosya sonu") else s[i]

        private fun expect(c: Char) {
            if (atEnd() || s[i] != c) fail("'$c' bekleniyordu")
            i++
        }
    }
}

// --- Okumayi kolaylastiran yardimcilar ---

internal fun JsonValue.obj(): Map<String, JsonValue> =
    (this as? JsonValue.Obj)?.fields ?: emptyMap()

internal fun JsonValue?.string(default: String = ""): String =
    (this as? JsonValue.Str)?.value ?: default

internal fun JsonValue?.int(default: Int = 0): Int =
    (this as? JsonValue.Num)?.value?.toInt() ?: default

internal fun JsonValue?.list(): List<JsonValue> =
    (this as? JsonValue.Arr)?.items ?: emptyList()

internal fun JsonValue?.stringList(): List<String> =
    this.list().mapNotNull { (it as? JsonValue.Str)?.value }

internal fun JsonValue?.stringMap(): Map<String, String> =
    (this as? JsonValue.Obj)?.fields
        ?.mapNotNull { (k, v) -> (v as? JsonValue.Str)?.let { k to it.value } }
        ?.toMap()
        ?: emptyMap()
