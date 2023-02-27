package com.michaelsgroi.baseballreference

import org.junit.jupiter.api.Test

class BrReportsTest {

    private val testee = BrReports(BrWarDaily())
    @Test
    fun run() {
        testee.run()
    }
}