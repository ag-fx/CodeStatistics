package com.airsaid.codestatistics.view

import com.airsaid.codestatistics.constant.Messages
import tornadofx.FX.Companion.messages
import tornadofx.View
import tornadofx.borderpane
import tornadofx.get
import tornadofx.paddingAll

/**
 * @author airsaid
 */
class MainView : View(messages[Messages.APPLICATION_NAME]) {

  private val requiredView: RequiredView by inject()

  override val root = borderpane {
    paddingAll = 10

    left = requiredView.root
  }

}