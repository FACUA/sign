package ui.util.extensions

import javafx.scene.Node

/**
 * Makes a node not occupy space on the scene when it is not visible.
 */
fun Node.enableTrueHidden() = this
	.managedProperty()
	.bind(this.visibleProperty())