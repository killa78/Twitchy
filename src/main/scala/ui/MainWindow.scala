package org.bone.ircballoon

import org.bone.ircballoon.actor._
import org.bone.ircballoon.actor.message._

import akka.actor._

import org.eclipse.swt.widgets.{List => SWTList, _}
import org.eclipse.swt.layout._
import org.eclipse.swt.events._
import org.eclipse.swt.graphics._
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.custom.StackLayout
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.program.Program

import org.eclipse.swt._
import I18N.i18n._

/**
 *  主視窗
 */
object MainWindow extends SWTHelper
{
  Display.setAppName("Twitchy")

  val display = new Display
  val shell = new Shell(display)

  val menu = createMenu()
  val logginLabel = createLabel(tr("Login Method"))
  val logginTab = createTabFolder()
  val justinSetting = new JustinSetting(logginTab, e => updateConnectButtonState())
  val ircSetting = new IRCSetting(logginTab, e => updateConnectButtonState())
  

  val displayLabel = createLabel(tr("Display Method"))
  val displayTab = createTabFolder(true)

  val blockScroll = new ScrolledComposite(displayTab, SWT.V_SCROLL)
  val ballonScroll = new ScrolledComposite(displayTab, SWT.V_SCROLL)

  val balloonSetting = new BalloonSetting(displayTab, ballonScroll, e => updateConnectButtonState())
  val blockSetting = new BlockSetting(displayTab, blockScroll, e => updateConnectButtonState())
  
  val connectButton = createConnectButton()
  val logTextArea = createLogTextArea()

  lazy val actorSystem = ActorSystem("Twitchy")
  lazy val controller = actorSystem.actorOf(Props[ControllerActor])

  def createOptionMenu(optionHeader: MenuItem) =
  {
    val optionMenu = new Menu(shell, SWT.DROP_DOWN)
    val emoteItem = new MenuItem(optionMenu, SWT.PUSH)
    val avatarItem = new MenuItem(optionMenu, SWT.PUSH)

    emoteItem.setText(tr("Emotes"))
    emoteItem.addSelectionListener { e: SelectionEvent =>
      val emotePreference = new EmoteWindow(shell)
      emotePreference.open()
    }

    avatarItem.setText(tr("Avatar / Nickname"))
    avatarItem.addSelectionListener { e: SelectionEvent =>
      val avatarPreference = new AvatarWindow(shell)
      avatarPreference.open()
    }

    optionMenu
  }

  def createVoteMenu(voteHeader: MenuItem) = 
  {
    val voteMenu = new Menu(shell, SWT.DROP_DOWN)
    val startVoteItem = new MenuItem(voteMenu, SWT.PUSH)

    startVoteItem.setText(tr("Start Vote"))
    startVoteItem.addSelectionListener { e: SelectionEvent =>
      val voteWindow = new VoteWindow(shell)
      voteWindow.open()
    }

    voteMenu
  }

  def createDonateMenu(donateHeader: MenuItem) = 
  {
    val donateMenu = new Menu(shell, SWT.DROP_DOWN)
    val startDonateItem = new MenuItem(donateMenu, SWT.PUSH)

    startDonateItem.setText(tr("Click to Donate!"))
    startDonateItem.addSelectionListener { e: SelectionEvent =>
     Program.launch("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=XS2T7KRBMHPH8")
    }

    donateMenu
  }
  def createMenu() =
  {
    val menuBar = new Menu(shell, SWT.BAR)
    val optionHeader = new MenuItem(menuBar, SWT.CASCADE)
    val voteHeader = new MenuItem(menuBar, SWT.CASCADE)
    val donateHeader = new MenuItem(menuBar, SWT.CASCADE)

    optionHeader.setText(tr("&Preference"))
    voteHeader.setText(tr("&Vote"))
    donateHeader.setText(tr("&Donate!"))

    optionHeader.setMenu(createOptionMenu(optionHeader))
    voteHeader.setMenu(createVoteMenu(voteHeader))
    donateHeader.setMenu(createDonateMenu(donateHeader))

    shell.setMenuBar(menuBar)
    menuBar
  }

  def createLabel(title: String) =
  {
    val label = new Label(shell, SWT.LEFT)
    label.setText(title)
    label
  }

  def createTabFolder(adjustHeight: Boolean = false) = 
  {
    val layoutData = new GridData(SWT.FILL, SWT.FILL, true, adjustHeight)
    val tabFolder = new TabFolder(shell, SWT.NONE)

    if (adjustHeight) {
      layoutData.minimumHeight = 250
    }

    tabFolder.setLayoutData(layoutData)
    tabFolder
  }

  def setTrayIcon()
  {
    val tray = display.getSystemTray()

    if (tray != null) {
      val trayIcon = new TrayItem (tray, SWT.NONE)
      trayIcon.setImage(MyIcon.appIcon)
      trayIcon.addSelectionListener { e: SelectionEvent =>
        controller ! ToggleNotification
      }
    }
  }

  def appendLog(message: String)
  {
    if (display.isDisposed) {
      return
    }

    display.asyncExec(new Runnable() {
      override def run()
      {
        if (!logTextArea.isDisposed) {
          logTextArea.append(message + "\n")
        }
      }
    })
  }

  def createLogTextArea() =
  {
    val layoutData = new GridData(SWT.FILL, SWT.FILL, true, true)
    val text = new Text(shell, SWT.BORDER|SWT.MULTI|SWT.WRAP|SWT.V_SCROLL|SWT.READ_ONLY)
    layoutData.horizontalSpan = 2
    layoutData.minimumHeight = 50

    text.setLayoutData(layoutData)
    text
  }

  def updateConnectButtonState()
  {
    val connectSettingOK = 
      (logginTab.getSelectionIndex == 0 && justinSetting.isSettingOK) ||
      (logginTab.getSelectionIndex == 1 && ircSetting.isSettingOK)

    val displayStettingOK = 
      (displayTab.getSelectionIndex == 0 && blockSetting.isSettingOK) ||
      (displayTab.getSelectionIndex == 1 && blockSetting.isSettingOK)

    connectButton.setEnabled(connectSettingOK && displayStettingOK)
  }

  def getIRCInfo = logginTab.getSelectionIndex match {
    case 0 => justinSetting.getIRCInfo
    case 1 => ircSetting.getIRCInfo
  }

  def createNotificationService() = {
    displayTab.getSelectionIndex match {
      case 0 => balloonSetting.createBalloonController
      case 1 => blockSetting.createNotificationBlock
    }
  }

  def setConnectButtonListener()
  {
    def startBot()
    {
      val connectMessage = tr("Connecting to IRC server, please wait...\n")

      setUIEnabled(false)
      logTextArea.setText(connectMessage)
      controller ! SetNotification(createNotificationService)
      controller ! StartIRCBot(getIRCInfo)
      controller ! SystemNotice(connectMessage)
    }
  
    def stopBot()
    {
      controller ! StopNotification
      controller ! StopIRCBot
      setUIEnabled(true)
    }

    connectButton.addSelectionListener { e: SelectionEvent =>
      connectButton.getSelection match {
        case true => startBot()
        case false => stopBot()
      }
    }
  }

  def displayError(exception: Exception)
  {
    display.syncExec(new Runnable() {
      def outputToLogTextArea()
      {
        logTextArea.append(exception.toString + "\n")
        exception.getStackTrace.foreach { trace =>
          logTextArea.append("\t at " + trace.toString + "\n")
        }
      }

      override def run() {
        val dialog = new MessageBox(MainWindow.shell, SWT.ICON_ERROR)

        outputToLogTextArea()
        dialog.setMessage(tr("Error:") + exception.getMessage)
        dialog.open()
        connectButton.setSelection(!connectButton.getSelection)
        setUIEnabled(true)
      }
    })
  }

  def setUIEnabled(isEnabled: Boolean)
  {
    logginTab.setEnabled(isEnabled)
    displayTab.setEnabled(isEnabled)
    // ircSetting.setUIEnabled(isEnabled)
    justinSetting.setUIEnabled(isEnabled)
    blockSetting.setUIEnabled(isEnabled)
    balloonSetting.setUIEnabled(isEnabled)
  }

  def createConnectButton() =
  {
    val layoutData = new GridData(SWT.FILL, SWT.NONE, true, false)
    val button = new Button(shell, SWT.TOGGLE)

    layoutData.horizontalSpan = 2
    button.setLayoutData(layoutData)
    button.setText(tr("Connect"))
    button.setEnabled(false)
    button
  }

  def setLayout()
  {
    val gridLayout = new GridLayout(1,  false)
    shell.setLayout(gridLayout)
  }

  def main(args: Array[String])
  {   
    setLayout()
    setConnectButtonListener()
    setTrayIcon()
    
    Preference.read(justinSetting)
    Preference.read(ircSetting)
    Preference.read(balloonSetting)
    Preference.read(blockSetting)
    Preference.readEmotes()
    Preference.readAvatars()

    shell.setText(tr("Twitchy"))
    shell.setImage(MyIcon.appIcon)

    shell.pack()
    shell.addShellListener(new ShellAdapter() {
      override def shellClosed(e: ShellEvent) {
        Preference.save(justinSetting)
        Preference.save(ircSetting)
        Preference.save(balloonSetting)
        Preference.save(blockSetting)
        Preference.saveEmotes()
        Preference.saveAvatars()
      }
    })
        
    shell.open()
    logginTab.setFocus()

    while (!shell.isDisposed()) {
      if (!display.readAndDispatch ()) display.sleep ();
    }

    display.dispose()
    sys.exit()
  }
}

