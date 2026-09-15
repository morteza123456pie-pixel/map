package com.aimaps.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoPointTest {

    @Test
    fun `distance to itself is zero`() {
        val point = GeoPoint(35.6892, 51.3890)

        assertEquals(0.0, point.distanceTo(point), 1e-6)
    }

    @Test
    fun `one degree of latitude is about 111 kilometres`() {
        val distance = GeoPoint(0.0, 0.0).distanceTo(GeoPoint(1.0, 0.0))

        // A degree of latitude is constant everywhere: earthRadius * pi / 180.
        assertEquals(111_194.9, distance, 1.0)
    }

    @Test
    fun `distance is symmetric`() {
        val tehran = GeoPoint(35.6892, 51.3890)
        val istanbul = GeoPoint(41.0082, 28.9784)

        assertEquals(tehran.distanceTo(istanbul), istanbul.distanceTo(tehran), 1e-6)
    }

    @Test
    fun `a degree of longitude shrinks towards the poles`() {
        val atEquator = GeoPoint(0.0, 0.0).distanceTo(GeoPoint(0.0, 1.0))
        val atSixtyDegrees = GeoPoint(60.0, 0.0).distanceTo(GeoPoint(60.0, 1.0))

        // cos(60 degrees) is exactly 0.5, so the spacing should halve.
        assertEquals(atEquator / 2.0, atSixtyDegrees, 1.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `the constructor rejects an out of range longitude`() {
        GeoPoint(latitude = 0.0, longitude = 181.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `the constructor rejects an out of range latitude`() {
        GeoPoint(latitude = 91.0, longitude = 0.0)
    }

    @Test
    fun `normalized wraps a longitude past the antimeridian`() {
        // Panning east past 180 is what MapLibre reports, and it must not throw.
        assertEquals(-170.0, GeoPoint.normalized(0.0, 190.0).longitude, 1e-9)
        assertEquals(170.0, GeoPoint.normalized(0.0, -190.0).longitude, 1e-9)
    }

    @Test
    fun `normalized leaves an in range longitude untouched`() {
        assertEquals(180.0, GeoPoint.normalized(0.0, 180.0).longitude, 1e-9)
        assertEquals(-180.0, GeoPoint.normalized(0.0, -180.0).longitude, 1e-9)
        assertEquals(51.3890, GeoPoint.normalized(0.0, 51.3890).longitude, 1e-9)
    }

    @Test
    fun `normalized wraps repeated laps around the globe`() {
        // 540 is one and a half turns east, which lands back on the antimeridian.
        assertEquals(-180.0, GeoPoint.normalized(0.0, 540.0).longitude, 1e-9)
    }

    @Test
    fun `normalized clamps latitude rather than wrapping it`() {
        // Wrapping latitude would silently teleport the camera to the far hemisphere.
        assertEquals(90.0, GeoPoint.normalized(100.0, 0.0).latitude, 1e-9)
        assertEquals(-90.0, GeoPoint.normalized(-100.0, 0.0).latitude, 1e-9)
    }

    @Test
    fun `normalized survives a non finite longitude`() {
        val point = GeoPoint.normalized(0.0, Double.NaN)

        assertEquals(0.0, point.longitude, 1e-9)
    }

    @Test
    fun `the default point is a valid coordinate`() {
        assertTrue(GeoPoint.DEFAULT.latitude in -90.0..90.0)
        assertTrue(GeoPoint.DEFAULT.longitude in -180.0..180.0)
    }
}
