package org.bone.ircballoon

import org.bone.ircballoon.model.IRCInfo

import org.eclipse.swt.widgets.{List => SWTList, _}
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.layout._
import org.eclipse.swt.events._
import org.eclipse.swt.graphics._
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.custom.StackLayout
import org.eclipse.swt.program.Program

import org.eclipse.swt._
import I18N.i18n._

class JustinSetting(parent: TabFolder, onModify: ModifyEvent => Any) extends 
       Composite(parent, SWT.NONE) with SWTHelper
{
  val tabItem = new TabItem(parent, SWT.NONE)
  val gridLayout = new GridLayout(2,  false)
  val loginGroup = createGroup(this, tr("Twitch Login Settings"))
  val username = createText(loginGroup, tr("Username:"))
  val password = createText(loginGroup, tr("oAuth Key:"), SWT.PASSWORD)
  val (link) = createUrlBar(this)
  val (onJoinButton, onLeaveButton) = createJoinLeaveButton(this)
 
  def getIRCInfo: IRCInfo = {
    val hostname = "irc.twitch.tv" format(username.getText)
    val password = Some(this.password.getText.trim)
    val channel = "#%s" format(username.getText.toLowerCase)

    IRCInfo(
      hostname, 6667, username.getText, channel, password, 
      onJoinButton.getSelection, onLeaveButton.getSelection
    )
  }

  def isSettingOK = {
    val username = this.username.getText.trim
    val password = this.password.getText.trim

    username.length > 0 && password.length > 0
  }

  def setModifyListener()
  {
    username.addModifyListener(onModify)
    password.addModifyListener(onModify)
  }

  def setUIEnabled(isEnabled: Boolean)
  {
    this.username.setEnabled(isEnabled)
    this.password.setEnabled(isEnabled)
  }
  
  def setLinkListener()
  {
    link.addSelectionListener { e: SelectionEvent =>
    Program.launch("http://twitchapps.com/tmi/")
    }
  }

  this.setLayout(gridLayout)
  this.setModifyListener()
  this.setLinkListener()
  this.tabItem.setText("Twitch")
  this.tabItem.setControl(this)
  
}

