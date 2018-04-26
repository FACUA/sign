package ui

import tornadofx.App
import ui.view.MainView

class FacuaSign : App(MainView::class) {
	companion object {
		fun launch(args: Array<String>) = tornadofx.launch<FacuaSign>(args)
	}
}
