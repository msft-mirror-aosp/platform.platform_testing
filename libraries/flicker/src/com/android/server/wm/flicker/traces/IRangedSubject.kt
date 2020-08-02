package com.android.server.wm.flicker.traces

interface IRangedSubject<Entry> {
    /**
     * Run the assertions for all entries.
     */
    fun forAllEntries()

    /**
     * Run the assertions for entries within the specified time range.
     */
    fun forRange(startTime: Long, endTime: Long)  { throw UnsupportedOperationException() }

    /**
     * Run the assertions only in the first entry.
     */
    fun inTheBeginning() { throw UnsupportedOperationException() }

    /**
     * Run the assertions only in the last entry.
     */
    fun atTheEnd() { throw UnsupportedOperationException() }
}