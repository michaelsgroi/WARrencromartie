package com.michaelsgroi.baseballreference

import com.michaelsgroi.warrencromartie.War
import org.junit.jupiter.api.Test

class BrReportsTest {

    private val testee = BrReports(War())

    @Test
    fun run() {
        testee.run()
    }
}