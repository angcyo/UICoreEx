# wifi p2p

2022-8-8

WLAN 直连（对等连接或 P2P）概览 

https://developer.android.com/guide/topics/connectivity/wifip2p?hl=zh-cn

使用 WLAN 直连 (P2P) 技术，可以让具备相应硬件的 Android 4.0（API 级别 14）或更高版本设备在没有中间接入点的情况下，通过 WLAN 进行直接互联。使用这些 API，您可以实现支持 WLAN P2P 的设备间相互发现和连接，从而获得比蓝牙连接更远距离的高速连接通信效果。对于多人游戏或照片共享等需要在用户之间共享数据的应用而言，这一技术非常有用。

WLAN P2P API 包含以下主要部分：

支持您发现、请求，以及连接到对等设备的方法（在 WifiP2pManager 类中定义）。

支持您获知 WifiP2pManager 方法调用成功与否的侦听器。调用 WifiP2pManager 方法时，每个方法均可收到作为参数传入的特定侦听器。

通知您 WLAN P2P 框架检测到的特定事件（例如连接断开或新发现对等设备）的 Intent。

通常，您可以一起使用 API 的这三个主要组件。例如，您可以为针对 discoverPeers() 的调用提供 WifiP2pManager.ActionListener，这样您便可以通过 ActionListener.onSuccess() 和 ActionListener.onFailure() 方法来接收通知。如果 discoverPeers() 方法发现对等设备列表已经更改，则还将广播 WIFI_P2P_PEERS_CHANGED_ACTION Intent。

API 概览
WifiP2pManager 类提供的方法使您可以在设备上与 WLAN 硬件交互，以执行发现和连接对等设备等操作。可执行的操作如下：

表 1. WLAN P2P 方法

方法	说明
initialize()	通过 WLAN 框架注册应用。必须先调用此方法，然后再调用任何其他 WLAN P2P 方法。
connect()	启动与具有指定配置的设备的对等连接。
cancelConnect()	取消任何正在进行的对等群组协商。
requestConnectInfo()	请求设备连接信息。
createGroup()	以群组所有者的身份，使用当前设备创建对等群组。
removeGroup()	移除当前对等群组。
requestGroupInfo()	请求对等群组信息。
discoverPeers()	启动对等设备发现
requestPeers()	请求已发现对等设备的当前列表。
WifiP2pManager 方法使您可以在侦听器中进行传递，以便 WLAN P2P 框架可以向您的 Activity 通知通话状态。下表介绍可用的侦听器接口和使用侦听器的相应 WifiP2pManager 方法调用：

表 2. WLAN P2P 侦听器

侦听器接口	相关操作
WifiP2pManager.ActionListener	connect()、cancelConnect()、createGroup()、removeGroup() 和 discoverPeers()
WifiP2pManager.ChannelListener	initialize()
WifiP2pManager.ConnectionInfoListener	requestConnectInfo()
WifiP2pManager.GroupInfoListener	requestGroupInfo()
WifiP2pManager.PeerListListener	requestPeers()
WLAN P2P API 定义当发生特定 WLAN P2P 事件时会广播的 Intent，例如发现新的对等设备时，或设备的 WLAN 状态更改时。您可以通过创建处理这些 Intent 的广播接收器，在应用中注册接收这些 Intent：

表 3. WLAN P2P Intent

Intent	说明
WIFI_P2P_CONNECTION_CHANGED_ACTION	当设备的 WLAN 连接状态更改时广播。
WIFI_P2P_PEERS_CHANGED_ACTION	当您调用 discoverPeers() 时广播。如果您在应用中处理此 Intent，则通常需要调用 requestPeers() 以获取对等设备的更新列表。
WIFI_P2P_STATE_CHANGED_ACTION	当 WLAN P2P 在设备上启用或停用时广播。
WIFI_P2P_THIS_DEVICE_CHANGED_ACTION	当设备的详细信息（例如设备名称）更改时广播。
为 WLAN P2P Intent 创建广播接收器
广播接收器允许您通过 Android 系统接收 Intent 广播，以便您的应用对您感兴趣的事件作出响应。创建广播接收器以处理 WLAN P2P Intent 的基本步骤如下：

创建扩展 BroadcastReceiver 类的类。对于类的构造函数，您很可能希望具备 WifiP2pManager、WifiP2pManager.Channel，以及此广播接收器将在其中注册的 Activity 的参数。这使广播接收器可以向 Activity 发送更新，访问 WLAN 硬件并获得通信通道（如果需要）。

在广播接收器中，查看您感兴趣的 Intent onReceive()。根据接收到的 Intent，执行任何必要操作。例如，如果广播接收器接收到 WIFI_P2P_PEERS_CHANGED_ACTION Intent，则您可以调用 requestPeers() 方法，以获得当前所发现对等设备的列表。

以下代码展示如何创建典型的广播接收器。广播接收器以 WifiP2pManager 对象和 Activity 作为参数，并在接收到 Intent 时，使用这两个类恰当地执行所需操作：

Kotlin
Java

/**
* A BroadcastReceiver that notifies of important Wi-Fi p2p events.
  */
  class WiFiDirectBroadcastReceiver(
  private val manager: WifiP2pManager,
  private val channel: WifiP2pManager.Channel,
  private val activity: MyWifiActivity
  ) : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
  val action: String = intent.action
  when (action) {
  WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
  // Check to see if Wi-Fi is enabled and notify appropriate activity
  }
  WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
  // Call WifiP2pManager.requestPeers() to get a list of current peers
  }
  WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
  // Respond to new connection or disconnections
  }
  WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
  // Respond to this device's wifi state changing
  }
  }
  }
  }
  通过 Android Q，以下广播 Intent 已从粘性变为非粘性：

WIFI_P2P_CONNECTION_CHANGED_ACTION
应用可使用 requestConnectionInfo()、requestNetworkInfo() 或 requestGroupInfo() 来检索当前连接信息。
WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
应用可使用 requestDeviceInfo() 来检索当前连接信息。
创建 WLAN P2P 应用
创建 WLAN P2P 应用涉及为应用创建并注册广播接收器、发现对等设备，连接到对等设备，以及将数据传输到对等设备。以下部分将介绍如何完成此操作。

初始设置
在使用 WLAN P2P API 之前，您必须确保您的应用可以访问硬件，并且设备支持 WLAN P2P API 协议。如果设备支持 WLAN P2P，您可以获得 WifiP2pManager 的实例，创建并注册广播接收器，然后开始使用 WLAN P2P API。

请求在设备上使用 WLAN 硬件的权限，同时声明您的应用在 Android 清单中具有正确的最低 SDK 版本：


<uses-sdk android:minSdkVersion="14" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
除上述权限以外，您还需要启用位置信息模式才能使用下列 API：

discoverPeers
discoverServices
requestPeers
检查 WLAN P2P 是否开启并受支持。您可以在广播接收器收到 WIFI_P2P_STATE_CHANGED_ACTION Intent 时，在接收器中检查此项。向您的 Activity 通知 WLAN P2P 的状态，并作出相应回应：

Kotlin
Java

override fun onReceive(context: Context, intent: Intent) {
...
val action: String = intent.action
when (action) {
WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
when (state) {
WifiP2pManager.WIFI_P2P_STATE_ENABLED -> {
// Wifi P2P is enabled
}
else -> {
// Wi-Fi P2P is not enabled
}
}
}
}
...
}
在 Activity 的 onCreate() 方法中，获取 WifiP2pManager 的实例，并通过调用 initialize()，在 WLAN P2P 框架中注册您的应用。此方法会返回 WifiP2pManager.Channel，用于将您的应用连接到 WLAN P2P 框架。此外，您还应该通过 WifiP2pManager 和 WifiP2pManager.Channel 对象以及对 Activity 的引用，创建广播接收器实例。这样广播接收器便可通知 Activity 感兴趣的事件并进行相应更新。此外，您还可以操纵设备的 WLAN 状态（如有必要）：

Kotlin
Java

val manager: WifiP2pManager? by lazy(LazyThreadSafetyMode.NONE) {
getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
}

var mChannel: WifiP2pManager.Channel? = null
var receiver: BroadcastReceiver? = null

override fun onCreate(savedInstanceState: Bundle?) {
...

    mChannel = manager?.initialize(this, mainLooper, null)
    mChannel?.also { channel ->
        receiver = WiFiDirectBroadcastReceiver(manager, channel, this)
    }

}
创建 Intent 过滤器，然后添加与广播接收器检查内容相同的 Intent：

Kotlin
Java

val intentFilter = IntentFilter().apply {
addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
}
在 Activity 的 onResume() 方法中注册广播接收器，然后在 Activity 的onPause() 方法中取消注册该接收器：

Kotlin
Java

/* register the broadcast receiver with the intent values to be matched */
override fun onResume() {
super.onResume()
mReceiver?.also { receiver ->
registerReceiver(receiver, intentFilter)
}
}

/* unregister the broadcast receiver */
override fun onPause() {
super.onPause()
mReceiver?.also { receiver ->
unregisterReceiver(receiver)
}
}
获取 WifiP2pManager.Channel 并设置广播接收器后，应用便可调用 WLAN P2P 方法并收到 WLAN P2P Intent。

现在，您可以实现应用，然后通过调用 WifiP2pManager 中的方法，使用 WLAN P2P 功能。下一部分介绍如何执行常见操作，例如发现和连接到对等设备。

发现对等设备
如要发现可连接的对等设备，请调用 discoverPeers()，以检测范围内的可用对等设备。对此功能的调用为异步操作，如果您已创建 WifiP2pManager.ActionListener，则系统会通过 onSuccess() 和 onFailure() 告知应用成功与否。onSuccess() 方法仅会通知您发现进程已成功，但不会提供有关其发现的实际对等设备（如有）的任何信息：

Kotlin
Java

manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {

    override fun onSuccess() {
        ...
    }

    override fun onFailure(reasonCode: Int) {
        ...
    }
})
如果发现进程成功并检测到对等设备，则系统会广播 WIFI_P2P_PEERS_CHANGED_ACTION Intent，您可以在广播接收器中侦听该 Intent，以获取对等设备列表。当应用接收到 WIFI_P2P_PEERS_CHANGED_ACTION Intent 时，您可以通过 requestPeers() 请求已发现对等设备的列表。以下代码展示如何完成此项设置：

Kotlin
Java

override fun onReceive(context: Context, intent: Intent) {
val action: String = intent.action
when (action) {
...
WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
manager?.requestPeers(channel) { peers: WifiP2pDeviceList? ->
// Handle peers list
}
}
...
}
}
requestPeers() 方法也为异步操作，并可在对等设备列表可用时通过 onPeersAvailable()（定义见 WifiP2pManager.PeerListListener 接口）通知您的 Activity。onPeersAvailable() 方法为您提供 WifiP2pDeviceList，您可对其进行迭代以查找希望连接的对等设备。

连接到对等设备
获取可能对等设备的列表，且已确定您要连接的设备后，调用connect() 方法即可连接到相应设备。调用此方法需要使用 WifiP2pConfig 对象，其中包含要连接的设备的信息。您可以通过 WifiP2pManager.ActionListener 获知连接是否成功。以下代码展示如何创建与所需设备的连接：

Kotlin
Java

val device: WifiP2pDevice = ...
val config = WifiP2pConfig()
config.deviceAddress = device.deviceAddress
mChannel?.also { channel ->
manager?.connect(channel, config, object : WifiP2pManager.ActionListener {

        override fun onSuccess() {
            //success logic
        }

        override fun onFailure(reason: Int) {
            //failure logic
        }
}
})
传输数据
建立连接后，您可以通过套接字在设备之间传输数据。数据传输的基本步骤如下：

创建 ServerSocket。此套接字会在指定端口等待来自客户端的连接，然后加以屏蔽直到连接发生，因此请在后台线程中也执行此操作。

创建客户端 Socket。客户端使用 IP 地址和服务器套接字端口连接到服务器设备。

将数据从客户端发送到服务器。客户端套接字成功连接到服务器套接字后，您可以通过字节流将数据从客户端发送到服务器。

服务器套接字等待客户端连接（通过 accept() 方法）。在客户端连接前，此调用会屏蔽连接，所以这是另一个线程。发生连接时，服务器设备可接收到客户端数据。对这些数据执行任何操作，例如将其保存到文件中，或向用户显示这些数据。

以下示例（修改自 WLAN P2P 演示示例）展示如何创建此客户端-服务器套接字通信，以及如何通过服务将 JPEG 图像从客户端传输到服务器。如需完整工作示例，请编译并运行 WLAN P2P 演示示例。

Kotlin
Java

class FileServerAsyncTask(
private val context: Context,
private var statusText: TextView
) : AsyncTask<Void, Void, String?>() {

    override fun doInBackground(vararg params: Void): String? {
        /**
         * Create a server socket.
         */
        val serverSocket = ServerSocket(8888)
        return serverSocket.use {
            /**
             * Wait for client connections. This call blocks until a
             * connection is accepted from a client.
             */
            val client = serverSocket.accept()
            /**
             * If this code is reached, a client has connected and transferred data
             * Save the input stream from the client as a JPEG file
             */
            val f = File(Environment.getExternalStorageDirectory().absolutePath +
                    "/${context.packageName}/wifip2pshared-${System.currentTimeMillis()}.jpg")
            val dirs = File(f.parent)

            dirs.takeIf { it.doesNotExist() }?.apply {
                mkdirs()
            }
            f.createNewFile()
            val inputstream = client.getInputStream()
            copyFile(inputstream, FileOutputStream(f))
            serverSocket.close()
            f.absolutePath
        }
    }

    private fun File.doesNotExist(): Boolean = !exists()

    /**
     * Start activity that can handle the JPEG image
     */
    override fun onPostExecute(result: String?) {
        result?.run {
            statusText.text = "File copied - $result"
            val intent = Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse("file://$result"), "image/*")
            }
            context.startActivity(intent)
        }
    }
}
在客户端上，通过客户端套接字连接到服务器套接字，然后传输数据。本示例传输的是客户端设备文件系统中的 JPEG 文件。

Kotlin
Java

val context = applicationContext
val host: String
val port: Int
val len: Int
val socket = Socket()
val buf = ByteArray(1024)
...
try {
/**
* Create a client socket with the host,
* port, and timeout information.
*/
socket.bind(null)
socket.connect((InetSocketAddress(host, port)), 500)

    /**
     * Create a byte stream from a JPEG file and pipe it to the output stream
     * of the socket. This data is retrieved by the server device.
     */
    val outputStream = socket.getOutputStream()
    val cr = context.contentResolver
    val inputStream: InputStream = cr.openInputStream(Uri.parse("path/to/picture.jpg"))
    while (inputStream.read(buf).also { len = it } != -1) {
        outputStream.write(buf, 0, len)
    }
    outputStream.close()
    inputStream.close()
} catch (e: FileNotFoundException) {
//catch logic
} catch (e: IOException) {
//catch logic
} finally {
/**
* Clean up any open sockets when done
* transferring or if an exception occurred.
*/
socket.takeIf { it.isConnected }?.apply {
close()
}
}




