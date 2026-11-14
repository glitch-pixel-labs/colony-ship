package uk.saul.colonyship

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import uk.saul.colonyship.audio.Cues
import uk.saul.colonyship.data.Store
import uk.saul.colonyship.ui.ColonyApp
import uk.saul.colonyship.ui.GameController

class MainActivity : ComponentActivity() {
    private lateinit var cues: Cues
    private lateinit var game: GameController

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(SystemBarStyle.dark(0xFF140D09.toInt()), SystemBarStyle.dark(0xFF0E0906.toInt()))
        super.onCreate(savedInstanceState)
        cues = Cues(applicationContext)
        game = GameController(Store(applicationContext), cues)
        setContent { ColonyApp(game) }
    }

    override fun onResume() { super.onResume(); cues.startMusic() }
    override fun onPause() { cues.pauseMusic(); super.onPause() }
    override fun onDestroy() { if (isFinishing) cues.release(); super.onDestroy() }
}
