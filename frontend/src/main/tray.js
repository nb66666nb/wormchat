import { Tray, Menu, nativeImage, nativeTheme, app, BrowserWindow, screen } from 'electron'
import { join } from 'path'
import { is } from '@electron-toolkit/utils'

let tray = null
let currentUserInfo = null
let adminWindow = null
let themeListener = null

// 托盘图标随任务栏主题切换：浅色任务栏→黑线，深色任务栏→白线
const trayIcon = () => {
  const dark = nativeTheme.shouldUseDarkColors
  const name = dark ? 'icon-tray-dark.png' : 'icon-tray-light.png'
  return nativeImage.createFromPath(join(__dirname, '../../resources', name))
}

const createTray = () => {
  tray = new Tray(trayIcon())
  tray.setToolTip('虫聊')
  updateTrayMenu()

  themeListener = () => {
    if (tray && !tray.isDestroyed()) tray.setImage(trayIcon())
  }
  nativeTheme.on('updated', themeListener)

  return tray
}

const createAdminWindow = () => {
  if (adminWindow && !adminWindow.isDestroyed()) {
    adminWindow.show()
    adminWindow.focus()
    return
  }

  const { width: screenWidth, height: screenHeight } = screen.getPrimaryDisplay().workAreaSize
  const windowWidth = 1100
  const windowHeight = 720
  const x = Math.round((screenWidth - windowWidth) / 2)
  const y = Math.round((screenHeight - windowHeight) / 2)

  adminWindow = new BrowserWindow({
    width: windowWidth,
    height: windowHeight,
    minWidth: 900,
    minHeight: 600,
    x,
    y,
    resizable: true,
    show: false,
    autoHideMenuBar: true,
    title: '管理员面板',
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: false
    }
  })

  adminWindow.on('ready-to-show', () => {
    adminWindow.show()
  })

  adminWindow.on('closed', () => {
    adminWindow = null
  })

  if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
    adminWindow.loadURL(`${process.env['ELECTRON_RENDERER_URL']}#/admin-panel`)
  } else {
    adminWindow.loadFile(join(__dirname, '../renderer/index.html'), {
      hash: '/admin-panel'
    })
  }
}

const updateTrayMenu = () => {
  if (!tray) return

  const userName = currentUserInfo?.userName || '未登录'
  const isAdmin = currentUserInfo?.admin ? '是' : '否'

  const menuItems = [
    { label: `用户名: ${userName}`, enabled: false },
    { label: `管理员: ${isAdmin}`, enabled: false },
    { type: 'separator' },
    {
      label: '显示主窗口',
      click: () => {
        const main = BrowserWindow.getAllWindows().find(w =>
          !w.isDestroyed() &&
          !w.webContents.getURL().includes('/meeting-room') &&
          w.getTitle() !== '通话进行中')
        if (main) { main.show(); main.focus() }
      }
    },
    { type: 'separator' }
  ]

  if (currentUserInfo?.admin) {
    menuItems.push({
      label: '管理员面板',
      click: () => {
        createAdminWindow()
      }
    })
    menuItems.push({ type: 'separator' })
  }

  menuItems.push({
    label: '退出登录',
    click: () => {
      currentUserInfo = null
      if (tray) {
        tray.destroy()
        tray = null
      }
      app.exit()
    }
  })

  const contextMenu = Menu.buildFromTemplate(menuItems)
  tray.setContextMenu(contextMenu)
}

const setUserInfo = (userInfo) => {
  currentUserInfo = userInfo
  if (!tray) {
    createTray()
  }
  updateTrayMenu()
}

const destroyTray = () => {
  if (themeListener) {
    nativeTheme.off('updated', themeListener)
    themeListener = null
  }
  if (tray) {
    tray.destroy()
    tray = null
  }
}

const isTrayExists = () => !!tray && !tray.isDestroyed()

export { createTray, setUserInfo, destroyTray, isTrayExists }