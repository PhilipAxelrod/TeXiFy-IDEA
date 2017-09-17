package nl.rubensten.texifyidea.util

import nl.rubensten.texifyidea.psi.*

/**
 * Get the token type (including `@`) of the BibTeX entry (e.g. `@article`).
 */
fun BibtexEntry.tokenType(): String? = type.text.toLowerCase()

/**
 * Get the token type (excluding `@`) of the BibTeX entry (e.g. `article`).
 */
fun BibtexEntry.tokenName(): String? = tokenType()?.substring(1)

/**
 * Get the identifier/label of the BibTeX entry (e.g. `someAuthor:23b`).
 */
fun BibtexEntry.identifier(): String? = firstChildOfType(BibtexId::class)?.text?.substringEnd(1)

/**
 * Get all the tags in the entry.
 */
fun BibtexEntry.tags(): Collection<BibtexTag> = childrenOfType(BibtexTag::class)

/**
 * Get all the key objects in the entry.
 */
fun BibtexEntry.keys(): Collection<BibtexKey> = childrenOfType(BibtexKey::class)

/**
 * Get all the names of all entry's keys.
 */
fun BibtexEntry.keyNames(): Collection<String> = keys().map { it.text }

/**
 * Checks if the entry is a @string.
 */
fun BibtexEntry.isString() = tokenName()?.toLowerCase() == "string"

/**
 * Checks if the entry is a @preamble.
 */
fun BibtexEntry.isPreamble() = tokenName()?.toLowerCase() == "preamble"

/**
 * Get the key of the BibTeX tag.
 */
fun BibtexTag.key(): BibtexKey? = firstChildOfType(BibtexKey::class)

/**
 * Get the key of the BibTeX tag in string form.
 */
fun BibtexTag.keyName(): String? = key()?.text

/**
 * Get the content of the BibTeX tag.
 */
fun BibtexTag.content(): BibtexContent? = firstChildOfType(BibtexContent::class)

/**
 * Evaluates the string contents and returns a nicely concatenated version.
 *
 * E.g. `{Bambi} # space # "Broodje"` with `@string{space=" "}` will become `Bambi Broodje`.
 */
fun BibtexContent.evaluate(): String {
    val result = StringBuilder()

    for (string in childrenOfType(BibtexString::class)) {
        val braced = string.bracedString
        val quoted = string.quotedString
        val defined = string.definedString

        if (braced != null) {
            result.append(braced.evaluate())
        }
        else if (quoted != null) {
            result.append(quoted.evaluate())
        }
        else if (defined != null) {
            result.append(defined.evaluate())
        }
    }

    if (result.isEmpty()) {
        result.append(text)
    }

    return result.toString()
}

/**
 * Returns the value of the defined string.
 *
 * When no value could be found, `$IDENTIFIER` is returned where `IDENTIFIER` is the name of the (un)defined string.
 *
 * E.g. `@string{ test = "Hello"}` and defined string `test` becomes `Hello`.
 */
fun BibtexDefinedString.evaluate(): String {
    val file = containingFile
    val stringName = text

    if (file == null) {
        return ""
    }

    // Look up all string entries.
    for (entry in file.childrenOfType(BibtexEntry::class)) {
        val token = entry.tokenName()?.toLowerCase()
        if (token != "string") {
            continue
        }

        val tags = entry.tags()
        if (tags.isEmpty()) {
            continue
        }

        val tag = tags.first()
        val key = tag.keyName() ?: continue
        if (key != stringName) {
            continue
        }

        // Prevent perpetual recursion.
        if (key == tag.content.text) {
            continue
        }

        return tag.content.evaluate()
    }

    return "${'$'}$stringName${'$'}"
}

/**
 * Returns the contents of the braced string.
 *
 * E.g. `{{T}est $\alpha$ {H}ello}` becomes `{T}est $\alpha$ {H}ello`.
 */
fun BibtexBracedString.evaluate(): String {
    val text = text
    return text.substring(1 until text.length - 1)
}

/**
 * Returns the contents of the quoted string.
 *
 * E.g. `"Test Hello"` becomes `Test Hello`.
 */
fun BibtexQuotedString.evaluate(): String {
    val text = text
    return text.substring(1 until text.length - 1)
}