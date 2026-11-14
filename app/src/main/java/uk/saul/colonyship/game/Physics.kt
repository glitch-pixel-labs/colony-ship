package uk.saul.colonyship.game

import kotlin.math.*

/**
 * Special-relativistic transit model for a fusion/ramscoop ship.
 *
 * Each hop is: boost at constant proper acceleration to the cruise speed, coast, then brake at the
 * same acceleration. For a constant proper acceleration a reaching rapidity φ (β = tanh φ):
 *   ship (proper) time  τ = (c/a)·φ
 *   Earth time          t = (c/a)·sinh φ
 *   distance            x = (c²/a)·(cosh φ − 1)
 * The coast phase covers the remaining distance at β, taking t = d/β Earth-years and τ = t/γ ship-years.
 * Units: light years and years, so c = 1 ly/yr and 1 g ≈ 1.032 ly/yr².
 */
object Physics {
    const val LAUNCH_YEAR = 2187
    const val G_LY_YR2 = 1.032          // 9.81 m/s² expressed in ly/yr²
    const val ACCEL_G = 0.5             // boost/brake at half a gravity
    const val DAYS_PER_YEAR = 365.25

    data class Transit(val distanceLy: Double, val beta: Double, val shipYears: Double, val earthYears: Double) {
        val gamma get() = 1.0 / sqrt(1.0 - beta * beta)
        val shipDays get() = (shipYears * DAYS_PER_YEAR).roundToInt()
        val earthDays get() = earthYears * DAYS_PER_YEAR
    }

    fun gamma(beta: Double) = 1.0 / sqrt(1.0 - beta * beta)

    fun transit(distanceLy: Double, cruiseBeta: Double, accelG: Double = ACCEL_G): Transit {
        val a = accelG * G_LY_YR2
        val phiMax = atanh(cruiseBeta.coerceIn(0.01, 0.97))
        // Distance needed to reach cruise speed; if the hop is too short, peak at the midpoint instead.
        val xAcc = (cosh(phiMax) - 1.0) / a
        val phi = if (2 * xAcc <= distanceLy) phiMax else acosh(1.0 + a * distanceLy / 2.0)
        val beta = tanh(phi)
        val xBoth = 2 * (cosh(phi) - 1.0) / a
        val coast = (distanceLy - xBoth).coerceAtLeast(0.0)
        val tEarth = 2 * sinh(phi) / a + coast / beta
        val tShip = 2 * phi / a + coast / beta / gamma(beta)
        return Transit(distanceLy, beta, tShip, tEarth)
    }

    fun earthYear(s: GameState): Int = LAUNCH_YEAR + floor(s.earthDays / DAYS_PER_YEAR).toInt()
    fun shipYears(s: GameState): Double = s.day / DAYS_PER_YEAR
    fun earthYears(s: GameState): Double = s.earthDays / DAYS_PER_YEAR
    fun distanceFromSol(s: GameState): Double = sqrt(s.posX * s.posX + s.posY * s.posY + s.posZ * s.posZ)
    /** Year a radio message sent now would reach Earth. */
    fun messageArrives(s: GameState): Int = earthYear(s) + ceil(distanceFromSol(s)).toInt()

    fun fmtLy(d: Double) = if (d < 10) "%.1f".format(d) else "%.1f".format(d)
    fun shipClock(s: GameState): String {
        val y = floor(shipYears(s)).toInt(); val d = (s.day - y * DAYS_PER_YEAR).roundToInt().coerceAtLeast(0)
        return "YR $y · DAY $d"
    }

    /** Move the ship [d] light years mostly outward from Sol, with some sideways wander. */
    fun advance(s: GameState, d: Double, r: kotlin.random.Random) {
        val len = distanceFromSol(s)
        val (ox, oy, oz) = if (len < 0.01) Triple(0.6, 0.64, 0.48) else Triple(s.posX / len, s.posY / len, s.posZ / len)
        var dx = ox + (r.nextDouble() - 0.5) * 0.9
        var dy = oy + (r.nextDouble() - 0.5) * 0.9
        var dz = oz + (r.nextDouble() - 0.5) * 0.9
        val n = sqrt(dx * dx + dy * dy + dz * dz)
        dx /= n; dy /= n; dz /= n
        s.posX += dx * d; s.posY += dy * d; s.posZ += dz * d
    }
}
