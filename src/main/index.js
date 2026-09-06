import { app, shell, BrowserWindow, ipcMain } from 'electron'
import { join } from 'path'
import { electronApp, optimizer, is } from '@electron-toolkit/utils'
import icon from '../../resources/icon.png?asset'

// 设置终端输出编码为 UTF-8
process.env.LANG = 'zh_CN.UTF-8'
if (process.platform === 'win32') {
  // Windows 平台特殊处理
  try {
    const { spawnSync } = require('child_process')
    spawnSync('chcp', ['65001'], { stdio: 'ignore' })
  } catch (e) {
    console.log('设置编码失败:', e)
  }
}
import {
  setMainWindow,
  registerChangeWindowSize,
  registerSaveUserInfo,
  registerOpenMeetingWindow,
  registerLogout,
  registerMessageHandlers,
  registerWebRtcSignaling,
  registerGetServerConfig,
  registerServerTest,
  registerApiProxy,
  registerFileHandlers,
  registerSettingsHandlers
} from './ipc'
import { isTrayExists } from './tray'
import { getData } from './store'

// 读取软件设置（主进程侧镜像，由渲染进程 settings-update 同步而来；未同步时用默认值兜底）
const readSettings = () => {
  const defaults = { minimizeToTray: true, closeToTray: true, desktopNotification: true, soundNotification: true }
  try {
    const raw = getData('softwareSettings')
    return raw ? { ...defaults, ...JSON.parse(raw) } : { ...defaults }
  } catch (_) {
    return { ...defaults }
  }
}

function createWindow() {
  const mainWindow = new BrowserWindow({
    width: 380,
    height: 520,
    minWidth: 320,
    minHeight: 400,
    resizable: true,
    show: false,
    title: '虫聊',
    autoHideMenuBar: true,
    icon,
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: false,
      webSecurity: false
    }
  })

  setMainWindow(mainWindow)

  // 按软件设置：最小化/关闭窗口时隐藏到托盘（而非退出应用）
  mainWindow.on('minimize', (e) => {
    const cfg = readSettings()
    if (cfg.minimizeToTray && isTrayExists()) {
      e.preventDefault()
      mainWindow.hide()
    }
  })
  mainWindow.on('close', (e) => {
    const cfg = readSettings()
    if (cfg.closeToTray && isTrayExists()) {
      e.preventDefault()
      mainWindow.hide()
    }
  })

  mainWindow.on('ready-to-show', () => {
    mainWindow.show()
    if (is.dev) {
      mainWindow.webContents.openDevTools({ mode: 'detach' })
    }
  })

  mainWindow.webContents.setWindowOpenHandler((details) => {
    shell.openExternal(details.url)
    return { action: 'deny' }
  })

  if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
    mainWindow.loadURL(process.env['ELECTRON_RENDERER_URL'])
  } else {
    mainWindow.loadFile(join(__dirname, '../renderer/index.html'))
  }
}

app.whenReady().then(() => {
  electronApp.setAppUserModelId('com.wormchat.app')

  app.on('browser-window-created', (_, window) => {
    optimizer.watchWindowShortcuts(window)
  })

  ipcMain.on('ping', () => console.log('pong'))

  createWindow()

  registerChangeWindowSize()
  registerSaveUserInfo()
  registerOpenMeetingWindow()
  registerLogout()
  registerMessageHandlers()
  registerWebRtcSignaling()
  registerGetServerConfig()
  registerServerTest()
  registerApiProxy()
  registerFileHandlers()
  registerSettingsHandlers()

  app.on('activate', function () {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})
