package com.aimaps.app.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraMathTest {

    @Test
    fun `resolution at zoom zero on the equator matches the Web Mercator constant`() {
        // The canonical figure for a 256 px tile grid.
        assertEquals(156_543.03, CameraMath.metresPerPixel(latitudeDegrees = 0.0, zoom = 0.0), 0.01)
    }

    @Test
    fun `each zoom level halves the ground distance per pixel`() {
        val atZoomTen = CameraMath.metresPerPixel(latitudeDegrees = 0.0, zoom = 10.0)
        val atZoomEleven = CameraMath.metresPerPixel(latitudeDegrees = 0.0, zoom = 11.0)

        assertEquals(atZoomTen / 2.0, atZoomEleven, 1e-9)
    }

    @Test
    fun `resolution shrinks with latitude`() {
        val atEquator = CameraMath.metresPerPixel(latitudeDegrees = 0.0, zoom = 12.0)
        val atSixtyDegrees = CameraMath.metresPerPixel(latitudeDegrees = 60.0, zoom = 12.0)

        // cos(60 degrees) is exactly 0.5.
        assertEquals(atEquator / 2.0, atSixtyDegrees, 1e-6)
    }

    @Test
    fun `the accuracy disc grows as the map zooms in`() {
        val accuracy = 50f
        val latitude = 35.6892

        val atZoomTwelve = CameraMath.accuracyRadiusDp(accuracy, latitude, zoom = 12.0)
        val atZoomSixteen = CameraMath.accuracyRadiusDp(accuracy, latitude, zoom = 16.0)

        assertTrue(
            "expected a larger radius when zoomed in, got $atZoomTwelve then $atZoomSixteen",
            atZoomSixteen > atZoomTwelve,
        )
    }

    @Test
    fun `a precise fix still draws a visible disc`() {
        // Sub-metre accuracy would otherwise round to a radius of nothing.
        val radius = CameraMath.accuracyRadiusDp(
            accuracyMetres = 0.5f,
            latitudeDegrees = 0.0,
            zoom = 3.0,
        )

        assertEquals(14f, radius, 1e-4f)
    }

    @Test
    fun `a hopeless fix cannot paint over the whole viewport`() {
        val radius = CameraMath.accuracyRadiusDp(
            accuracyMetres = 5_000f,
            latitudeDegrees = 0.0,
            zoom = 20.0,
        )

        assertEquals(1_200f, radius, 1e-4f)
    }

    @Test
    fun `a degenerate latitude does not produce a broken radius`() {
        // cos(90 degrees) is effectively zero, so the naive division explodes.
        val radius = CameraMath.accuracyRadiusDp(
            accuracyMetres = 30f,
            latitudeDegrees = 90.0,
            zoom = 15.0,
        )

        assertTrue("radius must stay finite, was $radius", radius.isFinite())
        assertTrue("radius must stay within the clamp, was $radius", radius in 14f..1_200f)
    }
}
