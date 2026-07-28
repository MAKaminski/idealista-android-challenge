package dev.mkaminski.idealista.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdLinksTest {

    @Test
    fun `listing url is built from the property code`() {
        assertEquals("https://www.idealista.com/inmueble/3/", AdLinks.listingUrl("3"))
    }

    @Test
    fun `map uri uses a geo scheme so the device's map app handles it`() {
        val uri = AdLinks.mapUri(40.4362687, -3.6833686, "calle de Lagasca")

        assertTrue(uri.startsWith("geo:40.4362687,-3.6833686"))
        assertTrue(uri.contains("calle%20de%20Lagasca"))
    }

    @Test
    fun `an ad without coordinates still gets a usable link`() {
        val uri = AdLinks.mapUri(null, null, "calle de Lagasca")

        assertTrue(uri.startsWith("https://"))
        assertTrue(uri.contains("calle%20de%20Lagasca"))
    }

    @Test
    fun `a label with a comma does not break the geo query`() {
        val uri = AdLinks.mapUri(1.0, 2.0, "calle de Lagasca, Madrid")

        assertTrue(uri.contains("%2C"))
    }
}
