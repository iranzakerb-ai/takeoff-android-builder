package ai.takeoff.insightscompanion

/** Explicit nullable ByteArray helper used by the OCR fallback gate. */
internal fun ByteArray?.isNullOrEmpty(): Boolean = this == null || this.isEmpty()
