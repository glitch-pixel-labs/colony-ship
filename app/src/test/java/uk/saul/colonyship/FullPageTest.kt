package uk.saul.colonyship

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import uk.saul.colonyship.audio.Cues
import uk.saul.colonyship.data.Store
import uk.saul.colonyship.game.*
import uk.saul.colonyship.ui.*

/** Tall renders so the whole scrolling ship console can be reviewed in one image. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h2000dp-port-420dpi")
class FullPageTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Test fun shipFull() {
        val ctx = rule.activity.applicationContext
        val g = GameController(Store(ctx), Cues(ctx))
        var s = Engine.newGame(3, 99L, 0L)
        repeat(3) { s = Engine.jump(s); if (s.phase == Phase.EVENT) s = Engine.choose(s, 2.coerceAtMost(Events.byId(s.currentEvent!!)!!.available(s).size - 1)); if (s.phase == Phase.OUTCOME) s = Engine.arrive(s) }
        g.debugSet(s); g.screen = Screen.GAME; g.tab = Tab.SHIP
        rule.mainClock.autoAdvance = false
        rule.setContent { ColonyApp(g) }
        rule.mainClock.advanceTimeBy(1200)
        rule.onRoot().captureRoboImage(System.getProperty("user.dir") + "/build/shots/full-ship-console.png")
    }
}
